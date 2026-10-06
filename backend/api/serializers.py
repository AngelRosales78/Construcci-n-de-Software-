"""
Authentication serializers for the S&P 500 platform.

Uses DRF serializers with Yup-equivalent validation logic (pydantic-style
schema validation in Python) for all auth endpoints.
"""
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from django.contrib.auth.password_validation import validate_password
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
        }
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
    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'first_name', 'last_name',
                  'phone_number', 'user_type', 'is_verified', 'created_at']
        read_only_fields = ['id', 'is_verified', 'created_at']


class CompanySerializer(serializers.ModelSerializer):
    """
    Serializer for S&P 500 company data.
    """
    sector_display = serializers.CharField(source='get_sector_display', read_only=True)

    class Meta:
        model = Company
        fields = [
            'id', 'ticker', 'name', 'sector', 'sector_display',
            'market_cap', 'per', 'roe',
            'working_capital', 'total_assets', 'retained_earnings',
            'ebit', 'total_liabilities', 'sales', 'equity_value',
        ]


class SearchHistorySerializer(serializers.ModelSerializer):
    """
    Serializer for user search history.
    """
    class Meta:
        model = SearchHistory
        fields = ['id', 'query', 'created_at']
