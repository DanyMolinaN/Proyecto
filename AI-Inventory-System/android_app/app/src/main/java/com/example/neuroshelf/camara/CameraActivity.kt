package com.example.neuroshelf.camara

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.neuroshelf.camara.Analyzer.FrameAnalyzer
import com.example.neuroshelf.domain.DetectionManager

class CameraActivity : ComponentActivity() {
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)

    private lateinit var detectionManager: DetectionManager

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach { 
                if (it.key in requiredPermissions && !it.value) permissionGranted = false
            }
            if (!permissionGranted) {
                finish()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detectionManager = DetectionManager()

        if (allPermissionsGranted()) startCamera() else activityResultLauncher.launch(requiredPermissions)

        setContent {
            // Jetpack Compose UI: preview + overlays would go here
            // For brevity not included; focus is on CameraX pipeline.
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val previewUseCase = androidx.camera.core.Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val frameAnalyzer = FrameAnalyzer(detectionManager)
            imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this), frameAnalyzer)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() =
        requiredPermissions.all { ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED }
}
