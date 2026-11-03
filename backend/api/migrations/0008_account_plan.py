from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [('api', '0007_analysis_records')]
    operations = [migrations.CreateModel(
        name='AccountPlan',
        fields=[
            ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
            ('plan', models.CharField(choices=[('free', 'Free'), ('pro', 'Pro')], default='free', max_length=8)),
            ('active', models.BooleanField(default=True)),
            ('expires_at', models.DateTimeField(blank=True, null=True)),
            ('user', models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name='account_plan', to=settings.AUTH_USER_MODEL)),
        ],
    )]
