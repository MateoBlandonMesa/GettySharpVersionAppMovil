package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Appointment
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

data class AppointmentsUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val errorMessage: String? = null
)

class AppointmentsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()
    
    private fun formatErrorMessage(e: Exception, defaultMessage: String): String {
        return when {
            e.message?.contains("Failed to connect", ignoreCase = true) == true || 
            e.message?.contains("timeout", ignoreCase = true) == true ||
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            e.message?.contains("Connection refused", ignoreCase = true) == true ->
                "No se pudo conectar al servidor. Verifica tu conexión a internet y que el backend esté disponible en https://getty-sharp-hub.onrender.com"
            else -> e.message ?: defaultMessage
        }
    }
    
    fun loadClientAppointments(clientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val response: Response<List<Appointment>> = ApiClient.service.getAppointmentsByClient(clientId)
                if (response.isSuccessful) {
                    val appointments = response.body() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = appointments,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = emptyList(),
                        errorMessage = "Error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    appointments = emptyList(),
                    errorMessage = formatErrorMessage(e, "Error desconocido al cargar las citas")
                )
            }
        }
    }
    
    fun loadProfessionalAppointments(professionalId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val response: Response<List<Appointment>> = ApiClient.service.getAppointmentsByProfessional(professionalId)
                if (response.isSuccessful) {
                    val appointments = response.body() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = appointments,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = emptyList(),
                        errorMessage = "Error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    appointments = emptyList(),
                    errorMessage = formatErrorMessage(e, "Error desconocido al cargar las citas del profesional")
                )
            }
        }
    }
    
    fun createAppointment(
        clientId: String,
        professionalId: String,
        startDate: String,
        endDate: String,
        availabilityBlockId: String? = null,
        locationId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val request = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.CreateAppointmentRequest(
                    idCliente = clientId,
                    idProfesional = professionalId,
                    fechaInicioCita = startDate,
                    fechaFinCita = endDate,
                    availabilityBlockId = availabilityBlockId,
                    ubicacionCita = locationId
                )
                
                val response: Response<Appointment> = ApiClient.service.createAppointment(request)
                if (response.isSuccessful) {
                    val newAppointment = response.body()
                    if (newAppointment != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            appointments = _uiState.value.appointments + newAppointment,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al crear cita: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatErrorMessage(e, "Error desconocido al crear la cita")
                )
            }
        }
    }
    
    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val response: Response<Unit> = ApiClient.service.cancelAppointment(appointmentId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = _uiState.value.appointments.filter { it.id != appointmentId },
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cancelar cita: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatErrorMessage(e, "Error desconocido al cancelar la cita")
                )
            }
        }
    }
    
    fun rescheduleAppointment(appointmentId: String, newStart: String, newEnd: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Ensure ISO format (DateTime in C# backend expects ISO 8601)
                val startIso = try {
                    java.time.Instant.parse(newStart).toString()
                } catch (e: Exception) {
                    newStart
                }
                val endIso = try {
                    java.time.Instant.parse(newEnd).toString()
                } catch (e: Exception) {
                    newEnd
                }
                
                // Backend JSON serializer converts to camelCase
                val request = mapOf(
                    "newStart" to startIso,
                    "newEnd" to endIso
                )
                
                val response: Response<Appointment> = ApiClient.service.rescheduleAppointment(appointmentId, request)
                if (response.isSuccessful) {
                    val updatedAppointment = response.body()
                    if (updatedAppointment != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            appointments = _uiState.value.appointments.map {
                                if (it.id == appointmentId) updatedAppointment else it
                            },
                            errorMessage = null
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al reagendar cita: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = formatErrorMessage(e, "Error desconocido al reagendar la cita")
                )
            }
        }
    }
}

