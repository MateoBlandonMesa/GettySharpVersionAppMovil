package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.*
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.ApiClient
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AppointmentsViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.UserRole
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(
    navController: NavController,
    mode: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: AppointmentsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showCancelDialog by remember { mutableStateOf<Appointment?>(null) }
    var showRescheduleDialog by remember { mutableStateOf<Appointment?>(null) }
    var showRatingDialog by remember { mutableStateOf<ClientHistoryItem?>(null) }
    var panelMode by remember { mutableStateOf("active") }
    var selectedSlot by remember { mutableStateOf<AvailabilityBlock?>(null) }
    var availableSlots by remember { mutableStateOf<List<AvailabilityBlock>>(emptyList()) }
    var loadingAvailability by remember { mutableStateOf(false) }
    var availabilityError by remember { mutableStateOf<String?>(null) }
    var ratingScore by remember { mutableStateOf(0) }
    var ratingComment by remember { mutableStateOf("") }
    var submittingRating by remember { mutableStateOf(false) }
    
    val isManageMode = mode == "manage" && uiState.userRole is UserRole.Barber
    
    LaunchedEffect(Unit) {
        viewModel.loadAppointments(context, mode)
    }
    
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            // Show error toast or handle error
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (isManageMode) "Panel de Citas Clientes" else "Mis Citas")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
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
                uiState.isLoading && uiState.appointments.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null && uiState.appointments.isEmpty() -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Error desconocido",
                        onRetry = { viewModel.loadAppointments(context, mode) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isManageMode -> {
                    BarberManagePanel(
                        uiState = uiState,
                        panelMode = panelMode,
                        onPanelModeChange = { panelMode = it },
                        onStatusChange = { appointmentId, statusId ->
                            viewModel.updateAppointmentStatus(appointmentId, statusId)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    ClientAppointmentsView(
                        uiState = uiState,
                        onCancel = { showCancelDialog = it },
                        onReschedule = { appointment ->
                            showRescheduleDialog = appointment
                        },
                        onRate = { showRatingDialog = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    showCancelDialog?.let { appointment ->
        AlertDialog(
            onDismissRequest = { showCancelDialog = null },
            title = { Text("Cancelar cita") },
            text = { Text("¿Estás seguro de que deseas cancelar esta cita? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelAppointment(appointment.id)
                        showCancelDialog = null
                    }
                ) {
                    Text("Cancelar cita", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = null }) {
                    Text("No")
                }
            }
        )
    }
    
    // Load availability when reschedule dialog opens
    LaunchedEffect(showRescheduleDialog?.id) {
        if (showRescheduleDialog != null) {
            loadingAvailability = true
            availabilityError = null
            selectedSlot = null
            availableSlots = emptyList()
            
            try {
                val response = ApiClient.service.getAvailability(
                    professionalId = showRescheduleDialog!!.idProfesional
                )
                if (response.isSuccessful) {
                    val allSlots = response.body() ?: emptyList()
                    availableSlots = allSlots.filter { slot ->
                        slot.status?.lowercase() == "disponible"
                    }
                    if (availableSlots.isEmpty()) {
                        availabilityError = "Este profesional no tiene disponibilidad publicada en este momento."
                    }
                } else {
                    availabilityError = "No se pudo cargar la disponibilidad."
                }
            } catch (e: Exception) {
                availabilityError = e.message ?: "Error al cargar disponibilidad"
            } finally {
                loadingAvailability = false
            }
        }
    }
    
    // Reschedule dialog
    showRescheduleDialog?.let { appointment ->
        RescheduleDialog(
            appointment = appointment,
            availableSlots = availableSlots,
            loading = loadingAvailability,
            error = availabilityError,
            selectedSlot = selectedSlot,
            onSlotSelected = { selectedSlot = it },
            onDismiss = { 
                showRescheduleDialog = null
                selectedSlot = null
                availableSlots = emptyList()
                availabilityError = null
            },
            onConfirm = {
                selectedSlot?.let { slot ->
                    viewModel.rescheduleAppointment(appointment.id, slot.start, slot.end)
                    showRescheduleDialog = null
                    selectedSlot = null
                    availableSlots = emptyList()
                    availabilityError = null
                }
            }
        )
    }
    
    // Rating dialog
    showRatingDialog?.let { historyItem ->
        RatingDialog(
            historyItem = historyItem,
            initialScore = historyItem.rating?.score ?: 0,
            initialComment = historyItem.rating?.comment ?: "",
            submitting = submittingRating,
            onDismiss = {
                showRatingDialog = null
                ratingScore = 0
                ratingComment = ""
            },
            onSubmit = { score, comment ->
                scope.launch {
                    val session = AuthService.getSession(context)
                    session?.userId?.let { clientId ->
                        submittingRating = true
                        viewModel.submitRating(historyItem.appointmentId, clientId, score, comment)
                        kotlinx.coroutines.delay(1000) // Wait for submission
                        viewModel.loadClientHistory(clientId)
                        submittingRating = false
                        showRatingDialog = null
                        ratingScore = 0
                        ratingComment = ""
                    }
                }
            }
        )
    }
}

@Composable
fun ClientAppointmentsView(
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AppointmentsUiState,
    onCancel: (Appointment) -> Unit,
    onReschedule: (Appointment) -> Unit,
    onRate: (ClientHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingAppointments = uiState.appointments.filter { appointment ->
        val statusName = appointment.statusName?.lowercase() ?: ""
        statusName.contains("pendiente") || statusName.contains("reservada") || 
        statusName.contains("confirmada")
    }
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pendingAppointments.isEmpty() && !uiState.isLoading) {
            item {
                EmptyStateView(
                    title = "No tienes citas pendientes",
                    message = "Agenda una cita con un barbero",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(pendingAppointments) { appointment ->
                ClientAppointmentCard(
                    appointment = appointment,
                    onCancel = { onCancel(appointment) },
                    onReschedule = { onReschedule(appointment) }
                )
            }
        }
        
        // History section
        if (!uiState.isLoadingHistory && uiState.history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Historial de citas completadas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.history) { historyItem ->
                HistoryItemCard(
                    historyItem = historyItem,
                    onRate = { onRate(historyItem) }
                )
            }
        } else if (uiState.isLoadingHistory) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun ClientAppointmentCard(
    appointment: Appointment,
    onCancel: () -> Unit,
    onReschedule: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = appointment.professionalPublicName ?: appointment.professionalName ?: "Barbero",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (appointment.statusName != null) {
                    StatusBadge(statusName = appointment.statusName)
                }
            }
            
            HorizontalDivider()
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = formatAppointmentDate(appointment.fechaInicioCita, appointment.fechaFinCita),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (appointment.locationAddress != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = appointment.locationAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReschedule,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reagendar")
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
fun BarberManagePanel(
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AppointmentsUiState,
    panelMode: String,
    onPanelModeChange: (String) -> Unit,
    onStatusChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Tabs
        TabRow(selectedTabIndex = if (panelMode == "active") 0 else 1) {
            Tab(
                selected = panelMode == "active",
                onClick = { onPanelModeChange("active") },
                text = { Text("Citas Activas") }
            )
            Tab(
                selected = panelMode == "history",
                onClick = { onPanelModeChange("history") },
                text = { Text("Historial") }
            )
        }
        
        // Content
        when (panelMode) {
            "active" -> {
                ActiveAppointmentsTable(
                    appointments = uiState.appointments.filter { appointment ->
                        val statusName = normalizeStatusName(appointment.statusName)
                        statusName.contains("pendiente") || statusName.contains("reservada") || 
                        statusName.contains("confirmada")
                    },
                    statusOptions = uiState.appointmentStatuses,
                    updatingAppointmentId = uiState.updatingAppointmentId,
                    onStatusChange = onStatusChange,
                    modifier = Modifier.fillMaxSize()
                )
            }
            "history" -> {
                HistoryAppointmentsTable(
                    appointments = uiState.appointments.filter { appointment ->
                        val statusName = normalizeStatusName(appointment.statusName)
                        statusName.contains("completada") || statusName.contains("cancelada") || 
                        statusName.contains("no_asistio")
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ActiveAppointmentsTable(
    appointments: List<Appointment>,
    statusOptions: List<AppointmentStatusOption>,
    updatingAppointmentId: String?,
    onStatusChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (appointments.isEmpty()) {
        EmptyStateView(
            title = "No tienes citas asignadas",
            message = "Cuando los clientes reserven una franja, aparecerá aquí para que puedas gestionarla.",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appointments) { appointment ->
                BarberAppointmentCard(
                    appointment = appointment,
                    statusOptions = statusOptions,
                    isUpdating = updatingAppointmentId == appointment.id,
                    onStatusChange = { statusId -> onStatusChange(appointment.id, statusId) }
                )
            }
        }
    }
}

@Composable
fun HistoryAppointmentsTable(
    appointments: List<Appointment>,
    modifier: Modifier = Modifier
) {
    if (appointments.isEmpty()) {
        EmptyStateView(
            title = "No hay citas en historial",
            message = "",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appointments) { appointment ->
                BarberHistoryCard(appointment = appointment)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun BarberAppointmentCard(
    appointment: Appointment,
    statusOptions: List<AppointmentStatusOption>,
    isUpdating: Boolean,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cita #${appointment.id.take(8)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cliente",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = appointment.clientName ?: appointment.clientEmail ?: "Cliente",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (appointment.clientEmail != null) {
                        Text(
                            text = appointment.clientEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Text(
                text = formatAppointmentDate(appointment.fechaInicioCita, appointment.fechaFinCita),
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (appointment.locationAddress != null) {
                Text(
                    text = "📍 ${appointment.locationAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (statusOptions.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = statusOptions.find { it.id == appointment.idEstadoCita }?.nombre ?: "Sin estado",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado de la cita") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isUpdating
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.nombre) },
                                onClick = {
                                    onStatusChange(status.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            if (isUpdating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun BarberHistoryCard(
    appointment: Appointment
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Cita #${appointment.id.take(8)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = appointment.clientName ?: appointment.clientEmail ?: "Cliente",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = formatAppointmentDate(appointment.fechaInicioCita, appointment.fechaFinCita),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (appointment.statusName != null) {
                StatusBadge(statusName = appointment.statusName)
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    historyItem: ClientHistoryItem,
    onRate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = historyItem.professionalPublicName ?: historyItem.professionalName ?: "Profesional",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatAppointmentDate(historyItem.start, historyItem.end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (historyItem.rating != null) {
                    StarRating(score = historyItem.rating.score)
                } else {
                    Text(
                        text = "Sin calificación",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            if (historyItem.rating?.comment != null) {
                Text(
                    text = "\"${historyItem.rating.comment}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (historyItem.canRate || historyItem.rating != null) {
                Button(
                    onClick = onRate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (historyItem.rating != null) "Actualizar calificación" else "Calificar")
                }
            }
        }
    }
}

@Composable
fun RescheduleDialog(
    appointment: Appointment,
    availableSlots: List<AvailabilityBlock>,
    loading: Boolean,
    error: String?,
    selectedSlot: AvailabilityBlock?,
    onSlotSelected: (AvailabilityBlock) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reagendar cita") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Profesional: ${appointment.professionalPublicName ?: appointment.professionalName ?: "Barbero"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
                } else if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (availableSlots.isEmpty()) {
                    Text(
                        text = "No hay franjas disponibles para reagendar con este profesional.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    availableSlots.forEach { slot ->
                        SlotItem(
                            slot = slot,
                            isSelected = selectedSlot?.id == slot.id,
                            onClick = { onSlotSelected(slot) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedSlot != null && !loading
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun SlotItem(
    slot: AvailabilityBlock,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatAppointmentDate(slot.start, slot.end),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun RatingDialog(
    historyItem: ClientHistoryItem,
    initialScore: Int,
    initialComment: String,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String?) -> Unit
) {
    var score by remember { mutableStateOf(initialScore) }
    var comment by remember { mutableStateOf(initialComment) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calificar cita") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Profesional: ${historyItem.professionalPublicName ?: historyItem.professionalName ?: "Profesional"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Tu calificación",
                    style = MaterialTheme.typography.labelLarge
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..5).forEach { value ->
                        IconButton(
                            onClick = { score = value }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$value estrellas",
                                tint = if (score >= value) Color(0xFFFFD700) else Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentario (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(score, comment.takeIf { it.isNotBlank() }) },
                enabled = score > 0 && !submitting
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Guardar calificación")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !submitting
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun StatusBadge(statusName: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when (statusName.lowercase()) {
            "confirmada", "completada" -> MaterialTheme.colorScheme.primaryContainer
            "cancelada" -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = statusName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun StarRating(score: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { value ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (value <= score) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

fun normalizeStatusName(statusName: String?): String {
    if (statusName == null) return ""
    return statusName
        .lowercase()
        .replace(" ", "_")
        .replace(Regex("[^a-z0-9_]"), "")
}

fun formatAppointmentDate(startDate: String, endDate: String): String {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd/MM/yyyy 'de' HH:mm 'a' HH:mm", Locale.getDefault())
        
        val start = dateFormat.parse(startDate) ?: return "$startDate - $endDate"
        val end = dateFormat.parse(endDate) ?: return "$startDate - $endDate"
        
        "${outputDateFormat.format(start)}"
    } catch (e: Exception) {
        "$startDate - $endDate"
    }
}

