package com.fieldcrm.android.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// A4 aspect ratio: 210mm × 297mm = 0.707
private const val A4_RATIO = 210f / 297f

/**
 * Unified camera component supporting three modes:
 *
 * - "OCR"           — live text recognition, single frame
 * - "PHOTO"         — single photo capture (for site visits)
 * - "DOCUMENT_SCAN" — multi-page A4 document scanner that assembles a PDF
 *                     and calls onPdfReady(pdfBytes) when the user taps Done
 */
@Composable
fun CameraOcrScanner(
    mode: String = "OCR",
    onTextScanned: (String) -> Unit = {},
    onPhotoCaptured: (String) -> Unit = {},
    onPdfReady: (ByteArray) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        PermissionDeniedContent(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }, onDismiss = onDismiss)
        return
    }

    when (mode) {
        "DOCUMENT_SCAN" -> DocumentScanMode(context, lifecycleOwner, onPdfReady, onDismiss)
        "PHOTO"         -> PhotoCaptureMode(context, lifecycleOwner, onPhotoCaptured, onDismiss)
        else            -> OcrLiveMode(context, lifecycleOwner, onTextScanned, onDismiss)
    }
}

// ---------------------------------------------------------------------------
// DOCUMENT_SCAN mode — multi-page A4 capture → PDF
// ---------------------------------------------------------------------------

@Composable
private fun DocumentScanMode(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onPdfReady: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    // Accumulated page bitmaps
    val pages = remember { mutableStateListOf<Bitmap>() }
    var statusMessage by remember { mutableStateOf("Align A4 document within the frame") }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(cameraProviderFuture) {
        val provider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
            )
        } catch (e: Exception) {
            Log.e("DocScan", "Camera bind failed", e)
            statusMessage = "Camera initialisation failed"
        }
    }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            try { if (cameraProviderFuture.isDone) cameraProviderFuture.get().unbindAll() } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Camera preview
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // A4 overlay
        A4Overlay()

        // Status bar at top
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DOCUMENT SCANNER — Page ${pages.size + 1}",
                style = FieldTheme.typography.label,
                color = FieldTheme.colors.purple400
            )
            Spacer(Modifier.height(4.dp))
            Text(statusMessage, style = FieldTheme.typography.body, color = Color.White, textAlign = TextAlign.Center)
        }

        // Thumbnail strip of captured pages
        if (pages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 12.dp)
                    .width(72.dp),
                verticalAlignment = Alignment.Top
            ) {
                itemsIndexed(pages) { idx, bmp ->
                    Box(contentAlignment = Alignment.TopEnd) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Page ${idx + 1}",
                            modifier = Modifier
                                .size(60.dp, 84.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, FieldTheme.colors.purple400, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(FieldTheme.colors.purple400, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${idx + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Bottom action bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                PrimaryButton(
                    text = if (isCapturing) "Capturing…" else "Capture Page",
                    enabled = !isCapturing,
                    onClick = {
                        isCapturing = true
                        statusMessage = "Capturing…"
                        val file = File(context.cacheDir, "doc_page_${System.currentTimeMillis()}.jpg")
                        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture.takePicture(opts, cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                    val bmp = cropToA4(BitmapFactory.decodeFile(file.absolutePath))
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        pages.add(bmp)
                                        isCapturing = false
                                        statusMessage = "Page ${pages.size} captured. Add another or tap Done."
                                    }
                                }
                                override fun onError(ex: ImageCaptureException) {
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        isCapturing = false
                                        statusMessage = "Capture failed — try again"
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (pages.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    text = "Done — Build PDF (${pages.size} page${if (pages.size > 1) "s" else ""})",
                    onClick = {
                        val pdfBytes = pagesToPdf(pages)
                        onPdfReady(pdfBytes)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Draws a semi-transparent A4-ratio viewfinder overlay. */
@Composable
private fun A4Overlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width * 0.88f
                val h = w / A4_RATIO
                val left = (size.width - w) / 2f
                val top = (size.height - h) / 2f

                drawRect(Color.Black.copy(alpha = 0.5f), size = size)
                drawRect(
                    Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )
                drawRect(
                    Color.White,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Corner marks
                val mark = 24.dp.toPx()
                val stroke = Stroke(width = 3.dp.toPx())
                listOf(
                    Offset(left, top) to listOf(Offset(left + mark, top), Offset(left, top + mark)),
                    Offset(left + w, top) to listOf(Offset(left + w - mark, top), Offset(left + w, top + mark)),
                    Offset(left, top + h) to listOf(Offset(left + mark, top + h), Offset(left, top + h - mark)),
                    Offset(left + w, top + h) to listOf(Offset(left + w - mark, top + h), Offset(left + w, top + h - mark)),
                ).forEach { (corner, ends) ->
                    ends.forEach { end ->
                        drawLine(Color(0xFFBA7EF4), start = corner, end = end, strokeWidth = stroke.width)
                    }
                }
            }
    )
}

/** Crops a captured bitmap to A4 ratio (centre crop). */
private fun cropToA4(source: Bitmap): Bitmap {
    val srcW = source.width.toFloat()
    val srcH = source.height.toFloat()
    val targetH = srcW / A4_RATIO
    return if (targetH <= srcH) {
        val top = ((srcH - targetH) / 2).toInt()
        Bitmap.createBitmap(source, 0, top, srcW.toInt(), targetH.toInt())
    } else {
        val targetW = srcH * A4_RATIO
        val left = ((srcW - targetW) / 2).toInt()
        Bitmap.createBitmap(source, left, 0, targetW.toInt(), srcH.toInt())
    }
}

/** Renders a list of bitmaps into a single multi-page PDF and returns the bytes. */
private fun pagesToPdf(pages: List<Bitmap>): ByteArray {
    val pdf = PdfDocument()
    pages.forEachIndexed { idx, bmp ->
        // Scale to A4 at 150 DPI: 1240 × 1754 px
        val pageW = 1240
        val pageH = 1754
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
        val page = pdf.startPage(info)
        val canvas = page.canvas
        val scaled = Bitmap.createScaledBitmap(bmp, pageW, pageH, true)
        canvas.drawBitmap(scaled, 0f, 0f, null)
        pdf.finishPage(page)
        if (scaled !== bmp) scaled.recycle()
    }
    val out = ByteArrayOutputStream()
    pdf.writeTo(out)
    pdf.close()
    return out.toByteArray()
}

// ---------------------------------------------------------------------------
// PHOTO mode — single photo capture
// ---------------------------------------------------------------------------

@Composable
private fun PhotoCaptureMode(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var isCapturing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Frame photo and tap Capture") }

    LaunchedEffect(cameraProviderFuture) {
        val provider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        } catch (e: Exception) { Log.e("PhotoCapture", "Bind failed", e) }
    }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            try { if (cameraProviderFuture.isDone) cameraProviderFuture.get().unbindAll() } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.fillMaxSize().drawBehind {
            val boxW = 340.dp.toPx(); val boxH = 340.dp.toPx()
            val left = (size.width - boxW) / 2; val top = (size.height - boxH) / 2
            drawRect(Color.Black.copy(alpha = 0.6f), size = size)
            drawRect(Color.Transparent, topLeft = Offset(left, top), size = Size(boxW, boxH),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
            drawRoundRect(Color.White, topLeft = Offset(left, top), size = Size(boxW, boxH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f), style = Stroke(2.dp.toPx()))
        })

        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SITE PHOTO CAPTURE", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
            Spacer(Modifier.height(4.dp))
            Text(statusMessage, style = FieldTheme.typography.body, color = Color.White, textAlign = TextAlign.Center)
        }

        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("Close", onDismiss, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            PrimaryButton(
                text = if (isCapturing) "Saving…" else "Capture Photo",
                enabled = !isCapturing,
                onClick = {
                    isCapturing = true
                    statusMessage = "Capturing image…"
                    val file = File(context.cacheDir, "visitation_${System.currentTimeMillis()}.jpg")
                    imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                (context as? android.app.Activity)?.runOnUiThread { onPhotoCaptured(file.absolutePath) }
                            }
                            override fun onError(ex: ImageCaptureException) {
                                isCapturing = false; statusMessage = "Capture failed"
                            }
                        })
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// OCR live mode
// ---------------------------------------------------------------------------

@Composable
private fun OcrLiveMode(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onTextScanned: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val previewView = remember { PreviewView(context) }

    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Align text inside box and tap Scan") }

    val transition = rememberInfiniteTransition(label = "scanLine")
    val scanProgress by transition.animateFloat(0f, 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "lineProgress"
    )

    LaunchedEffect(cameraProviderFuture) {
        val provider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        } catch (e: Exception) { Log.e("OcrLive", "Bind failed", e) }
    }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            try { if (cameraProviderFuture.isDone) cameraProviderFuture.get().unbindAll() } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.fillMaxSize().drawBehind {
            val boxW = 300.dp.toPx(); val boxH = 200.dp.toPx()
            val left = (size.width - boxW) / 2; val top = (size.height - boxH) / 2
            drawRect(Color.Black.copy(alpha = 0.6f), size = size)
            drawRect(Color.Transparent, topLeft = Offset(left, top), size = Size(boxW, boxH),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
            drawRoundRect(Color.White, topLeft = Offset(left, top), size = Size(boxW, boxH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f), style = Stroke(2.dp.toPx()))
            val lineY = top + (boxH * scanProgress)
            drawLine(Color(0xFFBA7EF4), Offset(left, lineY), Offset(left + boxW, lineY), 3.dp.toPx())
        })

        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ON-DEVICE OCR SCANNER", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
            Spacer(Modifier.height(4.dp))
            Text(statusMessage, style = FieldTheme.typography.body, color = Color.White, textAlign = TextAlign.Center)
        }

        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("Close", onDismiss, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            PrimaryButton(
                text = if (isScanning) "Reading…" else "Scan & Extract",
                enabled = !isScanning,
                onClick = {
                    isScanning = true
                    statusMessage = "Analyzing image frames…"
                    val provider = cameraProviderFuture.get()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(cameraExecutor) { proxy ->
                        processImageProxy(proxy, textRecognizer,
                            onSuccess = { text ->
                                isScanning = false
                                statusMessage = "OCR Successful!"
                                imageAnalysis.clearAnalyzer()
                                onTextScanned(text)
                            },
                            onFailure = { err ->
                                isScanning = false
                                statusMessage = "OCR Failed: ${err.localizedMessage}"
                                imageAnalysis.clearAnalyzer()
                            }
                        )
                    }
                    try {
                        provider.unbindAll()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                    } catch (e: Exception) {
                        isScanning = false; statusMessage = "Camera binding failed"
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Permission denied view
// ---------------------------------------------------------------------------

@Composable
private fun PermissionDeniedContent(onRequest: () -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera Permission Required", style = FieldTheme.typography.title, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Camera access is required to scan documents and capture site photos.",
                style = FieldTheme.typography.body, color = FieldTheme.colors.gray400, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Grant Permission", onRequest)
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Cancel", onDismiss)
        }
    }
}

// ---------------------------------------------------------------------------
// ML Kit image proxy helper
// ---------------------------------------------------------------------------

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(image)
            .addOnSuccessListener { onSuccess(it.text) }
            .addOnFailureListener { onFailure(it) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
