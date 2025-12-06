package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    
    @GET("rest/v1/tbl_estados")
    suspend fun getStatuses(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @QueryMap queryParams: Map<String, String>? = null
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
            // Try to get verification status - first try exact match, then try to find by name pattern using OkHttp
            var verifiedStatusId: String? = null
            
            // First, try exact match
            val statusResponse = service.getVerificationStatus(
                apiKey = supabaseKey,
                authorization = "Bearer $supabaseKey",
                select = "id,nombre_estado",
                statusName = "eq.Verificado"
            )
            
            if (statusResponse.isSuccessful && statusResponse.body()?.isNotEmpty() == true) {
                val verifiedStatus = statusResponse.body()?.firstOrNull()
                verifiedStatusId = verifiedStatus?.get("id")?.toString()
            }
            
            // If not found, try to get all statuses using OkHttp directly and find one that contains "verificado"
            if (verifiedStatusId == null) {
                try {
                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()
                    
                    val url = "$supabaseUrl/rest/v1/tbl_estados?select=id,nombre_estado"
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("apikey", supabaseKey)
                        .header("Authorization", "Bearer $supabaseKey")
                        .header("Content-Type", "application/json")
                        .get()
                        .build()
                    
                    val response: okhttp3.Response = withContext(Dispatchers.IO) {
                        okHttpClient.newCall(request).execute()
                    }
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "[]"
                        val jsonArray = org.json.JSONArray(responseBody)
                        val allStatuses = mutableListOf<Map<String, Any?>>()
                        
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val statusMap = mutableMapOf<String, Any?>()
                            val keys = jsonObject.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                statusMap[key] = jsonObject.get(key)
                            }
                            allStatuses.add(statusMap)
                        }
                        
                        verifiedStatusId = allStatuses.firstOrNull { status ->
                            val name = (status["nombre_estado"] as? String) ?: ""
                            val normalized = normalizeStatusName(name)
                            normalized.contains("verificado")
                        }?.get("id")?.toString()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SupabaseRestClient", "Could not fetch all verification statuses: ${e.message}")
                    // Continue without filtering
                }
            }
            
            // Get professionals (without nested relations)
            val professionalsResponse = service.getProfessionals(
                apiKey = supabaseKey,
                authorization = "Bearer $supabaseKey",
                select = "id,id_usuario,profesional_verificado,lugar_de_trabajo"
            )
            
            if (!professionalsResponse.isSuccessful) {
                val errorBody = professionalsResponse.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("SupabaseRestClient", "Failed to get professionals: ${professionalsResponse.code()}, body: $errorBody")
                return Result.failure(Exception("Failed to get professionals: ${professionalsResponse.code()} - $errorBody"))
            }
            
            val professionalsData = professionalsResponse.body() ?: emptyList()
            
            if (professionalsData.isEmpty()) {
                android.util.Log.d("SupabaseRestClient", "No professionals found")
                return Result.success(emptyList())
            }
            
            // Extract user IDs
            val userIds = professionalsData.mapNotNull { prof ->
                prof["id_usuario"]?.toString()
            }.distinct()
            
            if (userIds.isEmpty()) {
                android.util.Log.d("SupabaseRestClient", "No user IDs found in professionals")
                return Result.success(emptyList())
            }
            
            // Get users with their basic info and location ID using OkHttp
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            // Build query for users: id=in.(id1,id2,id3)
            val userIdsParam = userIds.joinToString(",")
            val usersUrl = "$supabaseUrl/rest/v1/tbl_usuarios?select=id,nombre,apellido,direccion,foto_perfil,id_ubicacion_usuario&id=in.($userIdsParam)"
            val usersRequest = okhttp3.Request.Builder()
                .url(usersUrl)
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Content-Type", "application/json")
                .get()
                .build()
            
            val usersResponse: okhttp3.Response = withContext(Dispatchers.IO) {
                okHttpClient.newCall(usersRequest).execute()
            }
            
            if (!usersResponse.isSuccessful) {
                val errorBody = usersResponse.body?.string() ?: "Unknown error"
                android.util.Log.e("SupabaseRestClient", "Failed to get users: ${usersResponse.code}, body: $errorBody")
                return Result.failure(Exception("Failed to get users: ${usersResponse.code} - $errorBody"))
            }
            
            val usersResponseBody = usersResponse.body?.string() ?: "[]"
            val usersJsonArray = org.json.JSONArray(usersResponseBody)
            val usersData = mutableListOf<Map<String, Any?>>()
            
            for (i in 0 until usersJsonArray.length()) {
                val jsonObject = usersJsonArray.getJSONObject(i)
                val userMap = mutableMapOf<String, Any?>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    userMap[key] = jsonObject.get(key)
                }
                usersData.add(userMap)
            }
            
            // Extract location IDs
            val ubicacionIds = usersData.mapNotNull { user ->
                user["id_ubicacion_usuario"]?.toString()
            }.distinct().filter { it.isNotEmpty() }
            
            // Get locations using OkHttp
            val ubicacionesById = mutableMapOf<String, Map<String, Any?>>()
            if (ubicacionIds.isNotEmpty()) {
                val ubicacionIdsParam = ubicacionIds.joinToString(",")
                val ubicacionesUrl = "$supabaseUrl/rest/v1/tbl_ubicacion_usuarios?select=id,latitud_usuario,longitud_usuario&id=in.($ubicacionIdsParam)"
                val ubicacionesRequest = okhttp3.Request.Builder()
                    .url(ubicacionesUrl)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $supabaseKey")
                    .header("Content-Type", "application/json")
                    .get()
                    .build()
                
                val ubicacionesResponse: okhttp3.Response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(ubicacionesRequest).execute()
                }
                
                if (ubicacionesResponse.isSuccessful) {
                    val ubicacionesResponseBody = ubicacionesResponse.body?.string() ?: "[]"
                    val ubicacionesJsonArray = org.json.JSONArray(ubicacionesResponseBody)
                    
                    for (i in 0 until ubicacionesJsonArray.length()) {
                        val jsonObject = ubicacionesJsonArray.getJSONObject(i)
                        val ubicacionMap = mutableMapOf<String, Any?>()
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            ubicacionMap[key] = jsonObject.get(key)
                        }
                        val ubicacionId = ubicacionMap["id"]?.toString()
                        if (ubicacionId != null) {
                            ubicacionesById[ubicacionId] = ubicacionMap
                        }
                    }
                } else {
                    android.util.Log.w("SupabaseRestClient", "Failed to get locations: ${ubicacionesResponse.code}")
                }
            }
            
            // Build users map with location data
            val usersById = mutableMapOf<String, Map<String, Any?>>()
            usersData.forEach { user ->
                val userId = user["id"]?.toString() ?: return@forEach
                val ubicacionId = user["id_ubicacion_usuario"]?.toString()
                val ubicacion = if (ubicacionId != null) ubicacionesById[ubicacionId] else null
                
                val enrichedUser = mutableMapOf<String, Any?>().apply {
                    putAll(user)
                    put("latitud", ubicacion?.get("latitud_usuario"))
                    put("longitud", ubicacion?.get("longitud_usuario"))
                    put("ubicacionId", ubicacionId)
                }
                usersById[userId] = enrichedUser
            }
            
            // Combine professionals with user and location data
            @Suppress("UNCHECKED_CAST")
            val allBarbers = professionalsData.mapNotNull { prof ->
                try {
                    val profId = prof["id"]?.toString() ?: return@mapNotNull null
                    val userId = prof["id_usuario"]?.toString()
                    val userData = if (userId != null) usersById[userId] else null
                    
                    Barber(
                        id = profId,
                        nombre = userData?.get("nombre")?.toString(),
                        apellido = userData?.get("apellido")?.toString(),
                        fotoPerfil = userData?.get("foto_perfil")?.toString(),
                        latitud = (userData?.get("latitud") as? Number)?.toDouble(),
                        longitud = (userData?.get("longitud") as? Number)?.toDouble(),
                        direccion = userData?.get("direccion")?.toString(),
                        ubicacionId = userData?.get("ubicacionId")?.toString(),
                        verificationStatusId = prof["profesional_verificado"]?.toString(),
                        lugarDeTrabajo = prof["lugar_de_trabajo"]?.toString()?.toIntOrNull(),
                        ratingAverage = null,
                        ratingsCount = null
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseRestClient", "Error creating barber: ${e.message}", e)
                    null
                }
            }
            
            // Filter by verification status if found, otherwise return all professionals
            val barbers = if (verifiedStatusId != null) {
                allBarbers.filter { it.verificationStatusId == verifiedStatusId }
            } else {
                // Return all professionals if verification status not found
                android.util.Log.d("SupabaseRestClient", "Verification status not found, returning all professionals")
                allBarbers
            }
            
            android.util.Log.d("SupabaseRestClient", "Loaded ${barbers.size} barbers (${allBarbers.size} total, filtered by status: ${verifiedStatusId != null})")
            Result.success(barbers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun normalizeStatusName(name: String): String {
        return java.text.Normalizer.normalize(name.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
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
            
            // Get gender ID and enrich with name from tbl_generos
            val genderValue = userData["genero"]
            val genderId = when (genderValue) {
                is Number -> genderValue.toInt()
                is String -> genderValue.toIntOrNull()
                else -> null
            }
            
            // Get gender name from tbl_generos
            var genderName = ""
            if (genderId != null) {
                try {
                    val gendersResponse = service.getGenders(
                        apiKey = supabaseKey,
                        authorization = "Bearer $accessToken",
                        select = "id,genero"
                    )
                    
                    if (gendersResponse.isSuccessful) {
                        val gendersData = gendersResponse.body() ?: emptyList()
                        val genderMap = gendersData.find { genderMap ->
                            val id = when (val idValue = genderMap["id"]) {
                                is Number -> idValue.toInt()
                                is String -> idValue.toIntOrNull()
                                else -> null
                            }
                            id == genderId
                        }
                        
                        genderName = genderMap?.get("genero")?.toString() ?: ""
                        if (genderName.isEmpty()) {
                            android.util.Log.w("SupabaseRestClient", "Gender with ID $genderId not found in tbl_generos")
                            genderName = genderId.toString() // Fallback to ID if not found
                        } else {
                            android.util.Log.d("SupabaseRestClient", "Enriched gender ID $genderId to name: $genderName")
                        }
                    } else {
                        android.util.Log.w("SupabaseRestClient", "Failed to load genders: ${gendersResponse.code()}")
                        genderName = genderId.toString() // Fallback to ID if API call fails
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseRestClient", "Error loading gender name: ${e.message}", e)
                    genderName = genderId?.toString() ?: "" // Fallback to ID if error
                }
            } else {
                // If gender is already a string name, use it directly
                genderName = genderValue?.toString() ?: ""
            }
            
            val profile = UserProfile(
                id = userData["id"]?.toString(),
                firstName = userData["nombre"]?.toString() ?: "",
                lastName = userData["apellido"]?.toString() ?: "",
                email = userData["email"]?.toString() ?: "",
                phone = formatPhoneNumber(userData["telefono"]) ?: "",
                idType = userData["tipo_documento"]?.toString() ?: "",
                idNumber = userData["numero_documento"]?.toString() ?: "",
                gender = genderName,
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
        return withContext(Dispatchers.IO) {
            try {
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
                    return@withContext Result.success(null)
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
    
    suspend fun getGenders(accessToken: String): Result<List<co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Gender>> {
        return try {
            val response = service.getGenders(
                apiKey = supabaseKey,
                authorization = "Bearer $accessToken",
                select = "id,genero"
            )
            
            if (response.isSuccessful) {
                val gendersData = response.body() ?: emptyList()
                val genders = gendersData.mapNotNull { genderMap ->
                    try {
                        val id = when (val idValue = genderMap["id"]) {
                            is Number -> idValue.toInt()
                            is String -> idValue.toIntOrNull()
                            else -> null
                        }
                        val genero = genderMap["genero"]?.toString() ?: ""
                        
                        if (id != null && genero.isNotEmpty()) {
                            co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Gender(
                                id = id,
                                genero = genero
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SupabaseRestClient", "Error parsing gender: ${e.message}")
                        null
                    }
                }
                Result.success(genders)
            } else {
                Result.failure(Exception("Failed to get genders: ${response.code()}"))
            }
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
    
    private fun formatPhoneNumber(phoneValue: Any?): String? {
        if (phoneValue == null) return null
        
        return when (phoneValue) {
            is Number -> {
                // Use toLong() to handle large numbers properly, then convert to string
                // This avoids scientific notation for large integers
                when (phoneValue) {
                    is Double, is Float -> {
                        // For floating point numbers, convert to long first
                        phoneValue.toLong().toString()
                    }
                    else -> {
                        // For integer types, use toLong() to handle large numbers
                        phoneValue.toLong().toString()
                    }
                }
            }
            is String -> phoneValue
            else -> phoneValue.toString()
        }
    }
}
