package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils

import android.net.Uri

object OAuthDeepLinkHandler {
    fun extractOAuthParams(uri: Uri): OAuthParams? {
        // Formato esperado: 
        // - gettysharp://oauth/callback?code=xxx&state=xxx (authorization code flow)
        // - gettysharp://oauth/callback#access_token=xxx&refresh_token=xxx (implicit flow)
        android.util.Log.d("OAuthDeepLinkHandler", "Extracting params from URI: $uri")
        android.util.Log.d("OAuthDeepLinkHandler", "Scheme: ${uri.scheme}, Host: ${uri.host}, Path: ${uri.path}")
        android.util.Log.d("OAuthDeepLinkHandler", "Fragment from uri.fragment: ${uri.fragment}")
        
        if (uri.scheme != "gettysharp" || uri.host != "oauth") {
            android.util.Log.d("OAuthDeepLinkHandler", "URI does not match expected scheme/host")
            return null
        }
        
        // Extraer fragmento manualmente del string del URI primero (para flujo implícito)
        val uriString = uri.toString()
        var fragment: String? = null
        
        // Buscar el fragmento después del #
        val fragmentIndex = uriString.indexOf('#')
        if (fragmentIndex != -1 && fragmentIndex < uriString.length - 1) {
            fragment = uriString.substring(fragmentIndex + 1)
            android.util.Log.d("OAuthDeepLinkHandler", "Found fragment manually from URI string")
        } else {
            // Si no encontramos #, intentar con uri.fragment
            fragment = uri.fragment
        }
        
        // Si hay fragmento, intentar extraer parámetros del fragmento primero (flujo implícito)
        if (fragment != null && fragment.isNotEmpty()) {
            android.util.Log.d("OAuthDeepLinkHandler", "No query params found, checking fragment")
            
            // Parsear el fragmento como si fuera una query string
            val fragmentParams = mutableMapOf<String, String>()
            fragment.split("&").forEach { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0]
                    // No decodificar aquí, los valores pueden tener caracteres que se decodifican incorrectamente
                    // Los tokens JWT no deben decodificarse como URL
                    val value = parts[1]
                    fragmentParams[key] = value
                    android.util.Log.d("OAuthDeepLinkHandler", "Fragment param: $key = ${value.take(50)}...")
                }
            }
            
            android.util.Log.d("OAuthDeepLinkHandler", "Fragment params keys: ${fragmentParams.keys}")
            
            // Verificar si tenemos access_token en el fragmento
            val accessToken = fragmentParams["access_token"]
            val refreshToken = fragmentParams["refresh_token"]
            val expiresAtStr = fragmentParams["expires_at"]
            val expiresInStr = fragmentParams["expires_in"]
            val fragmentError = fragmentParams["error"]
            val fragmentErrorDesc = fragmentParams["error_description"]
            val fragmentState = fragmentParams["state"]
            
            if (fragmentError != null) {
                android.util.Log.e("OAuthDeepLinkHandler", "OAuth error in fragment: $fragmentError")
                return OAuthParams(
                    code = null,
                    state = fragmentState,
                    error = fragmentError,
                    errorDescription = fragmentErrorDesc
                )
            }
            
            if (accessToken != null) {
                // Calcular expiresIn a partir de expiresAt o usar expires_in
                val expiresIn = expiresInStr?.toLongOrNull() ?: expiresAtStr?.toLongOrNull()?.let { 
                    val currentTime = System.currentTimeMillis() / 1000
                    (it - currentTime).coerceAtLeast(0)
                }
                
                android.util.Log.d("OAuthDeepLinkHandler", "Found access_token in fragment, expiresIn: $expiresIn")
                return OAuthParams(
                    code = null, // No hay código en el flujo implícito
                    state = fragmentState,
                    error = null,
                    errorDescription = null,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn
                )
            }
        }
        
        // Si no se encontró nada en el fragmento, intentar query parameters (authorization code flow)
        var code = uri.getQueryParameter("code")
        var state = uri.getQueryParameter("state")
        var error = uri.getQueryParameter("error")
        var errorDescription = uri.getQueryParameter("error_description")
        
        android.util.Log.d("OAuthDeepLinkHandler", "Code: $code, State: $state, Error: $error")
        
        if (error != null) {
            android.util.Log.e("OAuthDeepLinkHandler", "OAuth error: $error - $errorDescription")
            return OAuthParams(
                code = null,
                state = state,
                error = error,
                errorDescription = errorDescription
            )
        }
        
        if (code == null) {
            android.util.Log.w("OAuthDeepLinkHandler", "No code or access_token found in URI")
            return null
        }
        
        android.util.Log.d("OAuthDeepLinkHandler", "Successfully extracted OAuth parameters")
        return OAuthParams(
            code = code,
            state = state,
            error = null,
            errorDescription = null
        )
    }
}

data class OAuthParams(
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null
)
