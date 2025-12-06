package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseAuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils.CustomTabsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private var oauthTimeoutJob: Job? = null
    private var isOAuthInProgress = false
    
    fun checkAuthStatus(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val isAuthenticated = AuthService.isAuthenticated(context)
                
                if (isAuthenticated) {
                    val session = AuthService.getSession(context)
                    
                    // Always reload profile from Supabase to get enriched gender name
                    session?.userId?.let { userId ->
                        loadUserProfile(context, userId)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            userProfile = null,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        userProfile = null,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userProfile = null,
                    errorMessage = "Error al verificar sesión: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadUserProfile(context: Context, userId: String) {
        withContext(Dispatchers.IO) {
            // Obtener el access_token de la sesión guardada
            val session = AuthService.getSession(context)
            val accessToken = session?.accessToken
            
            if (accessToken == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se encontró el token de autenticación"
                )
                return@withContext
            }
            
            val result = SupabaseRestClient.getUserProfile(userId, accessToken)
            result.fold(
                onSuccess = { profile ->
                    profile?.let {
                        // Profile is already enriched with gender name from getUserProfile
                        // Usuario tiene perfil completo
                        AuthService.saveUserProfile(context, it)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userProfile = it,
                            errorMessage = null
                        )
                    } ?: run {
                        // Usuario autenticado pero sin perfil - necesita crear perfil
                        android.util.Log.d("AuthViewModel", "User authenticated but profile not found, redirecting to ProfileSetup")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true, // Mantener autenticado
                            userProfile = null, // Sin perfil aún
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        userProfile = null,
                        errorMessage = "Error al cargar perfil: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            // Cerrar cualquier sesión previa para forzar selección de cuenta
            withContext(Dispatchers.IO) {
                try {
                    val session = AuthService.getSession(context)
                    session?.accessToken?.let { token ->
                        SupabaseAuthService.signOut(token)
                    }
                    AuthService.clearSession(context)
                } catch (e: Exception) {
                    // Ignorar errores al cerrar sesión previa
                    android.util.Log.d("AuthViewModel", "No previous session to clear: ${e.message}")
                }
            }
            
            // Resetear el estado para forzar un nuevo inicio de sesión
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                errorMessage = null,
                isAuthenticated = false,
                userProfile = null
            )
            isOAuthInProgress = true
            
            try {
                // Obtener URL de OAuth
                val result = SupabaseAuthService.signInWithOAuth("google")
                result.fold(
                    onSuccess = { oauthUrl ->
                        // Abrir CustomTabs con la URL de OAuth
                        // Google requiere CustomTabs para OAuth, no WebView
                        CustomTabsHelper.openUrl(context, oauthUrl)
                        
                        // Iniciar timeout - si no hay respuesta en 5 minutos, cancelar
                        startOAuthTimeout()
                    },
                    onFailure = { error ->
                        isOAuthInProgress = false
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al iniciar OAuth: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                isOAuthInProgress = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
    
    private fun startOAuthTimeout() {
        oauthTimeoutJob?.cancel()
        oauthTimeoutJob = viewModelScope.launch {
            delay(5 * 60 * 1000L) // 5 minutos
            if (isOAuthInProgress) {
                cancelOAuth("Tiempo de espera agotado. Por favor, intenta de nuevo.")
            }
        }
    }
    
    fun cancelOAuth(message: String = "Autenticación cancelada") {
        oauthTimeoutJob?.cancel()
        oauthTimeoutJob = null
        isOAuthInProgress = false
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = message
        )
    }
    
    fun onResume() {
        // Si OAuth está en progreso y el usuario regresó sin completar, esperar un poco
        // para que el deep link pueda llegar antes de cancelar
        if (isOAuthInProgress && _uiState.value.isLoading) {
            android.util.Log.d("AuthViewModel", "onResume: OAuth in progress, waiting for deep link...")
            // Esperar un poco por si el deep link está por llegar
            viewModelScope.launch {
                delay(3000) // Esperar 3 segundos para dar tiempo al deep link
                // Si aún está cargando después de 3 segundos Y aún está en progreso, cancelar
                if (isOAuthInProgress && _uiState.value.isLoading) {
                    android.util.Log.w("AuthViewModel", "onResume: Timeout reached, canceling OAuth")
                    cancelOAuth()
                } else {
                    android.util.Log.d("AuthViewModel", "onResume: OAuth completed or no longer in progress")
                }
            }
        }
    }
    
    fun signInWithFacebook(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = SupabaseAuthService.signInWithOAuth("facebook")
                result.fold(
                    onSuccess = { oauthUrl ->
                        // Abrir CustomTabs con la URL de OAuth
                        CustomTabsHelper.openUrl(context, oauthUrl)
                        // No cambiar el estado aquí, esperaremos el callback
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al iniciar OAuth: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun handleOAuthCallback(context: Context, code: String, state: String? = null) {
        viewModelScope.launch {
            oauthTimeoutJob?.cancel()
            oauthTimeoutJob = null
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = SupabaseAuthService.handleOAuthCallback(code, state)
                result.fold(
                    onSuccess = { authResponse ->
                        if (authResponse.accessToken != null && authResponse.userId != null) {
                            // Guardar sesión
                            val session = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthSession(
                                userId = authResponse.userId,
                                email = authResponse.email,
                                accessToken = authResponse.accessToken,
                                refreshToken = authResponse.refreshToken,
                                expiresAt = authResponse.expiresIn?.let { 
                                    System.currentTimeMillis() + (it * 1000) 
                                }
                            )
                            
                            AuthService.saveSession(context, session)
                            
                            isOAuthInProgress = false
                            
                            // Cargar perfil del usuario
                            loadUserProfile(context, authResponse.userId)
                        } else {
                            isOAuthInProgress = false
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "No se pudo obtener información de autenticación"
                            )
                        }
                    },
                    onFailure = { error ->
                        isOAuthInProgress = false
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al completar autenticación: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                isOAuthInProgress = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun handleOAuthTokenDirectly(
        context: Context,
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long?
    ) {
        android.util.Log.d("AuthViewModel", "handleOAuthTokenDirectly called with accessToken length: ${accessToken.length}")
        viewModelScope.launch {
            oauthTimeoutJob?.cancel()
            oauthTimeoutJob = null
            
            isOAuthInProgress = false // Ya no está en progreso, estamos procesando el resultado
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                android.util.Log.d("AuthViewModel", "Calling SupabaseAuthService.handleOAuthTokenDirectly")
                val result = SupabaseAuthService.handleOAuthTokenDirectly(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn
                )
                
                result.fold(
                    onSuccess = { authResponse ->
                        if (authResponse.accessToken != null && authResponse.userId != null) {
                            // Guardar sesión
                            val session = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthSession(
                                userId = authResponse.userId,
                                email = authResponse.email,
                                accessToken = authResponse.accessToken,
                                refreshToken = authResponse.refreshToken,
                                expiresAt = authResponse.expiresIn?.let { 
                                    System.currentTimeMillis() + (it * 1000) 
                                } ?: expiresIn?.let {
                                    System.currentTimeMillis() + (it * 1000)
                                }
                            )
                            
                            AuthService.saveSession(context, session)
                            
                            isOAuthInProgress = false
                            
                            // Cargar perfil del usuario
                            loadUserProfile(context, authResponse.userId)
                        } else {
                            isOAuthInProgress = false
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "No se pudo obtener información de autenticación"
                            )
                        }
                    },
                    onFailure = { error ->
                        isOAuthInProgress = false
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al procesar token: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                isOAuthInProgress = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun handleOAuthError(error: String, errorDescription: String?) {
        viewModelScope.launch {
            oauthTimeoutJob?.cancel()
            oauthTimeoutJob = null
            isOAuthInProgress = false
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Error de OAuth: ${errorDescription ?: error}"
            )
        }
    }
    
    fun signOut(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val session = AuthService.getSession(context)
                session?.accessToken?.let { token ->
                    SupabaseAuthService.signOut(token)
                }
                
                AuthService.clearSession(context)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userProfile = null,
                    errorMessage = null
                )
            } catch (e: Exception) {
                // Aún así, limpiar la sesión local
                AuthService.clearSession(context)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userProfile = null,
                    errorMessage = null
                )
            }
        }
    }
}
