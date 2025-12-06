package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.constants.ColombiaConstants
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Gender
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AddressParts(
    val country: String = "Colombia",
    val department: String = "",
    val city: String = "",
    val neighborhood: String = "",
    val streetType: String = "",
    val streetNumber: String = "",
    val streetLetter: String = "",
    val streetQualifier: String = "",
    val secondaryNumber: String = "",
    val secondaryLetter: String = "",
    val secondaryQualifier: String = "",
    val plateNumber: String = "",
    val quadrant: String = "",
    val complement: String = ""
)

data class ProfileSetupUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val userExists: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val idType: String = "",
    val idNumber: String = "",
    val gender: String = "",
    val genders: List<Gender> = emptyList(),
    val addressParts: AddressParts = AddressParts(),
    val fullAddress: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileSetupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_KEY
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

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

                val userId = session.userId
                val accessToken = session.accessToken

                // Check if user already exists
                val userProfileResult = SupabaseRestClient.getUserProfile(userId, accessToken)
                val userProfile = userProfileResult.getOrNull()

                if (userProfile != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userExists = true,
                            errorMessage = "El perfil ya existe"
                        )
                    }
                    return@launch
                }

                // Extract user metadata for pre-filling
                val email = session.email ?: ""
                
                // Load genders
                loadGenders(accessToken)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        email = email,
                        userExists = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileSetupViewModel", "Error loading initial data: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error desconocido al cargar datos iniciales."
                    )
                }
            }
        }
    }

    private fun loadGenders(accessToken: String) {
        viewModelScope.launch {
            try {
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/tbl_generos?select=*&order=id")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonArray = org.json.JSONArray(response.body?.string() ?: "[]")
                    val genders = mutableListOf<Gender>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        genders.add(
                            Gender(
                                id = jsonObject.getInt("id"),
                                genero = jsonObject.getString("genero")
                            )
                        )
                    }
                    _uiState.update { it.copy(genders = genders) }
                } else {
                    // Fallback to default genders
                    _uiState.update {
                        it.copy(
                            genders = listOf(
                                Gender(1, "Masculino"),
                                Gender(2, "Femenino"),
                                Gender(3, "Otro")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileSetupViewModel", "Error loading genders: ${e.message}", e)
                // Fallback to default genders
                _uiState.update {
                    it.copy(
                        genders = listOf(
                            Gender(1, "Masculino"),
                            Gender(2, "Femenino"),
                            Gender(3, "Otro")
                        )
                    )
                }
            }
        }
    }

    fun updateField(field: String, value: String) {
        when (field) {
            "firstName" -> _uiState.update { it.copy(firstName = value) }
            "lastName" -> _uiState.update { it.copy(lastName = value) }
            "email" -> _uiState.update { it.copy(email = value) }
            "phone" -> _uiState.update { it.copy(phone = value) }
            "idType" -> _uiState.update { it.copy(idType = value) }
            "idNumber" -> _uiState.update { it.copy(idNumber = value.replace(Regex("[^0-9]"), "")) }
            "gender" -> _uiState.update { it.copy(gender = value) }
        }
        updateFullAddress()
    }

    fun updateAddressPart(field: String, value: String) {
        val currentParts = _uiState.value.addressParts
        val updatedParts = when (field) {
            "department" -> currentParts.copy(department = value, city = "")
            "city" -> currentParts.copy(city = value)
            "neighborhood" -> currentParts.copy(neighborhood = value)
            "streetType" -> currentParts.copy(streetType = value)
            "streetNumber" -> currentParts.copy(streetNumber = value.replace(Regex("[^0-9]"), ""))
            "streetLetter" -> currentParts.copy(streetLetter = value.replace(Regex("[^A-Za-z]"), "").uppercase().take(5))
            "streetQualifier" -> currentParts.copy(streetQualifier = value)
            "secondaryNumber" -> currentParts.copy(secondaryNumber = value.replace(Regex("[^0-9]"), ""))
            "secondaryLetter" -> currentParts.copy(secondaryLetter = value.replace(Regex("[^A-Za-z]"), "").uppercase().take(5))
            "secondaryQualifier" -> currentParts.copy(secondaryQualifier = value)
            "plateNumber" -> currentParts.copy(plateNumber = value.replace(Regex("[^0-9]"), ""))
            "quadrant" -> currentParts.copy(quadrant = value)
            "complement" -> currentParts.copy(complement = value)
            else -> currentParts
        }
        _uiState.update { it.copy(addressParts = updatedParts) }
        updateFullAddress()
    }

    private fun updateFullAddress() {
        val parts = _uiState.value.addressParts
        
        if (parts.streetType.isBlank() || parts.streetNumber.isBlank() || 
            parts.secondaryNumber.isBlank() || parts.plateNumber.isBlank()) {
            _uiState.update { it.copy(fullAddress = "") }
            return
        }

        val primary = listOfNotNull(
            parts.streetType,
            parts.streetNumber,
            parts.streetLetter.takeIf { it.isNotBlank() },
            parts.streetQualifier.takeIf { it.isNotBlank() }
        ).joinToString(" ")

        val secondary = listOfNotNull(
            parts.secondaryNumber,
            parts.secondaryLetter.takeIf { it.isNotBlank() },
            parts.secondaryQualifier.takeIf { it.isNotBlank() }
        ).joinToString(" ")

        if (primary.isBlank() || secondary.isBlank()) {
            _uiState.update { it.copy(fullAddress = "") }
            return
        }

        var address = "$primary # $secondary-${parts.plateNumber}"
        
        if (parts.quadrant.isNotBlank()) {
            address += " ${parts.quadrant}"
        }
        
        if (parts.neighborhood.isNotBlank()) {
            address += ", ${parts.neighborhood}"
        }
        
        if (parts.city.isNotBlank()) {
            address += ", ${parts.city}"
        }
        
        if (parts.department.isNotBlank()) {
            address += ", ${parts.department}"
        }
        
        address += ", ${parts.country}"
        
        if (parts.complement.isNotBlank()) {
            address += ". ${parts.complement}"
        }

        _uiState.update { it.copy(fullAddress = address.trim().replace(Regex("\\s+"), " ")) }
    }

    fun submitProfile(context: Context, onSuccess: () -> Unit) {
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
                if (state.firstName.isBlank() || state.lastName.isBlank() || state.email.isBlank() ||
                    state.phone.isBlank() || state.idType.isBlank() || state.idNumber.isBlank() ||
                    state.gender.isBlank() || state.fullAddress.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Por favor completa todos los campos requeridos."
                        )
                    }
                    return@launch
                }

                val userId = session.userId
                val accessToken = session.accessToken

                // Geocode address
                var lat: Double? = null
                var lon: Double? = null
                try {
                    val geocodeResponse = ApiClient.service.geocodeAddress(mapOf("address" to state.fullAddress))
                    if (geocodeResponse.isSuccessful) {
                        val geoData = geocodeResponse.body()
                        lat = geoData?.get("lat") as? Double
                        lon = geoData?.get("lon") as? Double
                    }
                } catch (e: Exception) {
                    Log.w("ProfileSetupViewModel", "Geocoding failed: ${e.message}", e)
                }

                // Get gender ID
                val selectedGender = state.genders.find { 
                    it.genero.equals(state.gender, ignoreCase = true) 
                }
                val genderId = selectedGender?.id ?: 1

                // Create user profile
                val userData = JSONObject().apply {
                    put("id", userId)
                    put("nombre", state.firstName)
                    put("apellido", state.lastName)
                    put("email", state.email)
                    put("telefono", state.phone.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0)
                    put("tipo_id", state.idType)
                    put("numero_documento", state.idNumber.toIntOrNull() ?: 0)
                    put("genero", genderId)
                    put("direccion", state.fullAddress)
                    put("es_aprobador", false)
                }

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/tbl_usuarios")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .post(userData.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    throw Exception("Error al crear perfil: ${response.code} - $errorBody")
                }

                // Save location if geocoded successfully
                if (lat != null && lon != null) {
                    val locationData = JSONObject().apply {
                        put("latitud_usuario", lat)
                        put("longitud_usuario", lon)
                        put("actualizado_el", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                    }

                    val locationRequest = Request.Builder()
                        .url("$supabaseUrl/rest/v1/tbl_ubicacion_usuarios")
                        .header("apikey", supabaseKey)
                        .header("Authorization", "Bearer $accessToken")
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                        .post(locationData.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val locationResponse = okHttpClient.newCall(locationRequest).execute()
                    if (locationResponse.isSuccessful) {
                        val locationResponseBody = locationResponse.body?.string() ?: "[]"
                        val locationJsonArray = org.json.JSONArray(locationResponseBody)
                        if (locationJsonArray.length() > 0) {
                            val locationId = locationJsonArray.getJSONObject(0).getString("id")
                            
                            // Update user with location ID
                            val updateUserRequest = Request.Builder()
                                .url("$supabaseUrl/rest/v1/tbl_usuarios?id=eq.$userId")
                                .header("apikey", supabaseKey)
                                .header("Authorization", "Bearer $accessToken")
                                .header("Content-Type", "application/json")
                                .header("Prefer", "return=representation")
                                .patch(JSONObject().apply {
                                    put("id_ubicacion_usuario", locationId)
                                }.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                            
                            okHttpClient.newCall(updateUserRequest).execute()
                        }
                    }
                }

                // Save profile locally
                val userProfile = UserProfile(
                    id = userId,
                    firstName = state.firstName,
                    lastName = state.lastName,
                    email = state.email,
                    phone = state.phone,
                    idType = state.idType,
                    idNumber = state.idNumber,
                    gender = state.gender,
                    address = state.fullAddress,
                    isBarber = false,
                    isApprover = false
                )
                AuthService.saveUserProfile(context, userProfile)

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "¡Perfil creado exitosamente!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileSetupViewModel", "Error submitting profile: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Error desconocido al crear perfil."
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

