package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.SupabaseRestClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AppointmentViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.BarbersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindBarbersScreen(
    navController: NavController,
    barbersViewModel: BarbersViewModel = viewModel(),
    appointmentViewModel: AppointmentViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by barbersViewModel.uiState.collectAsState()
    val appointmentState by appointmentViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    
    var selectedBarber by remember { mutableStateOf<co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber?>(null) }
    var showAppointmentDialog by remember { mutableStateOf(false) }
    var clientLocationId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Load client location when dialog opens
    LaunchedEffect(showAppointmentDialog) {
        if (showAppointmentDialog) {
            val session = AuthService.getSession(context)
            val accessToken = session?.accessToken
            val userId = session?.userId
            
            if (accessToken != null && userId != null) {
                val result = SupabaseRestClient.getUserLocationId(userId, accessToken)
                result.fold(
                    onSuccess = { locationId ->
                        clientLocationId = locationId
                    },
                    onFailure = {
                        clientLocationId = null
                    }
                )
            }
            
            // Load availability when dialog opens
            selectedBarber?.let { barber ->
                appointmentViewModel.loadAvailability(barber.id)
            }
        }
    }
    
    // Handle appointment creation success
    LaunchedEffect(appointmentState.success) {
        if (appointmentState.success) {
            Toast.makeText(context, "¡Cita creada exitosamente!", Toast.LENGTH_LONG).show()
            appointmentViewModel.resetSuccess()
            showAppointmentDialog = false
            selectedBarber = null
            barbersViewModel.refreshBarbers()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar Barberos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { barbersViewModel.refreshBarbers() }) {
                        Text("Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.errorMessage}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { barbersViewModel.refreshBarbers() }) {
                            Text("Reintentar")
                        }
                    }
                }
                uiState.barbers.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No hay barberos disponibles",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { barbersViewModel.refreshBarbers() }) {
                            Text("Actualizar")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.barbers) { barber ->
                            BarberCard(
                                barber = barber,
                                onClick = {
                                    selectedBarber = barber
                                    showAppointmentDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Complete Appointment Dialog
    if (showAppointmentDialog && selectedBarber != null) {
        AppointmentDialog(
            barber = selectedBarber!!,
            appointmentViewModel = appointmentViewModel,
            clientLocationId = clientLocationId,
            barberLocationId = selectedBarber!!.ubicacionId,
            barberWorkLocation = selectedBarber!!.lugarDeTrabajo,
            clientId = authState.userProfile?.id,
            onDismiss = {
                showAppointmentDialog = false
                selectedBarber = null
                appointmentViewModel.resetSuccess()
            },
            onRefreshAvailability = {
                selectedBarber?.let { barber ->
                    appointmentViewModel.loadAvailability(barber.id)
                }
            }
        )
    }
}

@Composable
fun BarberCard(
    barber: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile image
            if (barber.fotoPerfil != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(barber.fotoPerfil)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(80.dp)
                        .weight(0f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${barber.nombre ?: ""} ${barber.apellido ?: ""}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (barber.direccion != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = barber.direccion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (barber.ratingAverage != null && barber.ratingsCount != null && barber.ratingsCount!! > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format("%.1f", barber.ratingAverage) + " (${barber.ratingsCount} reseñas)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Sin calificaciones aún",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDialog(
    barber: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber,
    appointmentViewModel: AppointmentViewModel,
    clientLocationId: String?,
    barberLocationId: String?,
    barberWorkLocation: Int?,
    clientId: String?,
    onDismiss: () -> Unit,
    onRefreshAvailability: () -> Unit
) {
    val appointmentState by appointmentViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedSlot by remember { mutableStateOf<AvailabilityBlock?>(null) }
    var locationChoice by remember { mutableStateOf<String?>(null) }
    
    // Determine available location options
    val canShowHomeOption = (barberWorkLocation == 1 || barberWorkLocation == 3) && clientLocationId != null
    val canShowLocalOption = (barberWorkLocation == 2 || barberWorkLocation == 3) && barberLocationId != null
    
    // Auto-select location if only one option available
    LaunchedEffect(canShowHomeOption, canShowLocalOption) {
        if (canShowHomeOption && !canShowLocalOption) {
            locationChoice = "home"
        } else if (canShowLocalOption && !canShowHomeOption) {
            locationChoice = "local"
        } else {
            locationChoice = null
        }
    }
    
    // Reset selected slot if it's no longer available
    LaunchedEffect(appointmentState.availability) {
        selectedSlot?.let { slot ->
            val availableSlots = appointmentViewModel.getAvailableSlots()
            if (!availableSlots.any { it.id == slot.id }) {
                selectedSlot = null
            }
        }
    }
    
    val availableSlots = appointmentViewModel.getAvailableSlots()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Agendar cita con ${barber.nombre ?: "Barbero"}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (barber.ratingAverage != null && barber.ratingsCount != null && barber.ratingsCount!! > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFD700))
                                Text(
                                    text = String.format("%.1f", barber.ratingAverage) + " (${barber.ratingsCount} reseñas)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Divider()
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Availability Section
                    Text(
                        text = "Disponibilidad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (appointmentState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (appointmentState.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = appointmentState.errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Button(
                            onClick = onRefreshAvailability,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reintentar")
                        }
                    } else if (availableSlots.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Este profesional no tiene bloques de disponibilidad publicados.",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        availableSlots.forEach { slot ->
                            val isSelected = selectedSlot?.id == slot.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSlot = slot
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appointmentViewModel.formatAvailabilitySlot(slot),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Location Selection
                    Text(
                        text = "Ubicación de la cita",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (canShowHomeOption) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    locationChoice = "home"
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (locationChoice == "home") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (locationChoice == "home") {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null)
                                    Column {
                                        Text(
                                            text = "En mi domicilio",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (locationChoice == "home") FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "El barbero vendrá a tu casa",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (locationChoice == "home") {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    if (canShowLocalOption) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    locationChoice = "local"
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (locationChoice == "local") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (locationChoice == "local") {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                    Column {
                                        Text(
                                            text = "En el local del barbero",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (locationChoice == "local") FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = barber.direccion ?: "Local del barbero",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (locationChoice == "local") {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    if (!canShowHomeOption && !canShowLocalOption) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "No hay opciones de ubicación disponibles. Por favor, completa tu dirección en tu perfil.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                
                Divider()
                
                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            if (selectedSlot == null) {
                                Toast.makeText(context, "Selecciona un horario disponible", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            if (locationChoice == null) {
                                Toast.makeText(context, "Selecciona una ubicación para la cita", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            if (clientId == null) {
                                Toast.makeText(context, "Error: No se encontró el ID del cliente", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val locationId = when (locationChoice) {
                                "home" -> {
                                    if (clientLocationId == null) {
                                        Toast.makeText(context, "Completa tu dirección en tu perfil para recibir citas en casa", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    clientLocationId
                                }
                                "local" -> {
                                    if (barberLocationId == null) {
                                        Toast.makeText(context, "El barbero no tiene dirección registrada", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    barberLocationId
                                }
                                else -> null
                            }
                            
                            if (locationId == null) {
                                Toast.makeText(context, "Error al determinar la ubicación", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            coroutineScope.launch {
                                appointmentViewModel.createAppointment(
                                    clientId = clientId,
                                    professionalId = barber.id,
                                    startDate = selectedSlot!!.start,
                                    endDate = selectedSlot!!.end,
                                    availabilityBlockId = selectedSlot!!.id,
                                    locationId = locationId
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !appointmentState.isCreating && selectedSlot != null && locationChoice != null
                    ) {
                        if (appointmentState.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Confirmar Cita")
                        }
                    }
                }
                
                // Error message
                appointmentState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
