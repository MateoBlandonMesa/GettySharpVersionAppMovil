package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens.*
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
    ) {
        composable(Screen.Landing.route) {
            LandingScreen(navController = navController)
        }
        
        composable(Screen.Login.route) {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        
        composable(
            route = Screen.OAuthWebView.route,
            arguments = listOf(navArgument("oauthUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val oauthUrl = android.net.Uri.decode(backStackEntry.arguments?.getString("oauthUrl") ?: "")
            val context = LocalContext.current
            
            OAuthWebViewScreen(
                navController = navController,
                oauthUrl = oauthUrl,
                onOAuthComplete = { code, state ->
                    if (code != null) {
                        authViewModel.handleOAuthCallback(
                            context = context,
                            code = code,
                            state = state
                        )
                        navController.popBackStack()
                    }
                },
                onOAuthError = { error, errorDescription ->
                    authViewModel.handleOAuthError(
                        error = error ?: "Unknown error",
                        errorDescription = errorDescription
                    )
                    navController.popBackStack()
                },
                onCancel = {
                    authViewModel.cancelOAuth()
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(navController = navController)
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController, authViewModel = authViewModel)
        }
        
        composable(Screen.FindBarbers.route) {
            FindBarbersScreen(navController = navController)
        }
        
        composable(Screen.MyAppointments.route) {
            MyAppointmentsScreen(navController = navController)
        }
        
        composable(Screen.BarberSignup.route) {
            BarberSignupScreen(navController = navController)
        }
        
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }
    }
}


