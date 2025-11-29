package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils.OAuthDeepLinkHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthWebViewScreen(
    navController: NavController,
    oauthUrl: String,
    onOAuthComplete: (String?, String?) -> Unit,
    onOAuthError: (String?, String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar sesión") },
                navigationIcon = {
                    IconButton(onClick = {
                        webView?.goBack()
                        if (!canGoBack) {
                            onCancel()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = false
                            displayZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                canGoBack = view?.canGoBack() ?: false
                                
                                // Interceptar URLs que sean deep links
                                url?.let { currentUrl ->
                                    val uri = Uri.parse(currentUrl)
                                    
                                    // Verificar si es nuestro deep link
                                    if (uri.scheme == "gettysharp" && uri.host == "oauth") {
                                        // Extraer parámetros OAuth
                                        val oauthParams = OAuthDeepLinkHandler.extractOAuthParams(uri)
                                        
                                        if (oauthParams != null) {
                                            if (oauthParams.error != null) {
                                                onOAuthError(oauthParams.error, oauthParams.errorDescription)
                                            } else if (oauthParams.code != null) {
                                                onOAuthComplete(oauthParams.code, oauthParams.state)
                                            }
                                            // Detener la carga ya que manejamos el redirect
                                            view?.stopLoading()
                                            return
                                        }
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                // Interceptar deep links
                                val uri = Uri.parse(url)
                                if (uri.scheme == "gettysharp" && uri.host == "oauth") {
                                    val oauthParams = OAuthDeepLinkHandler.extractOAuthParams(uri)
                                    
                                    if (oauthParams != null) {
                                        if (oauthParams.error != null) {
                                            onOAuthError(oauthParams.error, oauthParams.errorDescription)
                                        } else if (oauthParams.code != null) {
                                            onOAuthComplete(oauthParams.code, oauthParams.state)
                                        }
                                        return true // Interceptamos el redirect
                                    }
                                }
                                
                                return false // Permitir la navegación normal
                            }
                        }

                        // Cargar la URL OAuth
                        loadUrl(oauthUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

