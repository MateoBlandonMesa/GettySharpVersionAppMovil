# Configuración de OAuth con Deep Linking

## ✅ Implementado

### 1. AndroidManifest.xml
- ✅ Intent filter agregado para manejar deep links `gettysharp://oauth/callback`
- ✅ La Activity está configurada para recibir deep links

### 2. Dependencias
- ✅ `androidx.browser:browser:1.9.0` agregada para CustomTabs

### 3. Utilidades Creadas
- ✅ **CustomTabsHelper**: Abre URLs en Chrome CustomTabs
- ✅ **OAuthDeepLinkHandler**: Extrae parámetros del callback OAuth
- ✅ **OAuthManager**: Gestiona el flujo de OAuth entre Activity y ViewModel

### 4. Servicios
- ✅ **SupabaseAuthService**: Genera URLs de OAuth y maneja callbacks
- ✅ **AuthService**: Guarda y carga sesiones usando DataStore

### 5. ViewModel y UI
- ✅ **AuthViewModel**: Maneja autenticación completa
- ✅ **LoginScreen**: Integrado con CustomTabs para abrir OAuth
- ✅ **MainActivity**: Procesa deep links en onCreate y onNewIntent

## 📋 Cómo Funciona

1. **Usuario hace clic en "Iniciar con Google/Facebook"**
   - Se genera la URL de OAuth de Supabase
   - Se abre en CustomTabs (Chrome integrado)

2. **Usuario se autentica en el navegador**
   - Google/Facebook redirige a Supabase
   - Supabase redirige a `gettysharp://oauth/callback?code=xxx`

3. **Android intercepta el deep link**
   - El intent filter captura la URL
   - MainActivity procesa el callback
   - Se extrae el código OAuth

4. **Intercambio de código por tokens**
   - AuthViewModel intercambia el código por access_token y refresh_token
   - Se guarda la sesión en DataStore
   - Se carga el perfil del usuario

5. **Redirección**
   - Si tiene perfil completo → Dashboard
   - Si no tiene perfil → ProfileSetup

## 🔧 Configuración Adicional Necesaria en Supabase

1. **Agregar Redirect URL en Supabase Dashboard**:
   - Ve a Authentication → URL Configuration
   - Agrega: `gettysharp://oauth/callback` a las "Redirect URLs"

2. **Verificar OAuth Providers**:
   - Asegúrate de que Google y Facebook estén habilitados
   - Configura las credenciales OAuth en Supabase Dashboard

## 🧪 Pruebas

1. Ejecuta la app
2. Ve a la pantalla de Login
3. Haz clic en "Continuar con Google" o "Continuar con Facebook"
4. Se abrirá CustomTabs con el login
5. Después de autenticarte, volverás a la app automáticamente
6. La sesión se guardará y estarás autenticado

## 📝 Notas

- El deep link `gettysharp://oauth/callback` ya está configurado
- CustomTabs se abre automáticamente cuando haces clic en los botones
- El flujo completo está implementado y listo para usar
- Solo falta configurar las URLs de redirect en Supabase Dashboard

