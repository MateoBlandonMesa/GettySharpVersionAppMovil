package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.navigation.Screen

@Composable
fun LandingScreen(navController: NavController) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = Color(0xFFE07410),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GS",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Getty Sharp",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Barbería de Lujo",
                fontSize = 18.sp,
                color = Color(0xFFE07410),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = { navController.navigate(Screen.Login.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE07410)
                )
            ) {
                Text(
                    text = "Iniciar Sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


