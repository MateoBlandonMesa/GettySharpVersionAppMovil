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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Appointment
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.ClientHistoryItem
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AppointmentViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AppointmentsViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.RatingViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(
    navController: NavController,
    appointmentsViewModel: AppointmentsViewModel = viewModel(),
    appointmentViewModel: AppointmentViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val appointmentsState by appointmentsViewModel.uiState.collectAsState()
    val appointmentState by appointmentViewModel.uiState.collectAsState()
    val ratingState by ratingViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    
    val clientId = authState.userProfile?.id
    
    var showCancelDialog by remember { mutableStateOf<Appointment?>(null) }
    var showRescheduleDialog by remember { mutableStateOf<Appointment?>(null) }
    var showRatingDialog by remember { mutableStateOf<ClientHistoryItem?>(null) }
    
    var selectedSlot by remember { mutableStateOf<AvailabilityBlock?>(null) }
    var ratingScore by remember { mutableStateOf(0) }
    var ratingComment by remember { mutableStateOf("") }
    
    // Load appointments and history when screen opens
    LaunchedEffect(clientId) {
        clientId?.let { id ->
            appointmentsViewModel.loadClientAppointments(id)
            ratingViewModel.loadClientHistory(id)
        }
    }
    
    // Load availability when reschedule dialog opens
    LaunchedEffect(showRescheduleDialog) {
        showRescheduleDialog?.let { appointment ->
            appointmentViewModel.loadAvailability(appointment.idProfesional)
            selectedSlot = null
        }
    }
    
    // Handle reschedule success
    LaunchedEffect(appointmentState.success) {
        if (appointmentState.success && showRescheduleDialog != null) {
            Toast.makeText(context, "Cita reagendada exitosamente", Toast.LENGTH_LONG).show()
            clientId?.let { appointmentsViewModel.loadClientAppointments(it) }
            appointmentViewModel.resetSuccess()
            showRescheduleDialog = null
            selectedSlot = null
        }
    }
    
    // Handle rating success
    LaunchedEffect(ratingState.submitSuccess) {
        if (ratingState.submitSuccess) {
            Toast.makeText(context, "¡Gracias por tu calificación!", Toast.LENGTH_LONG).show()
            clientId?.let { ratingViewModel.loadClientHistory(it) }
            ratingViewModel.resetSubmitSuccess()
            showRatingDialog = null
            ratingScore = 0
            ratingComment = ""
        }
    }
    
    // Open rating dialog with existing rating if present
    LaunchedEffect(showRatingDialog) {
        showRatingDialog?.let { item ->
            ratingScore = item.rating?.score ?: 0
            ratingComment = item.rating?.comment ?: ""
        }
    }
    
    // Filter pending appointments
    val pendingAppointments = appointmentsState.appointments.filter { appointment ->
        val status = appointment.statusName?.lowercase() ?: ""
        status in listOf("reservada", "pendiente", "confirmada")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Citas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            when {
                appointmentsState.isLoading && appointmentsState.appointments.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                appointmentsState.errorMessage != null && appointmentsState.appointments.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${appointmentsState.errorMessage}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                clientId?.let { appointmentsViewModel.loadClientAppointments(it) }
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pending Appointments Section
                        if (pendingAppointments.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Citas Pendientes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(pendingAppointments) { appointment ->
                                AppointmentCard(
                                    appointment = appointment,
                                    onCancel = {
                                        showCancelDialog = appointment
                                    },
                                    onReschedule = {
                                        showRescheduleDialog = appointment
                                    }
                                )
                            }
                        }
                        
                        // Completed Appointments / History Section
                        item {
                            Text(
                                text = "Historial de Citas Completadas",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        if (ratingState.isLoading && ratingState.history.isEmpty()) {
                            item {
                                CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(32.dp))
                            }
                        } else if (ratingState.errorMessage != null && ratingState.history.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = "Error: ${ratingState.errorMessage}",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else if (ratingState.history.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Aún no has completado ninguna cita",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "¡Agenda una para poder calificar a tu profesional!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(ratingState.history) { historyItem ->
                                HistoryItemCard(
                                    item = historyItem,
                                    onRate = {
                                        showRatingDialog = historyItem
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = null },
            title = { Text("Cancelar cita") },
            text = {
                Text(
                    "¿Estás seguro de que deseas cancelar esta cita con ${showCancelDialog!!.professionalPublicName ?: showCancelDialog!!.professionalName ?: "el barbero"}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog?.let {
                            appointmentsViewModel.cancelAppointment(it.id)
                            clientId?.let { id ->
                                appointmentsViewModel.loadClientAppointments(id)
                            }
                            showCancelDialog = null
                        }
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
    
    // Reschedule Dialog
    if (showRescheduleDialog != null) {
        RescheduleAppointmentDialog(
            appointment = showRescheduleDialog!!,
            appointmentViewModel = appointmentViewModel,
            selectedSlot = selectedSlot,
            onSlotSelected = { slot ->
                selectedSlot = slot
            },
            onConfirm = {
                if (selectedSlot != null) {
                    appointmentsViewModel.rescheduleAppointment(
                        appointmentId = showRescheduleDialog!!.id,
                        newStart = selectedSlot!!.start,
                        newEnd = selectedSlot!!.end
                    )
                } else {
                    Toast.makeText(context, "Selecciona un horario disponible", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                showRescheduleDialog = null
                selectedSlot = null
                appointmentViewModel.clearError()
            }
        )
    }
    
    // Rating Dialog
    if (showRatingDialog != null) {
        RatingDialog(
            historyItem = showRatingDialog!!,
            ratingScore = ratingScore,
            ratingComment = ratingComment,
            onScoreChanged = { score ->
                ratingScore = score
            },
            onCommentChanged = { comment ->
                ratingComment = comment
            },
            onSubmit = {
                if (ratingScore < 1 || ratingScore > 5) {
                    Toast.makeText(context, "Selecciona una calificación de 1 a 5 estrellas", Toast.LENGTH_SHORT).show()
                    return@RatingDialog
                }
                
                if (clientId == null) {
                    Toast.makeText(context, "Error: No se encontró el ID del cliente", Toast.LENGTH_SHORT).show()
                    return@RatingDialog
                }
                
                ratingViewModel.submitRating(
                    appointmentId = showRatingDialog!!.appointmentId,
                    clientId = clientId,
                    score = ratingScore,
                    comment = ratingComment.takeIf { it.isNotBlank() }
                )
            },
            onDismiss = {
                showRatingDialog = null
                ratingScore = 0
                ratingComment = ""
                ratingViewModel.clearError()
            },
            isSubmitting = ratingState.isSubmitting
        )
    }
}

@Composable
fun AppointmentCard(
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
            // Header with professional name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = appointment.professionalPublicName ?: appointment.professionalName ?: "Barbero",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Status badge
                appointment.statusName?.let { status ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (status.lowercase()) {
                            "confirmada" -> MaterialTheme.colorScheme.primaryContainer
                            "cancelada" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Divider()
            
            // Date and time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatAppointmentDate(appointment.fechaInicioCita, appointment.fechaFinCita),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Location
            if (appointment.locationAddress != null || appointment.professionalAddress != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = appointment.locationAddress ?: appointment.professionalAddress ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
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
fun HistoryItemCard(
    item: ClientHistoryItem,
    onRate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                        text = item.professionalPublicName ?: item.professionalName ?: "Profesional",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatAppointmentDate(item.start, item.end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Rating stars
                if (item.rating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (index < item.rating!!.score) Color(0xFFFFD700) else Color.Gray
                            )
                        }
                    }
                }
            }
            
            // Rating comment if exists
            item.rating?.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = "\"$comment\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // Rate button
            if (item.canRate || item.rating == null) {
                Button(
                    onClick = onRate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE07410)
                    )
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (item.rating != null) "Actualizar calificación" else "Calificar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleAppointmentDialog(
    appointment: Appointment,
    appointmentViewModel: AppointmentViewModel,
    selectedSlot: AvailabilityBlock?,
    onSlotSelected: (AvailabilityBlock) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by appointmentViewModel.uiState.collectAsState()
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
                    Text(
                        text = "Reagendar cita",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Text(
                    text = "Profesional: ${appointment.professionalPublicName ?: appointment.professionalName ?: "Barbero"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Elige una nueva franja disponible para reprogramar tu cita.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                // Availability list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                    } else if (uiState.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else if (availableSlots.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "No hay franjas disponibles para reagendar con este profesional.",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        availableSlots.forEach { slot ->
                            val isSelected = selectedSlot?.id == slot.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSlotSelected(slot) },
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
                }
                
                Divider()
                
                // Footer
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
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = selectedSlot != null && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE07410)
                        )
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingDialog(
    historyItem: ClientHistoryItem,
    ratingScore: Int,
    ratingComment: String,
    onScoreChanged: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
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
                    Text(
                        text = "Calificar cita",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Text(
                    text = "Profesional: ${historyItem.professionalPublicName ?: historyItem.professionalName ?: "Profesional"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Comparte tu experiencia con otros clientes y ayuda al profesional a seguir mejorando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                // Star rating
                Text(
                    text = "Tu calificación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(5) { index ->
                        val value = index + 1
                        val isSelected = ratingScore >= value
                        IconButton(
                            onClick = { onScoreChanged(value) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "$value estrellas",
                                modifier = Modifier.size(40.dp),
                                tint = if (isSelected) Color(0xFFFFD700) else Color.Gray
                            )
                        }
                    }
                }
                
                // Comment
                Text(
                    text = "Comentario (opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = ratingComment,
                    onValueChange = { if (it.length <= 2000) onCommentChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Comparte detalles de tu experiencia...") },
                    minLines = 5,
                    maxLines = 10
                )
                
                Text(
                    text = "${ratingComment.length}/2000",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
                
                Divider()
                
                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        enabled = ratingScore >= 1 && ratingScore <= 5 && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE07410)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Guardar calificación")
                        }
                    }
                }
            }
        }
    }
}

fun formatAppointmentDate(startDate: String, endDate: String): String {
    return try {
        val startInstant = Instant.parse(startDate)
        val endInstant = Instant.parse(endDate)
        
        val startLocal = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault())
        val endLocal = LocalDateTime.ofInstant(endInstant, ZoneId.systemDefault())
        
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "CO"))
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "CO"))
        
        "${startLocal.format(dateFormatter)} de ${startLocal.format(timeFormatter)} a ${endLocal.format(timeFormatter)}"
    } catch (e: Exception) {
        // Fallback to simple format
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("dd/MM/yyyy 'de' HH:mm 'a' HH:mm", Locale.getDefault())
            
            val start = dateFormat.parse(startDate) ?: return "$startDate - $endDate"
            val end = dateFormat.parse(endDate) ?: return "$startDate - $endDate"
            
            outputDateFormat.format(start)
        } catch (e2: Exception) {
            "$startDate - $endDate"
        }
    }
}
