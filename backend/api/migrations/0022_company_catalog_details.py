from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [('api', '0021_push_session_binding')]
    operations = [migrations.AddField(model_name='company', name='catalog_details',
                                      field=models.JSONField(blank=True, default=dict))]
