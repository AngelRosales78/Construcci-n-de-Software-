from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [('api', '0023_annual_provider_provenance')]
    operations = [migrations.AddField('financialmetrics', 'source_snapshot_sha256',
                    models.CharField(blank=True, default='', max_length=64))]
