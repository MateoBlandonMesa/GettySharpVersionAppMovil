# Implementación Completa de Funcionalidades - Getty Sharp Mobile App

## Estado Actual

### ✅ Completado
- Login con OAuth (funciona)
- Dependencias de Google Maps configuradas
- API Key de Google Maps configurada: `AIzaSyCrNKWjwKQweBoLVaJTi_eYHaHbFeKSMKI`
- Estructura básica de navegación
- Pantallas básicas creadas

### 🔨 En Progreso / Pendiente

#### 1. Dashboard Screen
**Funcionalidades requeridas:**
- Mostrar perfil completo del usuario
- Información personal (nombre, email, teléfono, dirección)
- Información profesional (si es barbero): especialidad, calificaciones, estado de verificación
- Acciones rápidas según rol:
  - Cliente: Buscar Barberos, Mis Citas
  - Barbero: Panel de citas, Publicar disponibilidad, Buscar Barberos, Mis Citas
  - Aprobador: Gestionar Verificaciones
- Botón para editar perfil
- Botón para cerrar sesión

#### 2. Find Barbers Screen
**Funcionalidades requeridas:**
- **Google Maps** mostrando ubicaciones de barberos verificados
- Marcadores en el mapa para cada barbero
- Marcador de ubicación del cliente (si tiene permisos de ubicación)
- Al hacer clic en un barbero:
  - Dialog mostrando información del barbero
  - Ver disponibilidad publicada
  - Seleccionar horario disponible
  - Elegir lugar de la cita (domicilio del cliente o local del profesional)
  - Confirmar y crear cita

#### 3. My Appointments Screen
**Funcionalidades requeridas:**
- Para clientes:
  - Ver citas pendientes
  - Cancelar citas
  - Reagendar citas
  - Ver historial de citas completadas
  - Calificar citas completadas (1-5 estrellas + comentario)
- Para barberos:
  - Panel de gestión con tabs:
    - Citas activas: tabla con citas pendientes/confirmadas
    - Historial: citas completadas/canceladas
  - Cambiar estado de citas
  - Ver información del cliente

#### 4. Edit Profile Screen
**Funcionalidades requeridas:**
- Editar información personal: teléfono, género, dirección
- Para barberos:
  - Editar nombre de usuario público
  - Editar especialidad
  - Editar lugar de trabajo
  - Subir/cambiar foto de perfil
  - Gestionar bloques de disponibilidad:
    - Crear bloques disponibles
    - Eliminar bloques
    - Ver bloques publicados

#### 5. Approvals Screen (NUEVA - No existe)
**Funcionalidades requeridas:**
- Solo visible para usuarios con rol de aprobador
- Listar profesionales pendientes de verificación
- Filtrar por estado (todos, en revisión, verificado, rechazado)
- Para cada profesional:
  - Ver información completa
  - Ver documentación adjunta
  - Botones: Marcar Verificado / Marcar Rechazado

#### 6. Barber Signup Screen
**Funcionalidades requeridas:**
- Formulario de registro profesional
- Nombre de usuario público
- Especialidad
- Descripción de servicios
- Lugar de trabajo
- Foto de perfil
- Documentación de soporte (requerida)

## APIs y Servicios Necesarios

### Backend API (ya existe en getty-sharp-hub/backend)
- `/api/appointments` - CRUD de citas
- `/api/professionals/{id}/schedule/availability` - Gestión de disponibilidad
- `/api/ratings/...` - Sistema de calificaciones
- `/api/approvals/...` - Sistema de aprobaciones
- `/api/geocoding` - Geocodificación de direcciones

### Supabase REST API
- Tablas principales:
  - `tbl_usuarios`
  - `tbl_profesionales`
  - `tbl_citas`
  - `tbl_ubicacion_usuarios`
  - `tbl_estados`
  - `tbl_calificaciones`

## Archivos a Crear/Modificar

### Nuevos Archivos Necesarios:
1. `ApprovalsScreen.kt` - Pantalla de aprobaciones
2. `ApprovalsViewModel.kt` - ViewModel para aprobaciones
3. `RatingScreen.kt` o Dialog - Para calificar citas
4. `AvailabilityManagementScreen.kt` o Dialog - Gestión de disponibilidad
5. `GoogleMapView.kt` - Componente de mapa reutilizable
6. `AppointmentBookingDialog.kt` - Dialog para agendar citas

### Archivos a Completar:
1. `DashboardScreen.kt` - Completar con toda la funcionalidad
2. `FindBarbersScreen.kt` - Agregar Google Maps y booking
3. `MyAppointmentsScreen.kt` - Agregar todas las funcionalidades
4. `EditProfileScreen.kt` - Completar edición y disponibilidad

### Modelos de Datos:
- Ya existen: `UserProfile`, `Barber`, `Appointment`, `AvailabilityBlock`
- Agregar si falta: `Rating`, `VerificationStatus`, `ProfessionalApproval`

### Servicios:
- `RatingService.kt` - Para calificaciones
- `ApprovalsService.kt` - Para aprobaciones
- Extender `ApiService.kt` con endpoints faltantes

## Pasos de Implementación Recomendados

1. ✅ Configurar Google Maps (YA HECHO)
2. Agregar pantalla de Approvals (nueva funcionalidad)
3. Completar Dashboard con información completa
4. Implementar Find Barbers con Google Maps
5. Implementar booking de citas
6. Completar My Appointments con todas las funciones
7. Completar Edit Profile con gestión de disponibilidad
8. Agregar sistema de calificaciones

## Notas Importantes

- El API key de Google Maps ya está configurado
- La base de datos está en Supabase (misma que la web)
- El backend API debe estar corriendo en `http://10.0.2.2:5000` (emulador) o IP local (dispositivo físico)
- Todas las funcionalidades de la web deben estar disponibles en la móvil
- Para mapas, usar Google Maps SDK en lugar de OpenStreetMap (que usa la web)

