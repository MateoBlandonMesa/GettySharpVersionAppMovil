package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.BarberSignupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberSignupScreen(
    navController: NavController,
    viewModel: BarberSignupViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var specialtyExpanded by remember { mutableStateOf(false) }
    var workLocationExpanded by remember { mutableStateOf(false) }
    var profileImageUrlInput by remember { mutableStateOf("") }
    
    // Image pickers
    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = viewModel.convertImageToBase64(it, context)
            if (base64 != null) {
                viewModel.setProfileImage(base64, base64)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Error al procesar la imagen",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    val documentationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
            
            val base64 = viewModel.convertDocumentToBase64(it, context)
            if (base64 != null) {
                viewModel.setDocumentation(base64, fileName)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Error al procesar el documento",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    // Load initial data
    LaunchedEffect(Unit) {
        viewModel.loadInitialData(context)
    }
    
    // Handle success navigation
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearSuccess()
                kotlinx.coroutines.delay(1500)
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.BarberSignup.route) { inclusive = true }
                }
            }
        }
    }
    
    // Handle errors
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
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { Text("Registro de Barbero") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
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
                    Text("Cargando información...")
                }
            }
        } else if (uiState.isAlreadyProfessional) {
            AlreadyProfessionalView(navController = navController, padding = padding)
        } else if (uiState.userProfile == null) {
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
                    Text(
                        text = uiState.errorMessage ?: "No se pudo cargar el perfil de usuario",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { viewModel.loadInitialData(context) }) {
                        Text("Reintentar")
                    }
                }
            }
        } else {
            BarberSignupForm(
                navController = navController,
                viewModel = viewModel,
                uiState = uiState,
                padding = padding,
                specialtyExpanded = specialtyExpanded,
                onSpecialtyExpandedChange = { specialtyExpanded = it },
                workLocationExpanded = workLocationExpanded,
                onWorkLocationExpandedChange = { workLocationExpanded = it },
                profileImageUrlInput = profileImageUrlInput,
                onProfileImageUrlInputChange = { profileImageUrlInput = it },
                onProfileImageClick = { profileImagePickerLauncher.launch("image/*") },
                onDocumentationClick = { documentationPickerLauncher.launch("*/*") },
                onSubmit = {
                    viewModel.submitProfessionalProfile(context) {
                        // Success handled in LaunchedEffect
                    }
                }
            )
        }
    }
}

@Composable
fun AlreadyProfessionalView(
    navController: NavController,
    padding: PaddingValues
) {
    // Auto-redirect after 5 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.BarberSignup.route) { inclusive = true }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = "Ya eres un profesional registrado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = "No puedes registrarte múltiples veces como un profesional en Getty Sharp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Nota: Si necesitas actualizar tu información profesional, ve a tu perfil en el dashboard.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
                
                Button(
                    onClick = { 
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.BarberSignup.route) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir al Dashboard")
                }
                
                Text(
                    text = "Serás redirigido automáticamente en unos segundos...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BarberSignupForm(
    navController: NavController,
    viewModel: BarberSignupViewModel,
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.BarberSignupUiState,
    padding: PaddingValues,
    specialtyExpanded: Boolean,
    onSpecialtyExpandedChange: (Boolean) -> Unit,
    workLocationExpanded: Boolean,
    onWorkLocationExpandedChange: (Boolean) -> Unit,
    profileImageUrlInput: String,
    onProfileImageUrlInputChange: (String) -> Unit,
    onProfileImageClick: () -> Unit,
    onDocumentationClick: () -> Unit,
    onSubmit: () -> Unit
) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Text(
                text = "¡Conviértete en Barbero!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Completa tu perfil profesional y comienza a recibir clientes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // User info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        text = "Tu Información Base",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "${uiState.userProfile?.firstName} ${uiState.userProfile?.lastName} • ${uiState.userProfile?.email}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Professional Information Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "Información Profesional",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Username
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("Nombre de Usuario Público *") },
                    placeholder = { Text("@mi_barberia_pro") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                    }
                )
                Text(
                    text = "Este será el nombre que verán tus clientes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Specialty
                Box {
                    OutlinedTextField(
                        value = uiState.specialty.let { spec ->
                            viewModel.getSpecialties().find { it.first == spec }?.second ?: ""
                        },
                        onValueChange = {},
                        label = { Text("Especialidad *") },
                        placeholder = { Text("Selecciona tu especialidad") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpecialtyExpandedChange(true) },
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null)
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    )
                    DropdownMenu(
                        expanded = specialtyExpanded,
                        onDismissRequest = { onSpecialtyExpandedChange(false) }
                    ) {
                        viewModel.getSpecialties().forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateSpecialty(value)
                                    onSpecialtyExpandedChange(false)
                                }
                            )
                        }
                    }
                }
                
                // Custom Specialty (if "otros")
                if (uiState.specialty == "otros") {
                    OutlinedTextField(
                        value = uiState.customSpecialty,
                        onValueChange = { viewModel.updateCustomSpecialty(it) },
                        label = { Text("Especifica tu especialidad") },
                        placeholder = { Text("Describe tu especialidad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Descripción de tus servicios") },
                    placeholder = { Text("Cuéntanos sobre tu experiencia, servicios especiales, horarios...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 5,
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
            }
        }
        
        // Profile Image Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Logo o Imagen de Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Profile image preview
                if (uiState.profileImageUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uiState.profileImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                // Upload image button
                OutlinedButton(
                    onClick = onProfileImageClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subir imagen desde dispositivo")
                }
                
                Text(
                    text = "O",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // URL input
                OutlinedTextField(
                    value = profileImageUrlInput,
                    onValueChange = {
                        onProfileImageUrlInputChange(it)
                        viewModel.setProfileImage(null, it)
                    },
                    label = { Text("Usar URL de imagen") },
                    placeholder = { Text("https://ejemplo.com/mi-foto.jpg") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Home, contentDescription = null)
                    }
                )
                Text(
                    text = "Formatos soportados: JPG, PNG, GIF (máx. 5MB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Documentation Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.documentationBase64 == null) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
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
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Documentación de soporte",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (uiState.documentationBase64 == null) {
                    Text(
                        text = "* Requerido",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.documentationFileName ?: "Documento adjuntado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setDocumentation(null, null) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                
                OutlinedButton(
                    onClick = onDocumentationClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.documentationBase64 == null
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.documentationBase64 == null) "Adjuntar archivo (PDF o imagen)" else "Cambiar archivo")
                }
                
                Text(
                    text = "Comparte licencias, certificaciones o material que respalde tu experiencia profesional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Work Location Section
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lugar de Trabajo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box {
                    OutlinedTextField(
                        value = uiState.workLocation,
                        onValueChange = {},
                        label = { Text("¿Dónde brindas tus servicios? *") },
                        placeholder = { Text("Selecciona lugar de trabajo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorkLocationExpandedChange(true) },
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null)
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    )
                    DropdownMenu(
                        expanded = workLocationExpanded,
                        onDismissRequest = { onWorkLocationExpandedChange(false) }
                    ) {
                        uiState.workLocations.forEach { location ->
                            val locationName = location["lugar_de_trabajo"]?.toString() ?: ""
                            DropdownMenuItem(
                                text = { Text(locationName) },
                                onClick = {
                                    viewModel.updateWorkLocation(locationName)
                                    onWorkLocationExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "¿Qué sigue?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("• Tu perfil será revisado por nuestro equipo")
                Text("• Verificaremos tu información profesional")
                Text("• Una vez aprobado, comenzarás a recibir solicitudes")
                Text("• Podrás configurar tus horarios y precios")
            }
        }
        
        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Creando perfil...")
            } else {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Perfil de Barbero")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
