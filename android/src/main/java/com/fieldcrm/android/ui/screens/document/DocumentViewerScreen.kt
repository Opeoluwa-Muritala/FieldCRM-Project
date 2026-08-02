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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale
import com.fieldcrm.android.data.api.MobileApiService
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import com.fieldcrm.android.data.repository.ApplicationRepository

@Composable
fun DocumentViewerScreen(
    applicationId: String = "",
    docType: String = "",
    initialDocUrl: String = "",
    onBackClick: () -> Unit
) {
    val applicationRepository: ApplicationRepository = koinInject()
    val scope = rememberCoroutineScope()
    var currentDocUrl by remember(initialDocUrl) { mutableStateOf(initialDocUrl) }
    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun refreshDocUrl() {
        if (applicationId.isNotBlank() && docType.isNotBlank()) {
            isRefreshing = true
            try {
                val detail = applicationRepository.getFullDetail(applicationId)
                if (detail != null) {
                    val matchingDoc = detail.documents.find {
                        val type = (it["doc_type"] as? String ?: "").replace("_", " ").trim()
                        val target = docType.replace("_", " ").trim()
                        type.equals(target, ignoreCase = true)
                    }
                    val freshUrl = matchingDoc?.get("secure_url") as? String
                        ?: matchingDoc?.get("file_url") as? String
                    if (!freshUrl.isNullOrBlank()) {
                        currentDocUrl = freshUrl
                    }
                }
            } catch (_: Exception) {} finally {
                isRefreshing = false
            }
        }
    }

    var zoomLevel by rememberSaveable(applicationId, docType) { mutableFloatStateOf(1.0f) }
    var rotationAngle by rememberSaveable(applicationId, docType) { mutableIntStateOf(0) }

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
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        ControlHeader(
                            zoomLevel = zoomLevel,
                            onZoomChange = { zoomLevel = it },
                            rotationAngle = rotationAngle,
                            onRotateClick = { rotationAngle = (rotationAngle + 90) % 360 }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ViewerCanvasBox(
                            rotationAngle = rotationAngle,
                            zoomLevel = zoomLevel,
                            docUrl = currentDocUrl,
                            onRetryClick = {
                                scope.launch {
                                    refreshDocUrl()
                                }
                            }
                        )
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
                    ViewerCanvasBox(
                        rotationAngle = rotationAngle,
                        zoomLevel = zoomLevel,
                        docUrl = currentDocUrl,
                        onRetryClick = {
                            scope.launch {
                                refreshDocUrl()
                            }
                        }
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
fun ViewerCanvasBox(
    rotationAngle: Int,
    zoomLevel: Float,
    docUrl: String = "",
    onRetryClick: () -> Unit = {}
) {
    val apiService: MobileApiService = koinInject()
    val context = LocalContext.current
    val bitmapState = remember(docUrl) { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    val isLoading = remember(docUrl) { mutableStateOf(false) }
    val hasError = remember(docUrl) { mutableStateOf(false) }

    LaunchedEffect(docUrl) {
        if (docUrl.isNotBlank()) {
            isLoading.value = true
            hasError.value = false
            val pagesList = mutableListOf<android.graphics.Bitmap>()
            try {
                // Try fetching page 1 first
                val pageOneUrl = if (docUrl.contains("?")) "$docUrl&page=1" else "$docUrl?page=1"
                var previewBytes = apiService.fetchDocumentPreview(pageOneUrl)
                if (previewBytes == null) {
                    // Try the original URL without page param
                    previewBytes = apiService.fetchDocumentPreview(docUrl)
                }

                if (previewBytes != null) {
                    val singlePage = android.graphics.BitmapFactory.decodeByteArray(previewBytes, 0, previewBytes.size)
                    if (singlePage != null) {
                        pagesList.add(singlePage)
                        // If page 1 was returned as an image, try to fetch pages 2, 3, ... sequentially until failure
                        var pageIndex = 2
                        var hasMore = true
                        while (hasMore && pageIndex <= 50) { // Safety limit of 50 pages
                            val nextPageUrl = if (docUrl.contains("?")) "$docUrl&page=$pageIndex" else "$docUrl?page=$pageIndex"
                            val nextPageBytes = apiService.fetchDocumentPreview(nextPageUrl)
                            if (nextPageBytes != null) {
                                val nextPageBitmap = android.graphics.BitmapFactory.decodeByteArray(nextPageBytes, 0, nextPageBytes.size)
                                if (nextPageBitmap != null) {
                                    pagesList.add(nextPageBitmap)
                                    pageIndex++
                                } else {
                                    hasMore = false
                                }
                            } else {
                                hasMore = false
                            }
                        }
                    } else {
                        // Not a direct image, try loading it as a PDF locally
                        val localPages = renderPdfPages(context.cacheDir, previewBytes)
                        if (localPages.isNotEmpty()) {
                            pagesList.addAll(localPages)
                        } else {
                            hasError.value = true
                        }
                    }
                } else {
                    hasError.value = true
                }
                bitmapState.value = pagesList
            } catch (e: Exception) {
                hasError.value = true
            } finally {
                isLoading.value = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(FieldTheme.colors.gray850, RoundedCornerShape(10.dp))
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading.value) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingSkeleton(modifier = Modifier.fillMaxWidth().weight(1f), height = 400.dp, width = 280.dp, cornerRadius = 8.dp)
                Spacer(modifier = Modifier.height(12.dp))
                LoadingSkeleton(height = 14.dp, width = 176.dp)
            }
        } else if (hasError.value) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = FieldIcons.AlertOutlined,
                    contentDescription = "Error",
                    tint = FieldTheme.colors.statusDanger,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Failed to load document preview",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The preview token may have expired or a connection issue occurred.",
                    style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                    color = FieldTheme.colors.gray400,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.purple600)
                ) {
                    Text("Refresh Token & Retry", color = Color.White)
                }
            }
        } else if (bitmapState.value.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bitmapState.value.forEachIndexed { index, bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Document page ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = zoomLevel,
                                scaleY = zoomLevel,
                                rotationZ = rotationAngle.toFloat()
                            )
                    )
                }
            }
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
                    text = "No document URL available",
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

private fun renderPdfPages(cacheDir: File, bytes: ByteArray): List<android.graphics.Bitmap> = runCatching {
    val temp = File.createTempFile("fieldcrm-preview-", ".pdf", cacheDir)
    try {
        temp.writeBytes(bytes)
        ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                (0 until renderer.pageCount).map { pageIndex ->
                    renderer.openPage(pageIndex).use { page ->
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            page.width.coerceAtLeast(1), page.height.coerceAtLeast(1),
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }
    } finally {
        temp.delete()
    }
}.getOrDefault(emptyList())

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
