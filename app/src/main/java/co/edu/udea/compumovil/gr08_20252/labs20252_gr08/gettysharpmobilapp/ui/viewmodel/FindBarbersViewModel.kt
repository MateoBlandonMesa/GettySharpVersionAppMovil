package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Appointment
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.CreateAppointmentRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FindBarbersUiState(
    val isLoading: Boolean = false,
    val barbers: List<Barber> = emptyList(),
    val errorMessage: String? = null,
    val clientLocation: ClientLocation? = null,
    val selectedBarber: Barber? = null,
    val availability: List<AvailabilityBlock> = emptyList(),
    val isLoadingAvailability: Boolean = false,
    val availabilityError: String? = null,
    val selectedSlot: AvailabilityBlock? = null,
    val locationChoice: LocationChoice? = null,
    val isCreatingAppointment: Boolean = false,
    val appointmentCreated: Appointment? = null
)

data class ClientLocation(
    val lat: Double,
    val lon: Double,
    val locationId: String?,
    val address: String?
)

sealed class LocationChoice {
    object Home : LocationChoice()
    object Local : LocationChoice()
}

class FindBarbersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FindBarbersUiState())
    val uiState: StateFlow<FindBarbersUiState> = _uiState.asStateFlow()

    init {
        loadBarbers()
    }

    fun loadBarbers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = SupabaseRestClient.getVerifiedBarbers()
            result.fold(
                onSuccess = { barbers ->
                    // Load ratings for barbers
                    loadBarberRatings(barbers)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        barbers = emptyList(),
                        errorMessage = error.message ?: "Error desconocido"
                    )
                }
            )
        }
    }

    private suspend fun loadBarberRatings(barbers: List<Barber>) {
        try {
            if (barbers.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    barbers = barbers
                )
                return
            }

            val professionalIds = barbers.map { it.id }
            val idsParam = professionalIds.joinToString(",")
            
            val response = ApiClient.service.getProfessionalRatingSummaries(idsParam)
            if (response.isSuccessful) {
                val summaries = response.body() ?: emptyMap()
                
                val enrichedBarbers = barbers.map { barber ->
                    val summary = summaries[barber.id]
                    if (summary != null) {
                        barber.copy(
                            ratingAverage = (summary["averageScore"] as? Number)?.toDouble(),
                            ratingsCount = (summary["ratingsCount"] as? Number)?.toInt()
                        )
                    } else {
                        barber
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    barbers = enrichedBarbers,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    barbers = barbers,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FindBarbersViewModel", "Error loading ratings", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                barbers = barbers,
                errorMessage = null
            )
        }
    }

    fun loadClientLocation(context: Context) {
        viewModelScope.launch {
            try {
                val session = AuthService.getSession(context) ?: return@launch
                val profile = AuthService.getUserProfile(context) ?: return@launch
                
                if (profile.id != null) {
                        val locationResult = SupabaseRestClient.getUserLocation(profile.id!!, session.accessToken ?: "")
                        locationResult.fold(
                            onSuccess = { location ->
                                location?.let {
                                    _uiState.value = _uiState.value.copy(
                                        clientLocation = ClientLocation(
                                            lat = it.latitud ?: 6.343463,
                                            lon = it.longitud ?: -75.559117,
                                            locationId = it.ubicacionId,
                                            address = it.direccion
                                        )
                                    )
                                } ?: run {
                                    // Set default location if no location found
                                    _uiState.value = _uiState.value.copy(
                                        clientLocation = ClientLocation(
                                            lat = 6.343463,
                                            lon = -75.559117,
                                            locationId = null,
                                            address = null
                                        )
                                    )
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.e("FindBarbersViewModel", "Error loading client location", error)
                                // Set default location on error
                                _uiState.value = _uiState.value.copy(
                                    clientLocation = ClientLocation(
                                        lat = 6.343463,
                                        lon = -75.559117,
                                        locationId = null,
                                        address = null
                                    )
                                )
                            }
                        )
                }
            } catch (e: Exception) {
                android.util.Log.e("FindBarbersViewModel", "Error loading client location", e)
            }
        }
    }

    fun selectBarber(barber: Barber) {
        _uiState.value = _uiState.value.copy(
            selectedBarber = barber,
            availability = emptyList(),
            availabilityError = null,
            selectedSlot = null,
            locationChoice = null
        )
        loadAvailability(barber.id)
        updateLocationChoice(barber)
    }

    private fun updateLocationChoice(barber: Barber) {
        val lugarTrabajo = barber.lugarDeTrabajo
        val canShowHome = (lugarTrabajo == 1 || lugarTrabajo == 3) && _uiState.value.clientLocation?.locationId != null
        val canShowLocal = (lugarTrabajo == 2 || lugarTrabajo == 3) && barber.ubicacionId != null

        val choice = when {
            canShowHome && !canShowLocal -> LocationChoice.Home
            canShowLocal && !canShowHome -> LocationChoice.Local
            else -> null
        }
        
        _uiState.value = _uiState.value.copy(locationChoice = choice)
    }

    fun loadAvailability(professionalId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingAvailability = true,
                availabilityError = null
            )
            
            try {
                val response = ApiClient.service.getAvailability(professionalId)
                if (response.isSuccessful) {
                    val allSlots = response.body() ?: emptyList()
                    val availableSlots = allSlots.filter {
                        (it.status ?: "").lowercase() == "disponible"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingAvailability = false,
                        availability = availableSlots,
                        availabilityError = if (availableSlots.isEmpty()) {
                            "Este profesional no tiene bloques de disponibilidad publicados."
                        } else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAvailability = false,
                        availability = emptyList(),
                        availabilityError = "No se pudo cargar la disponibilidad."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("FindBarbersViewModel", "Error loading availability", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingAvailability = false,
                    availability = emptyList(),
                    availabilityError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun selectSlot(slot: AvailabilityBlock) {
        _uiState.value = _uiState.value.copy(selectedSlot = slot)
    }

    fun setLocationChoice(choice: LocationChoice) {
        _uiState.value = _uiState.value.copy(locationChoice = choice)
    }

    fun createAppointment(context: Context) {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedBarber = state.selectedBarber ?: return@launch
            val selectedSlot = state.selectedSlot ?: return@launch
            val locationChoice = state.locationChoice ?: return@launch

            val session = AuthService.getSession(context) ?: return@launch
            val profile = AuthService.getUserProfile(context) ?: return@launch
            
            if (profile.id == null) return@launch

            val ubicacionCita = when (locationChoice) {
                is LocationChoice.Home -> {
                    val clientLocationId = state.clientLocation?.locationId
                    if (clientLocationId == null) {
                        _uiState.value = state.copy(
                            errorMessage = "Agrega una dirección a tu perfil para poder recibir la cita en tu domicilio."
                        )
                        return@launch
                    }
                    clientLocationId
                }
                is LocationChoice.Local -> {
                    val barberLocationId = selectedBarber.ubicacionId
                    if (barberLocationId == null) {
                        _uiState.value = state.copy(
                            errorMessage = "Este profesional no tiene una ubicación registrada."
                        )
                        return@launch
                    }
                    barberLocationId
                }
            }

            _uiState.value = state.copy(isCreatingAppointment = true, errorMessage = null)

            try {
                val request = CreateAppointmentRequest(
                    idCliente = profile.id!!,
                    idProfesional = selectedBarber.id,
                    fechaInicioCita = selectedSlot.start,
                    fechaFinCita = selectedSlot.end,
                    availabilityBlockId = selectedSlot.id,
                    ubicacionCita = ubicacionCita
                )

                val response = ApiClient.service.createAppointment(request)
                if (response.isSuccessful) {
                    val appointment = response.body()
                    _uiState.value = state.copy(
                        isCreatingAppointment = false,
                        appointmentCreated = appointment,
                        selectedBarber = null,
                        selectedSlot = null,
                        locationChoice = null,
                        availability = emptyList()
                    )
                    // Reload barbers to update availability
                    loadBarbers()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    _uiState.value = state.copy(
                        isCreatingAppointment = false,
                        errorMessage = if (errorBody.contains("franja de disponibilidad") || errorBody.contains("no longer available")) {
                            "Horario no disponible. Otro cliente tomó este horario. Elige otra franja."
                        } else {
                            "No se pudo crear la cita: $errorBody"
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("FindBarbersViewModel", "Error creating appointment", e)
                _uiState.value = state.copy(
                    isCreatingAppointment = false,
                    errorMessage = e.message ?: "Error al crear la cita"
                )
            }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedBarber = null,
            availability = emptyList(),
            availabilityError = null,
            selectedSlot = null,
            locationChoice = null,
            errorMessage = null
        )
    }

    fun refreshBarbers() {
        loadBarbers()
    }
}

