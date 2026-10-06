# Plataforma de Análisis Financiero S&P 500 con IA

## Descripción

Plataforma de análisis financiero del S&P 500 con inteligencia artificial. Incluye autenticación de usuarios, búsqueda de empresas, análisis financiero y dashboard interactivo.

## Características

### Épica 01 - Autenticación (Completada)
- Registro de usuarios
- Login con JWT
- Reset de contraseña
- Logout con token blacklist
- Refresh token

### Épica 03 - Búsqueda y Filtrado (Completada)
- Búsqueda por ticker/nombre con PostgreSQL Full-Text Search
- Autocompletado con Redis
- Filtros por sector
- Ordenamiento por Market Cap
- Historial de búsquedas

### Épica 04 - Perfil Visual y Fichas Técnicas (Completada)
- Salud financiera (semáforo)
- Ficha técnica (PER, ROE, Market Cap)
- Altman-Z Score
- Historial de precios con gráficos interactivos

## Tecnologías

### Backend
- Python 3.12
- Django 5.0
- Django REST Framework 3.15
- PostgreSQL 16
- Redis 7
- SimpleJWT

### Frontend
- React 18
- Vite 5
- React Router DOM 6
- Recharts 2.12
- Axios

### Mobile
- Kotlin
- Jetpack Compose
- Retrofit 2
- Hilt

## Instalación

### Backend
```bash
cd backend
pip install -r requirements.txt
python manage.py migrate
python manage.py seed_companies
python manage.py runserver
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Pruebas

### Backend (pytest)
```bash
cd backend
pytest
```

### Load Testing (Locust)
```bash
cd backend
locust -f locustfile.py
```

### E2E (Cypress)
```bash
cd frontend
npx cypress open
```

## Estructura del Proyecto

```
repo/
├── backend/          # API REST Django
├── frontend/         # Aplicación React
├── android/          # App Android
└── docker-compose.yml
```

## API Endpoints

### Autenticación
- POST /api/v1/auth/register/
- POST /api/v1/auth/token/
- POST /api/v1/auth/refresh/
- POST /api/v1/auth/logout/
- GET/PATCH /api/v1/auth/profile/

### Empresas
- GET /api/v1/companies/search/
- GET /api/v1/companies/autocomplete/
- GET /api/v1/companies/sectors/
- GET/POST /api/v1/companies/history/
- GET /api/v1/companies/<ticker>/overview/
- GET /api/v1/companies/<ticker>/history/
