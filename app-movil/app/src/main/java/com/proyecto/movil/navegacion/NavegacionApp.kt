package com.proyecto.movil.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyecto.movil.Dependencias
import com.proyecto.movil.model.TipoReto
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.view.PantallaCodigoAula
import com.proyecto.movil.view.PantallaConsigna
import com.proyecto.movil.view.PantallaDetalleNivel
import com.proyecto.movil.view.PantallaEditor
import com.proyecto.movil.view.PantallaEspera
import com.proyecto.movil.view.PantallaGrabacion
import com.proyecto.movil.view.PantallaLectura
import com.proyecto.movil.view.PantallaMapa
import com.proyecto.movil.view.PantallaPerfiles
import com.proyecto.movil.view.PantallaPin

private val ARGUMENTO_NIVEL = listOf(navArgument(Rutas.ARG_NIVEL) { type = NavType.IntType })

private fun NavBackStackEntry.nivelId() = arguments?.getInt(Rutas.ARG_NIVEL) ?: 0

// Borra el historial: con "atrás" no se regresa a la pantalla anterior (PIN o mapa de otro niño)
private fun NavHostController.irSinVolver(ruta: String) = navigate(ruta) {
    popUpTo(graph.id) { inclusive = true }
}

// Navegación de la app: qué pantalla se abre con cada acción del niño
@Composable
fun NavegacionApp(almacen: IAlmacenSesion = Dependencias.almacenSesion) {
    val nav = rememberNavController()
    // T-205: si el niño no cerró sesión, la tablet abre directo en su mapa (se decide una sola vez)
    val inicio = remember { if (almacen.leer() != null) Rutas.MAPA else Rutas.CODIGO_AULA }

    NavHost(navController = nav, startDestination = inicio) {
        composable(Rutas.CODIGO_AULA) {
            PantallaCodigoAula(onContinuar = { codigo ->
                val limpio = codigo.trim()
                almacen.guardarCodigoAula(limpio)   // T-205: para volver aquí al salir
                nav.navigate(Rutas.perfiles(limpio))
            })
        }
        composable(Rutas.PERFILES) { entrada ->
            PantallaPerfiles(
                codigoAula = entrada.arguments?.getString(Rutas.ARG_CODIGO_AULA) ?: "",
                onPerfilElegido = { perfil -> nav.navigate(Rutas.pin(perfil.id, perfil.nombre, perfil.avatarUrl)) }
            )
        }
        composable(
            Rutas.PIN,
            arguments = listOf(
                navArgument(Rutas.ARG_ESTUDIANTE) { type = NavType.LongType },
                navArgument(Rutas.ARG_NOMBRE) { defaultValue = "" },
                navArgument(Rutas.ARG_AVATAR) { defaultValue = "" }
            )
        ) { entrada ->
            val args = entrada.arguments
            PantallaPin(
                estudianteId = args?.getLong(Rutas.ARG_ESTUDIANTE) ?: 0L,
                nombre = args?.getString(Rutas.ARG_NOMBRE) ?: "",
                avatarUrl = args?.getString(Rutas.ARG_AVATAR) ?: "",
                onVolver = { nav.popBackStack() },
                onSesionIniciada = { nav.irSinVolver(Rutas.MAPA) }
            )
        }
        composable(Rutas.MAPA) {
            PantallaMapa(
                onAbrirNivel = { nivelId -> nav.navigate(Rutas.nivel(nivelId)) },
                // T-208: vuelve a "¿Quién eres?" del mismo salón para que entre otro niño
                onSalir = { codigoAula ->
                    nav.irSinVolver(Rutas.CODIGO_AULA)
                    if (!codigoAula.isNullOrBlank()) nav.navigate(Rutas.perfiles(codigoAula))
                }
            )
        }
        composable(Rutas.NIVEL, arguments = ARGUMENTO_NIVEL) { entrada ->
            PantallaDetalleNivel(
                nivelId = entrada.nivelId(),
                onVolver = { nav.popBackStack() },
                onComenzar = { detalle ->
                    when (detalle.reto) {
                        TipoReto.LECTURA, TipoReto.ORALIDAD -> nav.navigate(Rutas.lectura(detalle.id))
                        TipoReto.ESCRITURA -> nav.navigate(Rutas.consigna(detalle.id))
                        TipoReto.DESCONOCIDO -> Unit
                    }
                }
            )
        }
        composable(Rutas.LECTURA, arguments = ARGUMENTO_NIVEL) { entrada ->
            PantallaLectura(
                nivelId = entrada.nivelId(),
                onVolver = { nav.popBackStack() },
                onGrabar = { lectura -> nav.navigate(Rutas.grabar(lectura.nivelId)) }
            )
        }
        composable(Rutas.GRABAR, arguments = ARGUMENTO_NIVEL) { entrada ->
            PantallaGrabacion(
                nivelId = entrada.nivelId(),
                onVolver = { nav.popBackStack() },
                // T-418: al enviar pasa a la espera; "atrás" ya no regresa a la grabación
                onEnviada = { evidenciaId ->
                    nav.navigate(Rutas.espera(evidenciaId)) { popUpTo(Rutas.GRABAR) { inclusive = true } }
                }
            )
        }
        composable(
            Rutas.ESPERA,
            arguments = listOf(navArgument(Rutas.ARG_EVIDENCIA) { type = NavType.LongType })
        ) { entrada ->
            PantallaEspera(
                evidenciaId = entrada.arguments?.getLong(Rutas.ARG_EVIDENCIA) ?: 0L,
                onVolverAlMapa = { nav.popBackStack(Rutas.MAPA, inclusive = false) },
                // T-431: vuelve a la lectura del mismo nivel para leer y grabar otra vez
                onIntentarDeNuevo = { nivelId ->
                    if (!nav.popBackStack(Rutas.LECTURA, inclusive = false)) {
                        nav.navigate(Rutas.lectura(nivelId)) { popUpTo(Rutas.MAPA) }
                    }
                }
            )
        }
        composable(Rutas.CONSIGNA, arguments = ARGUMENTO_NIVEL) { entrada ->
            PantallaConsigna(
                nivelId = entrada.nivelId(),
                onVolver = { nav.popBackStack() },
                onEmpezar = { consigna -> nav.navigate(Rutas.escribir(consigna.nivelId)) }
            )
        }
        composable(Rutas.ESCRIBIR, arguments = ARGUMENTO_NIVEL) { entrada ->
            PantallaEditor(
                nivelId = entrada.nivelId(),
                onVolver = { nav.popBackStack() },
                // T-435: después de enviar el texto vuelve al mapa (se refresca con el nivel desbloqueado)
                onVolverAlMapa = { nav.popBackStack(Rutas.MAPA, inclusive = false) }
            )
        }
    }
}
