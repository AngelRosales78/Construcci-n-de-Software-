from django.db import migrations, models

class Migration(migrations.Migration):
    dependencies = [('api', '0025_financial_altman_reference')]
    operations = [migrations.AddField('financialmetrics', 'data_diagnostics', models.JSONField(default=dict, blank=True))]
