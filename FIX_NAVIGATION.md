# Solución para Error de Navigation Compose

Si encuentras el error "Unresolved reference 'navigation'" o "Unresolved reference 'rememberNavController'", sigue estos pasos:

## Pasos para solucionar:

1. **Sincroniza el proyecto con Gradle:**
   - En Android Studio: File → Sync Project with Gradle Files
   - O haz clic en el ícono de "Sync Now" que aparece en la barra superior

2. **Verifica que la dependencia esté correcta:**
   - El archivo `app/build.gradle.kts` debe contener:
   ```kotlin
   implementation("androidx.navigation:navigation-compose:2.8.4")
   ```

3. **Limpia y reconstruye el proyecto:**
   - Build → Clean Project
   - Build → Rebuild Project

4. **Si el problema persiste, invalida la caché:**
   - File → Invalidate Caches → Invalidate and Restart

5. **Verifica que los imports sean correctos:**
   ```kotlin
   import androidx.navigation.compose.rememberNavController
   import androidx.navigation.compose.NavHost
   import androidx.navigation.compose.composable
   ```

## Nota:
La dependencia de Navigation Compose ya está configurada en el proyecto. El error generalmente se debe a que el IDE no ha sincronizado correctamente con Gradle.


