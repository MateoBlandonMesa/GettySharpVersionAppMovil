package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.Normalizer

data class BarberSignupUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val userProfile: UserProfile? = null,
    val username: String = "",
    val specialty: String = "",
    val customSpecialty: String = "",
    val description: String = "",
    val workLocation: String = "",
    val profileImageBase64: String? = null,
    val profileImageUrl: String? = null,
    val documentationBase64: String? = null,
    val documentationFileName: String? = null,
    val workLocations: List<Map<String, Any?>> = emptyList(),
    val verificationStatuses: Map<String, String> = emptyMap(), // Map<key, statusId>
    val isAlreadyProfessional: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BarberSignupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BarberSignupUiState())
    val uiState: StateFlow<BarberSignupUiState> = _uiState.asStateFlow()
    
    private val specialties = listOf(
        "barberia" to "Barbería Tradicional",
        "estilista" to "Estilista",
        "cejas" to "Cejas y Depilación",
        "masajes" to "Masajes",
        "otros" to "Otros"
    )
    
    fun getSpecialties(): List<Pair<String, String>> = specialties
    
    fun loadInitialData(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val session = AuthService.getSession(context)
                if (session == null || session.userId == null || session.accessToken == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Usuario no autenticado. Por favor inicia sesión."
                        )
                    }
                    return@launch
                }
                
                // Load user profile
                val profileResult = SupabaseRestClient.getUserProfile(session.userId, session.accessToken)
                val profile = profileResult.getOrNull()
                
                if (profile == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Perfil de usuario no encontrado. Completa tu perfil primero."
                        )
                    }
                    return@launch
                }
                
                // Check if already professional
                val existsResult = SupabaseRestClient.checkIfProfessionalExists(session.userId, session.accessToken)
                val exists = existsResult.getOrNull() ?: false
                
                if (exists) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isAlreadyProfessional = true,
                            errorMessage = "Ya eres un profesional registrado."
                        )
                    }
                    return@launch
                }
                
                // Load work locations
                val workLocationsResult = SupabaseRestClient.getWorkLocations(session.accessToken)
                val workLocations = workLocationsResult.getOrNull() ?: emptyList()
                
                // Load verification statuses
                val statusesResult = SupabaseRestClient.getVerificationStatuses(session.accessToken)
                val statusesList = statusesResult.getOrNull() ?: emptyList()
                
                val statusMap = mutableMapOf<String, String>()
                statusesList.forEach { status ->
                    val id = status["id"]?.toString() ?: return@forEach
                    val name = (status["nombre_estado"] as? String) ?: return@forEach
                    val normalized = normalizeStatusName(name)
                    
                    when {
                        normalized.contains("revision") || normalized.contains("pendiente") -> {
                            statusMap["revision"] = id
                        }
                        normalized.contains("verificado") -> {
                            statusMap["verificado"] = id
                        }
                        normalized.contains("rechazado") -> {
                            statusMap["rechazado"] = id
                        }
                    }
                }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userProfile = profile,
                        workLocations = workLocations,
                        verificationStatuses = statusMap,
                        errorMessage = null
                    )
                }
                
            } catch (e: Exception) {
                Log.e("BarberSignupViewModel", "Error loading initial data: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error desconocido al cargar datos."
                    )
                }
            }
        }
    }
    
    private fun normalizeStatusName(name: String): String {
        return Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
    }
    
    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }
    
    fun updateSpecialty(specialty: String) {
        _uiState.update { 
            it.copy(
                specialty = specialty,
                customSpecialty = if (specialty != "otros") "" else it.customSpecialty
            )
        }
    }
    
    fun updateCustomSpecialty(customSpecialty: String) {
        _uiState.update { it.copy(customSpecialty = customSpecialty) }
    }
    
    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }
    
    fun updateWorkLocation(workLocation: String) {
        _uiState.update { it.copy(workLocation = workLocation) }
    }
    
    fun setProfileImage(base64: String?, url: String? = null) {
        _uiState.update { 
            it.copy(
                profileImageBase64 = base64,
                profileImageUrl = url ?: base64
            )
        }
    }
    
    fun setDocumentation(base64: String?, fileName: String? = null) {
        _uiState.update { 
            it.copy(
                documentationBase64 = base64,
                documentationFileName = fileName
            )
        }
    }
    
    fun convertImageToBase64(uri: Uri, context: Context): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            val outputStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e("BarberSignupViewModel", "Error converting image to base64: ${e.message}", e)
            null
        }
    }
    
    fun convertDocumentToBase64(uri: Uri, context: Context): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            bytes?.let {
                val base64 = Base64.encodeToString(it, Base64.NO_WRAP)
                // Determine MIME type from URI
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                "data:$mimeType;base64,$base64"
            }
        } catch (e: Exception) {
            Log.e("BarberSignupViewModel", "Error converting document to base64: ${e.message}", e)
            null
        }
    }
    
    fun submitProfessionalProfile(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            
            try {
                val session = AuthService.getSession(context)
                if (session == null || session.userId == null || session.accessToken == null) {
                    _uiState.update { 
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Usuario no autenticado. Por favor inicia sesión."
                        )
                    }
                    return@launch
                }
                
                val state = _uiState.value
                
                // Validations
                if (state.username.isBlank()) {
                    _uiState.update { 
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "El nombre de usuario público es requerido."
                        )
                    }
                    return@launch
                }
                
                if (state.specialty.isBlank()) {
                    _uiState.update { 
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Debes seleccionar una especialidad."
                        )
                    }
                    return@launch
                }
                
                if (state.specialty == "otros" && state.customSpecialty.isBlank()) {
                    _uiState.update { 
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Debes especificar tu especialidad personalizada."
                        )
                    }
                    return@launch
                }
                
                if (state.workLocation.isBlank()) {
                    _uiState.update { 
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Debes seleccionar un lugar de trabajo."
                        )
                    }
                    return@launch
                }
                
                if (state.documentationBase64 == null) {
                    _uiState.update { 
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Debes adjuntar documentación de soporte."
                        )
                    }
                    return@launch
                }
                
                // Get specialty value
                val specialtyValue = if (state.specialty == "otros") {
                    state.customSpecialty
                } else {
                    state.specialty
                }
                
                // Get work location ID
                val selectedLocation = state.workLocations.firstOrNull { location ->
                    location["lugar_de_trabajo"]?.toString() == state.workLocation
                }
                val workLocationId = (selectedLocation?.get("id") as? Number)?.toInt() ?: 1
                
                // Get verification status ID (revision/pending)
                val revisionStatusId = state.verificationStatuses["revision"]
                
                // Create professional profile
                val createResult = SupabaseRestClient.createProfessionalProfile(
                    userId = session.userId,
                    accessToken = session.accessToken,
                    username = state.username,
                    specialty = specialtyValue,
                    workLocationId = workLocationId,
                    description = state.description.takeIf { it.isNotBlank() },
                    verificationStatusId = revisionStatusId,
                    documentationBase64 = state.documentationBase64
                )
                
                val professionalData = createResult.getOrNull()
                    ?: throw Exception(createResult.exceptionOrNull()?.message ?: "Error al crear perfil profesional")
                
                // Update user profile image if provided
                if (state.profileImageBase64 != null || state.profileImageUrl != null) {
                    val imageToUpdate = state.profileImageBase64 ?: state.profileImageUrl
                    if (imageToUpdate != null) {
                        val updateResult = SupabaseRestClient.updateUserProfileImage(
                            userId = session.userId,
                            accessToken = session.accessToken,
                            imageBase64 = imageToUpdate
                        )
                        
                        if (updateResult.isFailure) {
                            Log.w("BarberSignupViewModel", "Failed to update profile image: ${updateResult.exceptionOrNull()?.message}")
                        }
                    }
                }
                
                // Update local user profile
                val updatedProfile = state.userProfile?.copy(
                    isBarber = true,
                    professionalId = professionalData["id"]?.toString(),
                    username = state.username,
                    specialty = specialtyValue,
                    workLocation = workLocationId.toString(),
                    description = state.description,
                    verified = false,
                    verificationStatus = revisionStatusId,
                    fotoPerfil = state.profileImageBase64 ?: state.profileImageUrl
                )
                
                if (updatedProfile != null) {
                    AuthService.saveUserProfile(context, updatedProfile)
                }
                
                _uiState.update { 
                    it.copy(
                        isSubmitting = false,
                        successMessage = "¡Perfil de barbero creado exitosamente! Tu perfil está siendo revisado."
                    )
                }
                
                // Call success callback after a short delay
                kotlinx.coroutines.delay(500)
                onSuccess()
                
            } catch (e: Exception) {
                Log.e("BarberSignupViewModel", "Error submitting professional profile: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Error desconocido al crear perfil profesional."
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

