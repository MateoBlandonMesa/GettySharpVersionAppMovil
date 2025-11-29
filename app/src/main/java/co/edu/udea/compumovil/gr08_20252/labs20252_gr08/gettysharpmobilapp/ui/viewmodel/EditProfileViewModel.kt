package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val genders: List<Pair<Int, String>> = emptyList(),
    val currentProfile: UserProfile? = null,
    val errorMessage: String? = null,
    val success: Boolean = false
)

class EditProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()
    
    fun loadProfile(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val profile = withContext(Dispatchers.IO) {
                    AuthService.getUserProfile(context)
                }
                
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentProfile = profile,
                        errorMessage = null
                    )
                    
                    // Cargar géneros
                    loadGenders(context)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo cargar el perfil"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar perfil: ${e.message}"
                )
            }
        }
    }
    
    private fun loadGenders(context: Context) {
        viewModelScope.launch {
            try {
                val session = AuthService.getSession(context)
                val accessToken = session?.accessToken
                
                if (accessToken == null) {
                    _uiState.value = _uiState.value.copy(
                        genders = listOf(
                            Pair(1, "Masculino"),
                            Pair(2, "Femenino"),
                            Pair(3, "Otro")
                        )
                    )
                    return@launch
                }
                
                val result = withContext(Dispatchers.IO) {
                    SupabaseRestClient.getGenders(accessToken)
                }
                
                result.fold(
                    onSuccess = { genders ->
                        _uiState.value = _uiState.value.copy(genders = genders)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            genders = listOf(
                                Pair(1, "Masculino"),
                                Pair(2, "Femenino"),
                                Pair(3, "Otro")
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    genders = listOf(
                        Pair(1, "Masculino"),
                        Pair(2, "Femenino"),
                        Pair(3, "Otro")
                    )
                )
            }
        }
    }
    
    fun updateProfile(
        context: Context,
        phone: String? = null,
        address: String? = null,
        gender: String? = null,
        fotoPerfil: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, success = false)
            
            try {
                val session = AuthService.getSession(context)
                val accessToken = session?.accessToken
                val userId = session?.userId
                
                if (accessToken == null || userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "No se encontró la sesión. Por favor, inicia sesión nuevamente."
                    )
                    return@launch
                }
                
                // Find gender ID if gender is provided
                val genderId = gender?.let { gen ->
                    _uiState.value.genders.find { it.second.equals(gen, ignoreCase = true) }?.first
                }
                
                // Update user profile
                val updateResult = withContext(Dispatchers.IO) {
                    SupabaseRestClient.updateUserProfile(
                        userId = userId,
                        phone = phone,
                        address = address,
                        genderId = genderId,
                        fotoPerfil = fotoPerfil,
                        accessToken = accessToken
                    )
                }
                
                updateResult.fold(
                    onSuccess = {
                        // Reload profile to get updated data
                        val updatedProfile = withContext(Dispatchers.IO) {
                            SupabaseRestClient.getUserProfile(userId, accessToken)
                        }
                        
                        updatedProfile.fold(
                            onSuccess = { profile ->
                                profile?.let {
                                    AuthService.saveUserProfile(context, it)
                                    _uiState.value = _uiState.value.copy(
                                        currentProfile = it,
                                        isSaving = false,
                                        success = true,
                                        errorMessage = null
                                    )
                                } ?: run {
                                    _uiState.value = _uiState.value.copy(
                                        isSaving = false,
                                        success = true,
                                        errorMessage = null
                                    )
                                }
                            },
                            onFailure = {
                                _uiState.value = _uiState.value.copy(
                                    isSaving = false,
                                    success = true, // Update succeeded even if reload failed
                                    errorMessage = null
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Error al actualizar perfil: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }
}

