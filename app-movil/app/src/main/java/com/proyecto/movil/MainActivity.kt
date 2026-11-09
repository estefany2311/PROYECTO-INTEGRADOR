package com.proyecto.movil

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.view.PantallaCodigoAula
import com.proyecto.movil.view.PantallaConsigna
import com.proyecto.movil.model.TipoReto
import com.proyecto.movil.view.PantallaDetalleNivel
import com.proyecto.movil.view.PantallaGrabacion
import com.proyecto.movil.view.PantallaLectura
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
                            PantallaMapa(
                                // T-215: el mapa queda debajo; "atrás" en el detalle vuelve aquí
                                onAbrirNivel = { nivelId -> navController.navigate("nivel/$nivelId") },
                                onSalir = { codigoAula ->
                                // T-208: vuelve a "¿Quién eres?" del mismo salón para que entre otro niño.
                                // Si no hay código guardado, vuelve a pedir el código del aula.
                                val destino = if (codigoAula.isNullOrBlank()) "codigoAula"
                                else "perfiles/${Uri.encode(codigoAula)}"
                                // Se borra el historial: con "atrás" no se regresa al mapa del niño anterior
                                navController.navigate("codigoAula") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                                if (!codigoAula.isNullOrBlank()) {
                                    navController.navigate("perfiles/${Uri.encode(codigoAula)}")
                                }
                            })
                        }
                        // T-215 (US-07): detalle del nivel con el botón "Comenzar"
                        composable(
                            "nivel/{nivelId}",
                            arguments = listOf(navArgument("nivelId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            PantallaDetalleNivel(
                                nivelId = backStackEntry.arguments?.getInt("nivelId") ?: 0,
                                onVolver = { navController.popBackStack() },
                                onComenzar = { detalle ->
                                    // T-219: los retos de lectura y de oralidad empiezan leyendo un texto.
                                    // T-321: el reto de escritura empieza con su consigna.
                                    when (detalle.reto) {
                                        TipoReto.LECTURA, TipoReto.ORALIDAD -> navController.navigate("lectura/${detalle.id}")
                                        TipoReto.ESCRITURA -> navController.navigate("consigna/${detalle.id}")
                                        TipoReto.DESCONOCIDO -> Unit
                                    }
                                }
                            )
                        }
                        // T-219 (US-10): el texto para leer en voz alta, con letra grande
                        composable(
                            "lectura/{nivelId}",
                            arguments = listOf(navArgument("nivelId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            PantallaLectura(
                                nivelId = backStackEntry.arguments?.getInt("nivelId") ?: 0,
                                onVolver = { navController.popBackStack() },
                                onGrabar = { lectura -> navController.navigate("grabar/${lectura.nivelId}") }
                            )
                        }
                        // T-321 (US-17): la consigna del nivel de escritura, con su ilustración
                        composable(
                            "consigna/{nivelId}",
                            arguments = listOf(navArgument("nivelId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val contexto = LocalContext.current
                            PantallaConsigna(
                                nivelId = backStackEntry.arguments?.getInt("nivelId") ?: 0,
                                onVolver = { navController.popBackStack() },
                                onEmpezar = {
                                    // El editor (T-323) se conecta en la siguiente tarea
                                    Toast.makeText(contexto, "El editor se abre en la siguiente versión", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        // US-11: grabar la lectura. T-313 pide antes el permiso del micrófono
                        composable(
                            "grabar/{nivelId}",
                            arguments = listOf(navArgument("nivelId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            PantallaGrabacion(
                                nivelId = backStackEntry.arguments?.getInt("nivelId") ?: 0,
                                onVolver = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}