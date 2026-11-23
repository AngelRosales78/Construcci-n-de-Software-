"""Contadores observados, configuración versionada y registros mínimos de operación."""
from django.conf import settings
from django.db import models


class AITariff(models.Model):
    provider = models.CharField(max_length=40)
    model = models.CharField(max_length=100)
    version = models.CharField(max_length=60)
    currency = models.CharField(max_length=3, default='USD')
    input_per_million = models.DecimalField(max_digits=12, decimal_places=6)
    output_per_million = models.DecimalField(max_digits=12, decimal_places=6)
    effective_at = models.DateTimeField()

    class Meta:
        constraints = [models.UniqueConstraint(fields=['provider', 'model', 'effective_at'], name='ai_tariff_effective_unique')]
        ordering = ['-effective_at', '-id']


class AIUsage(models.Model):
    provider = models.CharField(max_length=40, default='local_ai')
    model = models.CharField(max_length=100)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, on_delete=models.SET_NULL)
    started_at = models.DateTimeField(db_index=True)
    duration_ms = models.PositiveIntegerField(default=0)
    outcome = models.CharField(max_length=16)
    input_tokens = models.PositiveIntegerField(null=True)
    output_tokens = models.PositiveIntegerField(null=True)
    estimated_cost = models.DecimalField(max_digits=18, decimal_places=8, null=True)
    tariff = models.JSONField(default=dict)

    class Meta:
        ordering = ['-started_at', '-id']


class ProviderBudget(models.Model):
    provider = models.CharField(max_length=40, unique=True)
    daily_limit = models.PositiveIntegerField(default=1000)
    reset_timezone = models.CharField(max_length=60, default='America/Lima')
    version = models.PositiveIntegerField(default=0)


class BudgetWindow(models.Model):
    budget = models.ForeignKey(ProviderBudget, on_delete=models.CASCADE, related_name='windows')
    date = models.DateField()
    timezone = models.CharField(max_length=60)
    calls = models.PositiveIntegerField(default=0)
    warned_at = models.DateTimeField(null=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['budget', 'date', 'timezone'], name='budget_date_zone_unique')]


class SystemErrorLog(models.Model):
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)
    level = models.CharField(max_length=10, default='error', db_index=True)
    component = models.CharField(max_length=40, db_index=True)
    code = models.CharField(max_length=60)
    summary = models.CharField(max_length=200)
    context = models.JSONField(default=dict)

    class Meta:
        ordering = ['-created_at', '-id']
