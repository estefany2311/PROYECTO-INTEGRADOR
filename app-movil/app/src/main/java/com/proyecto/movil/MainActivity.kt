package com.proyecto.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.proyecto.movil.navegacion.NavegacionApp
import com.proyecto.movil.ui.theme.AppMovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppMovilTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavegacionApp()
                }
            }
        }
    }
}
