package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.constants.ColombiaConstants
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ProfileSetupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load initial data
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadInitialData(context)
    }

    // Handle user exists - redirect to dashboard
    LaunchedEffect(uiState.userExists) {
        if (uiState.userExists) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Ya tienes un perfil. Redirigiendo al dashboard...",
                    duration = SnackbarDuration.Short
                )
                kotlinx.coroutines.delay(2000)
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                }
            }
        }
    }

    // Handle success
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
                    popUpTo(Screen.ProfileSetup.route) { inclusive = true }
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configurar Perfil") },
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Cargando información...")
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
                // Header
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Crea tu Perfil",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Completa tu información para comenzar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Personal Information
                PersonalInformationSection(
                    uiState = uiState,
                    onFieldChange = { field, value -> viewModel.updateField(field, value) }
                )

                // Identity Document
                IdentityDocumentSection(
                    uiState = uiState,
                    onFieldChange = { field, value -> viewModel.updateField(field, value) }
                )

                // Gender
                GenderSection(
                    uiState = uiState,
                    onGenderChange = { gender -> viewModel.updateField("gender", gender) }
                )

                // Address
                AddressSection(
                    uiState = uiState,
                    onAddressPartChange = { field, value -> viewModel.updateAddressPart(field, value) }
                )

                // Generated Address Display
                if (uiState.fullAddress.isNotBlank()) {
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
                                text = "Dirección generada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.fullAddress,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Action Buttons
                ActionButtonsSection(
                    uiState = uiState,
                    onSubmit = {
                        viewModel.submitProfile(context) {
                            // Success handled in LaunchedEffect
                        }
                    },
                    onBarberSignup = {
                        if (uiState.firstName.isBlank() || uiState.lastName.isBlank() || uiState.email.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Completa al menos nombre, apellido y email antes de continuar",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            navController.navigate(Screen.BarberSignup.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PersonalInformationSection(
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ProfileSetupUiState,
    onFieldChange: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = "Información Personal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.firstName,
                    onValueChange = { onFieldChange("firstName", it) },
                    label = { Text("Nombre *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.lastName,
                    onValueChange = { onFieldChange("lastName", it) },
                    label = { Text("Apellido *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onFieldChange("email", it) },
                label = { Text("Correo Electrónico *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false, // Pre-filled from auth
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { onFieldChange("phone", it) },
                label = { Text("Teléfono *") },
                placeholder = { Text("+57 300 123 4567") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun IdentityDocumentSection(
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ProfileSetupUiState,
    onFieldChange: (String, String) -> Unit
) {
    var idTypeExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Documento de Identidad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Box {
                OutlinedTextField(
                    value = ColombiaConstants.idTypes.find { it.value == uiState.idType }?.label ?: "",
                    onValueChange = { },
                    label = { Text("Tipo de Documento *") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { idTypeExpanded = true }
                )
                DropdownMenu(
                    expanded = idTypeExpanded,
                    onDismissRequest = { idTypeExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    ColombiaConstants.idTypes.forEach { idType ->
                        DropdownMenuItem(
                            text = { Text(idType.label) },
                            onClick = {
                                onFieldChange("idType", idType.value)
                                idTypeExpanded = false
                            }
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = uiState.idNumber,
                onValueChange = { onFieldChange("idNumber", it) },
                label = { Text("Número de Documento *") },
                placeholder = { Text("123456789") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun GenderSection(
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ProfileSetupUiState,
    onGenderChange: (String) -> Unit
) {
    var genderExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Género *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Box {
                OutlinedTextField(
                    value = uiState.gender,
                    onValueChange = { },
                    label = { Text("Selecciona género") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { genderExpanded = true }
                )
                DropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    uiState.genders.forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender.genero) },
                            onClick = {
                                onGenderChange(gender.genero)
                                genderExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddressSection(
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ProfileSetupUiState,
    onAddressPartChange: (String, String) -> Unit
) {
    var departmentExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var streetTypeExpanded by remember { mutableStateOf(false) }
    var streetQualifierExpanded by remember { mutableStateOf(false) }
    var secondaryQualifierExpanded by remember { mutableStateOf(false) }
    var quadrantExpanded by remember { mutableStateOf(false) }

    val selectedDepartment = ColombiaConstants.departments.find { 
        it.name == uiState.addressParts.department 
    }
    val availableCities = selectedDepartment?.cities ?: emptyList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ubicación y Dirección",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Department and City
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.addressParts.department,
                        onValueChange = { },
                        label = { Text("Departamento *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { departmentExpanded = true }
                    )
                    DropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        ColombiaConstants.departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept.name) },
                                onClick = {
                                    onAddressPartChange("department", dept.name)
                                    departmentExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.addressParts.city,
                        onValueChange = { },
                        label = { Text("Ciudad *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        enabled = uiState.addressParts.department.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = uiState.addressParts.department.isNotBlank()) {
                                cityExpanded = true
                            }
                    )
                    DropdownMenu(
                        expanded = cityExpanded,
                        onDismissRequest = { cityExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        availableCities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    onAddressPartChange("city", city)
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Neighborhood and Complement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.addressParts.neighborhood,
                    onValueChange = { onAddressPartChange("neighborhood", it) },
                    label = { Text("Barrio / Localidad") },
                    placeholder = { Text("Ej: Laureles") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.addressParts.complement,
                    onValueChange = { onAddressPartChange("complement", it) },
                    label = { Text("Complemento") },
                    placeholder = { Text("Apartamento, interior...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Street Type, Number, Letter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.addressParts.streetType,
                        onValueChange = { },
                        label = { Text("Tipo de vía *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { streetTypeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = streetTypeExpanded,
                        onDismissRequest = { streetTypeExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        ColombiaConstants.streetTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    onAddressPartChange("streetType", type)
                                    streetTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.addressParts.streetNumber,
                    onValueChange = { onAddressPartChange("streetNumber", it) },
                    label = { Text("Número *") },
                    placeholder = { Text("80") },
                    modifier = Modifier.weight(0.7f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.addressParts.streetLetter,
                    onValueChange = { onAddressPartChange("streetLetter", it) },
                    label = { Text("Letra") },
                    placeholder = { Text("A") },
                    modifier = Modifier.weight(0.5f),
                    singleLine = true
                )
            }

            // Street Qualifier
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.addressParts.streetQualifier,
                    onValueChange = { },
                    label = { Text("Calificador") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { streetQualifierExpanded = true }
                )
                DropdownMenu(
                    expanded = streetQualifierExpanded,
                    onDismissRequest = { streetQualifierExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    ColombiaConstants.streetQualifiers.forEach { qualifier ->
                        DropdownMenuItem(
                            text = { Text(qualifier) },
                            onClick = {
                                onAddressPartChange("streetQualifier", qualifier)
                                streetQualifierExpanded = false
                            }
                        )
                    }
                }
            }

            // Secondary Number, Letter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.addressParts.secondaryNumber,
                    onValueChange = { onAddressPartChange("secondaryNumber", it) },
                    label = { Text("Número secundario *") },
                    placeholder = { Text("45") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.addressParts.secondaryLetter,
                    onValueChange = { onAddressPartChange("secondaryLetter", it) },
                    label = { Text("Letra secundaria") },
                    placeholder = { Text("A") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Secondary Qualifier
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.addressParts.secondaryQualifier,
                    onValueChange = { },
                    label = { Text("Calificador secundario") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { secondaryQualifierExpanded = true }
                )
                DropdownMenu(
                    expanded = secondaryQualifierExpanded,
                    onDismissRequest = { secondaryQualifierExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    ColombiaConstants.streetQualifiers.forEach { qualifier ->
                        DropdownMenuItem(
                            text = { Text(qualifier) },
                            onClick = {
                                onAddressPartChange("secondaryQualifier", qualifier)
                                secondaryQualifierExpanded = false
                            }
                        )
                    }
                }
            }

            // Plate Number and Quadrant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.addressParts.plateNumber,
                    onValueChange = { onAddressPartChange("plateNumber", it) },
                    label = { Text("Número de placa *") },
                    placeholder = { Text("30") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.addressParts.quadrant,
                        onValueChange = { },
                        label = { Text("Cuadrante") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { quadrantExpanded = true }
                    )
                    DropdownMenu(
                        expanded = quadrantExpanded,
                        onDismissRequest = { quadrantExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        ColombiaConstants.quadrants.forEach { quadrant ->
                            DropdownMenuItem(
                                text = { Text(quadrant) },
                                onClick = {
                                    onAddressPartChange("quadrant", quadrant)
                                    quadrantExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    uiState: co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.viewmodel.ProfileSetupUiState,
    onSubmit: () -> Unit,
    onBarberSignup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardando...")
            } else {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Perfil", fontSize = 16.sp)
            }
        }

        OutlinedButton(
            onClick = onBarberSignup,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("¡Soy Barbero!", fontSize = 16.sp)
        }
    }
}
