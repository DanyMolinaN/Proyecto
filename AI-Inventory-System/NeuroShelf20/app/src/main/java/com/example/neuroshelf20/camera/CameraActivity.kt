package com.example.neuroshelf20.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.neuroshelf20.domain.DetectionManager
import com.example.neuroshelf20.data.repository.EventRepositoryFirebaseImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import com.example.neuroshelf20.camera.analyzer.FrameAnalyzer

class CameraActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private val externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) startCamera() else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SOLO este repositorio porque DetectionManager solo recibe uno
        val eventRepo = EventRepositoryFirebaseImpl()

        val detectionManager = DetectionManager(
            context = this,
            externalScope = externalScope,
            eventRepository = eventRepo
        )

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    previewView = PreviewView(ctx)
                    previewView
                }
            )
        }

        if (!allPermissionsGranted()) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            startCamera()
        }

        // Pasamos el detectionManager al analyzer
        AnalyzerHolder.detectionManager = detectionManager
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        FrameAnalyzer(AnalyzerHolder.detectionManager!!)
                    )
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(baseContext, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
}

// Holder para pasar DetectionManager al FrameAnalyzer
object AnalyzerHolder {
    var detectionManager: DetectionManager? = null
}
