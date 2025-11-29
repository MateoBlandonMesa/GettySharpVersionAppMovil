package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.BarbersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindBarbersScreen(navController: NavController) {
    val viewModel: BarbersViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedBarber by remember { mutableStateOf<co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Barber?>(null) }
    var showAppointmentDialog by remember { mutableStateOf(false) }
    
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
                    IconButton(onClick = { viewModel.refreshBarbers() }) {
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
    
    // Appointment dialog (simplified - will be enhanced later)
    if (showAppointmentDialog && selectedBarber != null) {
        AlertDialog(
            onDismissRequest = { 
                showAppointmentDialog = false
                selectedBarber = null
            },
            title = { Text("Agendar con ${selectedBarber!!.nombre} ${selectedBarber!!.apellido}") },
            text = { Text("Funcionalidad de agendamiento será implementada próximamente.") },
            confirmButton = {
                TextButton(onClick = { 
                    showAppointmentDialog = false
                    selectedBarber = null
                }) {
                    Text("Cerrar")
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
