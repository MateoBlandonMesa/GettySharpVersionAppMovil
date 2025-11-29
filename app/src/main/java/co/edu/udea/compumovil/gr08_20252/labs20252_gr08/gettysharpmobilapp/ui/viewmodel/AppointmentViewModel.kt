package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Appointment
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.CreateAppointmentRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AppointmentUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val availability: List<AvailabilityBlock> = emptyList(),
    val errorMessage: String? = null,
    val success: Boolean = false
)

class AppointmentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()
    
    fun loadAvailability(professionalId: String, start: String? = null, end: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val response = ApiClient.service.getAvailability(
                    professionalId = professionalId,
                    start = start,
                    end = end
                )
                
                if (response.isSuccessful) {
                    val availabilityBlocks = response.body() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        availability = availabilityBlocks,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        availability = emptyList(),
                        errorMessage = "Error al cargar disponibilidad: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availability = emptyList(),
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun createAppointment(
        clientId: String,
        professionalId: String,
        startDate: String,
        endDate: String,
        availabilityBlockId: String?,
        locationId: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = null, success = false)
            
            try {
                val request = CreateAppointmentRequest(
                    idCliente = clientId,
                    idProfesional = professionalId,
                    fechaInicioCita = startDate,
                    fechaFinCita = endDate,
                    availabilityBlockId = availabilityBlockId,
                    ubicacionCita = locationId
                )
                
                val response = ApiClient.service.createAppointment(request)
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        success = true,
                        errorMessage = null
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        errorMessage = "Error al crear cita: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
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
    
    fun getAvailableSlots(): List<AvailabilityBlock> {
        return _uiState.value.availability.filter { 
            (it.status ?: "").lowercase() == "disponible" 
        }
    }
    
    fun formatAvailabilitySlot(slot: AvailabilityBlock): String {
        return try {
            val start = Instant.parse(slot.start)
            val end = Instant.parse(slot.end)
            
            val startLocal = LocalDateTime.ofInstant(start, java.time.ZoneId.systemDefault())
            val endLocal = LocalDateTime.ofInstant(end, java.time.ZoneId.systemDefault())
            
            val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            
            "${startLocal.format(dateFormatter)} - ${startLocal.format(timeFormatter)} a ${endLocal.format(timeFormatter)}"
        } catch (e: Exception) {
            "${slot.start} - ${slot.end}"
        }
    }
}

