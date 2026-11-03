from datetime import timedelta
from django.utils import timezone
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from rest_framework.response import Response
from .models import AccountPlan


def subscription(user):
    now = timezone.now()
    account = AccountPlan.objects.filter(user=user).first()
    plan = account.plan if account else 'free'
    active = (account.active and (account.expires_at is None or account.expires_at > now)) if account else True
    valid_until = now + timedelta(minutes=5)
    if account and active and account.expires_at:
        valid_until = min(valid_until, account.expires_at)
    return {'schema_version': 'subscription-v1', 'owner_id': str(user.pk),
            'plan': plan, 'active': active,
            'features': ['compare'] if plan == 'pro' and active else [],
            'valid_until': valid_until.isoformat()}


class SubscriptionView(APIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request):
        response = Response(subscription(request.user))
        response['Cache-Control'] = 'private, no-store'
        return response
