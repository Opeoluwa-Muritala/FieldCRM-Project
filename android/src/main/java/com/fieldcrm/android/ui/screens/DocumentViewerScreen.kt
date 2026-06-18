package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onBackClick: () -> Unit
) {
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var rotationAngle by remember { mutableIntStateOf(0) }
    var annotationText by remember { mutableStateOf("Verified match with original land title records at state registry.") }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Document: Deed_of_Pledge_Adaeze.pdf",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        ViewerCanvasBox(rotationAngle = rotationAngle, zoomLevel = zoomLevel)
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
                        AnnotationSection(annotationText = annotationText, onAnnotationChange = { annotationText = it })
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
                    ViewerCanvasBox(rotationAngle = rotationAngle, zoomLevel = zoomLevel)
                    AnnotationSection(annotationText = annotationText, onAnnotationChange = { annotationText = it })
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
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rotate",
                    tint = FieldTheme.colors.gray400
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rotate: $rotationAngle°", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
        }
    }
}

@Composable
fun ViewerCanvasBox(rotationAngle: Int, zoomLevel: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .background(FieldTheme.colors.gray850, RoundedCornerShape(10.dp))
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Search,
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
            Text(
                text = "Applied Transformation: scaleX=$zoomLevel, scaleY=$zoomLevel, rotation=$rotationAngle°",
                style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                color = FieldTheme.colors.purple400
            )
        }
    }
}

@Composable
fun AnnotationSection(
    annotationText: String,
    onAnnotationChange: (String) -> Unit
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
                    .background(FieldTheme.colors.purple950, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("APPROVED BY AUDIT", style = FieldTheme.typography.label, color = FieldTheme.colors.purple200)
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone Viewer", widthDp = 411, heightDp = 850)
@Composable
fun PreviewViewerCompact() {
    FieldCRMTheme {
        DocumentViewerScreen(onBackClick = {})
    }
}
