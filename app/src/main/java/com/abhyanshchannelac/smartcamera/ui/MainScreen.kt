package com.abhyanshchannelac.smartcamera.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.abhyanshchannelac.smartcamera.repository.ImageRecognitionRepository
import com.abhyanshchannelac.smartcamera.analytics.AnalyticsManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: ImageRecognitionRepository = remember { ImageRecognitionRepository() }
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCamera by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
            AnalyticsManager.logEvent("camera_permission_granted")
        } else {
            AnalyticsManager.logEvent("camera_permission_denied")
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Identify") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        isDarkTheme = !isDarkTheme
                        AnalyticsManager.logThemeChange(isDarkTheme)
                    }) {
                        Icon(
                            imageVector = if (isDarkTheme) {
                                Icons.Filled.LightMode
                            } else {
                                Icons.Filled.DarkMode
                            },
                            contentDescription = "Toggle theme"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showCamera) {
                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        val imageCapture = ImageCapture.Builder().build()
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                if (tags.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Identified Tags:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            tags.forEach { tag ->
                                Text(tag)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val imageCapture = ImageCapture.Builder().build()
                            imageCapture?.let { imageCapture ->
                                val photoFile = File(
                                    context.getExternalFilesDir(null),
                                    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
                                )

                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            imageUri = Uri.fromFile(photoFile)
                                            showCamera = false
                                            AnalyticsManager.logImageCapture(true)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            AnalyticsManager.logError("image_capture", exception.message ?: "Unknown error")
                                        }
                                    }
                                )
                            }
                            try {
                                val result = repository.recognizeImage(imageUri.toString())
                                tags = result.getOrNull()?.map { it.confidence.toString() + "% " + it.tag.en } ?: emptyList()
                                AnalyticsManager.logImageAnalysis(tags.size)
                            } catch (e: Exception) {
                                AnalyticsManager.logError("image_analysis", e.message ?: "Unknown error")
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Identify")
                }
            }
        }
    }
}
