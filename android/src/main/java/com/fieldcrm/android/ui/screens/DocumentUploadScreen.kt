package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun DocumentUploadScreen(
    onBackClick: () -> Unit,
    onComplete: () -> Unit
) {
    var documentName by remember { mutableStateOf("NIN_Card_Adaeze_Okonkwo.jpg") }
    var extractedName by remember { mutableStateOf("Adaeze Okonkwo") }
    var extractedBvn by remember { mutableStateOf("222333444") }
    var ocrConfidence by remember { mutableFloatStateOf(0.88f) }
    var selectedTab by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isWide = maxWidth >= 840.dp
        
        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Document OCR Pipeline",
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
            if (isWide) {
                // Wide Screen: Split Pane
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Left Pane: Document Preview Box
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(FieldTheme.colors.gray900)
                            .borderRight(0.5.dp, FieldTheme.colors.gray700)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ORIGINAL IMAGE SCAN",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 300.dp, height = 400.dp)
                                    .background(FieldTheme.colors.gray800, RoundedCornerShape(8.dp))
                                    .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "[ Scan Image Preview: $documentName ]",
                                    style = FieldTheme.typography.body,
                                    color = FieldTheme.colors.gray400
                                )
                            }
                        }
                    }
                    
                    // Right Pane: OCR Extraction correction fields
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        OcrDetailsSection(
                            extractedName = extractedName,
                            onNameChange = { extractedName = it },
                            extractedBvn = extractedBvn,
                            onBvnChange = { extractedBvn = it },
                            ocrConfidence = ocrConfidence,
                            onVerifyClick = onComplete
                        )
                    }
                }
            } else {
                // Compact Screen: Tabbed View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = FieldTheme.colors.gray900,
                        contentColor = FieldTheme.colors.purple400
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Scan View") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("OCR Fields") }
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        if (selectedTab == 0) {
                            Text(
                                text = "UPLOADED ATTACHMENT",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            FieldUploadDropzone(
                                title = "Upload Identity Card",
                                subtitle = "Drop NIN or BVN slip here",
                                onClick = {}
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            DocumentThumbnail(
                                fileName = documentName,
                                fileSize = "1.2 MB",
                                fileType = "jpg"
                            )
                        } else {
                            OcrDetailsSection(
                                extractedName = extractedName,
                                onNameChange = { extractedName = it },
                                extractedBvn = extractedBvn,
                                onBvnChange = { extractedBvn = it },
                                ocrConfidence = ocrConfidence,
                                onVerifyClick = onComplete
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OcrDetailsSection(
    extractedName: String,
    onNameChange: (String) -> Unit,
    extractedBvn: String,
    onBvnChange: (String) -> Unit,
    ocrConfidence: Float,
    onVerifyClick: () -> Unit
) {
    FieldCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OCR Text Extraction",
                style = FieldTheme.typography.title,
                color = FieldTheme.colors.gray100
            )
            SourceTag(source = "ocr")
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "SCANNER CONFIDENCE LEVEL",
            style = FieldTheme.typography.label,
            color = FieldTheme.colors.gray500
        )
        Spacer(modifier = Modifier.height(4.dp))
        ConfidenceBar(percentage = ocrConfidence)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FieldTextField(
            value = extractedName,
            onValueChange = onNameChange,
            label = "Extracted Name Check",
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FieldTextField(
            value = extractedBvn,
            onValueChange = onBvnChange,
            label = "Extracted BVN Reference",
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PrimaryButton(
            text = "Verify & Correct Parameters",
            onClick = onVerifyClick
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone Document Upload", widthDp = 411, heightDp = 850)
@Composable
fun PreviewDocUploadCompact() {
    FieldCRMTheme {
        DocumentUploadScreen(onBackClick = {}, onComplete = {})
    }
}

@Preview(name = "Tablet Document Upload Layout", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewDocUploadTablet() {
    FieldCRMTheme {
        DocumentUploadScreen(onBackClick = {}, onComplete = {})
    }
}
