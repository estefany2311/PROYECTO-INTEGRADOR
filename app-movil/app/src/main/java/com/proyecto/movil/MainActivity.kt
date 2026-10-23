package com.proyecto.movil

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.view.PantallaCodigoAula
import com.proyecto.movil.view.PantallaMapa
import com.proyecto.movil.view.PantallaPerfiles
import com.proyecto.movil.view.PantallaPin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppMovilTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val almacen = Dependencias.almacenSesion
                    // T-205: si el niño no cerró sesión, la tablet abre directo en su inicio.
                    // remember: se decide una sola vez (si cambiara, el NavHost reiniciaría la navegación)
                    val inicio = remember { if (almacen.leer() != null) "mapa" else "codigoAula" }

                    NavHost(navController = navController, startDestination = inicio) {
                        composable("codigoAula") {
                            PantallaCodigoAula(onContinuar = { codigo ->
                                val limpio = codigo.trim()
                                almacen.guardarCodigoAula(limpio)   // T-205: para volver aquí al salir
                                navController.navigate("perfiles/${Uri.encode(limpio)}")
                            })
                        }
                        composable("perfiles/{codigoAula}") { backStackEntry ->
                            val codigoAula = backStackEntry.arguments?.getString("codigoAula") ?: ""
                            PantallaPerfiles(
                                codigoAula = codigoAula,
                                onPerfilElegido = { perfil ->
                                    // Uri.encode: los nombres pueden tener tildes o espacios y el avatar es una URL con ? y &
                                    navController.navigate(
                                        "pin/${perfil.id}?nombre=${Uri.encode(perfil.nombre)}&avatar=${Uri.encode(perfil.avatarUrl)}"
                                    )
                                }
                            )
                        }
                        composable(
                            "pin/{estudianteId}?nombre={nombre}&avatar={avatar}",
                            arguments = listOf(
                                navArgument("estudianteId") { type = NavType.LongType },
                                navArgument("nombre") { defaultValue = "" },
                                navArgument("avatar") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val args = backStackEntry.arguments
                            PantallaPin(
                                estudianteId = args?.getLong("estudianteId") ?: 0L,
                                nombre = args?.getString("nombre") ?: "",
                                avatarUrl = args?.getString("avatar") ?: "",
                                onVolver = { navController.popBackStack() },
                                onSesionIniciada = {
                                    // T-205: el token ya se guardó en el ViewModel. Se borra el historial
                                    // para que el botón "atrás" no regrese al PIN.
                                    navController.navigate("mapa") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("mapa") {
                            PantallaMapa()
                        }
                    }
                }
            }
        }
    }
}