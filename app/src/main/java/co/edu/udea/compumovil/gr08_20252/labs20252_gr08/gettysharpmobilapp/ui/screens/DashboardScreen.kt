package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    // Observar el estado de autenticación para redirigir si se cierra sesión
    val uiState by authViewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isAuthenticated) {
        if (!uiState.isAuthenticated) {
            navController.navigate(Screen.Landing.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Getty Sharp") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.EditProfile.route) }) {
                        Icon(Icons.Default.Settings, "Configuración")
                    }
                    
                    // Menú desplegable para cerrar sesión
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Más opciones")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                showMenu = false
                                authViewModel.signOut(context)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Screen.FindBarbers.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Buscar Barberos",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Encuentra barberos cerca de ti",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Screen.MyAppointments.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mis Citas",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Gestiona tus citas",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Screen.BarberSignup.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "¡Hazte Barbero!",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Regístrate como profesional",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}

