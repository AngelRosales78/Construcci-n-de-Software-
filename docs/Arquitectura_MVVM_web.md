# MVVM en la aplicación web

Actualización del martes 13 de octubre de 2026. Rama: Magallanes.

La interfaz React utiliza Model–View–ViewModel. Se conservaron los componentes visuales y los contratos de la API; el estado, las peticiones y las acciones se separaron en once ViewModels implementados como hooks React. La extracción de autenticación reutiliza el servicio existente, sin duplicar sus reglas ni endpoints.

## Responsabilidades y dirección de dependencias

```text
View: components/*.jsx
        ↓ invoca un hook y vincula estado/acciones a la pantalla
ViewModel: viewmodels/use*ViewModel.js
        ↓ consulta repositorios y actualiza estado observable con hooks
Model: models/*Repository.js
        ↓ reutiliza services/api.js, authService y almacenamiento local
API Django → PostgreSQL / Redis
```

**Model:** representa el acceso a los datos y los contratos de empresas, precios, historial y sesión. Los repositorios reciben o devuelven los objetos de dominio proporcionados por la API. `companyRepository` convierte las respuestas HTTP a datos; `historyRepository` centraliza HTTP y la copia de historial por cuenta; `authRepository` reutiliza el servicio de autenticación existente. Las reglas financieras y validaciones persistentes continúan en el backend: MVVM organiza el cliente, no sustituye las capas del servidor.

**ViewModel:** guarda estado como resultados, carga, errores, filtros, rango y sesión, y expone acciones. No devuelve JSX ni importa componentes. Se implementa mediante hooks: React observa sus cambios de estado y vuelve a representar la vista. Es MVVM adaptado a React, con estado y acciones vinculados explícitamente mediante props y eventos; no se afirma que React incorpore automáticamente MVVM.

**View:** presenta JSX y conecta eventos a las acciones expuestas. No consulta Axios, repositorios ni LocalStorage. El formato monetario, marcado de coincidencias, configuración de Highcharts y fallo visual de una imagen son presentación; permanecen en los componentes. La fórmula Altman-Z sigue calculándose en el backend.

## Mapa de archivos para la exposición

| Función | View | ViewModel | Model |
|---|---|---|---|
| Buscar, filtrar y paginar | components/search/CompanySearch.jsx | viewmodels/useCompanySearchViewModel.js | models/companyRepository.js; models/historyRepository.js |
| Sugerir y manejar teclado/debounce | components/search/SearchBar.jsx | viewmodels/useSearchBarViewModel.js | models/companyRepository.js |
| Seleccionar sector y orden | components/search/CompanyFilters.jsx | viewmodels/useCompanyFiltersViewModel.js | models/companyRepository.js |
| Ver y borrar historial | components/search/SearchHistory.jsx | viewmodels/useSearchHistoryViewModel.js | models/historyRepository.js |
| Consultar ficha | components/company/CompanyDashboard.jsx | viewmodels/useCompanyDashboardViewModel.js | models/companyRepository.js |
| Consultar precios por rango | components/company/PriceChart.jsx | viewmodels/usePriceChartViewModel.js | models/companyRepository.js |
| Iniciar sesión | components/auth/Login.jsx | viewmodels/useLoginViewModel.js | models/authRepository.js |
| Registrar | components/auth/Register.jsx | viewmodels/useRegisterViewModel.js | models/authRepository.js |
| Solicitar restablecimiento | components/auth/ForgotPassword.jsx | viewmodels/useForgotPasswordViewModel.js | models/authRepository.js |
| Confirmar restablecimiento | components/auth/ResetPassword.jsx | viewmodels/useResetPasswordViewModel.js | models/authRepository.js |
| Mostrar cuenta y cerrar sesión | components/layout/WorkspaceLayout.jsx | viewmodels/useSessionViewModel.js | models/authRepository.js |

Las rutas de la tabla parten de `frontend/src/`. Las clases Python de `backend/api/models.py` representan las tablas del servidor; no deben confundirse con los repositorios Model del cliente.

## Ejemplo que se puede explicar mostrando código

1. Abrir `CompanyDashboard.jsx`: obtiene `company`, `loading` y `error` de `useCompanyDashboardViewModel`; muestra la pantalla según esos valores.
2. Abrir `useCompanyDashboardViewModel.js`: consulta cuando cambia el ticker, llama a `companyRepository.summary`, conserva el estado y descarta respuestas cuando la consulta ya no está activa.
3. Abrir `companyRepository.js`: `summary(ticker)` solicita `/companies/{ticker}/summary/` mediante el servicio HTTP compartido y devuelve los datos.
4. Abrir `backend/api/views.py`: se consultan métricas/precios relacionados y se calcula Altman-Z. La respuesta recorre las mismas capas en sentido inverso.

Frase para la exposición: «La vista representa datos y eventos; el ViewModel mantiene estado y acciones; el Model accede a los datos. La vista no realiza peticiones directamente. React actualiza el JSX cuando cambia el estado del ViewModel».

## Verificación y límites

El martes 13 pasaron 33 pruebas frontend y la compilación de producción. El recorrido en navegador terminó sin errores JavaScript; el gráfico de 1254 puntos se cargó en 558,17 ms en esta medición local.

`viewmodels/Mvvm.test.jsx` comprueba estado y acciones sin montar las pantallas, respuestas fuera de orden y errores del repositorio. `viewmodels/Architecture.test.jsx` impide dependencias directas de las vistas hacia HTTP, servicios de autenticación y almacenamiento, y comprueba que los once ViewModels dependan del Model.

Se conserva la regresión de búsqueda, historial, métricas y gráficos. El recorrido real de login, filtros, sugerencias, ficha, precios, renovación y logout se acredita en `docs/evidencias/integracion_mvvm_13_oct.json`. La compilación sigue utilizando Vite. Este cambio no certifica Android ni corrige por sí mismo los límites del backend de autenticación o la entrega de correos; separa y conserva los contratos existentes.
