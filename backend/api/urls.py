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
    GET /api/v1/companies/search/        - Company search
    GET/POST /api/v1/companies/history/  - Search history
"""
from django.urls import path
from . import views
from .pillar_views import CompanyPillarsView

app_name = 'auth'

urlpatterns = [
    path('companies/<str:ticker>/pillars/', CompanyPillarsView.as_view(), name='company-pillars'),
    path('auth/admin/summary/', views.AdminSummaryView.as_view(), name='admin-summary'),
    # Contrato de Rosales reutilizado; se conservan las rutas anteriores de Magallanes.
    path('auth/register/', views.RegisterView.as_view(), name='web-register'),
    path('auth/token/', views.CustomTokenObtainPairView.as_view(), name='web-token'),
    path('auth/refresh/', views.CustomTokenRefreshView.as_view(), name='web-refresh'),
    path('auth/logout/', views.LogoutView.as_view(), name='web-logout'),
    path('auth/password/reset/', views.PasswordResetRequestView.as_view(), name='web-password-reset'),
    path('auth/password/reset/confirm/', views.PasswordResetConfirmView.as_view(), name='web-reset-confirm'),
    path('auth/profile/', views.UserProfileView.as_view(), name='web-profile'),
    path('auth/change-password/', views.ChangePasswordView.as_view(), name='web-change-password'),
    # HU01_01: Registration
    path('register/', views.RegisterView.as_view(), name='register'),

    # HU01_02: Login
    path('token/', views.CustomTokenObtainPairView.as_view(), name='token'),

    # HU01_05: Token Refresh
    path('refresh/', views.CustomTokenRefreshView.as_view(), name='refresh'),

    # HU01_04: Logout
    path('logout/', views.LogoutView.as_view(), name='logout'),

    # HU01_03: Password Reset
    path('password/reset/', views.PasswordResetRequestView.as_view(), name='password-reset'),
    path('password/reset/confirm/', views.PasswordResetConfirmView.as_view(), name='password-reset-confirm'),

    # Change Password
    path('change-password/', views.ChangePasswordView.as_view(), name='change-password'),

    # User Profile
    path('profile/', views.UserProfileView.as_view(), name='profile'),

    path('search/', views.CompanySearchView.as_view(), name='search'),
    path('search/autocomplete/', views.CompanyAutocompleteView.as_view(), name='autocomplete'),
    path('search/history/', views.SearchHistoryView.as_view(), name='history'),
    path('search/history/clear/', views.SearchHistoryDeleteView.as_view(), name='history-clear'),
    path('search/history/<int:history_id>/', views.SearchHistoryDeleteView.as_view(), name='history-delete'),
    path('companies/history/clear/', views.SearchHistoryDeleteView.as_view(), name='company-history-clear'),
    path('companies/history/<int:history_id>/', views.SearchHistoryDeleteView.as_view(), name='company-history-delete'),

    # HU03_01: Company Search
    path('companies/search/', views.CompanySearchView.as_view(), name='company-search'),

    # HU03_02: Autocomplete
    path('companies/autocomplete/', views.CompanyAutocompleteView.as_view(), name='company-autocomplete'),

    # HU03_03: Sectors List
    path('companies/sectors/', views.CompanySectorsView.as_view(), name='company-sectors'),

    # HU03_05: Search History
    path('companies/history/', views.SearchHistoryView.as_view(), name='search-history'),

    path('companies/<str:ticker>/summary/', views.CompanySummaryView.as_view(), name='company-summary'),

    # HU04_02/03/04: Company Overview
    path('companies/<str:ticker>/overview/', views.CompanyOverviewView.as_view(), name='company-overview'),

    # HU04_04: Company History
    path('companies/<str:ticker>/history/', views.CompanyHistoryView.as_view(), name='company-history'),
]
