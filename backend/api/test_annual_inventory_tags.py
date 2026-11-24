"""Importador SEC: etiquetas alternativas y pasivo total derivado, sin inventar valores."""
from django.test import SimpleTestCase

from .management.commands.sync_company_data import annual_inventory

ANCHOR = {'form': '10-K', 'fp': 'FY', 'fy': 2024, 'start': '2024-01-01', 'end': '2024-12-31',
          'filed': '2025-02-01', 'accn': '0000050863-25-000010'}


def duration(val):
    return ANCHOR | {'val': val}


def instant(val, **extra):
    row = {k: v for k, v in ANCHOR.items() if k != 'start'} | {'val': val}
    return row | extra


def build(**tags):
    facts = {'Revenues': [duration(1000)]} | tags
    payload = {'entityName': 'Fixture', 'facts': {'us-gaap': {k: {'units': {'USD': v}} for k, v in facts.items()}}}
    return annual_inventory(payload, 'TEST', 50863, b'raw')['annual_records'][0]['values']


class AnnualInventoryTagTests(SimpleTestCase):
    def test_reported_liabilities_win_over_derivation(self):
        values = build(Liabilities=[instant(300)], LiabilitiesAndStockholdersEquity=[instant(999)], StockholdersEquity=[instant(1)])
        self.assertEqual(values['total_liabilities'], {'value': 300, 'tag': 'Liabilities', 'available': True})

    def test_liabilities_derived_with_noncontrolling_interest_as_equity(self):
        values = build(LiabilitiesAndStockholdersEquity=[instant(196485)],
                       StockholdersEquity=[instant(99270)],
                       StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest=[instant(105032)])
        self.assertEqual(values['total_liabilities']['value'], 91453)
        self.assertTrue(values['total_liabilities']['tag'].startswith('derived:LiabilitiesAndStockholdersEquity-'))

    def test_derivation_adds_minority_interest_and_excludes_temporary_equity(self):
        values = build(LiabilitiesAndStockholdersEquity=[instant(1000)], StockholdersEquity=[instant(500)],
                       MinorityInterest=[instant(50)],
                       TemporaryEquityCarryingAmountIncludingPortionAttributableToNoncontrollingInterests=[instant(20)])
        self.assertEqual(values['total_liabilities']['value'], 430)
        self.assertIn('MinorityInterest', values['total_liabilities']['tag'])
        self.assertIn('TemporaryEquity', values['total_liabilities']['tag'])

    def test_no_derivation_without_both_sides_or_with_ambiguous_values(self):
        self.assertIsNone(build(LiabilitiesAndStockholdersEquity=[instant(1000)])['total_liabilities']['value'])
        ambiguous = build(LiabilitiesAndStockholdersEquity=[instant(1000), instant(1100)], StockholdersEquity=[instant(500)])
        self.assertIsNone(ambiguous['total_liabilities']['value'])
        other_filing = build(LiabilitiesAndStockholdersEquity=[instant(1000, accn='0000050863-26-000001')], StockholdersEquity=[instant(500)])
        self.assertIsNone(other_filing['total_liabilities']['value'])

    def test_alternative_tags_fill_cash_investments_receivables_and_interest(self):
        values = build(CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents=[instant(8249)],
                       AvailableForSaleSecuritiesDebtSecuritiesCurrent=[instant(13813)],
                       ReceivablesNetCurrent=[instant(3478)],
                       InterestExpenseNonoperating=[duration(1034)])
        self.assertEqual((values['cash']['value'], values['cash']['tag']), (8249, 'CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents'))
        self.assertEqual(values['short_term_investments']['value'], 13813)
        self.assertEqual(values['receivables']['tag'], 'ReceivablesNetCurrent')
        self.assertEqual((values['interest_expense']['value'], values['interest_expense']['tag']), (1034, 'InterestExpenseNonoperating'))

    def test_preferred_tag_keeps_priority(self):
        values = build(CashAndCashEquivalentsAtCarryingValue=[instant(10)],
                       CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents=[instant(12)],
                       InterestExpense=[duration(5)], InterestExpenseNonoperating=[duration(7)])
        self.assertEqual(values['cash']['value'], 10)
        self.assertEqual(values['interest_expense']['value'], 5)


def full(**tags):
    """Inventory row plus the balance-sheet basics the not-reported rules require."""
    facts = {'Revenues': [duration(1000)], 'CashAndCashEquivalentsAtCarryingValue': [instant(50)],
             'AssetsCurrent': [instant(200)]} | tags
    payload = {'entityName': 'Fixture', 'facts': {'us-gaap': {k: {'units': {'USD': v}} for k, v in facts.items()}}}
    return annual_inventory(payload, 'TEST', 50863, b'raw')['annual_records'][0]


class NotReportedItemsTests(SimpleTestCase):
    def test_short_term_investments_not_reported_become_labelled_zero(self):
        row = full()
        self.assertEqual(row['values']['short_term_investments'], {'value': 0, 'tag': 'not_reported:ShortTermInvestments', 'available': True})

    def test_ambiguous_investment_line_stays_null(self):
        row = full(TradingSecurities=[instant(30)])
        self.assertIsNone(row['values']['short_term_investments']['value'])
        row = full(MarketableSecurities=[instant(30)])
        self.assertIsNone(row['values']['short_term_investments']['value'])

    def test_no_zero_without_a_readable_balance_sheet(self):
        payload = {'entityName': 'Bank', 'facts': {'us-gaap': {'Revenues': {'units': {'USD': [duration(1000)]}}}}}
        row = annual_inventory(payload, 'TEST', 50863, b'raw')['annual_records'][0]
        self.assertIsNone(row['values']['short_term_investments']['value'])

    def test_inventory_absent_from_filing_is_not_applicable_with_note(self):
        row = full()
        self.assertIs(row['inventory_applicable'], False)
        self.assertIn('no informa ninguna partida de inventario', row['inventory_applicability_note'])

    def test_inventory_components_without_total_stay_unavailable(self):
        row = full(InventoryRawMaterialsAndSupplies=[instant(40)])
        self.assertNotIn('inventory_applicable', row)
        self.assertIsNone(row['values']['inventory']['value'])
        self.assertNotIn('inventory_applicable', full(InventoryNet=[instant(40)]))
