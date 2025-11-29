package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.AuthViewModel
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.EditProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    editProfileViewModel: EditProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by editProfileViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    
    // Cargar perfil al iniciar
    LaunchedEffect(Unit) {
        editProfileViewModel.loadProfile(context)
    }
    
    // Campos editables (cargar desde perfil actual)
    var phone by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    // Actualizar campos cuando se carga el perfil
    LaunchedEffect(uiState.currentProfile) {
        uiState.currentProfile?.let { profile ->
            phone = profile.phone
            selectedGender = profile.gender
            address = profile.address
        }
    }
    
    // Navegar al dashboard si se actualizó exitosamente
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            // Recargar perfil en AuthViewModel
            authViewModel.checkAuthStatus(context)
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
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
            } else if (uiState.currentProfile == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No se pudo cargar el perfil",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Volver")
                    }
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
                    // Información del usuario (solo lectura)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Foto de perfil o iniciales
                            if (uiState.currentProfile?.fotoPerfil != null && uiState.currentProfile?.fotoPerfil?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = uiState.currentProfile?.fotoPerfil,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE07410)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${uiState.currentProfile?.firstName?.firstOrNull() ?: ""}${uiState.currentProfile?.lastName?.firstOrNull() ?: ""}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${uiState.currentProfile?.firstName ?: ""} ${uiState.currentProfile?.lastName ?: ""}".trim(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.currentProfile?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Formulario de edición
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
                                text = "Información Editable",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Campos de solo lectura (no se pueden editar)
                            OutlinedTextField(
                                value = uiState.currentProfile?.firstName ?: "",
                                onValueChange = {},
                                label = { Text("Nombre") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                readOnly = true
                            )
                            
                            OutlinedTextField(
                                value = uiState.currentProfile?.lastName ?: "",
                                onValueChange = {},
                                label = { Text("Apellido") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                readOnly = true
                            )
                            
                            OutlinedTextField(
                                value = uiState.currentProfile?.email ?: "",
                                onValueChange = {},
                                label = { Text("Correo Electrónico") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                readOnly = true
                            )
                            
                            // Teléfono (editable)
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Teléfono *") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("+57 300 123 4567") },
                                enabled = !uiState.isSaving
                            )
                            
                            // Género (editable)
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
                            
                            // Dirección (editable)
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
                                    if (phone.isBlank()) {
                                        return@Button
                                    }
                                    if (selectedGender.isBlank()) {
                                        return@Button
                                    }
                                    if (address.isBlank()) {
                                        return@Button
                                    }
                                    
                                    editProfileViewModel.clearError()
                                    editProfileViewModel.updateProfile(
                                        context = context,
                                        phone = phone.trim(),
                                        address = address.trim(),
                                        gender = selectedGender
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
                                        text = "Guardar Cambios",
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
