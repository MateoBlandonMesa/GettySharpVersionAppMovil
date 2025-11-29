# Configuración OAuth Google - Solución al Error 403

## ✅ Cambios Realizados

1. **Removido WebView** (bloqueado por Google)
2. **Volvimos a CustomTabs** (requerido por Google)
3. **Agregado tag `<queries>`** en AndroidManifest.xml para Android 11+

## ⚠️ IMPORTANTE: Verificaciones Necesarias

### 1. En Supabase Dashboard

1. Ve a **Authentication → URL Configuration**
2. En **Redirect URLs**, asegúrate de tener EXACTAMENTE:
   ```
   gettysharp://oauth/callback
   ```
3. También verifica la **Site URL** - puede que necesite estar configurada

### 2. En Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Selecciona tu proyecto
3. Ve a **APIs & Services → Credentials**
4. Encuentra tu **OAuth 2.0 Client ID** (el que usas con Supabase)
5. En **Authorized redirect URIs**, debe estar:
   ```
   https://rnsivwuxmekckvqwonaw.supabase.co/auth/v1/callback
   ```

### 3. El Problema del Navegador que se Cierra

Si el navegador (CustomTabs) se cierra inmediatamente después de abrirse, puede ser porque:

**Opción A: Supabase está redirigiendo al deep link demasiado pronto**

Esto puede pasar si:
- El usuario ya tiene una sesión activa en Supabase
- Hay algún problema con la configuración del redirect

**Solución Temporal:**
- Intenta cerrar sesión de Google en tu dispositivo
- O usa una cuenta diferente para probar
- O verifica si hay cookies/sesiones activas en el navegador

**Opción B: El deep link está siendo interceptado antes de tiempo**

Si el deep link se ejecuta inmediatamente, puede que necesitemos:
- Agregar un delay
- O manejar el redirect de manera diferente

## 🔍 Debugging

Para ver qué está pasando:

1. **Revisa los logs** en Android Studio (Logcat)
   - Filtra por: `SupabaseAuthService` o `OAuthDeepLinkHandler`
   - Busca la URL que se genera

2. **Prueba la URL manualmente**
   - Copia la URL que aparece en los logs
   - Ábrela en un navegador de escritorio
   - Observa qué sucede

3. **Verifica el flujo esperado:**
   - La URL debe llevar a Supabase
   - Supabase debe redirigir a Google
   - Google debe permitirte autenticarte
   - Google redirige a Supabase
   - Supabase redirige a `gettysharp://oauth/callback`

## 📝 Próximos Pasos

1. Verifica la configuración en Supabase Dashboard
2. Verifica la configuración en Google Cloud Console
3. Desinstala y reinstala la app
4. Intenta hacer login y observa qué sucede
5. Comparte los logs si el problema persiste

## 💡 Nota Importante

Google requiere **CustomTabs** para OAuth por seguridad. No podemos usar WebView. Si CustomTabs se cierra inmediatamente, el problema es probablemente la configuración o cómo Supabase maneja el redirect.

