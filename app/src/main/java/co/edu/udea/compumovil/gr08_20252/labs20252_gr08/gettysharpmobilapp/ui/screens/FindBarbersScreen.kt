package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.FindBarbersViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ClientLocation
import java.text.SimpleDateFormat
import java.util.*

// Default location: Bello, Antioquia
private val DEFAULT_BELLO = LatLng(6.343463, -75.559117)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindBarbersScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: FindBarbersViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // Load client location when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadClientLocation(context)
    }
    
    var showBookingDialog by remember { mutableStateOf(false) }
    
    // Show booking dialog when barber is selected
    LaunchedEffect(uiState.selectedBarber) {
        if (uiState.selectedBarber != null) {
            showBookingDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar Barberos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshBarbers() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Cargando barberos...")
                        }
                    }
                }
                uiState.errorMessage != null && uiState.barbers.isEmpty() -> {
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
                        Button(onClick = { viewModel.refreshBarbers() }) {
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
                        Button(onClick = { viewModel.refreshBarbers() }) {
                            Text("Actualizar")
                        }
                    }
                }
                else -> {
                    // Google Maps View
                    BarberMapView(
                        barbers = uiState.barbers,
                        clientLocation = uiState.clientLocation,
                        onBarberSelected = { barber ->
                            viewModel.selectBarber(barber)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Booking Dialog
            if (showBookingDialog && uiState.selectedBarber != null) {
                BookingDialog(
                    viewModel = viewModel,
                    onDismiss = {
                        showBookingDialog = false
                        viewModel.clearSelection()
                    },
                    onAppointmentCreated = {
                        showBookingDialog = false
                        viewModel.clearSelection()
                        // Navigate to appointments or show success message
                        navController.navigate(Screen.MyAppointments.route)
                    }
                )
            }
        }
    }
}

@Composable
fun BarberMapView(
    barbers: List<Barber>,
    clientLocation: ClientLocation?,
    onBarberSelected: (Barber) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val markers = remember { mutableMapOf<String, Marker>() }
    
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                mapView = this
                onCreate(null)
                getMapAsync { map ->
                    googleMap = map
                    
                    // Configure map
                    map.uiSettings.isZoomControlsEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = true
                    
                    // Determine center and bounds
                    val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    var hasValidPoints = false
                    
                    // Add client location marker
                    if (clientLocation != null) {
                        val location: ClientLocation = clientLocation
                        val clientLatLng = LatLng(location.lat, location.lon)
                        boundsBuilder.include(clientLatLng)
                        hasValidPoints = true
                        
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(clientLatLng)
                                .title("Tu ubicación")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ROSE
                                ))
                        )
                        marker?.let { markers["client"] = it }
                    }
                    
                    // Add barber markers
                    barbers.forEach { barber ->
                        val lat = barber.latitud ?: DEFAULT_BELLO.latitude
                        val lng = barber.longitud ?: DEFAULT_BELLO.longitude
                        val barberLatLng = LatLng(lat, lng)
                        
                        boundsBuilder.include(barberLatLng)
                        hasValidPoints = true
                        
                        val name = "${barber.nombre ?: ""} ${barber.apellido ?: ""}".trim()
                        val snippet = buildString {
                            barber.direccion?.let { append("$it\n") }
                            if (barber.ratingAverage != null && barber.ratingsCount != null && barber.ratingsCount!! > 0) {
                                append("⭐ ${String.format("%.1f", barber.ratingAverage)} (${barber.ratingsCount} reseñas)")
                            } else {
                                append("Sin calificaciones aún")
                            }
                        }
                        
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(barberLatLng)
                                .title(name)
                                .snippet(snippet)
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
                                ))
                        )
                        marker?.let { markers[barber.id] = it }
                    }
                    
                    // Set up marker click listener
                    map.setOnMarkerClickListener { marker ->
                        if (marker.id != markers["client"]?.id) {
                            val barber = barbers.find { markers[it.id]?.id == marker.id }
                            barber?.let { onBarberSelected(it) }
                        }
                        true
                    }
                    
                    // Move camera to show all markers
                    if (hasValidPoints) {
                        val bounds = boundsBuilder.build()
                        val padding = 100
                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                        map.moveCamera(cameraUpdate)
                    } else {
                        // Fallback to default location
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_BELLO, 12f))
                    }
                }
            }
        },
        modifier = modifier
    )
    
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
        }
    }
}

@Composable
fun BookingDialog(
    viewModel: FindBarbersViewModel,
    onDismiss: () -> Unit,
    onAppointmentCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val selectedBarber = uiState.selectedBarber ?: return
    
    // Check if appointment was created
    LaunchedEffect(uiState.appointmentCreated) {
        if (uiState.appointmentCreated != null) {
            onAppointmentCreated()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Agendar cita con ${selectedBarber.nombre ?: "Barbero"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (selectedBarber.ratingAverage != null && selectedBarber.ratingsCount != null && selectedBarber.ratingsCount!! > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFA500),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", selectedBarber.ratingAverage)} (${selectedBarber.ratingsCount} reseñas)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Availability Section
                item {
                    Text(
                        text = "Disponibilidad publicada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                when {
                    uiState.isLoadingAvailability -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cargando disponibilidad...")
                            }
                        }
                    }
                    uiState.availabilityError != null -> {
                        item {
                            Text(
                                text = uiState.availabilityError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    uiState.availability.isEmpty() -> {
                        item {
                            Text(
                                text = "No hay bloques disponibles. Intenta en otro horario.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        items(uiState.availability) { slot ->
                            AvailabilitySlotCard(
                                slot = slot,
                                isSelected = uiState.selectedSlot?.id == slot.id,
                                onClick = { viewModel.selectSlot(slot) }
                            )
                        }
                    }
                }
                
                // Location Choice Section
                if (uiState.availability.isNotEmpty() && uiState.selectedSlot != null) {
                    item {
                        HorizontalDivider()
                    }
                    
                    item {
                        Text(
                            text = "Lugar de la cita",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Show local option if available
                    if (shouldShowLocalOption(selectedBarber.lugarDeTrabajo)) {
                        item {
                            LocationChoiceCard(
                                title = "En el local",
                                description = selectedBarber.direccion ?: "El profesional aún no registra una dirección",
                                isSelected = uiState.locationChoice is co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.LocationChoice.Local,
                                enabled = selectedBarber.ubicacionId != null,
                                onClick = { viewModel.setLocationChoice(co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.LocationChoice.Local) }
                            )
                        }
                    }
                    
                    // Show home option if available
                    if (shouldShowHomeOption(selectedBarber.lugarDeTrabajo)) {
                        item {
                            LocationChoiceCard(
                                title = "En mi domicilio",
                                description = uiState.clientLocation?.address ?: "Agrega tu dirección en el perfil para habilitar esta opción",
                                isSelected = uiState.locationChoice is co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.LocationChoice.Home,
                                enabled = uiState.clientLocation?.locationId != null,
                                onClick = { viewModel.setLocationChoice(co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.LocationChoice.Home) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.createAppointment(context)
                },
                enabled = uiState.selectedSlot != null && 
                         uiState.locationChoice != null && 
                         !uiState.isCreatingAppointment
            ) {
                if (uiState.isCreatingAppointment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creando...")
                } else {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
    
    // Show error message if any
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Error is shown in dialog text area
        }
    }
}

@Composable
fun AvailabilitySlotCard(
    slot: AvailabilityBlock,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
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
                    text = formatRange(slot.start, slot.end),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Duración mínima: ${getSlotDurationMinutes(slot)} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun LocationChoiceCard(
    title: String,
    description: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled
            )
        }
    }
}

fun shouldShowHomeOption(lugarTrabajo: Int?): Boolean {
    return lugarTrabajo == null || lugarTrabajo == 1 || lugarTrabajo == 3
}

fun shouldShowLocalOption(lugarTrabajo: Int?): Boolean {
    return lugarTrabajo == null || lugarTrabajo == 2 || lugarTrabajo == 3
}

fun formatRange(start: String, end: String): String {
    return try {
        val startDate = java.time.Instant.parse(start).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        val endDate = java.time.Instant.parse(end).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val startFormatted = dateFormat.format(Date.from(startDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
        val endFormatted = dateFormat.format(Date.from(endDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
        
        "$startFormatted - $endFormatted"
    } catch (e: Exception) {
        "$start - $end"
    }
}

fun getSlotDurationMinutes(slot: AvailabilityBlock): Int {
    return try {
        val start = java.time.Instant.parse(slot.start)
        val end = java.time.Instant.parse(slot.end)
        java.time.Duration.between(start, end).toMinutes().toInt()
    } catch (e: Exception) {
        0
    }
}
