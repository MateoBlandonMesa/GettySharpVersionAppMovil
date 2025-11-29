package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
}
