from django.db import migrations, models

class Migration(migrations.Migration):
    dependencies = [('api', '0024_financial_snapshot_identity')]
    operations = [migrations.AddField('financialmetrics', 'altman_reference', models.JSONField(default=dict, blank=True))]
