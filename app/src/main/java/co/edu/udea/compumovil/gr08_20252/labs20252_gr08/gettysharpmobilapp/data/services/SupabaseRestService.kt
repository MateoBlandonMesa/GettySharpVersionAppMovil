package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
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
        @Query("select") select: String = "*",
        @Query("order") order: String = "id.asc"
    ): Response<List<Map<String, Any?>>>
    
    @POST("rest/v1/tbl_usuarios")
    suspend fun createUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body userData: Map<String, Any?>
    ): Response<List<Map<String, Any?>>>
    
    @PATCH("rest/v1/tbl_usuarios")
    suspend fun updateUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") userIdQuery: String,
        @Body userData: Map<String, Any?>
    ): Response<List<Map<String, Any?>>>
    
    @POST("rest/v1/tbl_ubicacion_usuarios")
    suspend fun createUserLocation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body locationData: Map<String, Any?>
    ): Response<List<Map<String, Any?>>>
    
    @PATCH("rest/v1/tbl_ubicacion_usuarios")
    suspend fun updateUserLocation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") locationIdQuery: String,
        @Body locationData: Map<String, Any?>
    ): Response<List<Map<String, Any?>>>
    
    @GET("rest/v1/tbl_profesionales")
    suspend fun getProfessionalsByUserId(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id_usuario") userIdQuery: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any?>>>
    
    @POST("rest/v1/tbl_profesionales")
    suspend fun createProfessional(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body professionalData: Map<String, Any?>
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
            
            val profile = UserProfile(
                id = userData["id"]?.toString(),
                firstName = userData["nombre"]?.toString() ?: "",
                lastName = userData["apellido"]?.toString() ?: "",
                email = userData["email"]?.toString() ?: "",
                phone = userData["telefono"]?.toString() ?: "",
                idType = userData["tipo_documento"]?.toString() ?: "",
                idNumber = userData["numero_documento"]?.toString() ?: "",
                gender = userData["genero"]?.toString() ?: "",
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
                ratingsCount = 0
            )
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGenders(accessToken: String): Result<List<Pair<Int, String>>> {
        return try {
            val response = service.getGenders(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                select = "id,genero",
                order = "id.asc"
            )
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to get genders: ${response.code()}"))
            }
            
            val genders = response.body()?.mapNotNull {
                val id = it["id"]?.toString()?.toIntOrNull()
                val genero = it["genero"]?.toString()
                if (id != null && genero != null) {
                    Pair<Int, String>(id, genero)
                } else {
                    null
                }
            } ?: emptyList<Pair<Int, String>>()
            
            Result.success(genders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createUserProfile(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        idType: String,
        idNumber: String,
        genderId: Int,
        address: String,
        accessToken: String
    ): Result<Unit> {
        return try {
            val phoneInt = phone.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            val idNumberInt = idNumber.toIntOrNull() ?: 0
            val userData: Map<String, Any?> = hashMapOf(
                "id" to userId,
                "nombre" to firstName,
                "apellido" to lastName,
                "email" to email,
                "telefono" to phoneInt,
                "tipo_documento" to idType,
                "numero_documento" to idNumberInt,
                "genero" to genderId,
                "direccion" to address,
                "es_aprobador" to false
            )
            
            val response = service.createUser(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                userData = userData
            )
            
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                return Result.failure(Exception("Failed to create user: ${response.code()} - $errorBody"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(
        userId: String,
        phone: String? = null,
        address: String? = null,
        genderId: Int? = null,
        fotoPerfil: String? = null,
        accessToken: String
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any?>()
            phone?.let { updateData["telefono"] = it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
            address?.let { updateData["direccion"] = it }
            genderId?.let { updateData["genero"] = it }
            fotoPerfil?.let { updateData["foto_perfil"] = it }
            
            if (updateData.isEmpty()) {
                return Result.success(Unit) // Nothing to update
            }
            
            val response = service.updateUser(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                userIdQuery = "eq.$userId",
                userData = updateData
            )
            
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                return Result.failure(Exception("Failed to update user: ${response.code()} - $errorBody"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveOrUpdateUserLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        address: String? = null,
        accessToken: String
    ): Result<String> {
        return try {
            // First, check if user already has a location
            val userQueryParams = mapOf("id" to "eq.$userId")
            val userResponse = service.getUsers(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                select = "id_ubicacion_usuario",
                queryParams = userQueryParams
            )
            
            val existingLocationId = userResponse.body()?.firstOrNull()?.get("id_ubicacion_usuario")?.toString()
            
            val locationData = mapOf(
                "latitud_usuario" to latitude,
                "longitud_usuario" to longitude,
                "direccion" to (address ?: ""),
                "actualizado_el" to java.time.Instant.now().toString()
            )
            
            val locationResponse = if (existingLocationId != null) {
                // Update existing location
                service.updateUserLocation(
                    apiKey = supabaseKey,
                    authorization = "Bearer $accessToken",
                    locationIdQuery = "eq.$existingLocationId",
                    locationData = locationData
                )
            } else {
                // Create new location
                service.createUserLocation(
                    apiKey = supabaseKey,
                    authorization = "Bearer $accessToken",
                    locationData = locationData
                )
            }
            
            if (!locationResponse.isSuccessful) {
                val errorBody = locationResponse.errorBody()?.string() ?: "Unknown error"
                return Result.failure(Exception("Failed to save location: ${locationResponse.code()} - $errorBody"))
            }
            
            val newLocationId = locationResponse.body()?.firstOrNull()?.get("id")?.toString()
                ?: return Result.failure(Exception("Location created but ID not returned"))
            
            // Update user with location ID if it's new
            if (existingLocationId == null) {
                val updateUserResponse = service.updateUser(
                    apiKey = supabaseKey,
                    authorization = "Bearer $accessToken",
                    userIdQuery = "eq.$userId",
                    userData = mapOf("id_ubicacion_usuario" to newLocationId)
                )
                
                if (!updateUserResponse.isSuccessful) {
                    android.util.Log.w("SupabaseRestClient", "Failed to link location to user, but location was created")
                }
            }
            
            Result.success(newLocationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkIfProfessionalExists(userId: String, accessToken: String): Result<Boolean> {
        return try {
            val response = service.getProfessionalsByUserId(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                userIdQuery = "eq.$userId",
                select = "id"
            )
            
            if (!response.isSuccessful) {
                if (response.code() == 404 || response.code() == 401) {
                    return Result.success(false)
                }
                return Result.failure(Exception("Failed to check professional: ${response.code()}"))
            }
            
            val exists = response.body()?.isNotEmpty() == true
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createProfessionalProfile(
        userId: String,
        username: String,
        specialty: String,
        workLocationId: Int,
        description: String?,
        verificationStatusId: String?,
        documentationBase64: String?,
        accessToken: String
    ): Result<String> {
        return try {
            val professionalData: Map<String, Any?> = hashMapOf(
                "id_usuario" to userId,
                "nombre_usuario_publico" to username,
                "especialidad" to specialty,
                "lugar_de_trabajo" to workLocationId,
                "descripcion" to (description ?: ""),
                "profesional_verificado" to verificationStatusId,
                "documentacion" to documentationBase64
            )
            
            val response = service.createProfessional(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                professionalData = professionalData
            )
            
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                return Result.failure(Exception("Failed to create professional: ${response.code()} - $errorBody"))
            }
            
            val professionalId = response.body()?.firstOrNull()?.get("id")?.toString()
                ?: return Result.failure(Exception("Professional created but ID not returned"))
            
            Result.success(professionalId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVerificationStatuses(accessToken: String): Result<Map<String, String>> {
        return try {
            val response = service.getVerificationStatus(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                select = "id,nombre_estado",
                statusName = null
            )
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to get verification statuses: ${response.code()}"))
            }
            
            val statuses = response.body()?.associate {
                val name = it["nombre_estado"]?.toString()?.lowercase() ?: ""
                val normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                val id = it["id"]?.toString() ?: ""
                
                // Map common status names to keys
                val key = when {
                    normalized.contains("revision") || normalized.contains("pendiente") -> "revision"
                    normalized.contains("verificado") -> "verificado"
                    normalized.contains("rechazado") -> "rechazado"
                    else -> normalized
                }
                
                key to id
            } ?: emptyMap()
            
            Result.success(statuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserLocationId(userId: String, accessToken: String): Result<String?> {
        return try {
            val queryParams = mapOf("id" to "eq.$userId")
            val response = service.getUsers(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                select = "id_ubicacion_usuario",
                queryParams = queryParams
            )
            
            if (!response.isSuccessful) {
                return Result.success(null)
            }
            
            val locationId = response.body()?.firstOrNull()?.get("id_ubicacion_usuario")?.toString()
            Result.success(locationId)
        } catch (e: Exception) {
            Result.success(null)
        }
    }
}
