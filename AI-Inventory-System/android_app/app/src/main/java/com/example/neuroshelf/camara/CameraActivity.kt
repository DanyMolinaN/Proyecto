package com.example.neuroshelf.camara

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.neuroshelf.camara.Analyzer.FrameAnalyzer
import com.example.neuroshelf.domain.DetectionManager
import com.example.neuroshelf.domain.event.InMemoryEventRepository   // ✅ IMPORT CORRECTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CameraActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private lateinit var detectionManager: DetectionManager
    private lateinit var previewView: PreviewView

    // Scope para IA
    private val externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Repositorio de eventos en memoria
    private val eventRepository = InMemoryEventRepository()  // ✅ Ahora existe e importa bien

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) startCamera()
            else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        detectionManager = DetectionManager(
            context = this,
            externalScope = externalScope,
            eventRepository = eventRepository
        )

        setContent {
            AndroidView(
                factory = { context ->
                    previewView = PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (allPermissionsGranted()) startCamera()
        else permissionLauncher.launch(requiredPermissions)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation)
                .build()
                .apply {
                    setAnalyzer(
                        ContextCompat.getMainExecutor(this@CameraActivity),
                        FrameAnalyzer(detectionManager)
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
}
