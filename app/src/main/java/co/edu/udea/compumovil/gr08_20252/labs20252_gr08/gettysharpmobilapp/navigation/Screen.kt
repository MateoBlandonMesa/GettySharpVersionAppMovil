package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object OAuthWebView : Screen("oauth_webview/{oauthUrl}") {
        fun createRoute(oauthUrl: String) = "oauth_webview/${android.net.Uri.encode(oauthUrl)}"
    }
    object ProfileSetup : Screen("profile_setup")
    object Dashboard : Screen("dashboard")
    object FindBarbers : Screen("find_barbers")
    object MyAppointments : Screen("my_appointments/{mode}") {
        fun createRoute(mode: String? = null) = "my_appointments/${mode ?: ""}"
    }
    object BarberSignup : Screen("barber_signup")
    object EditProfile : Screen("edit_profile")
    object Approvals : Screen("approvals")
}


