package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

data class DashboardUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    val availabilityBlocks: List<AvailabilityBlock> = emptyList(),
    val isLoadingAvailability: Boolean = false,
    val isAddingAvailability: Boolean = false,
    val availabilityError: String? = null
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

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
                
                // Load profile directly from Supabase (it will be enriched with gender name)
                val profileResult = SupabaseRestClient.getUserProfile(session.userId, session.accessToken)
                
                if (profileResult.isSuccess) {
                    var profile = profileResult.getOrNull()
                    if (profile != null) {
                        // Fix profile image URL if it's Base64 without prefix
                        profile = fixProfileImageUrl(profile)
                        
                        // Check if user is a barber and load professional information
                        android.util.Log.d("DashboardViewModel", "Checking if user is a barber for userId: ${session.userId}")
                        val professionalResult = SupabaseRestClient.getProfessionalByUserId(session.userId, session.accessToken)
                        android.util.Log.d("DashboardViewModel", "Professional result: success=${professionalResult.isSuccess}, hasProfessional=${professionalResult.getOrNull() != null}")
                        
                        if (professionalResult.isSuccess) {
                            val professional = professionalResult.getOrNull()
                            if (professional != null) {
                                android.util.Log.d("DashboardViewModel", "User is a barber. Professional ID: ${professional["id"]}, verification status: ${professional["profesional_verificado"]}")
                                val professionalId = professional["id"]?.toString()
                                
                                // Get verification status name
                                var verificationStatusName: String? = null
                                var isVerified = false
                                val verificationStatusId = professional["profesional_verificado"]?.toString()
                                
                                if (verificationStatusId != null) {
                                    verificationStatusName = getVerificationStatusName(verificationStatusId, session.accessToken)
                                    isVerified = verificationStatusName?.lowercase()?.trim() == "verificado"
                                }
                                
                                // Get professional rating
                                var rating = 0.0
                                var ratingsCount = 0
                                
                                if (professionalId != null) {
                                    try {
                                        val ratingResponse = ApiClient.service.getProfessionalRatingSummary(professionalId)
                                        if (ratingResponse.isSuccessful) {
                                            val ratingData = ratingResponse.body()
                                            rating = (ratingData?.get("averageScore") as? Number)?.toDouble() ?: 0.0
                                            ratingsCount = (ratingData?.get("ratingsCount") as? Number)?.toInt() ?: 0
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.w("DashboardViewModel", "Error loading rating: ${e.message}")
                                    }
                                }
                                
                                // Get work location name if available
                                var workLocationText = professional["lugar_de_trabajo"]?.toString()
                                val workLocationId = workLocationText?.toIntOrNull()
                                
                                // Enrich work location with name from tbl_lugares_trabajo
                                if (workLocationId != null) {
                                    val workLocationName = getWorkLocationName(workLocationId, session.accessToken)
                                    if (workLocationName != null) {
                                        workLocationText = workLocationName
                                    }
                                }
                                
                                // Enrich profile with professional information
                                profile = profile.copy(
                                    isBarber = true,
                                    professionalId = professionalId,
                                    username = professional["nombre_usuario_publico"]?.toString(),
                                    specialty = professional["especialidad"]?.toString(),
                                    workLocation = workLocationText,
                                    description = professional["descripcion"]?.toString(),
                                    verified = isVerified,
                                    verificationStatus = verificationStatusName,
                                    rating = rating,
                                    ratingsCount = ratingsCount
                                )
                                android.util.Log.d("DashboardViewModel", "Profile enriched with barber info. isBarber=${profile.isBarber}, verified=${profile.verified}, status=${profile.verificationStatus}")
                            } else {
                                android.util.Log.d("DashboardViewModel", "Professional data is null")
                            }
                        } else {
                            android.util.Log.d("DashboardViewModel", "Failed to get professional data: ${professionalResult.exceptionOrNull()?.message}")
                        }
                        
                        // Save enriched profile to AuthService for future use
                        AuthService.saveUserProfile(context, profile)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profile = profile,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profile = null,
                            errorMessage = "No se pudo cargar el perfil"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = profileResult.exceptionOrNull()?.message ?: "Error al cargar perfil"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error loading profile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    private fun fixProfileImageUrl(profile: UserProfile): UserProfile {
        val fotoPerfil = profile.fotoPerfil
        if (fotoPerfil.isNullOrEmpty()) {
            return profile
        }
        
        // If it's Base64 without prefix, add it
        if (!fotoPerfil.startsWith("data:") && !fotoPerfil.startsWith("http")) {
            // Assume it's Base64 image data
            return profile.copy(fotoPerfil = "data:image/jpeg;base64,$fotoPerfil")
        }
        
        return profile
    }
    
    private suspend fun getVerificationStatusName(statusId: String, accessToken: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val supabaseUrl = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig.SUPABASE_URL
                val supabaseKey = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig.SUPABASE_KEY
                
                val url = "$supabaseUrl/rest/v1/tbl_estados?select=nombre_estado&id=eq.$statusId"
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .get()
                    .build()
                
                val response: okhttp3.Response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "[]"
                    val jsonArray = org.json.JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        return@withContext jsonArray.getJSONObject(0).getString("nombre_estado")
                    }
                } else {
                    android.util.Log.e("DashboardViewModel", "Failed to get verification status name: ${response.code}, body: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error getting verification status name: ${e.message}", e)
            }
            null
        }
    }
    
    private suspend fun getWorkLocationName(workLocationId: Int, accessToken: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val supabaseUrl = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig.SUPABASE_URL
                val supabaseKey = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig.SUPABASE_KEY
                
                val url = "$supabaseUrl/rest/v1/tbl_lugares_trabajo?select=lugar_de_trabajo&id=eq.$workLocationId"
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .get()
                    .build()
                
                val response: okhttp3.Response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "[]"
                    val jsonArray = org.json.JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        return@withContext jsonArray.getJSONObject(0).getString("lugar_de_trabajo")
                    }
                } else {
                    android.util.Log.e("DashboardViewModel", "Failed to get work location name: ${response.code}, body: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error getting work location name: ${e.message}", e)
            }
            null
        }
    }
    
    private fun normalizeStatusName(name: String): String {
        return Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
    }

    fun refreshProfile(context: Context) {
        loadProfile(context)
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
}

