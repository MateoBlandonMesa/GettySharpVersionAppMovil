package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ThemePreferenceManager
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.DashboardViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    val authUiState by authViewModel.uiState.collectAsState()
    val dashboardUiState by dashboardViewModel.uiState.collectAsState()
    
    // Theme state
    val themePreference by ThemePreferenceManager.getThemePreference(context).collectAsState(initial = null)
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = themePreference ?: isSystemDark
    val scope = rememberCoroutineScope()
    
    // Use profile from AuthViewModel if available, otherwise load from DashboardViewModel
    val currentProfile = remember(dashboardUiState.profile, authUiState.userProfile) {
        dashboardUiState.profile ?: authUiState.userProfile
    }
    
    // Load profile when screen appears if not available
    LaunchedEffect(Unit) {
        if (currentProfile == null && !dashboardUiState.isLoading) {
            dashboardViewModel.loadProfile(context)
        }
    }
    
    // Redirect if not authenticated
    LaunchedEffect(authUiState.isAuthenticated) {
        if (!authUiState.isAuthenticated) {
            navController.navigate(Screen.Landing.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Getty Sharp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.EditProfile.route) }) {
                        Icon(Icons.Default.Edit, "Editar Perfil")
                    }
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Más opciones")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (isDarkTheme) "☀️ Modo Claro" else "🌙 Modo Oscuro",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            },
                            onClick = {
                                scope.launch {
                                    ThemePreferenceManager.saveThemePreference(
                                        context,
                                        !isDarkTheme
                                    )
                                }
                                showMenu = false
                            }
                        )
                        
                        Divider()
                        
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
        when {
            dashboardUiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Cargando perfil...")
                    }
                }
            }
            dashboardUiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error: ${dashboardUiState.errorMessage}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { dashboardViewModel.refreshProfile(context) }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            currentProfile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se pudo cargar el perfil")
                }
            }
            else -> {
                val profile = currentProfile!!
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Card
                    ProfileCard(
                        profile = profile,
                        onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                        onBecomeBarber = {
                            navController.navigate(Screen.BarberSignup.route)
                        }
                    )
                    
                    // Quick Actions Card
                    QuickActionsCard(
                        profile = profile,
                        onFindBarbers = { navController.navigate(Screen.FindBarbers.route) },
                        onMyAppointments = { navController.navigate(Screen.MyAppointments.route) },
                        onManageAppointments = { 
                            navController.navigate("${Screen.MyAppointments.route}?mode=manage")
                        },
                        onPublishAvailability = {
                            // Will be implemented when we create the dialog
                            navController.navigate(Screen.EditProfile.route)
                        },
                        onApprovals = { navController.navigate(Screen.Approvals.route) }
                    )
                    
                    // Personal Information Card
                    PersonalInformationCard(profile = profile)
                    
                    // Professional Information Card (if barber)
                    if (profile.isBarber) {
                        ProfessionalInformationCard(profile = profile)
                    }
                    
                    if (profile.isBarber && !profile.verified) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Tu perfil profesional está en ${profile.verificationStatus ?: "revisión"}. Una vez sea verificado podrás gestionar citas, publicar disponibilidad y configurar tus servicios.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile,
    onEditProfile: () -> Unit,
    onBecomeBarber: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Image
            if (profile.fotoPerfil != null && profile.fotoPerfil.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profile.fotoPerfil)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${profile.firstName.firstOrNull() ?: ""}${profile.lastName.firstOrNull() ?: ""}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Name
            Text(
                text = if (profile.isBarber && !profile.username.isNullOrEmpty()) {
                    profile.username
                } else {
                    "${profile.firstName} ${profile.lastName}".trim()
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Role Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.isBarber) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Barbero Profesional",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Verification Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (profile.verified) {
                            Color(0xFF10B981)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (profile.verified) "✓ Verificado" else "⏳ En Revisión",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profile.verified) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Cliente Premium",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Ratings (if barber)
            if (profile.isBarber && profile.ratingsCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFE07410),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${String.format("%.1f", profile.rating)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${profile.ratingsCount} reseña${if (profile.ratingsCount == 1) "" else "s"})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Edit Profile Button
            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Perfil")
            }
            
            // Become Barber Button (if not barber)
            if (!profile.isBarber) {
                OutlinedButton(
                    onClick = onBecomeBarber,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¡Hazte Barbero!")
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    profile: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile,
    onFindBarbers: () -> Unit,
    onMyAppointments: () -> Unit,
    onManageAppointments: () -> Unit,
    onPublishAvailability: () -> Unit,
    onApprovals: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Acciones Rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()
            
            if (profile.isBarber) {
                // Barbero actions
                if (profile.verified) {
                    QuickActionButton(
                        icon = Icons.Default.Star,
                        text = "Panel de citas clientes",
                        onClick = onManageAppointments,
                        enabled = true
                    )
                    
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        text = "Publicar disponibilidad",
                        onClick = onPublishAvailability,
                        enabled = true
                    )
                } else {
                    QuickActionButton(
                        icon = Icons.Default.Star,
                        text = "Panel de citas clientes",
                        onClick = onManageAppointments,
                        enabled = false
                    )
                    
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        text = "Publicar disponibilidad",
                        onClick = onPublishAvailability,
                        enabled = false
                    )
                }
                
                QuickActionButton(
                    icon = Icons.Default.Search,
                    text = "Buscar Barberos",
                    onClick = onFindBarbers,
                    enabled = true
                )
                
                QuickActionButton(
                    icon = Icons.Default.Star,
                    text = "Mis Citas",
                    onClick = onMyAppointments,
                    enabled = true
                )
            } else {
                // Cliente actions
                QuickActionButton(
                    icon = Icons.Default.Search,
                    text = "Buscar Barberos",
                    onClick = onFindBarbers,
                    enabled = true
                )
                
                QuickActionButton(
                    icon = Icons.Default.Star,
                    text = "Mis Citas",
                    onClick = onMyAppointments,
                    enabled = true
                )
            }
            
            // Approver action
            if (profile.isApprover) {
                QuickActionButton(
                    icon = Icons.Default.Info,
                    text = "Gestionar Verificaciones",
                    onClick = onApprovals,
                    enabled = true
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            if (!enabled) {
                Text(
                    text = "Requiere verificación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun PersonalInformationCard(
    profile: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Información Personal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()
            
            InfoRow(
                label = "Nombre Completo",
                value = "${profile.firstName} ${profile.lastName}".trim()
            )
            
            InfoRow(
                label = "Correo Electrónico",
                value = profile.email,
                icon = Icons.Default.Email
            )
            
            if (profile.phone.isNotEmpty()) {
                InfoRow(
                    label = "Teléfono",
                    value = profile.phone,
                    icon = Icons.Default.Phone
                )
            }
            
            if (profile.address.isNotEmpty()) {
                InfoRow(
                    label = "Dirección",
                    value = profile.address,
                    icon = Icons.Default.LocationOn
                )
            }
            
            if (profile.gender.isNotEmpty()) {
                InfoRow(
                    label = "Género",
                    value = profile.gender.replaceFirstChar { it.uppercaseChar() }
                )
            }
        }
    }
}

@Composable
fun ProfessionalInformationCard(
    profile: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.UserProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Información Profesional",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()
            
            if (!profile.specialty.isNullOrEmpty()) {
                InfoRow(
                    label = "Especialidad",
                    value = profile.specialty
                )
            }
            
            if (!profile.workLocation.isNullOrEmpty()) {
                InfoRow(
                    label = "Lugar de Trabajo",
                    value = profile.workLocation,
                    icon = Icons.Default.LocationOn
                )
            }
            
            if (!profile.description.isNullOrEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Descripción de Servicios",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        profile.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            InfoRow(
                label = "Estado de verificación",
                value = profile.verificationStatus ?: "En revisión"
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                value.ifEmpty { "No especificado" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
