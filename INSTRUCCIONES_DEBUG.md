# Instrucciones para Debugging OAuth

## Problema Actual
El navegador se cierra antes de que el usuario pueda iniciar sesión con Google.

## Pasos para Debugging

### 1. Revisa los Logs

He agregado logs detallados para ayudar a diagnosticar el problema. Abre Android Studio y:

1. Conecta tu dispositivo o inicia el emulador
2. Abre la pestaña **Logcat**
3. Filtra por el tag: `SupabaseAuthService` o `OAuthDeepLinkHandler`
4. Intenta hacer login y observa los logs

### 2. Verifica la URL que se Abre

Cuando hagas clic en "Continuar con Google", busca en los logs una línea como:
```
OAuth URL generated: https://rnsivwuxmekckvqwonaw.supabase.co/auth/v1/authorize?...
```

Copia esa URL completa y:
- Verifica que incluya `redirect_to=gettysharp%3A%2F%2Foauth%2Fcallback`
- Verifica que incluya `provider=google`
- Verifica que incluya `apikey=...`

### 3. Prueba la URL Manualmente

1. Abre esa URL en un navegador de tu computadora
2. Observa qué pasa:
   - ¿Te lleva a Google para iniciar sesión?
   - ¿Redirige inmediatamente al deep link?
   - ¿Muestra algún error?

### 4. Verifica el Deep Link

Prueba si el deep link funciona manualmente:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "gettysharp://oauth/callback?code=test123"
```

Si esto abre tu app, el intent-filter está funcionando.

### 5. Revisa la Configuración de Supabase

Ve a Supabase Dashboard y verifica:

1. **Authentication → URL Configuration**:
   - `gettysharp://oauth/callback` debe estar en la lista de Redirect URLs
   - También puede estar en Site URL si aplica

2. **Authentication → Providers → Google**:
   - Verifica que Google esté habilitado
   - Verifica las credenciales OAuth
   - En Google Cloud Console, el redirect URI debe incluir la URL de Supabase

## Posibles Causas

### Causa 1: Supabase Redirige Inmediatamente
Si Supabase detecta que el redirect_to es un deep link, puede redirigir inmediatamente sin completar OAuth.

**Solución**: Podríamos necesitar usar una URL HTTP intermedia que luego redirija al deep link.

### Causa 2: Configuración Incorrecta en Supabase
La URL `gettysharp://oauth/callback` no está correctamente configurada.

**Solución**: Verifica que esté exactamente como se muestra en Redirect URLs.

### Causa 3: El Navegador Intercepta el Deep Link Muy Pronto
El navegador puede estar interceptando el deep link antes de que el usuario se autentique.

**Solución**: Podríamos necesitar usar un WebView personalizado.

## Próximos Pasos

1. **Ejecuta los pasos de debugging arriba**
2. **Comparte los logs** que veas cuando intentas hacer login
3. **Comparte la URL** que se genera
4. Con esa información, podremos determinar la mejor solución

## Solución Temporal: WebView Personalizado

Si el problema persiste, podemos implementar un WebView personalizado que:
- Intercepta todos los redirects
- Permite que el usuario complete el login
- Solo ejecuta el deep link después de que el usuario se haya autenticado

¿Quieres que implemente esta solución ahora, o primero quieres intentar el debugging?

