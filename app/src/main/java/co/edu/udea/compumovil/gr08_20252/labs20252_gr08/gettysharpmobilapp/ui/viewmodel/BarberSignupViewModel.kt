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

data class WorkLocation(
    val id: Int,
    val lugar_de_trabajo: String
)

data class BarberSignupUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAlreadyProfessional: Boolean = false,
    val workLocations: List<WorkLocation> = emptyList(),
    val verificationStatuses: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val success: Boolean = false,
    val professionalId: String? = null
)

class BarberSignupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BarberSignupUiState())
    val uiState: StateFlow<BarberSignupUiState> = _uiState.asStateFlow()
    
    fun loadInitialData(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val session = AuthService.getSession(context)
                val accessToken = session?.accessToken
                val userId = session?.userId
                
                if (accessToken == null || userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se encontró la sesión. Por favor, inicia sesión nuevamente."
                    )
                    return@launch
                }
                
                // Load work locations (hardcoded for now, matching web version)
                val workLocations = listOf(
                    WorkLocation(1, "A Domicilio"),
                    WorkLocation(2, "En mi Establecimiento"),
                    WorkLocation(3, "Ambos")
                )
                
                // Check if already professional
                val checkResult = withContext(Dispatchers.IO) {
                    SupabaseRestClient.checkIfProfessionalExists(userId, accessToken)
                }
                
                checkResult.fold(
                    onSuccess = { exists ->
                        if (exists) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAlreadyProfessional = true,
                                errorMessage = null
                            )
                        } else {
                            // Load verification statuses
                            val statusResult = withContext(Dispatchers.IO) {
                                SupabaseRestClient.getVerificationStatuses(accessToken)
                            }
                            
                            statusResult.fold(
                                onSuccess = { statuses ->
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        workLocations = workLocations,
                                        verificationStatuses = statuses,
                                        errorMessage = null
                                    )
                                },
                                onFailure = {
                                    // Use empty statuses, will default to null
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        workLocations = workLocations,
                                        verificationStatuses = emptyMap(),
                                        errorMessage = null
                                    )
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al verificar perfil profesional: ${error.message}",
                            workLocations = workLocations // Still load work locations
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
    
    fun createBarberProfile(
        context: Context,
        username: String,
        specialty: String,
        customSpecialty: String?,
        workLocationLabel: String,
        description: String?,
        fotoPerfil: String?,
        documentationBase64: String?
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
                
                if (documentationBase64.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Debes adjuntar documentación de soporte."
                    )
                    return@launch
                }
                
                // Find work location ID
                val workLocation = _uiState.value.workLocations.find { it.lugar_de_trabajo == workLocationLabel }
                val workLocationId = workLocation?.id ?: 1
                
                // Use custom specialty if specialty is "otros"
                val finalSpecialty = if (specialty == "otros" && !customSpecialty.isNullOrBlank()) {
                    customSpecialty
                } else {
                    specialty
                }
                
                // Get revision status ID
                val revisionStatusId = _uiState.value.verificationStatuses["revision"]
                
                // Create professional profile
                val createResult = withContext(Dispatchers.IO) {
                    SupabaseRestClient.createProfessionalProfile(
                        userId = userId,
                        username = username,
                        specialty = finalSpecialty,
                        workLocationId = workLocationId,
                        description = description,
                        verificationStatusId = revisionStatusId,
                        documentationBase64 = documentationBase64,
                        accessToken = accessToken
                    )
                }
                
                createResult.fold(
                    onSuccess = { professionalId ->
                        // Update user profile with foto_perfil if provided
                        if (!fotoPerfil.isNullOrBlank()) {
                            val updateResult = withContext(Dispatchers.IO) {
                                SupabaseRestClient.updateUserProfile(
                                    userId = userId,
                                    fotoPerfil = fotoPerfil,
                                    accessToken = accessToken
                                )
                            }
                            
                            // Continue even if photo update fails
                            updateResult.fold(
                                onSuccess = { },
                                onFailure = {
                                    android.util.Log.w("BarberSignupViewModel", "Failed to update profile photo: ${it.message}")
                                }
                            )
                        }
                        
                        // Update local user profile
                        val currentProfile = AuthService.getUserProfile(context)
                        val updatedProfile = currentProfile?.copy(
                            isBarber = true,
                            professionalId = professionalId,
                            username = username,
                            specialty = finalSpecialty,
                            workLocation = workLocationLabel,
                            description = description,
                            fotoPerfil = fotoPerfil ?: currentProfile.fotoPerfil,
                            verified = false,
                            verificationStatus = "En revisión"
                        )
                        
                        updatedProfile?.let {
                            AuthService.saveUserProfile(context, it)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            success = true,
                            professionalId = professionalId,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Error al crear perfil profesional: ${error.message}"
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

