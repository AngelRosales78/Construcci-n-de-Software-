from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [('api', '0022_company_catalog_details')]
    operations = [
        migrations.AddField('financialmetrics', 'reference_basis', models.CharField(default='reported', max_length=24)),
        migrations.AlterField('annualfinancialstatement', 'period_start', models.DateField(blank=True, null=True)),
        migrations.AlterField('annualfinancialstatement', 'filed', models.DateField(blank=True, null=True)),
        migrations.AlterField('annualfinancialstatement', 'accession', models.CharField(blank=True, default='', max_length=40)),
        migrations.AddField('annualfinancialstatement', 'source_kind', models.CharField(default='sec', max_length=20)),
        migrations.AddField('annualfinancialstatement', 'provenance', models.JSONField(blank=True, default=dict)),
    ]
