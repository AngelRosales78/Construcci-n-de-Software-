"""Web-only evidence assistant. Existing Android analysis contracts remain unchanged."""
import re
import unicodedata
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import serializers
from rest_framework.exceptions import PermissionDenied
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.throttling import UserRateThrottle
from rest_framework.views import APIView
from .access_views import subscription
from .assistant_facts import company_pack, comparison_pack, changes_pack, overview_cards, answer_for, LIMIT
from .assistant_lessons import LESSONS, lesson, examples_for
from .assistant_provider import plan_answer, validate_plan, TOPICS, VERSION
from .models import Company
from .assistant_context import outside_context
from .monitoring_services import usage_owner, BudgetExceeded, record_error


class AssistantThrottle(UserRateThrottle):
    scope='evidence_assistant'
    rate='60/min'


class AssistantAIThrottle(UserRateThrottle):
    scope='evidence_assistant_ai'
    rate='12/min'


class StrictInput(serializers.Serializer):
    def to_internal_value(self,data):
        if not isinstance(data,dict) or set(data)-set(self.fields):
            raise serializers.ValidationError({'non_field_errors':['Se recibieron campos no admitidos.']})
        if any(isinstance(data.get(k),bool) for k in ('fiscal_year','from_year','to_year','answer')):
            raise serializers.ValidationError({'non_field_errors':['Los años y la respuesta deben ser números enteros.']})
        return super().to_internal_value(data)


class HistoryInput(StrictInput):
    question=serializers.CharField(max_length=600)
    topics=serializers.ListField(child=serializers.ChoiceField(choices=TOPICS),max_length=3,min_length=1)


class AssistantInput(StrictInput):
    fiscal_year=serializers.IntegerField(min_value=1900,required=False,allow_null=True)
    action=serializers.ChoiceField(choices=('summary','question','changes','learn'),default='summary')
    question=serializers.CharField(max_length=600,required=False)
    history=HistoryInput(many=True,required=False,default=list,max_length=4)
    level=serializers.ChoiceField(choices=('simple','technical'),default='simple')
    from_year=serializers.IntegerField(min_value=1900,required=False)
    to_year=serializers.IntegerField(min_value=1900,required=False)
    topic=serializers.ChoiceField(choices=tuple(LESSONS),required=False)
    answer=serializers.IntegerField(min_value=0,max_value=1,required=False)
    a=serializers.RegexField(r'^[A-Z0-9.\-]{1,10}$',required=False)
    b=serializers.RegexField(r'^[A-Z0-9.\-]{1,10}$',required=False)

    def validate(self,attrs):
        for key in ('fiscal_year','from_year','to_year'):
            if attrs.get(key) and attrs[key]>timezone.localdate().year:
                raise serializers.ValidationError({key:'No se admiten ejercicios futuros.'})
        if attrs['action']=='question' and not attrs.get('question'):
            raise serializers.ValidationError({'question':'Escribe una pregunta.'})
        if attrs['action']=='learn' and not attrs.get('topic'):
            raise serializers.ValidationError({'topic':'Elige una lección.'})
        if attrs['action']=='changes' and (not attrs.get('from_year') or not attrs.get('to_year') or attrs['from_year']>=attrs['to_year']):
            raise serializers.ValidationError({'from_year':'Selecciona un ejercicio anterior y otro posterior distintos.'})
        return attrs


def presentation(pack):
    return pack | {'schema_version':VERSION,'overview':overview_cards(pack),
        'suggestions':(['¿Qué diferencias hay entre las dos empresas?','¿Qué datos impiden compararlas?','¿Qué significan estas notas?'] if pack['scope']=='comparison' else
           ['¿Qué destaca y qué merece atención?','¿Cómo están sus deudas?','¿Qué significa su Altman-Z?','¿Qué información falta?']),
        'lessons':[{'id':key,'title':item['title']} for key,item in LESSONS.items()],
        'disclaimer':LIMIT,'generated_at':timezone.now().isoformat(),'generation_mode':'calculated_evidence',
        'validation_scope':'Cifras calculadas por el servidor y explicaciones acotadas a sus referencias. No certifica fuentes ni acredita causas.'}


class CompanyAssistantView(APIView):
    permission_classes=(IsAuthenticated,)
    throttle_classes=(AssistantThrottle,)

    def get_throttles(self):
        data=self.request.data if self.request.method=='POST' else {}
        if self.request.method=='POST' and isinstance(data,dict) and data.get('action','summary') in ('summary','question'):
            return [AssistantAIThrottle()]
        return [AssistantThrottle()]

    def company(self,ticker):
        return get_object_or_404(Company.objects.select_related('financial_metrics','latest_price'),ticker=ticker.upper())

    def get(self,request,ticker):
        return Response(presentation(company_pack(self.company(ticker),request.query_params.get('fiscal_year'))),headers={'Cache-Control':'private, no-store'})

    def post(self,request,ticker):
        serializer=AssistantInput(data=request.data);serializer.is_valid(raise_exception=True)
        args=serializer.validated_data
        if args.get('a') or args.get('b'):
            raise serializers.ValidationError('El contexto de esta conversación es la empresa abierta.')
        company=self.company(ticker)
        if args['action']=='learn':
            return Response({'schema_version':VERSION,'scope':'lesson','tickers':[company.ticker],
                'fiscal_year':args.get('fiscal_year'),'lesson':lesson(args['topic'],args.get('answer')),
                'generation_mode':'reviewed_educational_content'},headers={'Cache-Control':'private, no-store'})
        if args['action']=='changes':
            if args.get('fiscal_year') and args['to_year']!=args['fiscal_year']:
                raise serializers.ValidationError({'to_year':'El ejercicio posterior debe ser el año seleccionado en la ficha.'})
            result=presentation(changes_pack(company,args['from_year'],args['to_year']))
            result['answer']=answer_for(result,['changes'])
            return Response(result,headers={'Cache-Control':'private, no-store'})
        pack=company_pack(company,args.get('fiscal_year'))
        return self.respond(request,pack,args)

    def respond(self,request,pack,args):
        question=args.get('question') if args['action']=='question' else ('Explica las diferencias entre las empresas y los datos faltantes.' if pack['scope']=='comparison' else 'Resume qué destaca, qué merece atención y qué falta conocer.')
        if outside_context(question,pack['tickers']):
            result=presentation(pack)
            result.update(status='available',generation_mode='context_guard',answer=answer_for(pack,['out_of_scope']))
            return Response(result,headers={'Cache-Control':'private, no-store'})
        token=usage_owner.set(request.user.pk)
        try:
            plan,hit=plan_answer(pack,question,args['history'],request.user.pk,args['level'])
            validate_plan(plan)
        except BudgetExceeded:
            raise
        except Exception:
            record_error('invalid_provider_response','local_ai')
            return Response({'status':'unavailable','message':'La IA local no pudo completar una respuesta validada. Tus datos y tu pregunta se conservan; puedes reintentar.'},status=503)
        finally:
            usage_owner.reset(token)
        topics=['out_of_scope'] if plan['context']=='other' else plan['topics']
        result=presentation(pack)
        answer=answer_for(pack,topics)
        normalized=''.join(c for c in unicodedata.normalize('NFKD',question.lower()) if not unicodedata.combining(c))
        answer['examples']=examples_for(topics) if re.search(r'ejemplo|explica\w* mas|mas sencillo|no entiend|no entend',normalized) else []
        result.update(status='available',generation_mode='local_ai_evidence_routing',cache_hit=hit,
            answer=answer,level=args['level'],
            ai_role='La IA identifica los temas de tu pregunta; el servidor aporta las cifras y las explicaciones respaldadas.')
        return Response(result,headers={'Cache-Control':'private, no-store'})


class ComparisonAssistantView(CompanyAssistantView):
    def get(self,request):
        return Response(presentation(self.pack(request,request.query_params)),headers={'Cache-Control':'private, no-store'})

    def pack(self,request,args):
        if 'compare' not in subscription(request.user)['features']:
            raise PermissionDenied('La explicación comparativa requiere el acceso Pro del comparador.')
        required=AssistantInput(data={'a':args.get('a'),'b':args.get('b'),'fiscal_year':args.get('fiscal_year')})
        required.is_valid(raise_exception=True)
        values=required.validated_data
        if not values.get('a') or not values.get('b') or not values.get('fiscal_year') or values['a']==values['b']:
            raise serializers.ValidationError('Selecciona dos empresas distintas y un ejercicio fiscal.')
        return comparison_pack(values['a'],values['b'],values['fiscal_year'])

    def post(self,request):
        serializer=AssistantInput(data=request.data);serializer.is_valid(raise_exception=True)
        args=serializer.validated_data
        pack=self.pack(request,args)
        if args['action'] not in ('summary','question'):
            raise serializers.ValidationError('Las lecciones y los cambios entre años se consultan desde la ficha de cada empresa.')
        return self.respond(request,pack,args)
