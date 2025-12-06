package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ThemePreferenceManager
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.NavGraph
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.theme.GettySharpMobilAppTheme
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils.DeepLinkHandler
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils.OAuthManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val context = LocalContext.current
            val themePreference by ThemePreferenceManager.getThemePreference(context).collectAsState(initial = null)
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = themePreference ?: isSystemDark
            
            GettySharpMobilAppTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
        
        // Procesar el intent inicial si hay un deep link
        handleDeepLink(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Primero procesar cualquier deep link que haya llegado
        handleDeepLink(intent)
        
        // Luego notificar al ViewModel que la Activity resumió
        // Esto ayudará a detectar si el usuario cerró el navegador sin completar OAuth
        // Pero con un pequeño delay para dar tiempo al deep link
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            OAuthManager.notifyActivityResumed()
        }, 1000) // Esperar 1 segundo antes de notificar
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        android.util.Log.d("MainActivity", "onNewIntent called with intent: ${intent.data}")
        // Procesar el nuevo intent cuando llega un deep link
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent) {
        val oauthParams = DeepLinkHandler.extractOAuthCodeFromIntent(intent)
        if (oauthParams != null) {
            android.util.Log.d("MainActivity", "Deep link extracted, attempting to process")
            // Guardar para el composable por si acaso (fallback)
            pendingOAuthParams = oauthParams
            
            // Procesar inmediatamente - OAuthManager manejará si el ViewModel está disponible o no
            // Usar un pequeño delay para asegurarse de que estamos en el contexto correcto
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                android.util.Log.d("MainActivity", "Processing OAuth params via OAuthManager")
                OAuthManager.handleDeepLink(this@MainActivity, oauthParams)
            }, 100) // Pequeño delay para asegurar que el contexto está listo
        }
    }
    
    companion object {
        var pendingOAuthParams: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils.OAuthParams? = null
    }
}

@Composable
fun MainContent() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel<AuthViewModel>()
    
    // Registrar el ViewModel en OAuthManager y procesar deep links
    LaunchedEffect(Unit) {
        android.util.Log.d("MainContent", "Setting up OAuthManager and processing deep links")
        OAuthManager.setAuthViewModel(authViewModel)
        
        // Verificar si hay un deep link de OAuth pendiente y procesarlo
        MainActivity.pendingOAuthParams?.let { params ->
            android.util.Log.d("MainContent", "Found pending OAuth params in composable, processing...")
            OAuthManager.handleDeepLink(context, params)
            MainActivity.pendingOAuthParams = null
        }
        
        // También verificar el intent actual de la Activity
        val activity = context as? ComponentActivity
        activity?.let {
            val oauthParams = DeepLinkHandler.extractOAuthCodeFromIntent(it.intent)
            oauthParams?.let { params ->
                android.util.Log.d("MainContent", "Found OAuth params in current intent in composable, processing...")
                OAuthManager.handleDeepLink(context, params)
            }
        }
    }
    
    // Observar cambios en el intent de la Activity para procesar deep links que lleguen después
    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent?.data) {
        activity?.intent?.let { intent ->
            val oauthParams = DeepLinkHandler.extractOAuthCodeFromIntent(intent)
            oauthParams?.let { params ->
                android.util.Log.d("MainContent", "Intent data changed, processing OAuth params from intent")
                OAuthManager.handleDeepLink(context, params)
            }
        }
    }
    
    NavGraph(navController = navController)
}
