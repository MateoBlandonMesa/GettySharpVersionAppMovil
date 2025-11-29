# Solución al Problema: Navegador se Cierra Antes de Iniciar Sesión

## Problema Identificado

Cuando el usuario intenta iniciar sesión, el navegador se cierra inmediatamente antes de que pueda completar el proceso de autenticación con Google. Esto sucede porque Supabase está redirigiendo al deep link `gettysharp://oauth/callback` demasiado pronto.

## Soluciones Implementadas

### 1. Intent-Filter Simplificado
- Cambiado de `pathPrefix="/callback"` a solo capturar cualquier ruta bajo `gettysharp://oauth`
- Esto permite más flexibilidad en el manejo del deep link

### 2. Parámetro skip_http_redirect
- Agregado `skip_http_redirect=false` para asegurar que el flujo HTTP se complete

## Verificaciones Necesarias

### 1. Verificar la URL Generada

Cuando hagas clic en "Continuar con Google", la URL que se abre debería verse así:
```
https://rnsivwuxmekckvqwonaw.supabase.co/auth/v1/authorize?provider=google&redirect_to=gettysharp%3A%2F%2Foauth%2Fcallback&apikey=...&skip_http_redirect=false
```

### 2. Verificar el Flujo Esperado

El flujo correcto debería ser:
1. Se abre CustomTabs con la URL de Supabase
2. Supabase redirige a Google para autenticación
3. Usuario se autentica en Google
4. Google redirige de vuelta a Supabase
5. Supabase procesa y luego redirige a `gettysharp://oauth/callback?code=xxx`
6. Android intercepta el deep link y abre la app

### 3. Si el Problema Persiste

Si el navegador todavía se cierra antes de completar el login, puede ser que:

**Opción A: Supabase necesita una URL HTTP intermedia**
- Puede ser necesario usar una URL HTTP que luego redirija al deep link
- Ejemplo: `https://tu-dominio.com/auth/callback` que luego redirige a `gettysharp://oauth/callback`

**Opción B: Usar un WebView en lugar de CustomTabs**
- Un WebView personalizado puede interceptar los redirects antes de que se ejecuten
- Esto nos da más control sobre el flujo

**Opción C: Verificar configuración de Google OAuth**
- Asegúrate de que en Google Cloud Console, el redirect URI incluya tanto la URL de Supabase como el deep link

## Debugging

Para ver qué está pasando:

1. **Habilita los logs** en `SupabaseAuthService.kt` (ya está habilitado)
2. **Revisa los logs** en Android Studio cuando intentas hacer login
3. **Verifica la URL** que se está abriendo en CustomTabs
4. **Revisa si el deep link está siendo interceptado** demasiado pronto

## Próximo Paso Recomendado

Si el problema persiste después de estos cambios, recomiendo implementar un **WebView personalizado** que pueda:
- Interceptar todos los redirects
- Manejar el flujo OAuth completamente dentro de la app
- Solo abrir el deep link después de que el usuario se haya autenticado correctamente

¿Quieres que implemente el WebView personalizado?

