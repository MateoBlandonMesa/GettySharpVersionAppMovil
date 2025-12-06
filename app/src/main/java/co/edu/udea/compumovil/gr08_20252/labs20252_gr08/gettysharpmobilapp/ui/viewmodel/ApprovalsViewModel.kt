package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.ProfessionalApproval
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.VerificationStatus
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

data class ApprovalsUiState(
    val isLoading: Boolean = false,
    val professionals: List<ProfessionalApproval> = emptyList(),
    val verificationStatuses: List<VerificationStatus> = emptyList(),
    val selectedStatusId: String? = null,
    val updatingProfessionalId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ApprovalsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ApprovalsUiState())
    val uiState: StateFlow<ApprovalsUiState> = _uiState.asStateFlow()
    
    private var currentApproverId: String? = null

    fun loadVerificationStatuses() {
        viewModelScope.launch {
            try {
                val response: Response<List<VerificationStatus>> = ApiClient.service.getVerificationStatuses()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        verificationStatuses = response.body() ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al cargar estados de verificación: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun loadProfessionalsForApproval(approverId: String, statusId: String? = null) {
        currentApproverId = approverId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val response: Response<List<ProfessionalApproval>> = ApiClient.service.getProfessionalsForApproval(
                    approverId = approverId,
                    statusId = statusId
                )
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        professionals = response.body() ?: emptyList(),
                        selectedStatusId = statusId,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        professionals = emptyList(),
                        errorMessage = "Error al cargar profesionales: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    professionals = emptyList(),
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun updateProfessionalStatus(approverId: String, professionalId: String, statusId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                updatingProfessionalId = professionalId,
                errorMessage = null
            )
            
            try {
                val response: Response<ProfessionalApproval> = ApiClient.service.updateProfessionalVerificationStatus(
                    professionalId = professionalId,
                    approverId = approverId,
                    request = mapOf("statusId" to statusId)
                )
                
                if (response.isSuccessful) {
                    // Reload professionals list
                    loadProfessionalsForApproval(approverId, _uiState.value.selectedStatusId)
                    _uiState.value = _uiState.value.copy(
                        updatingProfessionalId = null,
                        successMessage = "Estado actualizado correctamente"
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = _uiState.value.copy(
                        updatingProfessionalId = null,
                        errorMessage = "Error al actualizar estado: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    updatingProfessionalId = null,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun setSelectedStatus(statusId: String?) {
        _uiState.value = _uiState.value.copy(selectedStatusId = statusId)
        // Reload professionals with new filter
        currentApproverId?.let { approverId ->
            loadProfessionalsForApproval(approverId, statusId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

