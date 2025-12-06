package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.util.concurrent.TimeUnit

interface SupabaseRestService {
    @GET("rest/v1/tbl_profesionales")
    suspend fun getProfessionals(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("profesional_verificado") verificationStatusId: String? = null
    ): Response<List<Map<String, Any?>>>
    
    @GET("rest/v1/tbl_usuarios")
    suspend fun getUsers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @QueryMap queryParams: Map<String, String>? = null
    ): Response<List<Map<String, Any?>>>
    
    @GET("rest/v1/tbl_usuarios")
    suspend fun getUserById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any?>>>
    
    @GET("rest/v1/tbl_ubicacion_usuarios")
    suspend fun getUserLocations(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @QueryMap queryParams: Map<String, String>? = null
    ): Response<List<Map<String, Any?>>>
    
    @GET("rest/v1/tbl_estados")
    suspend fun getVerificationStatus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("nombre_estado") statusName: String? = null
    ): Response<List<Map<String, Any?>>>
    
    @GET("rest/v1/tbl_generos")
    suspend fun getGenders(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any?>>>
}

object SupabaseRestClient {
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_KEY
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            val original = chain.request()
            
            // Construir request con headers necesarios
            val requestBuilder = original.newBuilder()
                .header("apikey", supabaseKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
            
            // Solo agregar Authorization con supabaseKey si no existe ya
            // Si existe, significa que ya fue agregado por Retrofit con el access_token del usuario
            if (original.header("Authorization") == null) {
                requestBuilder.header("Authorization", "Bearer $supabaseKey")
            }
            
            val newRequest = requestBuilder.build()
            chain.proceed(newRequest)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("$supabaseUrl/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: SupabaseRestService = retrofit.create(SupabaseRestService::class.java)
    
    suspend fun getVerifiedBarbers(): Result<List<Barber>> {
        return try {
            // Get verification status
            val statusResponse = service.getVerificationStatus(
                apiKey = supabaseKey,
                authorization = "Bearer $supabaseKey",
                select = "id,nombre_estado",
                statusName = "eq.Verificado"
            )
            
            if (!statusResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get verification status: ${statusResponse.code()}"))
            }
            
            val verifiedStatus = statusResponse.body()?.firstOrNull()
            val verifiedStatusId = verifiedStatus?.get("id")?.toString()
            
            if (verifiedStatusId == null) {
                return Result.failure(Exception("Verification status not found"))
            }
            
            // Get professionals with verified status
            val professionalsResponse = service.getProfessionals(
                apiKey = supabaseKey,
                authorization = "Bearer $supabaseKey",
                select = "id,id_usuario,tbl_usuarios(nombre,apellido,foto_perfil),tbl_ubicacion_usuarios(latitud,longitud,direccion,id as ubicacion_id),profesional_verificado,profesional_lugar_de_trabajo"
            )
            
            if (!professionalsResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get professionals: ${professionalsResponse.code()}"))
            }
            
            val professionalsData = professionalsResponse.body() ?: emptyList()
            
            val barbers = professionalsData.mapNotNull { prof ->
                try {
                    val usuarioData = (prof["tbl_usuarios"] as? List<Map<String, Any?>>)?.firstOrNull()
                    val ubicacionData = (prof["tbl_ubicacion_usuarios"] as? List<Map<String, Any?>>)?.firstOrNull()
                    
                    Barber(
                        id = prof["id"]?.toString() ?: return@mapNotNull null,
                        nombre = usuarioData?.get("nombre")?.toString(),
                        apellido = usuarioData?.get("apellido")?.toString(),
                        fotoPerfil = usuarioData?.get("foto_perfil")?.toString(),
                        latitud = ubicacionData?.get("latitud")?.toString()?.toDoubleOrNull(),
                        longitud = ubicacionData?.get("longitud")?.toString()?.toDoubleOrNull(),
                        direccion = ubicacionData?.get("direccion")?.toString(),
                        ubicacionId = ubicacionData?.get("ubicacion_id")?.toString(),
                        verificationStatusId = prof["profesional_verificado"]?.toString(),
                        lugarDeTrabajo = prof["profesional_lugar_de_trabajo"]?.toString()?.toIntOrNull(),
                        ratingAverage = null,
                        ratingsCount = null
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(barbers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfile(userId: String, accessToken: String): Result<UserProfile?> {
        return try {
            val queryParams = mapOf("id" to "eq.$userId")
            val response = service.getUsers(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                select = "*",
                queryParams = queryParams
            )
            
            if (!response.isSuccessful) {
                // Si es 401 o 404, el usuario probablemente no existe en la tabla
                // Esto es normal para nuevos usuarios que necesitan crear su perfil
                if (response.code() == 401 || response.code() == 404) {
                    android.util.Log.d("SupabaseRestClient", "User not found in tbl_usuarios (${response.code()}), redirecting to profile setup")
                    return Result.success(null) // Usuario no encontrado, necesita crear perfil
                }
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("SupabaseRestClient", "Failed to get user: ${response.code()}, body: $errorBody")
                return Result.failure(Exception("Failed to get user: ${response.code()} - $errorBody"))
            }
            
            val userData = response.body()?.firstOrNull()
            if (userData == null) {
                android.util.Log.d("SupabaseRestClient", "User data is null, user needs to create profile")
                return Result.success(null)
            }
            
            // Check if user is approver
            val isApprover = (userData["es_aprobador"] as? Boolean) ?: false
            
            // Convert gender to string if it's a number
            val genderValue = userData["genero"]
            val genderString = when (genderValue) {
                is Number -> when (genderValue.toInt()) {
                    1 -> "masculino"
                    2 -> "femenino"
                    else -> "otro"
                }
                is String -> genderValue
                else -> ""
            }
            
            val profile = UserProfile(
                id = userData["id"]?.toString(),
                firstName = userData["nombre"]?.toString() ?: "",
                lastName = userData["apellido"]?.toString() ?: "",
                email = userData["email"]?.toString() ?: "",
                phone = userData["telefono"]?.toString() ?: "",
                idType = userData["tipo_documento"]?.toString() ?: "",
                idNumber = userData["numero_documento"]?.toString() ?: "",
                gender = genderString,
                address = userData["direccion"]?.toString() ?: "",
                fotoPerfil = userData["foto_perfil"]?.toString(),
                isBarber = false, // Will be checked separately
                professionalId = null,
                username = null,
                specialty = null,
                workLocation = null,
                description = null,
                verified = false,
                verificationStatus = null,
                rating = 0.0,
                ratingsCount = 0,
                isApprover = isApprover
            )
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class UserLocation(
        val latitud: Double?,
        val longitud: Double?,
        val ubicacionId: String?,
        val direccion: String?
    )
    
    suspend fun getUserLocation(userId: String, accessToken: String): Result<UserLocation?> {
        return try {
            val queryParams = mapOf("id" to "eq.$userId")
            val select = "id_ubicacion_usuario,direccion,tbl_ubicacion_usuarios(latitud_usuario,longitud_usuario)"
            val response = service.getUsers(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                select = select,
                queryParams = queryParams
            )
            
            if (!response.isSuccessful) {
                return Result.success(null)
            }
            
            val userData = response.body()?.firstOrNull() ?: return Result.success(null)
            
            val ubicacionId = userData["id_ubicacion_usuario"]?.toString()
            val direccion = userData["direccion"]?.toString()
            
            val ubicacionRaw = userData["tbl_ubicacion_usuarios"]
            val ubicacion = when {
                ubicacionRaw is List<*> && ubicacionRaw.isNotEmpty() -> ubicacionRaw[0] as? Map<*, *>
                ubicacionRaw is Map<*, *> -> ubicacionRaw
                else -> null
            }
            
            val latitud = (ubicacion?.get("latitud_usuario") as? Number)?.toDouble()
            val longitud = (ubicacion?.get("longitud_usuario") as? Number)?.toDouble()
            
            if (latitud == null || longitud == null) {
                return Result.success(null)
            }
            
            Result.success(
                UserLocation(
                    latitud = latitud,
                    longitud = longitud,
                    ubicacionId = ubicacionId,
                    direccion = direccion
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProfessionalByUserId(userId: String, accessToken: String): Result<Map<String, Any?>?> {
        return try {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val url = "$supabaseUrl/rest/v1/tbl_profesionales?id_usuario=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return Result.success(null)
            }
            
            val jsonArray = org.json.JSONArray(response.body?.string() ?: "[]")
            if (jsonArray.length() > 0) {
                val jsonObject = jsonArray.getJSONObject(0)
                val professionalMap = mutableMapOf<String, Any?>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    professionalMap[key] = jsonObject.get(key)
                }
                Result.success(professionalMap)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkIfProfessionalExists(userId: String, accessToken: String): Result<Boolean> {
        return try {
            val result = getProfessionalByUserId(userId, accessToken)
            Result.success(result.getOrNull() != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createProfessionalProfile(
        userId: String,
        accessToken: String,
        username: String,
        specialty: String,
        workLocationId: Int,
        description: String?,
        verificationStatusId: String?,
        documentationBase64: String
    ): Result<Map<String, Any?>> {
        return try {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val json = JSONObject().apply {
                put("id_usuario", userId)
                put("nombre_usuario_publico", username)
                put("especialidad", specialty)
                put("lugar_de_trabajo", workLocationId)
                if (description != null) put("descripcion", description)
                if (verificationStatusId != null) put("profesional_verificado", verificationStatusId)
                put("documentacion", documentationBase64)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/tbl_profesionales")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(requestBody)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return Result.failure(Exception("Failed to create professional: ${response.code} - $errorBody"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(responseBody)
            
            if (jsonArray.length() > 0) {
                val jsonObject = jsonArray.getJSONObject(0)
                val professionalMap = mutableMapOf<String, Any?>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    professionalMap[key] = jsonObject.get(key)
                }
                Result.success(professionalMap)
            } else {
                Result.failure(Exception("No data returned from professional creation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVerificationStatuses(accessToken: String): Result<List<Map<String, Any?>>> {
        return try {
            val response = service.getVerificationStatus(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken"
            )
            
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get verification statuses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getWorkLocations(accessToken: String): Result<List<Map<String, Any?>>> {
        return try {
            // For now, return default work locations
            // TODO: Implement query to tbl_lugares_trabajo when available
            val defaultLocations = listOf(
                mapOf("id" to 1, "lugar_de_trabajo" to "A Domicilio"),
                mapOf("id" to 2, "lugar_de_trabajo" to "En mi Establecimiento"),
                mapOf("id" to 3, "lugar_de_trabajo" to "Ambos")
            )
            Result.success(defaultLocations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfileImage(
        userId: String,
        accessToken: String,
        imageBase64: String
    ): Result<Unit> {
        return try {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val json = JSONObject().apply {
                put("foto_perfil", imageBase64)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/tbl_usuarios?id=eq.$userId")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .patch(requestBody)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return Result.failure(Exception("Failed to update profile image: ${response.code} - $errorBody"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
