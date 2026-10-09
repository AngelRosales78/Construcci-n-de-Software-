"""
URL configuration for the authentication API.

Endpoints:
    POST /api/v1/auth/register/          - User registration
    POST /api/v1/auth/token/             - Login (JWT)
    POST /api/v1/auth/refresh/           - Token refresh
    POST /api/v1/auth/logout/            - Logout (blacklist)
    POST /api/v1/auth/password/reset/    - Password reset request
    POST /api/v1/auth/password/reset/confirm/ - Password reset confirm
    PATCH /api/v1/auth/change-password/  - Change password
    GET/PATCH /api/v1/auth/profile/      - User profile
"""
from django.urls import path
from . import views

app_name = 'auth'

urlpatterns = [
    # HU01_01: Registration
    path('auth/register/', views.RegisterView.as_view(), name='register'),

    # HU01_02: Login
    path('auth/token/', views.CustomTokenObtainPairView.as_view(), name='token'),

    # HU01_05: Token Refresh
    path('auth/refresh/', views.CustomTokenRefreshView.as_view(), name='refresh'),

    # HU01_04: Logout
    path('auth/logout/', views.LogoutView.as_view(), name='logout'),

    # HU01_03: Password Reset
    path('auth/password/reset/', views.PasswordResetRequestView.as_view(), name='password-reset'),
    path('auth/password/reset/confirm/', views.PasswordResetConfirmView.as_view(), name='password-reset-confirm'),

    # Change Password
    path('auth/change-password/', views.ChangePasswordView.as_view(), name='change-password'),

    # User Profile
    path('auth/profile/', views.UserProfileView.as_view(), name='profile'),
]
