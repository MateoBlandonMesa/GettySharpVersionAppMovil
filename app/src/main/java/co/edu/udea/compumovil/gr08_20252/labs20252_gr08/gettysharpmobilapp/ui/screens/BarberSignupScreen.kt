package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.BarberSignupViewModel
import java.io.InputStream
import android.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberSignupScreen(
    navController: NavController,
    barberSignupViewModel: BarberSignupViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by barberSignupViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    
    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        barberSignupViewModel.loadInitialData(context)
    }
    
    // Campos del formulario
    var username by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var customSpecialty by remember { mutableStateOf("") }
    var workLocation by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fotoPerfilUrl by remember { mutableStateOf<String?>(null) }
    var fotoPerfilBase64 by remember { mutableStateOf<String?>(null) }
    var documentationBase64 by remember { mutableStateOf<String?>(null) }
    var documentationFileName by remember { mutableStateOf<String?>(null) }
    
    // Navegar al dashboard si se creó exitosamente
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            // Recargar perfil en AuthViewModel
            authViewModel.checkAuthStatus(context)
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.BarberSignup.route) { inclusive = true }
            }
        }
    }
    
    // Launchers para seleccionar archivos
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            fotoPerfilUrl = it.toString()
            // Convertir a base64
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                fotoPerfilBase64 = convertInputStreamToBase64(inputStream)
            }
        }
    }
    
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                documentationBase64 = convertInputStreamToBase64(inputStream)
                documentationFileName = getFileName(context, it)
            }
        }
    }
    
    // Mostrar mensaje si ya es profesional
    if (uiState.isAlreadyProfessional) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ya eres profesional") },
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
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF020A0E),
                                Color(0xFF020F14),
                                Color(0xFF020A0E)
                            )
                        )
                    )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Ya eres un profesional registrado",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "No puedes registrarte múltiples veces como profesional en Getty Sharp.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver")
                        }
                    }
                }
            }
        }
        return
    }
    
    Scaffold(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF020A0E),
                            Color(0xFF020F14),
                            Color(0xFF020A0E)
                        )
                    )
                )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "¡Conviértete en Barbero!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Completa tu perfil profesional y comienza a recibir clientes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Información del usuario base
                            if (authState.userProfile != null) {
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "${authState.userProfile?.firstName ?: ""} ${authState.userProfile?.lastName ?: ""}".trim(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = authState.userProfile?.email ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Nombre de usuario público
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Nombre de Usuario Público *") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("@mi_barberia_pro") },
                                enabled = !uiState.isSaving,
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            )
                            
                            Text(
                                text = "Este será el nombre que verán tus clientes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Especialidad
                            var specialtyExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = specialtyExpanded,
                                onExpandedChange = { specialtyExpanded = !specialtyExpanded }
                            ) {
                                OutlinedTextField(
                                    value = specialty,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Especialidad *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specialtyExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = !uiState.isSaving,
                                    placeholder = { Text("Selecciona tu especialidad") }
                                )
                                ExposedDropdownMenu(
                                    expanded = specialtyExpanded,
                                    onDismissRequest = { specialtyExpanded = false }
                                ) {
                                    listOf(
                                        "Barbería Tradicional" to "barberia",
                                        "Estilista" to "estilista",
                                        "Cejas y Depilación" to "cejas",
                                        "Masajes" to "masajes",
                                        "Otros" to "otros"
                                    ).forEach { (label, value) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                specialty = value
                                                specialtyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Especialidad personalizada si es "otros"
                            if (specialty == "otros") {
                                OutlinedTextField(
                                    value = customSpecialty,
                                    onValueChange = { customSpecialty = it },
                                    label = { Text("Especifica tu especialidad *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Describe tu especialidad") },
                                    enabled = !uiState.isSaving
                                )
                            }
                            
                            // Descripción
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Descripción de tus servicios") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Cuéntanos sobre tu experiencia, servicios especiales, horarios...") },
                                minLines = 3,
                                maxLines = 5,
                                enabled = !uiState.isSaving
                            )
                            
                            // Logo/Foto de perfil
                            Text(
                                text = "Logo o Imagen de Perfil",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Vista previa de imagen
                            if (fotoPerfilUrl != null || fotoPerfilBase64 != null) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .clip(CircleShape)
                                ) {
                                    AsyncImage(
                                        model = fotoPerfilBase64 ?: fotoPerfilUrl ?: "",
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // Botón para subir imagen
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE07410)
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seleccionar Imagen")
                            }
                            
                            // Campo de URL alternativa
                            OutlinedTextField(
                                value = fotoPerfilUrl ?: "",
                                onValueChange = { 
                                    fotoPerfilUrl = it
                                    fotoPerfilBase64 = null // Clear base64 if URL is entered
                                },
                                label = { Text("O usar URL de imagen") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("https://ejemplo.com/mi-foto.jpg") },
                                enabled = !uiState.isSaving
                            )
                            
                            // Documentación
                            Text(
                                text = "Documentación de Soporte *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (documentationFileName != null) {
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null)
                                            Text(
                                                text = documentationFileName ?: "Documento seleccionado",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                documentationBase64 = null
                                                documentationFileName = null
                                            },
                                            enabled = !uiState.isSaving
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { documentPickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE07410)
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (documentationFileName == null) "Adjuntar Documentación" else "Cambiar Documentación")
                            }
                            
                            Text(
                                text = "Comparte licencias, certificaciones o material que respalde tu experiencia profesional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Lugar de trabajo
                            var workLocationExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = workLocationExpanded,
                                onExpandedChange = { workLocationExpanded = !workLocationExpanded }
                            ) {
                                OutlinedTextField(
                                    value = workLocation,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("¿Dónde brindas tus servicios? *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workLocationExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = !uiState.isSaving && uiState.workLocations.isNotEmpty(),
                                    placeholder = { 
                                        if (uiState.workLocations.isEmpty()) {
                                            Text("Cargando...")
                                        } else {
                                            Text("Selecciona lugar de trabajo")
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationOn, contentDescription = null)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = workLocationExpanded,
                                    onDismissRequest = { workLocationExpanded = false }
                                ) {
                                    uiState.workLocations.forEach { location ->
                                        DropdownMenuItem(
                                            text = { Text(location.lugar_de_trabajo) },
                                            onClick = {
                                                workLocation = location.lugar_de_trabajo
                                                workLocationExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Mensaje informativo
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "¿Qué sigue?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    listOf(
                                        "Tu perfil será revisado por nuestro equipo",
                                        "Verificaremos tu información profesional",
                                        "Una vez aprobado, comenzarás a recibir solicitudes",
                                        "Podrás configurar tus horarios y precios"
                                    ).forEach { item ->
                                        Text(
                                            text = "• $item",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            
                            // Mensaje de error
                            uiState.errorMessage?.let { error ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            
                            // Botón de guardar
                            Button(
                                onClick = {
                                    // Validar campos
                                    if (username.isBlank()) {
                                        return@Button
                                    }
                                    if (specialty.isBlank()) {
                                        return@Button
                                    }
                                    if (specialty == "otros" && customSpecialty.isBlank()) {
                                        return@Button
                                    }
                                    if (workLocation.isBlank()) {
                                        return@Button
                                    }
                                    if (documentationBase64.isNullOrBlank()) {
                                        return@Button
                                    }
                                    
                                    barberSignupViewModel.clearError()
                                    barberSignupViewModel.createBarberProfile(
                                        context = context,
                                        username = username.trim(),
                                        specialty = specialty,
                                        customSpecialty = if (specialty == "otros") customSpecialty.trim() else null,
                                        workLocationLabel = workLocation,
                                        description = description.trim().takeIf { it.isNotBlank() },
                                        fotoPerfil = fotoPerfilBase64 ?: fotoPerfilUrl?.takeIf { it.isNotBlank() },
                                        documentationBase64 = documentationBase64
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE07410)
                                )
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Crear Perfil de Barbero",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to convert InputStream to Base64
private fun convertInputStreamToBase64(inputStream: InputStream): String {
    return try {
        val bytes = inputStream.readBytes()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        android.util.Log.e("BarberSignupScreen", "Error converting to base64: ${e.message}")
        ""
    }
}

// Helper function to get file name from URI
private fun getFileName(context: Context, uri: Uri): String? {
    return try {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        result
    } catch (e: Exception) {
        android.util.Log.e("BarberSignupScreen", "Error getting file name: ${e.message}")
        null
    }
}
