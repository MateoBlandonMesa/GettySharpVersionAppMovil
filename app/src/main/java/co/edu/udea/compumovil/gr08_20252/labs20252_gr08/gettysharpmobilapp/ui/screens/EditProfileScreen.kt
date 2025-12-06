package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.AvailabilityBlock
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.Gender
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.models.WorkLocation
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.EditProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var showAddAvailabilityDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<AvailabilityBlock?>(null) }
    
    // Form fields
    var phone by remember { mutableStateOf("") }
    var genderId by remember { mutableStateOf<Int?>(null) }
    var address by remember { mutableStateOf("") }
    var fotoPerfilUrl by remember { mutableStateOf<String?>(null) }
    var fotoPerfilBase64 by remember { mutableStateOf<String?>(null) }
    
    // Professional fields
    var username by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var workLocationId by remember { mutableStateOf<Int?>(null) }
    
    // Availability fields
    var availabilityDate by remember { mutableStateOf("") }
    var availabilityStartTime by remember { mutableStateOf("") }
    var availabilityEndTime by remember { mutableStateOf("") }
    var availabilityNotes by remember { mutableStateOf("") }
    
    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = viewModel.convertImageToBase64(it, context)
            if (base64 != null) {
                fotoPerfilBase64 = base64
                fotoPerfilUrl = "data:image/jpeg;base64,$base64"
            }
        }
    }
    
    // Load profile on screen appear
    LaunchedEffect(Unit) {
        viewModel.loadProfile(context)
    }
    
    // Update form fields when profile loads
    LaunchedEffect(uiState.profile) {
        uiState.profile?.let { profile ->
            phone = profile.phone ?: ""
            // Try to parse as ID first (e.g., "1", "2", "3"), then fall back to name matching
            genderId = profile.gender?.let { gender ->
                // First, try to parse as ID directly
                val genderIdInt = gender.trim().toIntOrNull()
                if (genderIdInt != null) {
                    // If it's a valid number, use it directly
                    genderIdInt.takeIf { uiState.genders.any { it.id == genderIdInt } }
                } else {
                    // Otherwise, try to match by name
                    val genderLower = gender.lowercase().trim()
                    uiState.genders.find { genderItem ->
                        val itemLower = genderItem.genero.lowercase().trim()
                        itemLower == genderLower || 
                        itemLower.contains(genderLower) || 
                        genderLower.contains(itemLower) ||
                        // Additional checks for common variations
                        (genderLower.contains("hombre") && (itemLower.contains("hombre") || itemLower.contains("masculino"))) ||
                        (genderLower.contains("mujer") && (itemLower.contains("mujer") || itemLower.contains("femenino"))) ||
                        (genderLower.contains("masculino") && (itemLower.contains("hombre") || itemLower.contains("masculino"))) ||
                        (genderLower.contains("femenino") && (itemLower.contains("mujer") || itemLower.contains("femenino"))) ||
                        (genderLower.contains("otro") && itemLower.contains("otro"))
                    }?.id
                }
            }
            address = profile.address ?: ""
            fotoPerfilUrl = profile.fotoPerfil
            username = profile.username ?: ""
            specialty = profile.specialty ?: ""
            workLocationId = profile.workLocation?.toIntOrNull()
        }
    }
    
    // Show success message
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
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
                CircularProgressIndicator()
            }
        } else if (uiState.profile == null) {
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
                        text = uiState.errorMessage ?: "No se pudo cargar el perfil",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { viewModel.loadProfile(context) }) {
                        Text("Reintentar")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Error message
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // Personal Information Section
                PersonalInformationSection(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    genderId = genderId,
                    genders = uiState.genders,
                    onGenderChange = { genderId = it },
                    address = address,
                    onAddressChange = { address = it },
                    fotoPerfilUrl = fotoPerfilUrl,
                    onImageClick = { imagePickerLauncher.launch("image/*") }
                )
                
                // Professional Information Section (only for barbers)
                if (uiState.profile?.isBarber == true) {
                    ProfessionalInformationSection(
                        username = username,
                        onUsernameChange = { username = it },
                        specialty = specialty,
                        onSpecialtyChange = { specialty = it },
                        workLocationId = workLocationId,
                        workLocations = uiState.workLocations,
                        onWorkLocationChange = { workLocationId = it }
                    )
                    
                    // Availability Management Section - REMOVED: Only shown in availability dialog from Dashboard
                    // This section is no longer shown in EditProfile, as availability is managed through
                    // the "Publicar disponibilidad" dialog from the Dashboard screen
                }
                
                // Save button
                Button(
                    onClick = {
                        val gender = genderId?.let { id ->
                            uiState.genders.find { it.id == id }?.genero
                        }
                        viewModel.updateProfile(
                            context = context,
                            phone = phone.takeIf { it.isNotEmpty() },
                            genderId = genderId,
                            address = address.takeIf { it.isNotEmpty() },
                            fotoPerfil = fotoPerfilBase64 ?: fotoPerfilUrl,
                            username = if (uiState.profile?.isBarber == true) username.takeIf { it.isNotEmpty() } else null,
                            specialty = if (uiState.profile?.isBarber == true) specialty.takeIf { it.isNotEmpty() } else null,
                            workLocationId = if (uiState.profile?.isBarber == true) workLocationId else null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar Cambios")
                }
            }
        }
    }
    
    // Add Availability Dialog
    if (showAddAvailabilityDialog && uiState.profile?.isBarber == true) {
        AddAvailabilityDialog(
            date = availabilityDate,
            onDateChange = { newValue -> availabilityDate = newValue },
            startTime = availabilityStartTime,
            onStartTimeChange = { newValue -> availabilityStartTime = newValue },
            endTime = availabilityEndTime,
            onEndTimeChange = { newValue -> availabilityEndTime = newValue },
            notes = availabilityNotes,
            onNotesChange = { newValue -> availabilityNotes = newValue },
            isAdding = uiState.isAddingAvailability,
            error = uiState.availabilityError,
            onDismiss = {
                showAddAvailabilityDialog = false
                availabilityDate = ""
                availabilityStartTime = ""
                availabilityEndTime = ""
                availabilityNotes = ""
            },
            onConfirm = {
                uiState.profile?.professionalId?.let { professionalId ->
                    viewModel.addAvailabilityBlock(
                        professionalId = professionalId,
                        date = availabilityDate,
                        startTime = availabilityStartTime,
                        endTime = availabilityEndTime,
                        notes = availabilityNotes.takeIf { it.isNotEmpty() }
                    )
                }
                showAddAvailabilityDialog = false
                availabilityDate = ""
                availabilityStartTime = ""
                availabilityEndTime = ""
                availabilityNotes = ""
            }
        )
    }
    
    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { block ->
        DeleteConfirmationDialog(
            block = block,
            onDismiss = { showDeleteConfirmation = null },
            onConfirm = {
                uiState.profile?.professionalId?.let { professionalId ->
                    viewModel.deleteAvailabilityBlock(professionalId, block.id)
                }
                showDeleteConfirmation = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun PersonalInformationSection(
    phone: String,
    onPhoneChange: (String) -> Unit,
    genderId: Int?,
    genders: List<Gender>,
    onGenderChange: (Int?) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    fotoPerfilUrl: String?,
    onImageClick: () -> Unit
) {
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
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Información Personal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            // Profile Photo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(onClick = onImageClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoPerfilUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(fotoPerfilUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Agregar foto",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Teléfono") },
                placeholder = { Text("+57 300 123 4567") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null)
                }
            )
            
            // Gender
            var genderExpanded by remember { mutableStateOf(false) }
            val selectedGenderText = genderId?.let { id ->
                genders.find { it.id == id }?.genero ?: ""
            } ?: ""
            
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded }
            ) {
                OutlinedTextField(
                    value = selectedGenderText,
                    onValueChange = {},
                    label = { Text("Género") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    genders.forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender.genero) },
                            onClick = {
                                onGenderChange(gender.id)
                                genderExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Address
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                label = { Text("Dirección") },
                placeholder = { Text("Calle 123 #45-67, Barrio, Ciudad") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun ProfessionalInformationSection(
    username: String,
    onUsernameChange: (String) -> Unit,
    specialty: String,
    onSpecialtyChange: (String) -> Unit,
    workLocationId: Int?,
    workLocations: List<WorkLocation>,
    onWorkLocationChange: (Int?) -> Unit
) {
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
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Información Profesional",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            // Username
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Nombre de Usuario Público") },
                placeholder = { Text("@mi_barberia_pro") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )
            
            // Specialty
            OutlinedTextField(
                value = specialty,
                onValueChange = onSpecialtyChange,
                label = { Text("Especialidad") },
                placeholder = { Text("Barbería Tradicional, Estilista, etc.") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Build, contentDescription = null)
                }
            )
            
            // Work Location
            var workLocationExpanded by remember { mutableStateOf(false) }
            val selectedWorkLocationText = workLocationId?.let { id ->
                workLocations.find { it.id == id }?.lugarDeTrabajo ?: ""
            } ?: ""
            
            ExposedDropdownMenuBox(
                expanded = workLocationExpanded,
                onExpandedChange = { workLocationExpanded = !workLocationExpanded }
            ) {
                OutlinedTextField(
                    value = selectedWorkLocationText,
                    onValueChange = {},
                    label = { Text("Lugar de Trabajo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = workLocationExpanded)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    }
                )
                ExposedDropdownMenu(
                    expanded = workLocationExpanded,
                    onDismissRequest = { workLocationExpanded = false }
                ) {
                    workLocations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location.lugarDeTrabajo) },
                            onClick = {
                                onWorkLocationChange(location.id)
                                workLocationExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AvailabilityManagementSection(
    availabilityBlocks: List<AvailabilityBlock>,
    isLoadingAvailability: Boolean,
    availabilityError: String?,
    onAddClick: () -> Unit,
    onDeleteClick: (AvailabilityBlock) -> Unit,
    onRefresh: () -> Unit
) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        "Gestión de Disponibilidad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                    }
                }
            }
            
            HorizontalDivider()
            
            if (isLoadingAvailability) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            } else if (availabilityError != null) {
                Text(
                    text = availabilityError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (availabilityBlocks.isEmpty()) {
                Text(
                    text = "No hay bloques de disponibilidad publicados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                availabilityBlocks.forEach { block ->
                    AvailabilityBlockItem(
                        block = block,
                        onDelete = { onDeleteClick(block) }
                    )
                }
            }
        }
    }
}

@Composable
fun AvailabilityBlockItem(
    block: AvailabilityBlock,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = formatAvailabilityBlock(block),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duración: ${getSlotDuration(block)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                block.status?.let { status ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAvailabilityDialog(
    date: String,
    onDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isAdding: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    
    // Date picker state
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    
    // DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            onDateChange(dateFormat.format(calendar.time))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Seleccionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Disponibilidad") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date picker button
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Fecha",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Text(
                                    text = date.ifEmpty { "Seleccionar fecha" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (date.isEmpty()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Seleccionar fecha",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Start Time picker button
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Hora Inicio",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                if (startTime.isNotEmpty()) {
                                    val timeParts = startTime.split(":")
                                    if (timeParts.size == 2) {
                                        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 0)
                                        calendar.set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                                    }
                                }
                                TimePickerDialog(
                                    context,
                                    { _: TimePicker, hour: Int, minute: Int ->
                                        onStartTimeChange(String.format("%02d:%02d", hour, minute))
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Text(
                                    text = startTime.ifEmpty { "Seleccionar hora inicio" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (startTime.isEmpty()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Seleccionar hora",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // End Time picker button
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Hora Fin",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                if (endTime.isNotEmpty()) {
                                    val timeParts = endTime.split(":")
                                    if (timeParts.size == 2) {
                                        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 0)
                                        calendar.set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                                    }
                                }
                                TimePickerDialog(
                                    context,
                                    { _: TimePicker, hour: Int, minute: Int ->
                                        onEndTimeChange(String.format("%02d:%02d", hour, minute))
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Text(
                                    text = endTime.ifEmpty { "Seleccionar hora fin" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (endTime.isEmpty()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Seleccionar hora",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notas (opcional)") },
                    placeholder = { Text("Información adicional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
                
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isAdding && date.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty()
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    block: AvailabilityBlock,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Disponibilidad") },
        text = {
            Column {
                Text("¿Estás seguro de que quieres eliminar este bloque de disponibilidad?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatAvailabilityBlock(block),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun formatAvailabilityBlock(block: AvailabilityBlock): String {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd/MM/yyyy 'de' HH:mm 'a' HH:mm", Locale.getDefault())
        
        val start = dateFormat.parse(block.start) ?: return "${block.start} - ${block.end}"
        val end = dateFormat.parse(block.end) ?: return "${block.start} - ${block.end}"
        
        "${outputDateFormat.format(start)}"
    } catch (e: Exception) {
        "${block.start} - ${block.end}"
    }
}

fun getSlotDuration(block: AvailabilityBlock): String {
    val value = block.minimumSlotLength
    return when {
        value is Number -> "${(value.toLong() / 60000)} min"
        value is String && value.contains(":") -> {
            val parts = value.split(":")
            val hours = parts[0].toIntOrNull() ?: 0
            val minutes = parts[1].toIntOrNull() ?: 0
            "${hours * 60 + minutes} min"
        }
        else -> {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val start = dateFormat.parse(block.start)
                val end = dateFormat.parse(block.end)
                if (start != null && end != null) {
                    val diffMinutes = ((end.time - start.time) / 60000).toInt()
                    "${maxOf(5, diffMinutes)} min"
                } else {
                    "30 min"
                }
            } catch (e: Exception) {
                "30 min"
            }
        }
    }
}
