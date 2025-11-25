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
import com.example.neuroshelf20.camera.analyzer.FrameAnalyzer
import com.example.neuroshelf20.camera.analyzer.ObjectAnalyzer
import com.example.neuroshelf20.domain.DetectionManager
import com.example.neuroshelf20.domain.ObjectDetectionManager
import com.example.neuroshelf20.data.repository.EventRepositoryFirebaseImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.Executors

class CameraActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private val perms = arrayOf(Manifest.permission.CAMERA)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra("mode") ?: "faces"

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PreviewView(this).also { previewView = it }
                }
            )
        }

        val repo = EventRepositoryFirebaseImpl()

        AnalyzerHolder.faceManager = DetectionManager(this, scope, repo)

        // LO DESACTIVAMOS HASTA QUE YOLO ESTÉ LISTO
        // AnalyzerHolder.objManager = ObjectDetectionManager(this)

        if (!hasPerms()) reqPerms.launch(perms) else startCamera(mode)
    }

    private val reqPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val granted = it.values.all { v -> v }
            if (granted) startCamera("faces") else finish()
        }

    private fun hasPerms() =
        perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun startCamera(mode: String) {

        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // 👇 EJECUTOR CORRECTO PARA EVITAR CRASH
            val executor = Executors.newSingleThreadExecutor()

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(
                        executor,
                        FrameAnalyzer(AnalyzerHolder.faceManager!!)
                    )
                }

            val selector = CameraSelector.DEFAULT_BACK_CAMERA

            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, analyzer)

        }, ContextCompat.getMainExecutor(this))
    }
}

object AnalyzerHolder {
    var faceManager: DetectionManager? = null
    var objManager: ObjectDetectionManager? = null
}
