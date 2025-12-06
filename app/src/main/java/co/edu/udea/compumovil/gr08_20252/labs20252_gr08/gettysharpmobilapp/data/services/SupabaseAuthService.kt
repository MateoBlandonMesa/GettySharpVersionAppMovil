package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services

import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long?,
    val userId: String?,
    val email: String?,
    val error: String?
)

object SupabaseAuthService {
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_KEY
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun signInWithOAuth(provider: String): Result<String> {
        // Retorna la URL de OAuth que se debe abrir en un navegador
        // En Android, esto se maneja con CustomTabs o un WebView
        val redirectUrl = "gettysharp://oauth/callback"
        
        // Construir URL con parámetros codificados correctamente
        // IMPORTANTE: El redirect_to debe estar configurado en Supabase Dashboard
        val encodedRedirect = java.net.URLEncoder.encode(redirectUrl, "UTF-8")
        
        // Agregar prompt=select_account para forzar que Google muestre la pantalla de selección de cuenta
        // NOTA: Google puede ignorar esto si tiene una sesión persistente en el navegador
        // El usuario puede necesitar cerrar sesión manualmente en el navegador para cambiar de cuenta
        val authUrl = buildString {
            append("$supabaseUrl/auth/v1/authorize?")
            append("provider=$provider")
            append("&redirect_to=$encodedRedirect")
            append("&apikey=$supabaseKey")
            
            // Intentar agregar query_params para forzar selección de cuenta
            // Supabase espera query_params como JSON codificado
            if (provider == "google") {
                // Formato: {"prompt":"select_account"}
                val queryParamsJson = """{"prompt":"select_account"}"""
                val encodedParams = java.net.URLEncoder.encode(queryParamsJson, "UTF-8")
                append("&query_params=$encodedParams")
            }
        }
        
        android.util.Log.d("SupabaseAuthService", "OAuth URL generated: $authUrl")
        
        return Result.success(authUrl)
    }
    
    suspend fun handleOAuthTokenDirectly(
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long?
    ): Result<AuthResponse> {
        return try {
            android.util.Log.d("SupabaseAuthService", "handleOAuthTokenDirectly: Starting token processing")
            // Cuando el access_token viene directamente del fragmento, necesitamos obtener
            // el userId del token JWT
            val userId = extractUserIdFromToken(accessToken)
            val email = extractEmailFromToken(accessToken)
            
            android.util.Log.d("SupabaseAuthService", "Extracted userId: $userId, email: $email")
            
            if (userId == null) {
                android.util.Log.e("SupabaseAuthService", "Failed to extract userId from token")
                return Result.failure(Exception("No se pudo extraer userId del token"))
            }
            
            val authResponse = AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                userId = userId,
                email = email,
                error = null
            )
            
            android.util.Log.d("SupabaseAuthService", "Successfully created AuthResponse")
            Result.success(authResponse)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseAuthService", "Error in handleOAuthTokenDirectly: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun extractUserIdFromToken(token: String): String? {
        return try {
            // El JWT tiene 3 partes separadas por puntos: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            // Decodificar el payload (segunda parte)
            val payload = parts[1]
            // Agregar padding si es necesario para base64
            val paddedPayload = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decodedBytes = android.util.Base64.decode(paddedPayload, android.util.Base64.URL_SAFE)
            val decodedString = String(decodedBytes)
            
            // Parsear el JSON para obtener el sub (user id)
            val json = org.json.JSONObject(decodedString)
            json.optString("sub", "").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseAuthService", "Error extracting userId from token: ${e.message}")
            null
        }
    }
    
    private fun extractEmailFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            val paddedPayload = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decodedBytes = android.util.Base64.decode(paddedPayload, android.util.Base64.URL_SAFE)
            val decodedString = String(decodedBytes)
            
            val json = org.json.JSONObject(decodedString)
            json.optString("email", "").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun handleOAuthCallback(code: String, state: String? = null): Result<AuthResponse> {
        return try {
            // Intercambiar el código por tokens
            val exchangeUrl = "$supabaseUrl/auth/v1/token?grant_type=authorization_code"
            
            val jsonBody = JSONObject().apply {
                put("code", code)
                state?.let { put("state", it) }
            }
            
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(exchangeUrl)
                .post(requestBody)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("OAuth callback failed: ${response.code}"))
            }
            
            val json = JSONObject(responseBody ?: "{}")
            
            val authResponse = AuthResponse(
                accessToken = json.optString("access_token", "").takeIf { it.isNotEmpty() },
                refreshToken = json.optString("refresh_token", "").takeIf { it.isNotEmpty() },
                expiresIn = json.optLong("expires_in", 0).takeIf { it > 0 },
                userId = json.optJSONObject("user")?.optString("id", "")?.takeIf { it.isNotEmpty() },
                email = json.optJSONObject("user")?.optString("email", "")?.takeIf { it.isNotEmpty() },
                error = null
            )
            
            Result.success(authResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        return try {
            val refreshUrl = "$supabaseUrl/auth/v1/token?grant_type=refresh_token"
            
            val jsonBody = JSONObject().apply {
                put("refresh_token", refreshToken)
            }
            
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(refreshUrl)
                .post(requestBody)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("Token refresh failed: ${response.code}"))
            }
            
            val json = JSONObject(responseBody ?: "{}")
            
            val authResponse = AuthResponse(
                accessToken = json.optString("access_token", "").takeIf { it.isNotEmpty() },
                refreshToken = json.optString("refresh_token", "").takeIf { it.isNotEmpty() },
                expiresIn = json.optLong("expires_in", 0).takeIf { it > 0 },
                userId = null, // Refresh no devuelve user
                email = null,
                error = null
            )
            
            Result.success(authResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(accessToken: String): Result<Unit> {
        return try {
            val signOutUrl = "$supabaseUrl/auth/v1/logout"
            
            val request = Request.Builder()
                .url(signOutUrl)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Sign out failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

