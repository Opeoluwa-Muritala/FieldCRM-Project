package com.fieldcrm.android.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fieldcrm.android.ui.theme.FieldTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraOcrScanner(
    mode: String = "OCR", // "OCR" or "PHOTO"
    onTextScanned: (String) -> Unit = {},
    onPhotoCaptured: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
            var isScanning by remember { mutableStateOf(false) }
            var statusMessage by remember {
                mutableStateOf(
                    if (mode == "OCR") "Align text inside box and tap SCAN"
                    else "Frame photo and tap CAPTURE"
                )
            }
            
            // Camera Executor
            val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
            val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

            // ImageCapture setup (needed for PHOTO mode)
            val imageCapture = remember {
                ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
            }

            // Infinite scanner line animation (only for OCR mode)
            val transition = rememberInfiniteTransition(label = "scannerLine")
            val scannerProgress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "lineProgress"
            )

            // Setup PreviewView
            val previewView = remember { PreviewView(context) }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder and Scanning Grid overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        
                        // Viewport box in the center (smaller for OCR, larger for photo)
                        val boxWidth = if (mode == "OCR") 300.dp.toPx() else 340.dp.toPx()
                        val boxHeight = if (mode == "OCR") 200.dp.toPx() else 340.dp.toPx()
                        val left = (width - boxWidth) / 2
                        val top = (height - boxHeight) / 2

                        // Draw dark overlay outside viewport
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            size = size
                        )
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(boxWidth, boxHeight),
                            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                        )

                        // Draw white viewport border
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Draw scanning laser line (only in OCR mode)
                        if (mode == "OCR") {
                            val lineY = top + (boxHeight * scannerProgress)
                            drawLine(
                                color = Color(0xFFBA7EF4),
                                start = Offset(left, lineY),
                                end = Offset(left + boxWidth, lineY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
            )

            // Top Status Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (mode == "OCR") "ON-DEVICE OCR SCANNER" else "SITE PHOTO CAPTURE",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.purple400
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusMessage,
                    style = FieldTheme.typography.body,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))

                PrimaryButton(
                    text = if (isScanning) {
                        if (mode == "OCR") "Reading..." else "Saving..."
                    } else {
                        if (mode == "OCR") "Scan & Extract" else "Capture Photo"
                    },
                    onClick = {
                        isScanning = true
                        if (mode == "OCR") {
                            statusMessage = "Analyzing image frames..."
                            
                            val cameraProvider = cameraProviderFuture.get()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(
                                    imageProxy, 
                                    textRecognizer,
                                    onSuccess = { text ->
                                        isScanning = false
                                        statusMessage = "OCR Successful!"
                                        imageAnalysis.clearAnalyzer()
                                        onTextScanned(text)
                                    },
                                    onFailure = { error ->
                                        isScanning = false
                                        statusMessage = "OCR Failed: ${error.localizedMessage}"
                                        imageAnalysis.clearAnalyzer()
                                    }
                                )
                            }

                            // Bind analysis to camera lifecycle
                            try {
                                cameraProvider.unbindAll()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                                
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("CameraOcrScanner", "Use case binding failed", e)
                                isScanning = false
                                statusMessage = "Camera binding failed"
                            }
                        } else {
                            // Capture actual photo
                            statusMessage = "Capturing image..."
                            val photoFile = File(
                                context.cacheDir,
                                "visitation_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            
                            imageCapture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        isScanning = false
                                        // Execute on Main Thread
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            onPhotoCaptured(photoFile.absolutePath)
                                        }
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        isScanning = false
                                        statusMessage = "Capture failed: ${exception.localizedMessage}"
                                        Log.e("CameraOcrScanner", "Photo capture failed", exception)
                                    }
                                }
                            )
                        }
                    },
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bind preview on startup
            LaunchedEffect(cameraProviderFuture) {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                try {
                    cameraProvider.unbindAll()
                    if (mode == "OCR") {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview
                        )
                    } else {
                        // Bind both Preview and ImageCapture for PHOTO mode
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    }
                } catch (e: Exception) {
                    Log.e("CameraOcrScanner", "Camera initialization failed", e)
                    statusMessage = "Could not open camera preview"
                }
            }

            DisposableEffect(cameraProviderFuture) {
                onDispose {
                    try {
                        if (cameraProviderFuture.isDone) {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.unbindAll()
                        }
                    } catch (e: Exception) {
                        Log.e("CameraOcrScanner", "Error unbinding camera on dispose", e)
                    }
                    cameraExecutor.shutdown()
                }
            }

        } else {
            // Permission Denied View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = FieldTheme.typography.title,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This application requires camera access to scan documents and capture site photos.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Grant Permission",
                    onClick = { launcher.launch(Manifest.permission.CAMERA) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                onSuccess(visionText.text)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
