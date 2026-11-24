"""
Custom User model for the S&P 500 platform.

Uses AbstractUser to extend the default Django user with additional
financial platform fields while leveraging the robust built-in
authentication backend.
"""
from django.contrib.auth.models import AbstractUser
from django.db import models
from django.core.validators import MinValueValidator
from decimal import Decimal
import uuid
from django.utils import timezone
from .monitoring_models import AITariff, AIUsage, ProviderBudget, BudgetWindow, SystemErrorLog
from django.contrib.postgres.indexes import GinIndex
from django.contrib.postgres.search import SearchVector
from django.utils.translation import gettext_lazy as _


class User(AbstractUser):
    """
    Custom user model extending Django's AbstractUser.

    Additional fields specific to the financial platform:
    - phone_number: Optional contact number
    - user_type: Distinguishes between 'retail' and 'institutional' accounts
    - is_verified: Email verification flag
    - last_login_ip: Track login source for security
    - created_at: Timestamp for user lifecycle tracking
    - updated_at: Timestamp for last modification
    """
    USER_TYPE_CHOICES = (
        ('retail', _('Retail Investor')),
        ('institutional', _('Institutional Investor')),
    )

    phone_number = models.CharField(_('phone number'), max_length=20, blank=True, default='')
    user_type = models.CharField(
        _('user type'),
        max_length=20,
        choices=USER_TYPE_CHOICES,
        default='retail'
    )
    is_verified = models.BooleanField(_('email verified'), default=False)
    # HU13: cambia al bloquear o cambiar de rol; invalida los tokens anteriores.
    session_version = models.PositiveBigIntegerField(default=0)
    blocked_reason = models.CharField(max_length=240, blank=True, default='')
    last_login_ip = models.GenericIPAddressField(_('last login IP'), blank=True, null=True)
    created_at = models.DateTimeField(_('created at'), auto_now_add=True)
    updated_at = models.DateTimeField(_('updated at'), auto_now=True)

    class Meta:
        db_table = 'auth_user'
        verbose_name = _('user')
        verbose_name_plural = _('users')
        ordering = ['-created_at']
        constraints = [
            # HU02_03: a registered phone identifies one account; empty phones remain allowed.
            models.UniqueConstraint(fields=['phone_number'], condition=~models.Q(phone_number=''),
                                    name='unique_nonempty_phone_number'),
        ]

    def __str__(self):
        return f"{self.username} ({self.get_user_type_display()})"


class Company(models.Model):
    """
    S&P 500 company model with financial data for analysis.
    """
    SECTOR_CHOICES = (
        ('technology', _('Technology')),
        ('healthcare', _('Healthcare')),
        ('financials', _('Financials')),
        ('consumer_discretionary', _('Consumer Discretionary')),
        ('consumer_staples', _('Consumer Staples')),
        ('industrials', _('Industrials')),
        ('energy', _('Energy')),
        ('materials', _('Materials')),
        ('utilities', _('Utilities')),
        ('real_estate', _('Real Estate')),
        ('communication_services', _('Communication Services')),
    )

    logo_url = models.URLField(blank=True, default='')
    latest_price = models.ForeignKey('PriceHistory', null=True, blank=True, on_delete=models.SET_NULL, related_name='+')
    ticker = models.CharField(_('ticker'), max_length=10, unique=True, db_index=True)
    name = models.CharField(_('name'), max_length=200, db_index=True)
    sector = models.CharField(_('sector'), max_length=50, choices=SECTOR_CHOICES, db_index=True)
    market_cap = models.DecimalField(_('market cap'), max_digits=20, decimal_places=2, null=True, blank=True)
    per = models.DecimalField(_('PER'), max_digits=10, decimal_places=2, null=True, blank=True)
    roe = models.DecimalField(_('ROE'), max_digits=10, decimal_places=2, null=True, blank=True)

    # HU14: catálogo administrativo.
    is_active = models.BooleanField(default=True, db_index=True)
    is_pilot = models.BooleanField(default=False, db_index=True)
    description = models.TextField(max_length=3000, blank=True, default='')
    website_url = models.URLField(max_length=500, blank=True, default='')
    sector_overridden = models.BooleanField(default=False)
    is_index_member = models.BooleanField(default=True)
    identity_source_url = models.URLField(max_length=500, blank=True, default='')
    identity_verified_at = models.DateTimeField(null=True, blank=True)

    working_capital = models.DecimalField(_('working capital'), max_digits=20, decimal_places=2, default=0)
    total_assets = models.DecimalField(_('total assets'), max_digits=20, decimal_places=2, default=0)
    retained_earnings = models.DecimalField(_('retained earnings'), max_digits=20, decimal_places=2, default=0)
    ebit = models.DecimalField(_('EBIT'), max_digits=20, decimal_places=2, default=0)
    total_liabilities = models.DecimalField(_('total liabilities'), max_digits=20, decimal_places=2, default=0)
    sales = models.DecimalField(_('sales'), max_digits=20, decimal_places=2, default=0)
    equity_value = models.DecimalField(_('equity value'), max_digits=20, decimal_places=2, default=0)

    created_at = models.DateTimeField(_('created at'), auto_now_add=True)
    updated_at = models.DateTimeField(_('updated at'), auto_now=True)

    class Meta:
        db_table = 'companies'
        verbose_name = _('company')
        verbose_name_plural = _('companies')
        ordering = ['-market_cap', 'ticker']
        indexes = [
            models.Index(fields=['sector', '-market_cap', 'ticker'], name='company_sector_cap_idx'),
            GinIndex(SearchVector('name', weight='A', config='simple') +
                     SearchVector('ticker', weight='B', config='simple'), name='company_search_gin'),
        ]

    def __str__(self):
        return f"{self.ticker} - {self.name}"


class SearchHistory(models.Model):
    """
    Stores user search history for quick access.
    """
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='search_history')
    query = models.CharField(_('query'), max_length=200)
    created_at = models.DateTimeField(_('created at'), auto_now_add=True)

    class Meta:
        db_table = 'search_history'
        verbose_name = _('search history')
        verbose_name_plural = _('search history')
        ordering = ['-created_at', '-id']
        indexes = [models.Index(fields=['user', '-created_at', '-id'], name='history_user_date_idx')]

    def __str__(self):
        return f"{self.user.username}: {self.query}"


class FinancialMetrics(models.Model):
    field_sources = models.JSONField(default=dict, blank=True)
    company = models.OneToOneField(Company, on_delete=models.CASCADE, related_name='financial_metrics')
    as_of = models.DateField()
    source = models.CharField(max_length=250)
    is_demo = models.BooleanField(default=False)
    market_cap = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True, validators=[MinValueValidator(0)])
    per = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    roe = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    beta = models.DecimalField(max_digits=10, decimal_places=4, null=True, blank=True)
    dividend_yield = models.DecimalField(max_digits=10, decimal_places=4, null=True, blank=True, validators=[MinValueValidator(0)])
    working_capital = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True)
    total_assets = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True, validators=[MinValueValidator(0)])
    retained_earnings = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True)
    ebit = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True)
    total_liabilities = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True, validators=[MinValueValidator(0)])
    sales = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True, validators=[MinValueValidator(0)])
    equity_value = models.DecimalField(max_digits=20, decimal_places=2, null=True, blank=True, validators=[MinValueValidator(0)])
    class Meta:
        db_table = 'financial_metrics'


class PriceHistory(models.Model):
    company = models.ForeignKey(Company, on_delete=models.CASCADE, related_name='prices')
    date = models.DateField()
    adjusted_close = models.DecimalField(max_digits=18, decimal_places=6, validators=[MinValueValidator(Decimal('0.000001'))])
    volume = models.BigIntegerField(null=True, blank=True, validators=[MinValueValidator(0)])
    currency = models.CharField(max_length=3, default='USD')
    source = models.CharField(max_length=250)
    is_demo = models.BooleanField(default=False)
    class Meta:
        db_table = 'price_history'
        ordering = ['date']
        constraints = [models.UniqueConstraint(fields=['company', 'date'], name='price_company_date_unique')]
        indexes = [models.Index(fields=['company', '-date'], name='price_company_date_idx')]


class AnnualFinancialStatement(models.Model):
    reporting_note = models.TextField(blank=True, null=True, default=None)
    """Componentes anuales separados de la ficha heredada de una sola fecha."""
    company = models.ForeignKey(Company, on_delete=models.CASCADE, related_name='annual_statements')
    fiscal_year = models.PositiveSmallIntegerField()
    period_start = models.DateField()
    period_end = models.DateField()
    currency = models.CharField(max_length=3, default='USD')
    source_url = models.URLField(max_length=500)
    accession = models.CharField(max_length=40)
    filed = models.DateField()
    is_demo = models.BooleanField(default=False)
    snapshot_sha256 = models.CharField(max_length=64)
    components = models.JSONField(default=dict)
    inventory_applicable = models.BooleanField(null=True, blank=True, default=None)
    inventory_applicability_note = models.CharField(max_length=250, blank=True, default='')

    class Meta:
        db_table = 'annual_financial_statements'
        ordering = ['-fiscal_year']
        constraints = [models.UniqueConstraint(fields=['company', 'fiscal_year'], name='annual_company_year_unique')]

    def clean(self):
        from .annual_financials import validate_statement
        validate_statement(self)


class Favorite(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    company = models.ForeignKey(Company, on_delete=models.CASCADE)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'company'], name='favorite_user_company')]
        ordering = ['-created_at', '-id']


class PersonalVersion(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE)
    version = models.PositiveBigIntegerField(default=0)


class FavoriteTag(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    name = models.CharField(max_length=40)
    normalized_name = models.CharField(max_length=120)
    color = models.CharField(max_length=7)
    favorites = models.ManyToManyField(Favorite, related_name='tags', blank=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'normalized_name'], name='tag_user_normalized_name')]


class CompanyVisit(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    company = models.ForeignKey(Company, on_delete=models.CASCADE)
    visited_at = models.DateTimeField()

    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'company'], name='visit_user_company')]


class VisitReceipt(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    event_id = models.CharField(max_length=80)
    company = models.ForeignKey(Company, on_delete=models.CASCADE)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'event_id'], name='visit_user_event')]


class UserSubscription(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE)
    plan = models.CharField(max_length=8, default='free', choices=[('free', 'Free'), ('pro', 'Pro')])
    active = models.BooleanField(default=True)
    valid_until = models.DateTimeField()
    # HU11_04/HU11_05: simulated billing period and renewal state.
    period_start = models.DateTimeField(null=True, blank=True)
    auto_renew = models.BooleanField(default=True)
    cancelled_at = models.DateTimeField(null=True, blank=True)
    cancel_reason = models.CharField(max_length=200, blank=True, default='')
    # HU13_02: Pro asignado por un administrador; no tiene importe, vencimiento ni renovación.
    granted = models.BooleanField(default=False)


class Invoice(models.Model):
    """HU11_03: receipt of a simulated payment. Card data is never stored beyond brand and last four digits."""
    KIND_CHOICES = [('purchase', 'Compra'), ('renewal', 'Renovación')]
    # HU13_05: el comprobante simulado se conserva sin cuenta al eliminarla.
    user = models.ForeignKey(User, null=True, on_delete=models.SET_NULL, related_name='invoices')
    number = models.CharField(max_length=32, unique=True)
    transaction_id = models.CharField(max_length=40, unique=True)
    request_id = models.CharField(max_length=64)
    kind = models.CharField(max_length=10, choices=KIND_CHOICES, default='purchase')
    plan = models.CharField(max_length=8, default='pro')
    amount = models.DecimalField(max_digits=8, decimal_places=2)
    currency = models.CharField(max_length=3, default='USD')
    card_brand = models.CharField(max_length=20, blank=True, default='')
    card_last4 = models.CharField(max_length=4, blank=True, default='')
    period_start = models.DateTimeField()
    period_end = models.DateTimeField()
    created_at = models.DateTimeField(auto_now_add=True)
    emailed_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ['-created_at', '-id']
        constraints = [models.UniqueConstraint(fields=['user', 'request_id'], name='invoice_user_request')]


class AnalysisSnapshot(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    version = models.CharField(max_length=500)
    a = models.CharField(max_length=10)
    b = models.CharField(max_length=10, blank=True, default='')
    fiscal_year = models.PositiveSmallIntegerField()
    level = models.CharField(max_length=12)
    origin = models.CharField(max_length=8)
    payload = models.JSONField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'version'], name='analysis_user_version')]


class ReportJob(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    request_id = models.CharField(max_length=80)
    request = models.JSONField()
    analysis_payload = models.JSONField(default=dict)
    status = models.CharField(max_length=12, default='queued')
    metadata = models.JSONField(default=dict)
    content = models.BinaryField(null=True)
    message = models.CharField(max_length=300, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'request_id'], name='report_user_request')]


# Sprint 4: equivalentes locales de los modelos de la rama magallanes-sprint4.

class UserAvatar(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='avatar')
    url = models.CharField(max_length=250, default='/api/v1/auth/profile/avatar/')
    content = models.BinaryField()
    sha256 = models.CharField(max_length=64)
    updated_at = models.DateTimeField(auto_now=True)


class AdministrationEvent(models.Model):
    actor = models.ForeignKey(User, null=True, on_delete=models.SET_NULL, related_name='administration_actions')
    subject = models.ForeignKey(User, null=True, on_delete=models.SET_NULL, related_name='administration_events')
    action = models.CharField(max_length=40)
    changes = models.JSONField(default=dict)
    created_at = models.DateTimeField(auto_now_add=True)


class CatalogRevision(models.Model):
    """Versión compartida por procesos para no reutilizar búsquedas desactualizadas."""
    revision = models.PositiveBigIntegerField(default=0)


class ProviderDailyUsage(models.Model):
    provider = models.CharField(max_length=30)
    day = models.DateField()
    calls = models.PositiveIntegerField(default=0)
    limit = models.PositiveIntegerField()
    last_called_at = models.DateTimeField(null=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['provider', 'day'], name='provider_day_unique')]


class MarketDataOperation(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    actor = models.ForeignKey(User, null=True, on_delete=models.SET_NULL)
    request_id = models.UUIDField()
    company = models.ForeignKey(Company, null=True, on_delete=models.SET_NULL, related_name='market_updates')
    ticker = models.CharField(max_length=10)
    kind = models.CharField(max_length=10, choices=[('verify', 'Verificar'), ('refresh', 'Actualizar')])
    sector = models.CharField(max_length=50, blank=True)
    status = models.CharField(max_length=12, default='running')
    started_at = models.DateTimeField(auto_now_add=True)
    finished_at = models.DateTimeField(null=True)
    error_code = models.CharField(max_length=40, blank=True)
    message = models.CharField(max_length=350, blank=True)
    result = models.JSONField(default=dict)
    source_url = models.URLField(max_length=500, blank=True)
    response_sha256 = models.CharField(max_length=64, blank=True)
    raw_response = models.BinaryField(null=True)

    class Meta:
        ordering = ['-started_at', '-id']
        constraints = [
            models.UniqueConstraint(fields=['actor', 'request_id'], name='market_actor_request_unique'),
            models.UniqueConstraint(fields=['ticker'], condition=models.Q(status='running'), name='one_running_market_ticker'),
        ]


class NotificationPreferences(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='notification_preferences')
    rating_changes = models.BooleanField(default=True)
    web_enabled = models.BooleanField(default=True)
    email_enabled = models.BooleanField(default=False)
    push_enabled = models.BooleanField(default=False)
    version = models.PositiveBigIntegerField(default=0)
    updated_at = models.DateTimeField(auto_now=True)


class CompanyRatingState(models.Model):
    company = models.OneToOneField(Company, on_delete=models.CASCADE)
    last_valid = models.JSONField(default=dict)
    revision = models.PositiveBigIntegerField(default=0)


class RatingChangeEvent(models.Model):
    company = models.ForeignKey(Company, on_delete=models.CASCADE)
    revision = models.PositiveBigIntegerField()
    before = models.JSONField()
    after = models.JSONField()
    created_at = models.DateTimeField(auto_now_add=True)
    processed_at = models.DateTimeField(null=True)
    is_demo = models.BooleanField(default=False)
    favorite_through_id = models.PositiveBigIntegerField(null=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['company', 'revision', 'is_demo'], name='rating_company_revision_unique')]


class Notification(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    event = models.ForeignKey(RatingChangeEvent, on_delete=models.CASCADE)
    created_at = models.DateTimeField(auto_now_add=True)
    read_at = models.DateTimeField(null=True)
    web_visible = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at', '-id']
        constraints = [models.UniqueConstraint(fields=['user', 'event'], name='notification_user_event_unique')]
        indexes = [models.Index(fields=['user', '-id'], name='notification_user_id_idx')]


class PushInstallation(models.Model):
    installation_id = models.UUIDField(unique=True)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='push_installations')
    token = models.TextField(unique=True)
    active = models.BooleanField(default=True)
    permission_granted = models.BooleanField(default=True)
    session_id = models.CharField(max_length=64, blank=True)
    updated_at = models.DateTimeField(auto_now=True)


class NotificationDelivery(models.Model):
    notification = models.ForeignKey(Notification, on_delete=models.CASCADE, related_name='deliveries')
    channel = models.CharField(max_length=8)
    installation = models.ForeignKey(PushInstallation, null=True, on_delete=models.CASCADE)
    status = models.CharField(max_length=12, default='pending')
    attempts = models.PositiveSmallIntegerField(default=0)
    next_attempt = models.DateTimeField(default=timezone.now)
    lease_until = models.DateTimeField(null=True)
    sent_at = models.DateTimeField(null=True)
    error_code = models.CharField(max_length=40, blank=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=['notification', 'channel', 'installation'], name='notification_channel_device_unique'),
                       models.UniqueConstraint(fields=['notification', 'channel'], condition=models.Q(channel='email'), name='notification_email_unique')]
