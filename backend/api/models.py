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
