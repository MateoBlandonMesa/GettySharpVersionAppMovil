# Debugging OAuth - Problema de Redirección

## Problema Actual
- El navegador se queda abierto en la versión web
- No se cierra automáticamente
- La app se queda cargando cuando se cierra el navegador manualmente

## Cambios Implementados

### 1. Timeout Automático
- Se agregó un timeout de 5 minutos
- Si no hay respuesta del OAuth, se cancela automáticamente

### 2. Detección de Cancelación
- Cuando el usuario regresa a la app (onResume), se verifica si OAuth está en progreso
- Si después de 2 segundos no hay deep link, se cancela el loading

### 3. Manejo de Estados
- Se agregó rastreo del estado de OAuth en progreso
- Se limpian los estados cuando se cancela o completa

## Verificación de Configuración

### Paso 1: Verificar Redirect URL en Supabase

1. Ve a Supabase Dashboard → Authentication → URL Configuration
2. Verifica que `gettysharp://oauth/callback` esté en la lista de Redirect URLs
3. **IMPORTANTE**: Si no está, agrégalo y guarda

### Paso 2: Verificar la URL Generada

La URL que se genera debe verse así:
```
https://rnsivwuxmekckvqwonaw.supabase.co/auth/v1/authorize?provider=google&redirect_to=gettysharp%3A%2F%2Foauth%2Fcallback&apikey=...
```

### Paso 3: Verificar Deep Link en Android

Prueba manualmente si el deep link funciona:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "gettysharp://oauth/callback?code=test123"
```

Si esto abre la app, el intent-filter está funcionando correctamente.

## Solución al Problema de Redirección Web

Si Supabase está redirigiendo a la versión web en lugar del deep link, es porque:

1. **El redirect_to no está siendo respetado** - Supabase puede estar usando su configuración por defecto
2. **La URL no está en la lista de Redirect URLs permitidas**

### Solución Temporal: Usar WebView en lugar de CustomTabs

Si el problema persiste, podemos cambiar a usar un WebView personalizado que intercepte el redirect antes de que se abra en el navegador.

## Debugging

Para ver qué está pasando:

1. Revisa los logs de Android Studio cuando abres OAuth
2. Verifica qué URL está recibiendo Supabase
3. Verifica si el deep link está siendo interceptado (logs de MainActivity)

## Próximos Pasos

1. Verifica la configuración en Supabase Dashboard
2. Prueba el deep link manualmente con adb
3. Revisa los logs cuando intentas hacer login
4. Si el problema persiste, podemos implementar un WebView personalizado

