package com.fieldcrm.android.ui.screens.document

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.shared.model.BorrowerModel
import java.util.Locale

@Composable
fun DocumentUploadScreen(
    borrower: BorrowerModel?,
    onBackClick: () -> Unit,
    onComplete: (BorrowerModel) -> Unit
) {
    var documentName by remember { mutableStateOf("NIN_Card_Adaeze_Okonkwo.jpg") }
    var extractedName by remember { mutableStateOf(borrower?.name ?: "") }
    var extractedBvn by remember { mutableStateOf(borrower?.bvn ?: "") }
    var ocrConfidence by remember { mutableFloatStateOf(0.88f) }
    var selectedTab by remember { mutableStateOf(0) }
    var showCameraScanner by remember { mutableStateOf(false) }

    if (showCameraScanner) {
        CameraOcrScanner(
            mode = "OCR",
            onTextScanned = { text ->
                val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    extractedName = lines.first()
                }
                val bvnRegex = "\\b\\d{9,11}\\b".toRegex()
                val match = bvnRegex.find(text)
                if (match != null) {
                    extractedBvn = match.value
                }
                ocrConfidence = 0.98f
                documentName = "Real_Camera_Scan_ID.jpg"
                showCameraScanner = false
            },
            onDismiss = { showCameraScanner = false }
        )
    } else {
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
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = FieldTheme.colors.gray400
                                )
                            }
                        }
                    )
                },
                containerColor = FieldTheme.colors.gray950
            ) { paddingValues ->
                val onVerifyAndConfirm = {
                    val fallbackBorrower = borrower ?: BorrowerModel(
                        id = "temp_1", org_id = "org_1", loan_officer_id = "LO_1",
                        name = "", phone = "", bvn = "", nin = "", status = "Active", created_at = ""
                    )
                    val updatedBorrower = fallbackBorrower.copy(
                        name = extractedName,
                        bvn = extractedBvn
                    )
                    onComplete(updatedBorrower)
                }

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
                                .borderRight(0.5.dp, FieldTheme.colors.gray700.copy(alpha = 0.3f))
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
                                        .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
                                        .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.CameraOutlined,
                                            contentDescription = "Camera",
                                            tint = FieldTheme.colors.purple400,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "[ Scan Preview: $documentName ]",
                                            style = FieldTheme.typography.body,
                                            color = FieldTheme.colors.gray400
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        PrimaryButton(
                                            text = "Scan ID Document",
                                            onClick = { showCameraScanner = true }
                                        )
                                    }
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
                                onVerifyClick = onVerifyAndConfirm
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
                                text = { 
                                    Text(
                                        text = "Scan View",
                                        color = if (selectedTab == 0) FieldTheme.colors.purple400 else FieldTheme.colors.gray400,
                                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 14.sp)
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { 
                                    Text(
                                        text = "OCR Fields",
                                        color = if (selectedTab == 1) FieldTheme.colors.purple400 else FieldTheme.colors.gray400,
                                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 14.sp)
                                    )
                                }
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
                                    text = "IDENTITY CARD ATTACHMENT",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                FieldUploadDropzone(
                                    title = "Scan ID Slip",
                                    subtitle = "Tap to open OCR scanner camera",
                                    onClick = { showCameraScanner = true }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                DocumentThumbnail(
                                    fileName = documentName,
                                    fileSize = if (documentName.startsWith("Real")) "2.1 MB" else "1.2 MB",
                                    fileType = "jpg"
                                )
                            } else {
                                OcrDetailsSection(
                                    extractedName = extractedName,
                                    onNameChange = { extractedName = it },
                                    extractedBvn = extractedBvn,
                                    onBvnChange = { extractedBvn = it },
                                    ocrConfidence = ocrConfidence,
                                    onVerifyClick = onVerifyAndConfirm
                                )
                            }
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
        Spacer(modifier = Modifier.height(8.dp))
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

@Preview(name = "Compact Phone Document Upload", widthDp = 411, heightDp = 850)
@Composable
fun PreviewDocUploadCompact() {
    val demoBorrower = BorrowerModel(
        id = "1", org_id = "org_1", loan_officer_id = "LO_1",
        name = "Adaeze Okonkwo", phone = "08012345678", bvn = "222333444", nin = "111222333",
        status = "Active", created_at = ""
    )
    FieldCRMTheme {
        DocumentUploadScreen(borrower = demoBorrower, onBackClick = {}, onComplete = {})
    }
}
