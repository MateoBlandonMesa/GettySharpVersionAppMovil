# Funcionalidades Pendientes - Getty Sharp Mobile

## ✅ Completado

### Autenticación
- ✅ OAuth con Google (con deep linking)
- ✅ Manejo de sesión con DataStore
- ✅ Cerrar sesión
- ✅ Cargar perfil de usuario desde Supabase

### Pantallas Base
- ✅ Landing Screen
- ✅ Login Screen
- ✅ Dashboard Screen (estructura básica con navegación)
- ✅ Find Barbers Screen (lista de barberos)
- ✅ My Appointments Screen (lista básica)
- ✅ Profile Setup Screen (estructura básica)
- ✅ Barber Signup Screen (estructura básica)
- ✅ Edit Profile Screen (estructura básica)

---

## 🔴 Prioridad Alta - Funcionalidades Core

### 1. Dashboard - Mostrar Información del Usuario
**Estado**: Solo muestra tarjetas de navegación
**Falta**:
- Mostrar nombre del usuario
- Mostrar foto de perfil
- Mostrar si es barbero o cliente
- Mostrar información de barbero (si aplica): rating, especialidad, etc.
- Botones de acción según el rol (clientes vs barberos)

**Archivo**: `DashboardScreen.kt`

---

### 2. ProfileSetupScreen - Formulario Completo
**Estado**: Solo tiene campos básicos (nombre, apellido) sin funcionalidad
**Falta**:
- Campos completos:
  - Nombre ✅ (parcial)
  - Apellido ✅ (parcial)
  - Teléfono
  - Tipo de documento
  - Número de documento
  - Género
  - Dirección
  - Foto de perfil (subir imagen)
- Validación de campos
- Guardar en Supabase (tabla `tbl_usuarios`)
- Subir foto de perfil a Supabase Storage
- Manejo de errores

**Archivo**: `ProfileSetupScreen.kt`

---

### 3. EditProfileScreen - Edición de Perfil
**Estado**: Solo tiene el título, completamente vacío
**Falta**:
- Formulario completo similar a ProfileSetupScreen
- Cargar datos actuales del usuario
- Permitir editar todos los campos
- Guardar cambios en Supabase
- Actualizar foto de perfil

**Archivo**: `EditProfileScreen.kt`

---

### 4. BarberSignupScreen - Registro de Barbero
**Estado**: Solo tiene el título, completamente vacío
**Falta**:
- Formulario completo:
  - Nombre público (username)
  - Especialidad
  - Ubicación de trabajo (dirección con geocodificación)
  - Descripción
  - Foto de perfil profesional
  - Documentos de respaldo (fotos/documents)
- Validación
- Guardar en Supabase (tabla `tbl_profesionales`)
- Subir imágenes a Supabase Storage
- Manejo de estados de verificación

**Archivo**: `BarberSignupScreen.kt`

---

### 5. FindBarbersScreen - Google Maps
**Estado**: Solo muestra lista de barberos
**Falta**:
- Integrar Google Maps Compose
- Mostrar marcadores de barberos en el mapa
- Permitir alternar entre vista de lista y mapa
- Filtrar barberos por ubicación/ubicación del usuario
- Mostrar información al hacer clic en marcador

**Archivo**: `FindBarbersScreen.kt`
**Dependencias**: Agregar Google Maps Compose (actualmente comentado)

---

### 6. FindBarbersScreen - Diálogo de Agendamiento
**Estado**: Diálogo simplificado/placeholder
**Falta**:
- Seleccionar fecha y hora disponibles
- Mostrar disponibilidad del barbero
- Seleccionar ubicación de la cita
- Confirmar y crear la cita
- Integración con API de citas

**Archivo**: `FindBarbersScreen.kt`

---

## 🟡 Prioridad Media

### 7. MyAppointmentsScreen - Funcionalidades Completas
**Estado**: Lista básica implementada
**Falta**:
- Reagendar citas
- Ver detalles completos de la cita
- Calificar después de completar la cita
- Diferentes vistas para clientes vs barberos (gestión de citas para barberos)

**Archivo**: `MyAppointmentsScreen.kt`

---

### 8. Google Maps - Configuración
**Estado**: Dependencias comentadas
**Falta**:
- Agregar dependencias de Google Maps Compose
- Configurar API key en AndroidManifest.xml
- Agregar permisos de ubicación
- Obtener API key de Google Cloud Console

**Archivos**: 
- `app/build.gradle.kts`
- `AndroidManifest.xml`

---

## 🟢 Prioridad Baja - Mejoras y Features Adicionales

### 9. Sistema de Calificaciones
- Permitir a clientes calificar barberos después de citas completadas
- Mostrar calificaciones en perfil de barbero
- Promedio de calificaciones

---

### 10. Mejoras de UI/UX
- Diseño más pulido y consistente
- Animaciones y transiciones
- Manejo de estados de carga/error más robusto
- Dark mode completo
- Indicadores de carga en todas las pantallas

---

### 11. Notificaciones Push
- Configurar Firebase Cloud Messaging
- Notificaciones de nuevas citas
- Recordatorios de citas

---

### 12. Soporte Offline
- Cache local con Room
- Sincronización cuando vuelva la conexión

---

## 📝 Notas Técnicas

### Dependencias Pendientes
- Google Maps Compose (comentado en `build.gradle.kts`)
- Room Database (comentado, para cache offline)

### APIs/Backend Necesarios
- Verificar que todas las rutas del backend estén disponibles
- Endpoints de citas funcionando
- Endpoints de profesionales funcionando

### Supabase
- Storage configurado para imágenes
- Políticas de acceso configuradas
- Tablas verificadas

---

## 🎯 Próximos Pasos Recomendados

1. **Completar ProfileSetupScreen** - Es fundamental para que los usuarios puedan usar la app
2. **Mejorar Dashboard** - Mostrar información del usuario hace la app más funcional
3. **Implementar BarberSignupScreen** - Permite que barberos se registren
4. **Integrar Google Maps** - Feature principal de la app
5. **Completar diálogo de agendamiento** - Permite el flujo completo de reservar citas
