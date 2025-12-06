package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.ProfessionalApproval
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ApprovalsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalsScreen(
    navController: NavController,
    viewModel: ApprovalsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var approverId by remember { mutableStateOf<String?>(null) }
    var isApprover by remember { mutableStateOf(false) }
    var showDocumentationDialog by remember { mutableStateOf<String?>(null) }
    var lastSelectedStatus by remember { mutableStateOf<String?>(null) }
    
    // Check if user is approver and get their ID
    LaunchedEffect(Unit) {
        val profile = AuthService.getUserProfile(context)
        if (profile?.isApprover == true && profile.id != null) {
            approverId = profile.id
            isApprover = true
            viewModel.loadVerificationStatuses()
            viewModel.loadProfessionalsForApproval(profile.id)
        } else {
            isApprover = false
            // Redirect to dashboard if not approver
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Approvals.route) { inclusive = true }
            }
        }
    }
    
    // Reload professionals when status filter changes (avoid infinite loop)
    LaunchedEffect(uiState.selectedStatusId) {
        if (uiState.selectedStatusId != lastSelectedStatus) {
            lastSelectedStatus = uiState.selectedStatusId
            approverId?.let { id ->
                viewModel.loadProfessionalsForApproval(id, uiState.selectedStatusId)
            }
        }
    }
    
    // Show success/error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearError()
            }
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = success,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSuccess()
            }
        }
    }
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { Text("Panel de Aprobaciones") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (!isApprover) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes permisos para acceder a esta sección",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Panel de Aprobaciones",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Revisa la documentación enviada por los profesionales y actualiza su estado de verificación.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Filter by status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Filtrar por estado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        var statusExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = uiState.selectedStatusId?.let { selectedId ->
                                    uiState.verificationStatuses.find { it.id == selectedId }?.nombre ?: "Todos"
                                } ?: "Todos",
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { statusExpanded = true },
                                readOnly = true,
                                label = { Text("Estado") },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            )
                            DropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos") },
                                    onClick = {
                                        viewModel.setSelectedStatus(null)
                                        statusExpanded = false
                                    }
                                )
                                uiState.verificationStatuses.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status.nombre) },
                                        onClick = {
                                            viewModel.setSelectedStatus(status.id)
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Professionals list
                if (uiState.isLoading && uiState.professionals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.professionals.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "No hay profesionales pendientes según el filtro seleccionado.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    uiState.professionals.forEach { professional ->
                        ProfessionalApprovalCard(
                            professional = professional,
                            verificationStatuses = uiState.verificationStatuses,
                            isUpdating = uiState.updatingProfessionalId == professional.id,
                            onViewDocumentation = {
                                if (professional.documentation != null) {
                                    showDocumentationDialog = professional.documentation
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Este profesional no adjuntó documentación.",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onApprove = {
                                approverId?.let { approver ->
                                    val verifiedStatusId = findStatusIdByName(
                                        uiState.verificationStatuses,
                                        "verificado"
                                    )
                                    if (verifiedStatusId != null) {
                                        viewModel.updateProfessionalStatus(
                                            approverId = approver,
                                            professionalId = professional.id,
                                            statusId = verifiedStatusId
                                        )
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Estado 'Verificado' no encontrado en la configuración",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                }
                            },
                            onReject = {
                                approverId?.let { approver ->
                                    val rejectedStatusId = findStatusIdByName(
                                        uiState.verificationStatuses,
                                        "rechazado"
                                    )
                                    if (rejectedStatusId != null) {
                                        viewModel.updateProfessionalStatus(
                                            approverId = approver,
                                            professionalId = professional.id,
                                            statusId = rejectedStatusId
                                        )
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Estado 'Rechazado' no encontrado en la configuración",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Documentation Dialog
    showDocumentationDialog?.let { doc ->
        DocumentationViewDialog(
            documentation = doc,
            snackbarHostState = snackbarHostState,
            onDismiss = { showDocumentationDialog = null }
        )
    }
}

@Composable
fun ProfessionalApprovalCard(
    professional: ProfessionalApproval,
    verificationStatuses: List<co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.VerificationStatus>,
    isUpdating: Boolean,
    onViewDocumentation: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusName = professional.verificationStatusName
        ?: verificationStatuses.find { it.id == professional.verificationStatusId }?.nombre
        ?: "Sin estado"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = professional.fullName
                        ?: professional.publicUsername
                        ?: "Profesional",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = statusName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Divider()
            
            // Email
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Correo electrónico",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = professional.email ?: "No registrado",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Public Username
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Usuario público",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = professional.publicUsername ?: "No definido",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Documentation button
                OutlinedButton(
                    onClick = onViewDocumentation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating && professional.documentation != null
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver documentación")
                }
                
                // Approve and Reject buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verificado")
                    }
                    
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rechazado")
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentationViewDialog(
    documentation: String,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Documentación del Profesional",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "La documentación se abrirá en tu navegador o visor de PDF preferido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = {
                        try {
                            val hasPrefix = documentation.startsWith("data:")
                            val url = if (hasPrefix) {
                                documentation
                            } else {
                                "data:application/pdf;base64,$documentation"
                            }
                            
                            // Try to open in browser or PDF viewer
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(url)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                type = "application/pdf"
                            }
                            
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                                onDismiss()
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "No se encontró una aplicación para abrir el PDF",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error al abrir documentación: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Documentación")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

fun findStatusIdByName(
    statuses: List<co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.VerificationStatus>,
    searchTerm: String
): String? {
    return statuses.firstOrNull { status ->
        val normalized = java.text.Normalizer.normalize(status.nombre.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        normalized.contains(searchTerm.lowercase())
    }?.id
}

