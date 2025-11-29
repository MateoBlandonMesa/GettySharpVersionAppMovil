# Getty Sharp - Versión Móvil

Aplicación móvil Android para buscar y agendar citas de barbería, desarrollada con Jetpack Compose y Kotlin.

## Funcionalidades Implementadas

### Estructura Base ✅
- ✅ Configuración del proyecto Android con Jetpack Compose
- ✅ Sistema de navegación con Navigation Compose
- ✅ Estructura de modelos de datos
- ✅ Servicios para Supabase y API REST
- ✅ Pantallas principales (básicas)

### Pantallas Creadas
1. **Landing Screen** - Pantalla de inicio
2. **Login Screen** - Autenticación (Google/Facebook - requiere configuración adicional)
3. **Dashboard Screen** - Panel principal con acceso a funciones
4. **Find Barbers Screen** - Búsqueda de barberos (preparada para mapa)
5. **My Appointments Screen** - Gestión de citas
6. **Profile Setup Screen** - Configuración inicial de perfil
7. **Barber Signup Screen** - Registro como barbero
8. **Edit Profile Screen** - Edición de perfil

### Dependencias Configuradas
- Jetpack Compose (Material 3)
- Navigation Compose
- Supabase SDK (Postgres, Auth, Storage)
- Retrofit + Gson (API REST)
- Google Maps Compose
- Coil (carga de imágenes)
- Coroutines

## Configuración Necesaria

### 1. Google Maps API Key
Edita `app/src/main/AndroidManifest.xml` y reemplaza `YOUR_MAPS_API_KEY` con tu clave de API de Google Maps.

### 2. API Backend
El backend debe estar corriendo en `http://localhost:5000` (emulador) o configurar la IP para dispositivos físicos en:
- `app/src/main/java/.../data/services/ApiService.kt` - línea `BASE_URL`

### 3. Supabase
Las credenciales ya están configuradas en:
- `app/src/main/java/.../data/services/SupabaseService.kt`

### 4. OAuth (Autenticación Social)
La autenticación con Google/Facebook requiere configuración adicional:
- Deep linking setup en Android
- Configuración en Supabase Dashboard
- Ver `AuthViewModel.kt` para más detalles

## Próximos Pasos

### Funcionalidades Pendientes
1. **Autenticación completa** - Implementar OAuth correctamente
2. **Mapa de barberos** - Integrar Google Maps con marcadores
3. **Sistema de citas completo** - CRUD completo de citas
4. **Gestión de disponibilidad** - Para barberos
5. **Calificaciones** - Sistema de reseñas
6. **Notificaciones** - Push notifications
7. **Offline support** - Cache local con Room

### Mejoras de UI
- Diseño más pulido y consistente
- Animaciones y transiciones
- Dark mode completo
- Manejo de estados de carga y error

## Estructura del Proyecto

```
app/src/main/java/co/edu/udea/compumovil/gr08_20252/labs20252_gr08/gettysharpmobilapp/
├── data/
│   ├── models/          # Modelos de datos
│   └── services/        # Servicios (Supabase, API)
├── navigation/          # Navegación
├── ui/
│   ├── screens/         # Pantallas
│   └── viewmodel/       # ViewModels
└── MainActivity.kt      # Actividad principal
```

## Icono de la Aplicación

Para configurar el icono desde el archivo `.ico`:
1. Convierte el `.ico` a diferentes resoluciones PNG
2. Colócalos en las carpetas `mipmap-*` correspondientes
3. O usa herramientas como Android Asset Studio

## Notas Importantes

- El proyecto está configurado para Android SDK 24+ (Android 7.0+)
- Usa Kotlin 2.0.21
- Compose BOM 2024.09.00
- Las pantallas actuales son versiones básicas que deben expandirse con la lógica completa


