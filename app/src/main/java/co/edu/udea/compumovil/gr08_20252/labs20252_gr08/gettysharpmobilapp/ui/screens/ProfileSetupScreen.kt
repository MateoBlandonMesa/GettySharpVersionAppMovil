package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(navController: NavController) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Completa tu perfil",
                style = MaterialTheme.typography.headlineMedium
            )
            
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { navController.navigate(Screen.Dashboard.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar")
            }
        }
    }
}

