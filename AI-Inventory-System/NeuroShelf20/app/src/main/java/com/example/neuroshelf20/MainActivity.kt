package com.example.neuroshelf20

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.neuroshelf20.ui.MainMenuScreen
import com.example.neuroshelf20.camera.CameraActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainMenuScreen(
                    onStartCamera = {
                        startActivity(Intent(this, CameraActivity::class.java))
                    }
                )
            }
        }
    }
}
