package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ProfileSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    profileSetupViewModel: ProfileSetupViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by profileSetupViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    
    // Cargar géneros al iniciar
    LaunchedEffect(Unit) {
        profileSetupViewModel.loadGenders(context)
    }
    
    // Cargar datos del usuario autenticado para pre-llenar
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    
    // Cargar email y nombre desde sesión cuando el componente se carga
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val session = co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.services.AuthService.getSession(context)
            email = session?.email ?: authState.userProfile?.email ?: ""
            firstName = authState.userProfile?.firstName ?: ""
            lastName = authState.userProfile?.lastName ?: ""
        }
    }
    
    // Campos del formulario
    var phone by remember { mutableStateOf("") }
    var idType by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    // Navegar al dashboard si el perfil se creó exitosamente
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            // Recargar perfil en AuthViewModel
            authViewModel.checkAuthStatus(context)
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.ProfileSetup.route) { inclusive = true }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
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
                                text = "Completa tu perfil",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Por favor completa la siguiente información para continuar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Nombre
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("Nombre *") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSaving
                            )
                            
                            // Apellido
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Apellido *") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSaving
                            )
                            
                            // Email (read-only, viene de OAuth)
                            OutlinedTextField(
                                value = email,
                                onValueChange = {},
                                label = { Text("Correo Electrónico *") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                readOnly = true
                            )
                            
                            // Teléfono
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Teléfono *") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("+57 300 123 4567") },
                                enabled = !uiState.isSaving
                            )
                            
                            // Tipo de documento
                            var idTypeExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = idTypeExpanded,
                                onExpandedChange = { idTypeExpanded = !idTypeExpanded }
                            ) {
                                OutlinedTextField(
                                    value = idType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tipo de Documento *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = idTypeExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = !uiState.isSaving
                                )
                                ExposedDropdownMenu(
                                    expanded = idTypeExpanded,
                                    onDismissRequest = { idTypeExpanded = false }
                                ) {
                                    listOf(
                                        "Cédula de Ciudadanía" to "cedula",
                                        "Tarjeta de Identidad" to "tarjeta_identidad",
                                        "Pasaporte" to "pasaporte",
                                        "Otro" to "otro"
                                    ).forEach { (label, value) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                idType = value
                                                idTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Número de documento
                            OutlinedTextField(
                                value = idNumber,
                                onValueChange = { idNumber = it },
                                label = { Text("Número de Documento *") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSaving
                            )
                            
                            // Género
                            var genderExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = genderExpanded,
                                onExpandedChange = { genderExpanded = !genderExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedGender,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Género *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = !uiState.isSaving,
                                    placeholder = { 
                                        if (uiState.genders.isEmpty()) {
                                            Text("Cargando...")
                                        } else {
                                            Text("Selecciona un género")
                                        }
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = genderExpanded,
                                    onDismissRequest = { genderExpanded = false }
                                ) {
                                    uiState.genders.forEach { (_, genderName) ->
                                        DropdownMenuItem(
                                            text = { Text(genderName) },
                                            onClick = {
                                                selectedGender = genderName
                                                genderExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Dirección (simplificada para móvil)
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Dirección *") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ej: Calle 80 # 45-30, Medellín") },
                                minLines = 2,
                                maxLines = 3,
                                enabled = !uiState.isSaving
                            )
                            
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
                                    var hasError = false
                                    val errors = mutableListOf<String>()
                                    
                                    if (firstName.isBlank()) {
                                        errors.add("El nombre es requerido")
                                        hasError = true
                                    }
                                    if (lastName.isBlank()) {
                                        errors.add("El apellido es requerido")
                                        hasError = true
                                    }
                                    if (email.isBlank()) {
                                        errors.add("El correo electrónico es requerido")
                                        hasError = true
                                    }
                                    if (phone.isBlank()) {
                                        errors.add("El teléfono es requerido")
                                        hasError = true
                                    }
                                    if (idType.isBlank()) {
                                        errors.add("El tipo de documento es requerido")
                                        hasError = true
                                    }
                                    if (idNumber.isBlank()) {
                                        errors.add("El número de documento es requerido")
                                        hasError = true
                                    }
                                    if (selectedGender.isBlank()) {
                                        errors.add("El género es requerido")
                                        hasError = true
                                    }
                                    if (address.isBlank()) {
                                        errors.add("La dirección es requerida")
                                        hasError = true
                                    }
                                    
                                    if (hasError) {
                                        // Mostrar solo el primer error
                                        return@Button
                                    }
                                    
                                    profileSetupViewModel.clearError()
                                    profileSetupViewModel.createProfile(
                                        context = context,
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        email = email.trim(),
                                        phone = phone.trim(),
                                        idType = idType,
                                        idNumber = idNumber.trim(),
                                        gender = selectedGender,
                                        address = address.trim()
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
                                    Text(
                                        text = "Guardar Perfil",
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
