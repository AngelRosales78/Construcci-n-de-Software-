"""Pilares con partidas no publicadas: 0 explícito en liquidez y «no aplica» en inventario."""
from datetime import date

from django.test import TestCase

from .models import AnnualFinancialStatement, Company
from .pillars import build_pillars, METHODOLOGY_VERSION


class NotReportedPillarTests(TestCase):
    def statement(self, company, year, components, **extra):
        return AnnualFinancialStatement.objects.create(company=company, fiscal_year=year, period_start=date(year, 1, 1),
            period_end=date(year, 12, 31), filed=date(year + 1, 2, 1), source_url='https://www.sec.gov/test',
            accession=f'0000000001-{year % 100:02d}-000001', snapshot_sha256='a' * 64,
            components={k: {'value': v, 'tag': t} for k, (v, t) in components.items()}, **extra)

    def test_quick_ratio_uses_zero_and_says_so(self):
        company = Company.objects.create(ticker='SOFT', name='Software', sector='technology')
        base = {'current_assets': ('300', 'AssetsCurrent'), 'current_liabilities': ('100', 'LiabilitiesCurrent'),
                'cash': ('60', 'Cash'), 'receivables': ('40', 'AccountsReceivableNetCurrent'),
                'short_term_investments': ('0', 'not_reported:ShortTermInvestments')}
        current = self.statement(company, 2024, base)
        data = build_pillars(company, current, None, 2024, [2024], {2024: current})
        quick = next(m for m in data['pillars'][0]['metrics'] if m['id'] == 'quick_ratio')
        self.assertEqual(quick['status'], 'available')
        self.assertEqual(quick['display_value'], '1,00')
        self.assertIn('no informa esta partida', quick['assumptions'][0])
        current_ratio = next(m for m in data['pillars'][0]['metrics'] if m['id'] == 'current_ratio')
        self.assertNotIn('assumptions', current_ratio)
        self.assertEqual(data['methodology_version'], METHODOLOGY_VERSION)

    def test_inventory_not_applicable_is_excluded_from_efficiency_score(self):
        company = Company.objects.create(ticker='BANK', name='Bank', sector='financials')
        values = {'revenue': ('100', 'Revenues'), 'total_assets': ('200', 'Assets')}
        note = 'El 10-K de este ejercicio no informa ninguna partida de inventario.'
        previous = self.statement(company, 2023, values, inventory_applicable=False, inventory_applicability_note=note)
        current = self.statement(company, 2024, values, inventory_applicable=False, inventory_applicability_note=note)
        data = build_pillars(company, current, previous, 2024, [2024, 2023], {2023: previous, 2024: current})
        efficiency = next(p for p in data['pillars'] if p['id'] == 'efficiency')
        self.assertEqual(efficiency['score']['status'], 'available')
        self.assertEqual(efficiency['score']['excluded_metrics'][0]['id'], 'inventory_days')
