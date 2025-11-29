package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull

data class AuthSession(
    val userId: String? = null,
    val email: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val userProfileJson: String? = null
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session")

object AuthService {
    private const val USER_ID_KEY = "user_id"
    private const val EMAIL_KEY = "email"
    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val EXPIRES_AT_KEY = "expires_at"
    private const val USER_PROFILE_KEY = "user_profile"
    private const val IS_AUTHENTICATED_KEY = "is_authenticated"
    
    private val userIdKey = stringPreferencesKey(USER_ID_KEY)
    private val emailKey = stringPreferencesKey(EMAIL_KEY)
    private val accessTokenKey = stringPreferencesKey(ACCESS_TOKEN_KEY)
    private val refreshTokenKey = stringPreferencesKey(REFRESH_TOKEN_KEY)
    private val expiresAtKey = stringPreferencesKey(EXPIRES_AT_KEY)
    private val userProfileKey = stringPreferencesKey(USER_PROFILE_KEY)
    private val isAuthenticatedKey = booleanPreferencesKey(IS_AUTHENTICATED_KEY)
    
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_KEY
    private val gson = Gson()
    
    suspend fun saveSession(context: Context, session: AuthSession) {
        context.dataStore.edit { preferences ->
            session.userId?.let { preferences[userIdKey] = it }
            session.email?.let { preferences[emailKey] = it }
            session.accessToken?.let { preferences[accessTokenKey] = it }
            session.refreshToken?.let { preferences[refreshTokenKey] = it }
            session.expiresAt?.let { preferences[expiresAtKey] = it.toString() }
            session.userProfileJson?.let { preferences[userProfileKey] = it }
            preferences[isAuthenticatedKey] = session.userId != null
        }
    }
    
    suspend fun getSession(context: Context): AuthSession? {
        val preferences = context.dataStore.data.first()
        
        val userId = preferences[userIdKey]
        val email = preferences[emailKey]
        val accessToken = preferences[accessTokenKey]
        val refreshToken = preferences[refreshTokenKey]
        val expiresAtStr = preferences[expiresAtKey]
        val userProfileJson = preferences[userProfileKey]
        val isAuthenticated = preferences[isAuthenticatedKey] ?: false
        
        if (!isAuthenticated || userId == null) {
            return null
        }
        
        return AuthSession(
            userId = userId,
            email = email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAtStr?.toLongOrNull(),
            userProfileJson = userProfileJson
        )
    }
    
    suspend fun getUserProfile(context: Context): UserProfile? {
        val session = getSession(context) ?: return null
        val profileJson = session.userProfileJson ?: return null
        
        return try {
            gson.fromJson(profileJson, UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun saveUserProfile(context: Context, profile: UserProfile) {
        val session = getSession(context) ?: return
        val profileJson = gson.toJson(profile)
        
        saveSession(context, session.copy(userProfileJson = profileJson))
    }
    
    suspend fun clearSession(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(userIdKey)
            preferences.remove(emailKey)
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
            preferences.remove(expiresAtKey)
            preferences.remove(userProfileKey)
            preferences[isAuthenticatedKey] = false
        }
    }
    
    suspend fun isAuthenticated(context: Context): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[isAuthenticatedKey] ?: false
    }
    
    fun getOAuthUrl(provider: String): String {
        // Para OAuth, necesitaremos un deep link URL scheme
        // Por ahora, retornamos la URL de Supabase OAuth
        val redirectUrl = "gettysharp://oauth/callback"
        return "$supabaseUrl/auth/v1/authorize?provider=$provider&redirect_to=$redirectUrl"
    }
}

