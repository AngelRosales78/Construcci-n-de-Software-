"""
Authentication serializers for the S&P 500 platform.

Uses DRF serializers with Yup-equivalent validation logic (pydantic-style
schema validation in Python) for all auth endpoints.
"""
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from django.contrib.auth.password_validation import validate_password
from django.utils.translation import gettext as _
from .models import User


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
    Allows login with either username or email.
    """
    @classmethod
    def get_token(cls, user):
        token = super().get_token(user)
        return token

    def validate(self, attrs):
        username = attrs.get('username', '')
        password = attrs.get('password', '')

        # Try to find user by username or email
        try:
            if '@' in username:
                user = User.objects.get(email=username)
            else:
                user = User.objects.get(username=username)
        except User.DoesNotExist:
            raise serializers.ValidationError(
                {'non_field_errors': _('Invalid credentials.')},
                code='authorization',
            )

        # Set the user and validate password
        if not user.check_password(password):
            raise serializers.ValidationError(
                {'non_field_errors': _('Invalid credentials.')},
                code='authorization',
            )

        # Generate tokens using the parent serializer's token generation
        refresh = self.get_token(user)

        data = {
            'refresh': str(refresh),
            'access': str(refresh.access_token),
            'user': {
                'id': user.id,
                'username': user.username,
                'email': user.email,
                'first_name': user.first_name,
                'last_name': user.last_name,
                'user_type': user.user_type,
                'is_verified': user.is_verified,
            },
            'message': _('Login successful.')
        }

        return data


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
