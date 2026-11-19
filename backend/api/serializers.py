"""
Authentication serializers for the S&P 500 platform.

Uses DRF serializers with Yup-equivalent validation logic (pydantic-style
schema validation in Python) for all auth endpoints.
"""
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from django.contrib.auth.password_validation import validate_password
from django.utils.translation import gettext_lazy as _
from .models import User, Company, SearchHistory


class RegisterSerializer(serializers.ModelSerializer):
    """
    Serializer for user registration (HU01_01).
    Implements field-level validation equivalent to Yup schema:
    - username: required, min 3 chars, unique
    - email: required, valid email format, unique
    - password: required, min 8 chars, complex (upper, lower, digit, special)
    - password_confirm: must match password
    """
    password = serializers.CharField(write_only=True, required=True, validators=[validate_password])
    password_confirm = serializers.CharField(write_only=True, required=True)
    email = serializers.EmailField(required=True)

    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'password', 'password_confirm', 'user_type', 'first_name', 'last_name']
        extra_kwargs = {
            'first_name': {'required': False, 'allow_blank': True},
            'last_name': {'required': False, 'allow_blank': True},
            'user_type': {'required': False},
        }

    def validate_username(self, value):
        if len(value) < 3:
            raise serializers.ValidationError(
                _('Username must be at least 3 characters long.')
            )
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError(
                _('A user with this username already exists.')
            )
        return value

    def validate_email(self, value):
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError(
                _('A user with this email already exists.')
            )
        return value

    def validate(self, attrs):
        if attrs.get('password') != attrs.get('password_confirm'):
            raise serializers.ValidationError(
                {"password_confirm": _("Passwords must match.")}
            )
        return attrs

    def create(self, validated_data):
        validated_data.pop('password_confirm')
        user = User.objects.create_user(**validated_data)
        return user


class CustomTokenObtainPairSerializer(TokenObtainPairSerializer):
    """
    Extends TokenObtainPairSerializer to include user data in login response (HU01_02).
    """
    @classmethod
    def get_token(cls, user):
        token = super().get_token(user)
        token['sid'] = token['jti']
        token['sv'] = user.session_version
        return token

    def validate(self, attrs):
        data = super().validate(attrs)
        data['user'] = {
            'id': self.user.id,
            'username': self.user.username,
            'email': self.user.email,
            'first_name': self.user.first_name,
            'last_name': self.user.last_name,
            'user_type': self.user.user_type,
            'is_verified': self.user.is_verified,
            'role': 'admin' if self.user.is_staff or self.user.is_superuser else 'user',
        }
        profile = UserProfileSerializer(self.user).data
        data['user'].update(avatar_url=profile['avatar_url'], avatar_version=profile['avatar_version'])
        data['message'] = _('Login successful.')
        return data

    def validate_username(self, value):
        if len(value) < 3:
            raise serializers.ValidationError(
                _('Username must be at least 3 characters.')
            )
        return value


class ChangePasswordSerializer(serializers.Serializer):
    """
    Serializer for password change (used in reset flow).
    """
    old_password = serializers.CharField(required=True, write_only=True)
    new_password = serializers.CharField(required=True, write_only=True, validators=[validate_password])
    new_password_confirm = serializers.CharField(required=True, write_only=True)

    def validate(self, attrs):
        if attrs.get('new_password') != attrs.get('new_password_confirm'):
            raise serializers.ValidationError(
                {"new_password_confirm": _("Passwords must match.")}
            )
        return attrs


class PasswordResetRequestSerializer(serializers.Serializer):
    """
    Serializer for password reset token request.
    """
    email = serializers.EmailField(required=True)


class PasswordResetConfirmSerializer(serializers.Serializer):
    """
    Serializer for confirming password reset with token.
    """
    token = serializers.CharField(required=True)
    new_password = serializers.CharField(required=True, write_only=True, validators=[validate_password])
    new_password_confirm = serializers.CharField(required=True, write_only=True)

    def validate(self, attrs):
        if attrs.get('new_password') != attrs.get('new_password_confirm'):
            raise serializers.ValidationError(
                {"new_password_confirm": _("Passwords must match.")}
            )
        return attrs


class UserProfileSerializer(serializers.ModelSerializer):
    """
    Serializer for user profile retrieval and update.
    """
    role = serializers.SerializerMethodField()
    avatar = serializers.FileField(write_only=True, required=False, allow_null=True)
    avatar_url = serializers.SerializerMethodField()
    avatar_version = serializers.SerializerMethodField()

    def get_avatar_url(self, user):
        avatar = getattr(user, 'avatar', None)
        return avatar.url if avatar else None

    def get_avatar_version(self, user):
        avatar = getattr(user, 'avatar', None)
        return avatar.sha256 if avatar else None

    def validate_avatar(self, value):
        if value is None:
            return None
        from .user_management import prepare_avatar
        return prepare_avatar(value)

    def update(self, instance, validated_data):
        from django.db import transaction
        from .models import UserAvatar
        import hashlib
        avatar = validated_data.pop('avatar', serializers.empty)
        with transaction.atomic():
            instance = User.objects.select_for_update().get(pk=instance.pk)
            instance = super().update(instance, validated_data)
            if avatar is None:
                UserAvatar.objects.filter(user=instance).delete()
            elif avatar is not serializers.empty:
                UserAvatar.objects.update_or_create(user=instance,
                    defaults={'content': avatar, 'sha256': hashlib.sha256(avatar).hexdigest()})
        return instance

    def get_role(self, user):
        return 'admin' if user.is_staff or user.is_superuser else 'user'

    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'first_name', 'last_name',
                  'phone_number', 'user_type', 'is_verified', 'created_at', 'role',
                  'avatar', 'avatar_url', 'avatar_version']
        read_only_fields = ['id', 'is_verified', 'created_at']


class CompanySerializer(serializers.ModelSerializer):
    """
    Serializer for S&P 500 company data.
    """
    sector_display = serializers.CharField(source='get_sector_display', read_only=True)
    has_annual_data = serializers.BooleanField(read_only=True, default=False)
    has_price_data = serializers.BooleanField(read_only=True, default=False)

    class Meta:
        model = Company
        fields = [
            'id', 'ticker', 'name', 'sector', 'sector_display', 'logo_url', 'is_index_member',
            'market_cap', 'per', 'roe', 'has_annual_data', 'has_price_data',
        ]


class SearchHistorySerializer(serializers.ModelSerializer):
    """
    Serializer for user search history.
    """
    class Meta:
        model = SearchHistory
        fields = ['id', 'query', 'created_at']
