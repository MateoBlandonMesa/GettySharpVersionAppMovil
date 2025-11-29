package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils

import android.content.Context
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseAuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object OAuthManager {
    private var authViewModel: AuthViewModel? = null
    private var pendingDeepLink: Pair<Context, OAuthParams>? = null
    
    fun setAuthViewModel(viewModel: AuthViewModel) {
        android.util.Log.d("OAuthManager", "setAuthViewModel called")
        authViewModel = viewModel
        
        // Procesar cualquier deep link pendiente ahora que el ViewModel está disponible
        pendingDeepLink?.let { (context, oauthParams) ->
            android.util.Log.d("OAuthManager", "Processing pending deep link now that ViewModel is available")
            pendingDeepLink = null
            handleDeepLinkInternal(context, oauthParams, viewModel)
        }
    }
    
    fun notifyActivityResumed() {
        authViewModel?.onResume()
    }
    
    fun handleDeepLink(context: Context, oauthParams: OAuthParams) {
        android.util.Log.d("OAuthManager", "handleDeepLink called with oauthParams: accessToken=${oauthParams.accessToken != null}, code=${oauthParams.code != null}, error=${oauthParams.error}")
        
        authViewModel?.let { viewModel ->
            android.util.Log.d("OAuthManager", "AuthViewModel found, processing deep link immediately")
            handleDeepLinkInternal(context, oauthParams, viewModel)
        } ?: run {
            android.util.Log.w("OAuthManager", "AuthViewModel is null! Storing deep link for later processing.")
            // Guardar para procesar más tarde cuando el ViewModel esté disponible
            pendingDeepLink = Pair(context, oauthParams)
        }
    }
    
    private fun handleDeepLinkInternal(context: Context, oauthParams: OAuthParams, viewModel: AuthViewModel) {
        android.util.Log.d("OAuthManager", "handleDeepLinkInternal: processing deep link")
        CoroutineScope(Dispatchers.Main).launch {
            if (oauthParams.error != null) {
                android.util.Log.e("OAuthManager", "OAuth error: ${oauthParams.error}")
                viewModel.handleOAuthError(
                    error = oauthParams.error,
                    errorDescription = oauthParams.errorDescription
                )
            } else if (oauthParams.accessToken != null) {
                // Si tenemos access_token directamente del fragmento (flujo implícito)
                android.util.Log.d("OAuthManager", "Processing access_token directly")
                viewModel.handleOAuthTokenDirectly(
                    context = context,
                    accessToken = oauthParams.accessToken,
                    refreshToken = oauthParams.refreshToken,
                    expiresIn = oauthParams.expiresIn
                )
            } else if (oauthParams.code != null) {
                // Flujo de código de autorización normal
                android.util.Log.d("OAuthManager", "Processing authorization code")
                viewModel.handleOAuthCallback(
                    context = context,
                    code = oauthParams.code,
                    state = oauthParams.state
                )
            } else {
                android.util.Log.w("OAuthManager", "No valid OAuth parameters found")
            }
        }
    }
}

