package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

object ThemePreferenceManager {
    private val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
    
    /**
     * Obtiene el estado actual del tema como Flow (true = oscuro, false = claro)
     * Si no está configurado, retorna null para usar el tema del sistema
     */
    fun getThemePreference(context: Context): Flow<Boolean?> {
        return context.themeDataStore.data.map { preferences ->
            preferences[IS_DARK_MODE_KEY]
        }
    }
    
    /**
     * Obtiene el valor actual del tema de forma síncrona
     * Retorna null si no está configurado (usa tema del sistema)
     */
    suspend fun getThemePreferenceSync(context: Context): Boolean? {
        return context.themeDataStore.data.map { preferences ->
            preferences[IS_DARK_MODE_KEY]
        }.first()
    }
    
    /**
     * Guarda la preferencia de tema del usuario
     * @param isDarkMode true para modo oscuro, false para modo claro, null para usar tema del sistema
     */
    suspend fun saveThemePreference(context: Context, isDarkMode: Boolean?) {
        context.themeDataStore.edit { preferences ->
            if (isDarkMode != null) {
                preferences[IS_DARK_MODE_KEY] = isDarkMode
            } else {
                preferences.remove(IS_DARK_MODE_KEY)
            }
        }
    }
}

