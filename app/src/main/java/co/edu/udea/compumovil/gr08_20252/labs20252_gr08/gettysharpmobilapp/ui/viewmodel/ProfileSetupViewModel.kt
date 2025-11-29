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

data class ProfileSetupUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val genders: List<Pair<Int, String>> = emptyList(),
    val errorMessage: String? = null,
    val success: Boolean = false
)

class ProfileSetupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()
    
    fun loadGenders(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val session = AuthService.getSession(context)
                val accessToken = session?.accessToken
                
                if (accessToken == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se encontró el token de autenticación"
                    )
                    return@launch
                }
                
                val result = withContext(Dispatchers.IO) {
                    SupabaseRestClient.getGenders(accessToken)
                }
                
                result.fold(
                    onSuccess = { genders ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            genders = genders,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al cargar géneros: ${error.message}",
                            genders = listOf(
                                Pair(1, "Masculino"),
                                Pair(2, "Femenino"),
                                Pair(3, "Otro")
                            ) // Fallback
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}",
                    genders = listOf(
                        Pair(1, "Masculino"),
                        Pair(2, "Femenino"),
                        Pair(3, "Otro")
                    ) // Fallback
                )
            }
        }
    }
    
    fun createProfile(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        idType: String,
        idNumber: String,
        gender: String,
        address: String
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
                
                // Find gender ID
                val genderId = _uiState.value.genders.find { it.second.equals(gender, ignoreCase = true) }?.first
                    ?: 1
                
                // Create user profile
                val createResult = withContext(Dispatchers.IO) {
                    SupabaseRestClient.createUserProfile(
                        userId = userId,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        idType = idType,
                        idNumber = idNumber,
                        genderId = genderId,
                        address = address,
                        accessToken = accessToken
                    )
                }
                
                createResult.fold(
                    onSuccess = {
                        // Save location (simplified - using address geocoding)
                        // For now, we'll skip geocoding and save with default coordinates
                        val locationResult = withContext(Dispatchers.IO) {
                            SupabaseRestClient.saveOrUpdateUserLocation(
                                userId = userId,
                                latitude = 0.0, // Will be geocoded later
                                longitude = 0.0,
                                address = address,
                                accessToken = accessToken
                            )
                        }
                        
                        locationResult.fold(
                            onSuccess = {
                                // Update local profile
                                val newProfile = UserProfile(
                                    id = userId,
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    phone = phone,
                                    idType = idType,
                                    idNumber = idNumber,
                                    gender = gender,
                                    address = address
                                )
                                
                                AuthService.saveUserProfile(context, newProfile)
                                
                                _uiState.value = _uiState.value.copy(
                                    isSaving = false,
                                    success = true,
                                    errorMessage = null
                                )
                            },
                            onFailure = { locationError ->
                                // Profile created but location failed - still consider success
                                android.util.Log.w("ProfileSetupViewModel", "Profile created but location failed: ${locationError.message}")
                                val newProfile = UserProfile(
                                    id = userId,
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    phone = phone,
                                    idType = idType,
                                    idNumber = idNumber,
                                    gender = gender,
                                    address = address
                                )
                                
                                AuthService.saveUserProfile(context, newProfile)
                                
                                _uiState.value = _uiState.value.copy(
                                    isSaving = false,
                                    success = true,
                                    errorMessage = null
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Error al crear perfil: ${error.message}"
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

