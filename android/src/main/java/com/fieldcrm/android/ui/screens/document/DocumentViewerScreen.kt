package com.fieldcrm.android.ui.screens.document

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun DocumentViewerScreen(
    docType: String = "",
    docUrl: String = "",
    onBackClick: () -> Unit
) {
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var rotationAngle by remember { mutableIntStateOf(0) }
    var annotationText by remember { mutableStateOf("") }
    var isAuditApproved by remember { mutableStateOf(false) }

    val displayTitle = if (docType.isNotBlank()) "Document: $docType" else "Document Viewer"

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = displayTitle,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWide = maxWidth >= 840.dp
            
            if (isWide) {
                // Wide Screen Split View: File scan Left, Annotations Right
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ControlHeader(
                            zoomLevel = zoomLevel,
                            onZoomChange = { zoomLevel = it },
                            rotationAngle = rotationAngle,
                            onRotateClick = { rotationAngle = (rotationAngle + 90) % 360 }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ViewerCanvasBox(rotationAngle = rotationAngle, zoomLevel = zoomLevel, docUrl = docUrl)
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(FieldTheme.colors.gray900)
                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(0.dp))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AnnotationSection(
                            annotationText = annotationText,
                            onAnnotationChange = { annotationText = it },
                            isAuditApproved = isAuditApproved,
                            onToggleAudit = { isAuditApproved = !isAuditApproved }
                        )
                    }
                }
            } else {
                // Compact Screen: Scrolling Single Pane
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ControlHeader(
                        zoomLevel = zoomLevel,
                        onZoomChange = { zoomLevel = it },
                        rotationAngle = rotationAngle,
                        onRotateClick = { rotationAngle = (rotationAngle + 90) % 360 }
                    )
                    ViewerCanvasBox(rotationAngle = rotationAngle, zoomLevel = zoomLevel, docUrl = docUrl)
                    AnnotationSection(
                        annotationText = annotationText,
                        onAnnotationChange = { annotationText = it },
                        isAuditApproved = isAuditApproved,
                        onToggleAudit = { isAuditApproved = !isAuditApproved }
                    )
                }
            }
        }
    }
}

@Composable
fun ControlHeader(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    rotationAngle: Int,
    onRotateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text("Zoom: ${(zoomLevel * 100).toInt()}%", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
            Spacer(modifier = Modifier.width(12.dp))
            Slider(
                value = zoomLevel,
                onValueChange = onZoomChange,
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    activeTrackColor = FieldTheme.colors.purple600,
                    inactiveTrackColor = FieldTheme.colors.gray700,
                    thumbColor = FieldTheme.colors.purple400
                ),
                modifier = Modifier.width(100.dp)
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRotateClick) {
                Icon(
                    imageVector = FieldIcons.RefreshOutlined,
                    contentDescription = "Rotate",
                    tint = FieldTheme.colors.gray400
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rotate: $rotationAngle°", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
        }
    }
}

// Helper extension to map Slider value change on older APIs
@Composable
fun RowScope.Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: SliderColors,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = colors,
        modifier = modifier
    )
}

@Composable
fun ViewerCanvasBox(rotationAngle: Int, zoomLevel: Float, docUrl: String = "") {
    val bitmapState = remember(docUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val isLoading = remember(docUrl) { mutableStateOf(false) }

    LaunchedEffect(docUrl) {
        if (docUrl.isNotBlank() && (docUrl.startsWith("http://") || docUrl.startsWith("https://"))) {
            isLoading.value = true
            kotlinx.coroutines.Dispatchers.IO.let { ioDispatcher ->
                kotlinx.coroutines.withContext(ioDispatcher) {
                    try {
                        val url = java.net.URL(docUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.doInput = true
                        connection.connect()
                        val input = connection.inputStream
                        val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                        bitmapState.value = bitmap
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading.value = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .background(FieldTheme.colors.gray850, RoundedCornerShape(10.dp))
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading.value) {
            CircularProgressIndicator(
                color = FieldTheme.colors.brandPrimary,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        } else if (bitmapState.value != null) {
            Image(
                bitmap = bitmapState.value!!.asImageBitmap(),
                contentDescription = "Document Scan Image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .graphicsLayer(
                        scaleX = zoomLevel,
                        scaleY = zoomLevel,
                        rotationZ = rotationAngle.toFloat()
                    )
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = FieldIcons.SearchOutlined,
                    contentDescription = "PDF Scan Preview",
                    tint = FieldTheme.colors.gray500,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "[ In-App PDF / Image Document Viewer Canvas ]",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray300
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (docUrl.isNotBlank()) {
                    Text(
                        text = docUrl,
                        style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                        color = FieldTheme.colors.purple400
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = "Applied Transformation: scaleX=$zoomLevel, scaleY=$zoomLevel, rotation=$rotationAngle°",
                    style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                    color = FieldTheme.colors.gray500
                )
            }
        }
    }
}

@Composable
fun AnnotationSection(
    annotationText: String,
    onAnnotationChange: (String) -> Unit,
    isAuditApproved: Boolean,
    onToggleAudit: () -> Unit
) {
    FieldCard {
        Text("AUDIT ANNOTATION LOG", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
        Spacer(modifier = Modifier.height(16.dp))
        
        FieldTextField(
            value = annotationText,
            onValueChange = onAnnotationChange,
            label = "Annotation text",
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (isAuditApproved) FieldTheme.colors.statusSuccess.copy(alpha = 0.1f) else FieldTheme.colors.statusWarning.copy(alpha = 0.1f),
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        0.5.dp,
                        if (isAuditApproved) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusWarning,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleAudit() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isAuditApproved) "APPROVED BY AUDIT (CLICK TO CHANGE)" else "UNDER REVIEW BY AUDIT",
                    style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                    color = if (isAuditApproved) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusWarning
                )
            }
        }
    }
}

@Preview(name = "Compact Phone Viewer", widthDp = 411, heightDp = 850)
@Composable
fun PreviewViewerCompact() {
    FieldCRMTheme {
        DocumentViewerScreen(onBackClick = {})
    }
}

@Preview(name = "Tablet Viewer Layout", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewViewerTablet() {
    FieldCRMTheme {
        DocumentViewerScreen(onBackClick = {})
    }
}
