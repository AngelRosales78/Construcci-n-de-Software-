import io
import json
import os
import re
from unittest.mock import patch
from django.test import SimpleTestCase
from django.core.cache import cache
from .local_ai import optional_local_ai, IDS

class LocalAiTests(SimpleTestCase):
    def setUp(self): cache.clear()
    def tearDown(self): cache.clear()
    def report(self):
        return {'ticker':'TEST','fiscal_year':2024,'level':'simple','generation_mode':'rules',
                'validation':{'status':'passed'},'pillars':[{'pillar_id':key,'text':'Valor verificado: 2,00','metric_ids':list(ids)} for key,ids in IDS.items()]}
    @patch.dict(os.environ, {'LOCAL_AI_MODEL':''})
    def test_without_model_does_not_claim_ai(self):
        self.assertEqual(optional_local_ai(self.report())['generation_mode'],'rules')
    @patch.dict(os.environ, {'LOCAL_AI_MODEL':'fixture-model'})
    def test_real_provider_reply_keeps_server_values(self):
        def reply(request, **kwargs):
            prompt=json.loads(request.data)['messages'][-1]['content']
            topic=re.search(r'Tema: ([^.]+)',prompt).group(1)
            text=f'El pilar de {topic} permite examinar los indicadores del balance. Debe interpretarse con los datos disponibles y sus limitaciones.'
            return io.BytesIO(json.dumps({'done':True,'done_reason':'stop','message':{'content':json.dumps({'explanation':text})}}).encode())
        with patch('api.local_ai.urlopen',side_effect=reply) as provider:
            report=optional_local_ai(self.report())
            cached=optional_local_ai(self.report())
        self.assertEqual(report['generation_mode'],'ai')
        self.assertEqual(provider.call_count,5)
        self.assertEqual(cached['generation_mode'],'ai')
        self.assertIn('2,00',report['pillars'][0]['text'])
        self.assertEqual(report['provider'],'ollama-local:fixture-model')
    @patch.dict(os.environ, {'LOCAL_AI_MODEL':'fixture-model'})
    def test_model_cannot_inject_numerical_claim(self):
        text='El pilar de liquidez presenta una rentabilidad de 999 por ciento.'
        payload={'done':True,'message':{'content':json.dumps({'explanation':text})}}
        with patch('api.local_ai.urlopen',return_value=io.BytesIO(json.dumps(payload).encode())):
            report=optional_local_ai(self.report())
        self.assertEqual(report['generation_mode'],'rules')
        self.assertNotIn('999',report['pillars'][0]['text'])
    @patch.dict(os.environ, {'LOCAL_AI_MODEL':'fixture-model'})
    def test_provider_failure_is_labelled_fallback(self):
        with patch('api.local_ai.urlopen',side_effect=OSError('offline')):
            report=optional_local_ai(self.report())
        self.assertEqual(report['generation_mode'],'rules')
        self.assertIn('sin IA',report['generation_notice'])
