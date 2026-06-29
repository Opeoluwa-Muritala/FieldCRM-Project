package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(
    documentTitle: String,
    role: UserRole?,
    onBackClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val showDelete = role != UserRole.AUDITOR // Auditor never sees delete

    if (isTablet) {
        // Tablet Layout: Inline card within existing detail view (never take over the full screen)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = FieldTheme.colors.gray900),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, FieldTheme.colors.gray800),
            shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = documentTitle,
                        style = FieldTheme.typography.title,
                        color = FieldTheme.colors.gray100
                    )
                    
                    // Tablet Actions Inline
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { /* Download */ },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Download", color = FieldTheme.colors.purple600, style = FieldTheme.typography.bodyStrong)
                        }
                        TextButton(
                            onClick = { /* Share */ },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Share", color = FieldTheme.colors.purple600, style = FieldTheme.typography.bodyStrong)
                        }
                        if (showDelete) {
                            TextButton(
                                onClick = { /* Delete */ },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Delete", color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.bodyStrong)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Document preview canvas matching the theme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
                        .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DOCUMENT PREVIEW\n[ $documentTitle ]",
                        style = FieldTheme.typography.bodyStrong,
                        color = FieldTheme.colors.gray300,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        // Phone Layout: Full-screen black background (neutral void)
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        var dragOffsetY by remember { mutableStateOf(0f) }
        
        var showMenu by remember { mutableStateOf(false) }

        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale *= zoomChange
            offset += offsetChange
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > 150f) {
                                onBackClick()
                            }
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragOffsetY = 0f
                        },
                        onDrag = { _, dragAmount ->
                            if (dragAmount.y > 0) {
                                dragOffsetY += dragAmount.y
                            }
                        }
                    )
                }
                .offset { IntOffset(0, dragOffsetY.roundToInt()) }
        ) {
            // Main document preview area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = state)
                    .graphicsLayer(
                        scaleX = scale.coerceIn(0.8f, 3f),
                        scaleY = scale.coerceIn(0.8f, 3f),
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp, 320.dp)
                        .background(Color.DarkGray, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DOCUMENT PREVIEW\n[ $documentTitle ]",
                        style = FieldTheme.typography.bodyStrong.copy(lineHeight = 20.sp),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Topbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = documentTitle,
                    style = FieldTheme.typography.title.copy(fontSize = 16.sp),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(FieldTheme.colors.gray900)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download", color = FieldTheme.colors.gray100, style = FieldTheme.typography.body) },
                            onClick = { showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", color = FieldTheme.colors.gray100, style = FieldTheme.typography.body) },
                            onClick = { showMenu = false }
                        )
                        if (showDelete) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.bodyStrong) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
            }

            // Page Dots & Zoom Reset at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (scale != 1f) {
                    TextButton(
                        onClick = {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Reset Zoom", style = FieldTheme.typography.bodyStrong)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(48.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color.Gray, CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color.Gray, CircleShape))
                }
            }
        }
    }
}
