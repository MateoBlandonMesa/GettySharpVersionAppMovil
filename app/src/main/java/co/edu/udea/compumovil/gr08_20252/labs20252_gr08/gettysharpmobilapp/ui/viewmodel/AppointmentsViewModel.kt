package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.*
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.*

data class AppointmentsUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val history: List<ClientHistoryItem> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val errorMessage: String? = null,
    val userRole: UserRole = UserRole.Client,
    val isManageMode: Boolean = false,
    val appointmentStatuses: List<AppointmentStatusOption> = emptyList(),
    val isLoadingStatuses: Boolean = false,
    val updatingAppointmentId: String? = null
)

sealed class UserRole {
    object Client : UserRole()
    object Barber : UserRole()
}

class AppointmentsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()
    
    fun loadAppointments(context: Context, mode: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val session = AuthService.getSession(context) ?: return@launch
                val profile = AuthService.getUserProfile(context) ?: return@launch
                
                if (profile.id == null) return@launch
                
                val isBarber = profile.isBarber && profile.professionalId != null
                val isManageMode = mode == "manage" && isBarber
                
                _uiState.value = _uiState.value.copy(
                    userRole = if (isBarber) UserRole.Barber else UserRole.Client,
                    isManageMode = isManageMode
                )
                
                if (isBarber && isManageMode) {
                    // Load professional appointments for management
                    profile.professionalId?.let { professionalId ->
                        loadProfessionalAppointments(professionalId)
                        loadAppointmentStatuses()
                    }
                } else if (isBarber) {
                    // Load client appointments for barber viewing their own appointments
                    loadClientAppointments(profile.id!!)
                    loadClientHistory(profile.id!!)
                } else {
                    // Load client appointments
                    loadClientAppointments(profile.id!!)
                    loadClientHistory(profile.id!!)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error loading appointments", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    private fun loadClientAppointments(clientId: String) {
        viewModelScope.launch {
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
                android.util.Log.e("AppointmentsViewModel", "Error loading client appointments", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    appointments = emptyList(),
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    private fun loadProfessionalAppointments(professionalId: String) {
        viewModelScope.launch {
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
                android.util.Log.e("AppointmentsViewModel", "Error loading professional appointments", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    appointments = emptyList(),
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun loadClientHistory(clientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
            try {
                val response: Response<List<Map<String, Any>>> = ApiClient.service.getClientHistory(clientId)
                if (response.isSuccessful) {
                    val historyData = response.body() ?: emptyList()
                    val history = historyData.mapNotNull { map ->
                        try {
                            val ratingMap = map["rating"] as? Map<*, *>
                            val rating = ratingMap?.let {
                                RatingRecord(
                                    id = it["id"]?.toString() ?: "",
                                    professionalId = it["professionalId"]?.toString() ?: "",
                                    score = (it["score"] as? Number)?.toInt() ?: 0,
                                    comment = it["comment"]?.toString(),
                                    createdAt = it["createdAt"]?.toString()
                                )
                            }
                            
                            ClientHistoryItem(
                                appointmentId = map["appointmentId"]?.toString() ?: "",
                                professionalId = map["professionalId"]?.toString() ?: "",
                                professionalName = map["professionalName"]?.toString(),
                                professionalPublicName = map["professionalPublicName"]?.toString(),
                                start = map["start"]?.toString() ?: "",
                                end = map["end"]?.toString() ?: "",
                                statusName = map["statusName"]?.toString(),
                                rating = rating,
                                canRate = (map["canRate"] as? Boolean) ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingHistory = false,
                        history = history
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingHistory = false,
                        history = emptyList()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error loading history", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    history = emptyList()
                )
            }
        }
    }
    
    fun loadAppointmentStatuses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStatuses = true)
            try {
                val response: Response<List<Map<String, String>>> = ApiClient.service.getAppointmentStatuses()
                if (response.isSuccessful) {
                    val statusData = response.body() ?: emptyList()
                    val statuses = statusData.mapNotNull { map ->
                        try {
                            AppointmentStatusOption(
                                id = map["id"] ?: "",
                                nombre = map["nombre"] ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingStatuses = false,
                        appointmentStatuses = statuses
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingStatuses = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error loading statuses", e)
                _uiState.value = _uiState.value.copy(isLoadingStatuses = false)
            }
        }
    }
    
    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingAppointmentId = appointmentId)
            try {
                val response: Response<Unit> = ApiClient.service.cancelAppointment(appointmentId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        appointments = _uiState.value.appointments.filter { it.id != appointmentId },
                        updatingAppointmentId = null,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        updatingAppointmentId = null,
                        errorMessage = "Error al cancelar cita: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error cancelling appointment", e)
                _uiState.value = _uiState.value.copy(
                    updatingAppointmentId = null,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun rescheduleAppointment(appointmentId: String, newStart: String, newEnd: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingAppointmentId = appointmentId)
            try {
                val request = mapOf(
                    "newStart" to newStart,
                    "newEnd" to newEnd
                )
                
                val response: Response<Appointment> = ApiClient.service.rescheduleAppointment(appointmentId, request)
                if (response.isSuccessful) {
                    val updatedAppointment = response.body()
                    if (updatedAppointment != null) {
                        _uiState.value = _uiState.value.copy(
                            appointments = _uiState.value.appointments.map { 
                                if (it.id == appointmentId) updatedAppointment else it 
                            },
                            updatingAppointmentId = null,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            updatingAppointmentId = null,
                            errorMessage = "Error al reagendar cita"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        updatingAppointmentId = null,
                        errorMessage = "Error al reagendar cita: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error rescheduling appointment", e)
                _uiState.value = _uiState.value.copy(
                    updatingAppointmentId = null,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun updateAppointmentStatus(appointmentId: String, statusId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingAppointmentId = appointmentId)
            try {
                val request = mapOf("statusId" to statusId)
                
                val response: Response<Appointment> = ApiClient.service.updateAppointmentStatus(appointmentId, request)
                if (response.isSuccessful) {
                    val updatedAppointment = response.body()
                    if (updatedAppointment != null) {
                        _uiState.value = _uiState.value.copy(
                            appointments = _uiState.value.appointments.map { 
                                if (it.id == appointmentId) updatedAppointment else it 
                            },
                            updatingAppointmentId = null,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            updatingAppointmentId = null,
                            errorMessage = "Error al actualizar estado"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        updatingAppointmentId = null,
                        errorMessage = "Error al actualizar estado: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error updating status", e)
                _uiState.value = _uiState.value.copy(
                    updatingAppointmentId = null,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun submitRating(appointmentId: String, clientId: String, score: Int, comment: String?) {
        viewModelScope.launch {
            try {
                val request = mapOf(
                    "clientId" to clientId,
                    "score" to score,
                    "comment" to (comment ?: "")
                )
                
                val response: Response<Map<String, Any>> = ApiClient.service.submitRating(appointmentId, request)
                if (response.isSuccessful) {
                    // Reload history to reflect the new rating
                    loadClientHistory(clientId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al enviar calificación: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error submitting rating", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
