package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Gender
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.WorkLocation
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val genders: List<Gender> = emptyList(),
    val workLocations: List<WorkLocation> = emptyList(),
    val availabilityBlocks: List<AvailabilityBlock> = emptyList(),
    val isLoadingAvailability: Boolean = false,
    val isAddingAvailability: Boolean = false,
    val availabilityError: String? = null,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

class EditProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_KEY
    private val gson = Gson()
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        loadGenders()
        loadWorkLocations()
    }

    fun loadProfile(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val session = AuthService.getSession(context)
                if (session == null || session.userId == null || session.accessToken == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No hay sesión activa"
                    )
                    return@launch
                }

                // Load user profile
                val profileResult = SupabaseRestClient.getUserProfile(session.userId, session.accessToken)
                if (profileResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = profileResult.exceptionOrNull()?.message ?: "Error al cargar perfil"
                    )
                    return@launch
                }

                var userProfile = profileResult.getOrNull()
                if (userProfile == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Perfil no encontrado"
                    )
                    return@launch
                }

                // Check if user is a barber
                val professionalResult = SupabaseRestClient.getProfessionalByUserId(session.userId, session.accessToken)
                if (professionalResult.isSuccess) {
                    val professional = professionalResult.getOrNull()
                    if (professional != null) {
                        userProfile = userProfile.copy(
                            isBarber = true,
                            professionalId = professional["id"]?.toString(),
                            username = professional["nombre_usuario_publico"]?.toString(),
                            specialty = professional["especialidad"]?.toString(),
                            workLocation = professional["lugar_de_trabajo"]?.toString()
                        )
                        
                        // Load availability if barber
                        professional["id"]?.toString()?.let { professionalId ->
                            loadAvailability(professionalId)
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = userProfile
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }

    private fun loadGenders() {
        viewModelScope.launch {
            try {
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/tbl_generos?select=*&order=id")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $supabaseKey")
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
                    _uiState.value = _uiState.value.copy(genders = genders)
                } else {
                    // Fallback to default genders
                    _uiState.value = _uiState.value.copy(
                        genders = listOf(
                            Gender(1, "Masculino"),
                            Gender(2, "Femenino"),
                            Gender(3, "Otro")
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback to default genders
                _uiState.value = _uiState.value.copy(
                    genders = listOf(
                        Gender(1, "Masculino"),
                        Gender(2, "Femenino"),
                        Gender(3, "Otro")
                    )
                )
            }
        }
    }

    private fun loadWorkLocations() {
        _uiState.value = _uiState.value.copy(
            workLocations = listOf(
                WorkLocation(id = 1, lugarDeTrabajo = "A Domicilio"),
                WorkLocation(id = 2, lugarDeTrabajo = "En mi Establecimiento"),
                WorkLocation(id = 3, lugarDeTrabajo = "Ambos")
            )
        )
    }

    fun loadAvailability(professionalId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAvailability = true, availabilityError = null)
            try {
                val response = ApiClient.service.getAvailability(professionalId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAvailability = false,
                        availabilityBlocks = response.body() ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAvailability = false,
                        availabilityError = "Error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAvailability = false,
                    availabilityError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun updateProfile(
        context: Context,
        phone: String?,
        genderId: Int?,
        address: String?,
        fotoPerfil: String?,
        username: String?,
        specialty: String?,
        workLocationId: Int?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, saveSuccess = false)
            
            try {
                val session = AuthService.getSession(context)
                if (session == null || session.userId == null || session.accessToken == null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "No hay sesión activa"
                    )
                    return@launch
                }

                val userId = session.userId
                val accessToken = session.accessToken

                // Update user profile
                val userUpdate = JSONObject().apply {
                    if (phone != null) put("telefono", phone.toIntOrNull())
                    if (genderId != null) put("genero", genderId)
                    if (address != null) put("direccion", address)
                    if (fotoPerfil != null) put("foto_perfil", fotoPerfil)
                }

                val userRequest = Request.Builder()
                    .url("$supabaseUrl/rest/v1/tbl_usuarios?id=eq.$userId")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .patch(userUpdate.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val userResponse = okHttpClient.newCall(userRequest).execute()
                if (!userResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Error al actualizar usuario: ${userResponse.code}"
                    )
                    return@launch
                }

                // Update professional profile if barber
                val currentProfile = _uiState.value.profile
                if (currentProfile?.isBarber == true && (username != null || specialty != null || workLocationId != null)) {
                    val professionalUpdate = JSONObject().apply {
                        if (username != null) put("nombre_usuario_publico", username)
                        if (specialty != null) put("especialidad", specialty)
                        if (workLocationId != null) put("lugar_de_trabajo", workLocationId)
                    }

                    val professionalRequest = Request.Builder()
                        .url("$supabaseUrl/rest/v1/tbl_profesionales?id_usuario=eq.$userId")
                        .header("apikey", supabaseKey)
                        .header("Authorization", "Bearer $accessToken")
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                        .patch(professionalUpdate.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val professionalResponse = okHttpClient.newCall(professionalRequest).execute()
                    if (!professionalResponse.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Error al actualizar perfil profesional: ${professionalResponse.code}"
                        )
                        return@launch
                    }
                }

                // Geocode address if updated
                if (address != null && address.isNotEmpty()) {
                    try {
                        val geocodeRequest = JSONObject().apply {
                            put("address", address)
                        }
                        val geocodeResponse = ApiClient.service.geocodeAddress(
                            mapOf("address" to address)
                        )
                        if (geocodeResponse.isSuccessful) {
                            val geocodeData = geocodeResponse.body()
                            val lat = (geocodeData?.get("lat") as? Number)?.toDouble()
                            val lon = (geocodeData?.get("lon") as? Number)?.toDouble()
                            
                            if (lat != null && lon != null) {
                                // Update location in Supabase
                                // This would require implementing saveOrUpdateUserLocation
                                // For now, we'll skip it
                            }
                        }
                    } catch (e: Exception) {
                        // Geocoding failed, but continue anyway
                        android.util.Log.w("EditProfileViewModel", "Geocoding failed: ${e.message}")
                    }
                }

                // Reload profile
                loadProfile(context)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun addAvailabilityBlock(professionalId: String, date: String, startTime: String, endTime: String, notes: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingAvailability = true, availabilityError = null)
            
            try {
                val start = "${date}T${startTime}:00"
                val end = "${date}T${endTime}:00"
                
                val startDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(start)
                val endDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(end)
                
                if (startDate == null || endDate == null || endDate <= startDate) {
                    _uiState.value = _uiState.value.copy(
                        isAddingAvailability = false,
                        availabilityError = "La hora fin debe ser mayor que la hora inicio"
                    )
                    return@launch
                }

                val durationMinutes = maxOf(5, ((endDate.time - startDate.time) / 60000).toInt())
                val minimumSlotLength = String.format("%02d:%02d:00", durationMinutes / 60, durationMinutes % 60)

                val requestBody = mapOf(
                    "start" to startDate.toInstant().toString(),
                    "end" to endDate.toInstant().toString(),
                    "minimumSlotLength" to minimumSlotLength,
                    "notes" to (notes ?: ""),
                    "status" to "disponible"
                )

                val response = ApiClient.service.createAvailabilityBlock(professionalId, requestBody)
                if (response.isSuccessful) {
                    loadAvailability(professionalId)
                    _uiState.value = _uiState.value.copy(isAddingAvailability = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAddingAvailability = false,
                        availabilityError = "Error al crear disponibilidad: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingAvailability = false,
                    availabilityError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun deleteAvailabilityBlock(professionalId: String, availabilityId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAvailability = true, availabilityError = null)
            try {
                val response = ApiClient.service.deleteAvailabilityBlock(professionalId, availabilityId)
                if (response.isSuccessful) {
                    loadAvailability(professionalId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAvailability = false,
                        availabilityError = "Error al eliminar disponibilidad: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAvailability = false,
                    availabilityError = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun updateProfileField(field: String, value: Any?) {
        val currentProfile = _uiState.value.profile ?: return
        val updatedProfile = when (field) {
            "phone" -> currentProfile.copy(phone = value as? String ?: "")
            "gender" -> currentProfile.copy(gender = value as? String ?: "")
            "address" -> currentProfile.copy(address = value as? String ?: "")
            "fotoPerfil" -> currentProfile.copy(fotoPerfil = value as? String)
            "username" -> currentProfile.copy(username = value as? String)
            "specialty" -> currentProfile.copy(specialty = value as? String)
            "workLocation" -> currentProfile.copy(workLocation = value as? String)
            else -> currentProfile
        }
        _uiState.value = _uiState.value.copy(profile = updatedProfile)
    }

    fun convertImageToBase64(uri: Uri, context: Context): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            val outputStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

