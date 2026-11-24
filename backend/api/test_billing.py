"""Epic 11 (simulated payments) and HU02_03 phone uniqueness."""
from datetime import timedelta

from django.core import mail
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework.test import APIClient

from .billing import luhn_valid
from .models import Invoice, User, UserSubscription

VISA = '4242424242424242'
NEXT_YEAR = timezone.now().year + 1


@override_settings(LOCAL_DEVELOPMENT_PRO=False,
                   EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend')
class SimulatedBillingTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(username='payer', email='payer@example.test', password='TestsOnly8!')
        self.other = User.objects.create_user(username='other', password='TestsOnly8!')
        self.client = APIClient()
        self.client.force_authenticate(self.user)

    def pay(self, request_id='req-00000001', **card):
        body = {'request_id': request_id, 'card_number': VISA, 'holder': 'Ana Prueba',
                'exp_month': 12, 'exp_year': NEXT_YEAR, 'cvc': '123'} | card
        return self.client.post('/api/v1/payments/simulate/', body, format='json')

    def subscription(self):
        return self.client.get('/api/v1/user/subscription/').data

    def test_luhn(self):
        self.assertTrue(luhn_valid(VISA))
        self.assertTrue(luhn_valid('5555555555554444'))
        self.assertFalse(luhn_valid('4242424242424241'))
        self.assertFalse(luhn_valid('12345'))

    def test_plans_table_marks_pro_popular_and_compare_only_in_pro(self):
        data = self.client.get('/api/v1/payments/plans/').data
        free, pro = data['plans']
        self.assertTrue(pro['popular'])
        self.assertFalse(next(f for f in free['features'] if f['id'] == 'compare')['included'])
        self.assertTrue(all(f['included'] for f in pro['features']))

    def test_payment_upgrades_immediately_emails_receipt_and_never_stores_card(self):
        self.assertEqual(self.subscription()['plan'], 'free')
        self.assertEqual(self.client.get('/api/v1/compare/?a=AAPL&b=MSFT&fiscal_year=2024').status_code, 403)
        with self.captureOnCommitCallbacks(execute=True):
            response = self.pay(card_number='4242 4242 4242 4242')
        self.assertEqual(response.status_code, 201)
        invoice = response.data['invoice']
        self.assertEqual((invoice['card_brand'], invoice['card_last4'], invoice['amount']), ('Visa', '4242', '9.99'))
        self.assertTrue(invoice['transaction_id'].startswith('TXN-'))
        info = response.data['subscription']
        self.assertEqual((info['plan'], info['active'], info['auto_renew']), ('pro', True, True))
        self.assertIn('compare', info['features'])
        self.assertEqual(len(mail.outbox), 1)
        self.assertIn(invoice['number'], mail.outbox[0].subject)
        self.assertEqual(mail.outbox[0].attachments[0][0], f"{invoice['number']}.html")
        stored = Invoice.objects.get()
        self.assertNotIn(VISA, str(stored.__dict__))
        self.assertIsNotNone(stored.emailed_at)

    def test_same_request_is_idempotent_and_active_pro_cannot_pay_twice(self):
        first = self.pay()
        again = self.pay()
        self.assertEqual(again.status_code, 200)
        self.assertEqual(first.data['invoice']['number'], again.data['invoice']['number'])
        self.assertEqual(Invoice.objects.count(), 1)
        self.assertEqual(self.pay('req-00000002').status_code, 400)

    def test_invalid_cards_are_rejected_without_changing_plan(self):
        cases = [{'card_number': '4242424242424241'}, {'cvc': '12'}, {'holder': ''},
                 {'exp_month': 1, 'exp_year': 2020}, {'card_number': '4000000000000002'},
                 {'exp_month': 'xx'}]
        for index, card in enumerate(cases):
            response = self.pay(f'req-bad-{index:04d}', **card)
            self.assertEqual(response.status_code, 400, card)
            self.assertIn('detail', response.data)
        self.assertEqual(self.pay('bad id').status_code, 400)
        self.assertEqual(self.subscription()['plan'], 'free')
        self.assertFalse(Invoice.objects.exists())

    def test_invoices_are_private(self):
        number = self.pay().data['invoice']['number']
        self.assertEqual(self.client.get('/api/v1/payments/invoices/').data['results'][0]['number'], number)
        self.assertEqual(self.client.get(f'/api/v1/payments/invoices/{number}/').status_code, 200)
        self.client.force_authenticate(self.other)
        self.assertEqual(self.client.get('/api/v1/payments/invoices/').data['results'], [])
        self.assertEqual(self.client.get(f'/api/v1/payments/invoices/{number}/').status_code, 404)

    def test_cancel_keeps_pro_until_period_end_then_returns_to_free(self):
        self.assertEqual(self.client.post('/api/v1/subscription/cancel/', {}, format='json').status_code, 400)
        self.pay()
        cancelled = self.client.post('/api/v1/subscription/cancel/', {'reason': 'price', 'comment': 'caro'}, format='json')
        self.assertEqual(cancelled.status_code, 200)
        self.assertEqual((cancelled.data['plan'], cancelled.data['active'], cancelled.data['auto_renew']), ('pro', True, False))
        self.assertIsNotNone(cancelled.data['cancelled_at'])
        self.assertEqual(self.client.post('/api/v1/subscription/cancel/', {'reason': 'nope'}, format='json').status_code, 400)
        UserSubscription.objects.update(valid_until=timezone.now() - timedelta(seconds=1))
        info = self.subscription()
        self.assertEqual((info['plan'], info['features']), ('free', []))
        self.assertEqual(Invoice.objects.count(), 1)
        self.assertEqual(self.pay('req-00000003').status_code, 201)

    def test_auto_renew_rolls_period_forward_with_renewal_receipt(self):
        self.pay()
        UserSubscription.objects.update(valid_until=timezone.now() - timedelta(days=1))
        info = self.subscription()
        self.assertEqual((info['plan'], info['active']), ('pro', True))
        self.assertGreater(info['period_end'], timezone.now().isoformat())
        self.assertEqual(list(Invoice.objects.values_list('kind', flat=True).order_by('id')), ['purchase', 'renewal'])


class PhoneRegistrationTests(TestCase):
    def register(self, username, phone):
        return APIClient().post('/api/v1/auth/register/', {
            'username': username, 'email': f'{username}@example.test', 'password': 'TestsOnly8!x',
            'password_confirm': 'TestsOnly8!x', 'phone_number': phone}, format='json')

    def test_phone_is_optional_normalized_and_unique(self):
        self.assertEqual(self.register('nophone', '').status_code, 201)
        self.assertEqual(self.register('nophone2', '').status_code, 201)
        self.assertEqual(self.register('withphone', '+51 987 654 321').status_code, 201)
        self.assertEqual(User.objects.get(username='withphone').phone_number, '+51987654321')
        duplicate = self.register('dupphone', '+51987654321')
        self.assertEqual(duplicate.status_code, 400)
        self.assertIn('phone_number', duplicate.data)
        self.assertEqual(self.register('badphone', 'abc').status_code, 400)
