package com.proyecto.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.view.PantallaCodigoAula
import com.proyecto.movil.view.PantallaPerfiles

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppMovilTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "codigoAula") {
                        composable("codigoAula") {
                            PantallaCodigoAula(onContinuar = { codigo ->
                                navController.navigate("perfiles/$codigo")
                            })
                        }
                        composable("perfiles/{codigoAula}") { backStackEntry ->
                            val codigoAula = backStackEntry.arguments?.getString("codigoAula") ?: ""
                            PantallaPerfiles(codigoAula = codigoAula)
                        }
                    }
                }
            }
        }
    }
}