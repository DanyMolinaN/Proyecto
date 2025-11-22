package com.example.neuroshelf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.neuroshelf.camara.CameraActivity
import com.example.neuroshelf.ui.theme.NeuroShelfTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NeuroShelfTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartCamera = {
                            startActivity(Intent(this, CameraActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, onStartCamera: () -> Unit) {
    Column(modifier = modifier) {
        Text("NeuroShelf — Sistema de Inventario Inteligente")

        Button(onClick = onStartCamera) {
            Text("Iniciar Cámara")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    NeuroShelfTheme {
        MainScreen(onStartCamera = {})
    }
}
