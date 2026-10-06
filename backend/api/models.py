"""
Custom User model for the S&P 500 platform.

Uses AbstractUser to extend the default Django user with additional
financial platform fields while leveraging the robust built-in
authentication backend.
"""
from django.contrib.auth.models import AbstractUser
from django.db import models
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
    last_login_ip = models.GenericIPAddressField(_('last login IP'), blank=True, null=True)
    created_at = models.DateTimeField(_('created at'), auto_now_add=True)
    updated_at = models.DateTimeField(_('updated at'), auto_now=True)

    class Meta:
        db_table = 'auth_user'
        verbose_name = _('user')
        verbose_name_plural = _('users')
        ordering = ['-created_at']

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

    ticker = models.CharField(_('ticker'), max_length=10, unique=True, db_index=True)
    name = models.CharField(_('name'), max_length=200, db_index=True)
    sector = models.CharField(_('sector'), max_length=50, choices=SECTOR_CHOICES, db_index=True)
    market_cap = models.DecimalField(_('market cap'), max_digits=20, decimal_places=2, default=0)
    per = models.DecimalField(_('PER'), max_digits=10, decimal_places=2, null=True, blank=True)
    roe = models.DecimalField(_('ROE'), max_digits=10, decimal_places=2, null=True, blank=True)

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
        ordering = ['-market_cap']

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
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.user.username}: {self.query}"
