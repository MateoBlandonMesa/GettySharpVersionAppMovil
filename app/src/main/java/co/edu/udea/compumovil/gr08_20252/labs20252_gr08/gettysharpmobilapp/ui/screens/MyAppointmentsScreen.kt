package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Appointment
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(navController: NavController, clientId: String? = "demo-user-id") {
    val viewModel: AppointmentsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var showCancelDialog by remember { mutableStateOf<Appointment?>(null) }
    
    LaunchedEffect(clientId) {
        // TODO: Get actual user ID from auth/session
        if (clientId != null) {
            viewModel.loadClientAppointments(clientId)
        }
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
                        Button(
                            onClick = {
                                if (clientId != null) {
                                    viewModel.loadClientAppointments(clientId)
                                }
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                uiState.appointments.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tienes citas programadas",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Agenda una cita con un barbero",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.appointments) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                onCancel = {
                                    showCancelDialog = appointment
                                }
                            )
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
            text = { Text("¿Estás seguro de que deseas cancelar esta cita?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog?.let {
                            viewModel.cancelAppointment(it.id)
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
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onCancel: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Professional name
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
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = appointment.professionalPublicName ?: appointment.professionalName ?: "Barbero",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Status badge
                if (appointment.statusName != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (appointment.statusName.lowercase()) {
                            "confirmada", "completed" -> MaterialTheme.colorScheme.primaryContainer
                            "cancelada", "cancelled" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = appointment.statusName,
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
                Text(
                    text = "📍 ${appointment.locationAddress ?: appointment.professionalAddress ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancelar cita")
            }
        }
    }
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
