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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig

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
            android.util.Log.d("AppointmentsViewModel", "loadAppointments called with mode: $mode")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val session = AuthService.getSession(context)
                if (session == null) {
                    android.util.Log.e("AppointmentsViewModel", "No session found")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se encontró sesión activa"
                    )
                    return@launch
                }
                
                val profile = AuthService.getUserProfile(context)
                if (profile == null) {
                    android.util.Log.e("AppointmentsViewModel", "No profile found")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se encontró perfil de usuario"
                    )
                    return@launch
                }
                
                if (profile.id == null) {
                    android.util.Log.e("AppointmentsViewModel", "Profile ID is null")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "ID de usuario no encontrado"
                    )
                    return@launch
                }
                
                android.util.Log.d("AppointmentsViewModel", "Loading appointments for user: ${profile.id}, isBarber: ${profile.isBarber}, professionalId: ${profile.professionalId}")
                
                val isBarber = profile.isBarber && profile.professionalId != null
                val isManageMode = mode == "manage" && isBarber
                
                _uiState.value = _uiState.value.copy(
                    userRole = if (isBarber) UserRole.Barber else UserRole.Client,
                    isManageMode = isManageMode
                )
                
                if (isBarber && isManageMode) {
                    // Load professional appointments for management
                    profile.professionalId?.let { professionalId ->
                        android.util.Log.d("AppointmentsViewModel", "Loading professional appointments for management: $professionalId")
                        loadProfessionalAppointments(professionalId)
                        loadAppointmentStatuses()
                    }
                } else if (isBarber) {
                    // Load client appointments for barber viewing their own appointments
                    android.util.Log.d("AppointmentsViewModel", "Loading client appointments as barber: ${profile.id}")
                    loadClientAppointments(profile.id!!)
                    loadClientHistory(profile.id!!)
                } else {
                    // Load client appointments
                    android.util.Log.d("AppointmentsViewModel", "Loading client appointments as client: ${profile.id}")
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
                android.util.Log.d("AppointmentsViewModel", "Calling API to get appointments for client: $clientId")
                val response: Response<List<Appointment>> = ApiClient.service.getAppointmentsByClient(clientId)
                android.util.Log.d("AppointmentsViewModel", "API response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val appointments = response.body() ?: emptyList()
                    android.util.Log.d("AppointmentsViewModel", "Loaded ${appointments.size} appointments")
                    appointments.forEach { appointment ->
                        android.util.Log.d("AppointmentsViewModel", "Appointment: ${appointment.id}, statusId: ${appointment.idEstadoCita}, dates: ${appointment.fechaInicioCita} - ${appointment.fechaFinCita}")
                    }
                    
                    // Enrich appointments with status names
                    val enrichedAppointments = enrichAppointments(appointments)
                    
                    enrichedAppointments.forEach { appointment ->
                        android.util.Log.d("AppointmentsViewModel", "Enriched Appointment: ${appointment.id}, statusName: ${appointment.statusName}, dates: ${appointment.fechaInicioCita} - ${appointment.fechaFinCita}")
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = enrichedAppointments,
                        errorMessage = null
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    android.util.Log.e("AppointmentsViewModel", "API error: ${response.code()}, body: $errorBody")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = emptyList(),
                        errorMessage = "Error: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error loading client appointments", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    appointments = emptyList(),
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    private suspend fun enrichAppointments(appointments: List<Appointment>): List<Appointment> {
        return withContext(Dispatchers.IO) {
            try {
                // Extract unique status IDs
                val statusIds = appointments.mapNotNull { it.idEstadoCita }.distinct()
                if (statusIds.isEmpty()) {
                    android.util.Log.d("AppointmentsViewModel", "No status IDs to enrich")
                    return@withContext appointments
                }
                
                android.util.Log.d("AppointmentsViewModel", "Enriching appointments with ${statusIds.size} unique status IDs")
                
                // Get status names from Supabase
                val statusNameMap = getStatusNameMap(statusIds)
                
                android.util.Log.d("AppointmentsViewModel", "Retrieved ${statusNameMap.size} status names")
                
                // Enrich each appointment with status name
                appointments.map { appointment ->
                    val statusName = appointment.idEstadoCita?.let { statusNameMap[it] }
                    appointment.copy(statusName = statusName)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error enriching appointments", e)
                e.printStackTrace()
                appointments // Return original appointments if enrichment fails
            }
        }
    }
    
    private suspend fun getStatusNameMap(statusIds: List<String>): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val supabaseUrl = BuildConfig.SUPABASE_URL
                val supabaseKey = BuildConfig.SUPABASE_KEY
                
                // Build query with multiple IDs: id=in.(id1,id2,id3)
                val statusIdsParam = statusIds.joinToString(",")
                val url = "$supabaseUrl/rest/v1/tbl_estados?select=id,nombre_estado&id=in.($statusIdsParam)"
                
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $supabaseKey")
                    .header("Content-Type", "application/json")
                    .get()
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    android.util.Log.e("AppointmentsViewModel", "Failed to get status names: ${response.code}, body: $errorBody")
                    return@withContext emptyMap()
                }
                
                val responseBody = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(responseBody)
                val statusMap = mutableMapOf<String, String>()
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val id = jsonObject.optString("id", "")
                    val nombreEstado = jsonObject.optString("nombre_estado", "")
                    if (id.isNotEmpty() && nombreEstado.isNotEmpty()) {
                        statusMap[id] = nombreEstado
                    }
                }
                
                android.util.Log.d("AppointmentsViewModel", "Status name map: $statusMap")
                statusMap
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsViewModel", "Error getting status name map", e)
                e.printStackTrace()
                emptyMap()
            }
        }
    }
    
    private fun loadProfessionalAppointments(professionalId: String) {
        viewModelScope.launch {
            try {
                val response: Response<List<Appointment>> = ApiClient.service.getAppointmentsByProfessional(professionalId)
                if (response.isSuccessful) {
                    val appointments = response.body() ?: emptyList()
                    // Enrich appointments with status names
                    val enrichedAppointments = enrichAppointments(appointments)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointments = enrichedAppointments,
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
