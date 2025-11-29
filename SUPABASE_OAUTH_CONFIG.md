# Configuración de OAuth en Supabase

## ⚠️ IMPORTANTE: Configuración Requerida

Para que el OAuth funcione correctamente en la app móvil y redirija de vuelta a la app en lugar de quedarse en el navegador, debes configurar lo siguiente en Supabase:

### 1. Agregar Redirect URL en Supabase Dashboard

1. Ve a tu proyecto en [Supabase Dashboard](https://supabase.com/dashboard)
2. Navega a **Authentication** → **URL Configuration**
3. En la sección **Redirect URLs**, agrega:
   ```
   gettysharp://oauth/callback
   ```
4. **Guarda los cambios**

### 2. Verificar que Google OAuth esté Configurado

1. Ve a **Authentication** → **Providers**
2. Asegúrate de que **Google** esté habilitado
3. Verifica que tengas configuradas las credenciales OAuth:
   - Client ID
   - Client Secret
   - Redirect URL (debe incluir `gettysharp://oauth/callback`)

### 3. Cómo Funciona el Flujo

1. **Usuario hace clic en "Continuar con Google"**
   - La app genera una URL OAuth con `redirect_to=gettysharp://oauth/callback`
   - Se abre en Chrome CustomTabs (navegador integrado)

2. **Usuario se autentica con Google**
   - Google verifica las credenciales
   - Redirige a Supabase con el código de autorización

3. **Supabase procesa y redirige**
   - Supabase intercambia el código
   - Redirige a `gettysharp://oauth/callback?code=xxx`
   - **IMPORTANTE**: Si esta URL no está en la lista de Redirect URLs de Supabase, el redirect fallará

4. **Android intercepta el deep link**
   - El sistema Android detecta el esquema `gettysharp://`
   - Abre la app con el intent que contiene el código
   - La app procesa el código y completa el login

### 4. Solución de Problemas

#### Problema: El navegador se queda abierto en lugar de regresar a la app

**Causa**: La URL `gettysharp://oauth/callback` no está configurada en Supabase o el intent-filter no está funcionando.

**Solución**:
1. Verifica que `gettysharp://oauth/callback` esté en la lista de Redirect URLs
2. Verifica que el AndroidManifest.xml tenga el intent-filter configurado
3. Prueba el deep link manualmente:
   ```bash
   adb shell am start -W -a android.intent.action.VIEW -d "gettysharp://oauth/callback?code=test"
   ```

#### Problema: Error "redirect_uri_mismatch"

**Causa**: La URL de redirect no coincide con la configurada en Supabase o en Google OAuth.

**Solución**:
1. En Supabase Dashboard, asegúrate de que `gettysharp://oauth/callback` esté en Redirect URLs
2. En Google Cloud Console, agrega `gettysharp://oauth/callback` como redirect URI autorizado

#### Problema: El código OAuth no se procesa

**Causa**: El deep link no está siendo interceptado correctamente.

**Solución**:
1. Verifica que `android:launchMode="singleTask"` esté en el AndroidManifest
2. Verifica que el intent-filter tenga todas las categorías correctas
3. Limpia y reconstruye la app después de cambiar el manifest

### 5. Verificación Final

Después de configurar todo:

1. **Limpia y reconstruye la app**
2. **Desinstala la versión anterior** (para limpiar los intent filters)
3. **Instala la nueva versión**
4. **Prueba el login**:
   - Debería abrirse Chrome CustomTabs
   - Después de autenticarte, debería cerrarse y regresar a la app automáticamente
   - Deberías quedar autenticado

### 6. Notas Adicionales

- El deep link `gettysharp://oauth/callback` es específico para esta app
- Si cambias el esquema, también debes actualizarlo en:
  - `SupabaseAuthService.kt` (línea 36)
  - `AndroidManifest.xml` (línea 46-48)
  - Supabase Dashboard (Redirect URLs)

