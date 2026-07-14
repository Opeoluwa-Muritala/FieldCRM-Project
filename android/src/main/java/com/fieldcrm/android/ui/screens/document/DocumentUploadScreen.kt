package com.fieldcrm.android.ui.screens.document

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.DocumentUploadViewModel
import com.fieldcrm.android.ui.viewmodel.UploadState
import com.fieldcrm.shared.model.BorrowerModel
import org.koin.androidx.compose.koinViewModel

private val DOCUMENT_CATEGORIES = listOf(
    "id" to "Identity Document (NIN / Intl. Passport / Driver's License)",
    "payslip" to "Pay Slip",
    "bank_statement" to "Bank Statement",
    "guarantor" to "Guarantor Document",
    "pledge" to "Deed of Pledge",
    "other" to "Other Supporting"
)

@Composable
fun DocumentUploadScreen(
    applicationId: String,
    borrower: BorrowerModel?,
    onBackClick: () -> Unit,
    onComplete: (BorrowerModel) -> Unit
) {
    val viewModel: DocumentUploadViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Reset on launch so stale state from a previous screen visit doesn't persist
    LaunchedEffect(applicationId) {
        viewModel.reset()
        viewModel.refreshOcrFields(applicationId)
    }

    // File picker — reads bytes + filename from the chosen URI
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { s -> s.readBytes() }
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "document"
            } ?: "document"
            if (bytes != null) viewModel.setFile(bytes, name)
        }
    }

    var showCameraScanner by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    if (showCameraScanner) {
        CameraOcrScanner(
            mode = "OCR",
            onTextScanned = { text ->
                val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                val bvnRegex = "\\b\\d{9,11}\\b".toRegex()
                val bvn = bvnRegex.find(text)?.value ?: uiState.extractedBvn
                viewModel.setOcrResult(
                    name = lines.firstOrNull() ?: uiState.extractedName,
                    bvn = bvn,
                    // On-device text parsing does not provide field confidence.
                    // Persisted server OCR values supply confidence after upload.
                    confidence = 0f,
                    fileName = "camera_scan_${System.currentTimeMillis()}.jpg"
                )
                showCameraScanner = false
            },
            onDismiss = { showCameraScanner = false }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isWide = maxWidth >= 840.dp

        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Document Upload${borrower?.name?.let { " — $it" } ?: ""}",
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

            val onVerifyAndConfirm: () -> Unit = {
                // Submit OCR corrections to API, then update borrower and navigate back
                viewModel.submitOcrReview(applicationId) {
                    val base = borrower ?: BorrowerModel(
                        id = "temp_${System.currentTimeMillis()}",
                        org_id = "", loan_officer_id = "",
                        name = "", phone = "", bvn = "", nin = "", status = "Active", created_at = ""
                    )
                    onComplete(base.copy(name = uiState.extractedName, bvn = uiState.extractedBvn))
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Left pane — source selection + preview
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(FieldTheme.colors.gray900)
                            .borderRight(0.5.dp, FieldTheme.colors.gray700.copy(alpha = 0.3f))
                            .padding(24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "DOCUMENT SOURCE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            SourceSelectionPanel(
                                fileName = uiState.fileName,
                                uploadState = uiState.uploadState,
                                onScanCamera = { showCameraScanner = true },
                                onPickFile = { filePickerLauncher.launch("*/*") },
                                onUpload = { viewModel.uploadDocument(applicationId) }
                            )
                        }
                    }

                    // Right pane — category + OCR fields
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CategorySelector(
                            selected = uiState.category,
                            onSelect = viewModel::setCategory
                        )
                        OcrFieldsCard(
                            extractedName = uiState.extractedName,
                            onNameChange = viewModel::setExtractedName,
                            extractedBvn = uiState.extractedBvn,
                            onBvnChange = viewModel::setExtractedBvn,
                            ocrConfidence = uiState.ocrConfidence,
                            extractedFields = uiState.extractedFields,
                            isOcrProcessing = uiState.isOcrProcessing,
                            isRefreshingOcr = uiState.isRefreshingOcr,
                            onRefreshOcr = { viewModel.refreshOcrFields(applicationId) },
                            submitState = uiState.ocrSubmitState,
                            onVerifyClick = onVerifyAndConfirm
                        )
                    }
                }
            } else {
                // Compact — tab layout
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
                        listOf("Upload", "OCR Fields").forEachIndexed { i, label ->
                            Tab(
                                selected = selectedTab == i,
                                onClick = { selectedTab = i },
                                text = {
                                    Text(
                                        text = label,
                                        color = if (selectedTab == i) FieldTheme.colors.purple400 else FieldTheme.colors.gray400,
                                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 14.sp)
                                    )
                                }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedTab == 0) {
                            Text(
                                text = "IDENTITY CARD ATTACHMENT",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            CategorySelector(
                                selected = uiState.category,
                                onSelect = viewModel::setCategory
                            )
                            SourceSelectionPanel(
                                fileName = uiState.fileName,
                                uploadState = uiState.uploadState,
                                onScanCamera = { showCameraScanner = true },
                                onPickFile = { filePickerLauncher.launch("*/*") },
                                onUpload = { viewModel.uploadDocument(applicationId) }
                            )
                        } else {
                            OcrFieldsCard(
                                extractedName = uiState.extractedName,
                                onNameChange = viewModel::setExtractedName,
                                extractedBvn = uiState.extractedBvn,
                                onBvnChange = viewModel::setExtractedBvn,
                                ocrConfidence = uiState.ocrConfidence,
                                extractedFields = uiState.extractedFields,
                                isOcrProcessing = uiState.isOcrProcessing,
                                isRefreshingOcr = uiState.isRefreshingOcr,
                                onRefreshOcr = { viewModel.refreshOcrFields(applicationId) },
                                submitState = uiState.ocrSubmitState,
                                onVerifyClick = onVerifyAndConfirm
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-composables ─────────────────────────────────────────

@Composable
private fun SourceSelectionPanel(
    fileName: String,
    uploadState: UploadState,
    onScanCamera: () -> Unit,
    onPickFile: () -> Unit,
    onUpload: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drop zone / file picker
        FieldUploadDropzone(
            title = if (fileName.isBlank()) "Choose from Device" else fileName,
            subtitle = if (fileName.isBlank()) "Tap to browse files — PDF, JPG, PNG up to 5 MB" else "Tap to replace",
            onClick = onPickFile
        )

        // Camera scan option
        SecondaryButton(
            text = "Scan with Camera (OCR)",
            onClick = onScanCamera
        )

        // Upload to server — only active once a file is selected
        when (uploadState) {
            is UploadState.Idle -> {
                if (fileName.isNotBlank()) {
                    PrimaryButton(text = "Upload to Server", onClick = onUpload)
                }
            }
            is UploadState.InFlight -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FieldTheme.colors.brandPrimary
                    )
                    Text(
                        text = "Uploading…",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400
                    )
                }
            }
            is UploadState.Done -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = FieldIcons.CheckOutlined,
                        contentDescription = "Uploaded",
                        tint = FieldTheme.colors.statusSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Uploaded successfully",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.statusSuccess
                    )
                }
            }
            is UploadState.Failed -> {
                Text(
                    text = uploadState.message,
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.statusDanger
                )
                SecondaryButton(text = "Retry Upload", onClick = onUpload)
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "DOCUMENT CATEGORY",
            style = FieldTheme.typography.label,
            color = FieldTheme.colors.gray500
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
        ) {
            DOCUMENT_CATEGORIES.forEachIndexed { index, (key, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(key) }
                        .background(
                            if (selected == key) FieldTheme.colors.brandPrimary.copy(alpha = 0.08f)
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = FieldTheme.typography.body.copy(
                            fontWeight = if (selected == key) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = if (selected == key) FieldTheme.colors.purple400 else FieldTheme.colors.gray300
                    )
                    if (selected == key) {
                        Icon(
                            imageVector = FieldIcons.CheckOutlined,
                            contentDescription = null,
                            tint = FieldTheme.colors.purple400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (index < DOCUMENT_CATEGORIES.lastIndex) {
                    FieldDivider()
                }
            }
        }
    }
}

@Composable
fun OcrFieldsCard(
    extractedName: String,
    onNameChange: (String) -> Unit,
    extractedBvn: String,
    onBvnChange: (String) -> Unit,
    ocrConfidence: Float,
    extractedFields: List<com.fieldcrm.android.data.api.OcrExtractedField> = emptyList(),
    isOcrProcessing: Boolean = false,
    isRefreshingOcr: Boolean = false,
    onRefreshOcr: () -> Unit = {},
    submitState: UploadState,
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
        Spacer(modifier = Modifier.height(12.dp))

        if (isOcrProcessing) {
            Text(
                text = "Extraction is still processing. You can enter values manually and refresh the results.",
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray400
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (extractedFields.isNotEmpty()) {
            Text(
                text = "Only fields marked for review need extra checking.",
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray400
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else if (ocrConfidence > 0f) {
            Text(
                text = "SCANNER CONFIDENCE LEVEL",
                style = FieldTheme.typography.label,
                color = FieldTheme.colors.gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConfidenceBar(percentage = ocrConfidence)
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            Text(
                text = "Scan a document with the camera to extract identity fields, or fill in manually.",
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray500
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        FieldTextField(
            value = extractedName,
            onValueChange = onNameChange,
            label = "Full Name (from document)",
            isRequired = true
        )
        OcrFieldConfidenceNotice(
            field = extractedFields.firstOrNull { it.field_name == "applicant_name" || it.field_name == "full_name" }
        )
        Spacer(modifier = Modifier.height(12.dp))
        FieldTextField(
            value = extractedBvn,
            onValueChange = onBvnChange,
            label = "BVN Reference",
            isRequired = true
        )
        OcrFieldConfidenceNotice(field = extractedFields.firstOrNull { it.field_name == "bvn" })
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(
            text = if (isRefreshingOcr) "Refreshing extraction…" else "Refresh extracted fields",
            onClick = onRefreshOcr,
            enabled = !isRefreshingOcr
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (submitState) {
            is UploadState.InFlight -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FieldTheme.colors.brandPrimary
                    )
                    Text("Submitting…", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                }
            }
            is UploadState.Done -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(FieldIcons.CheckOutlined, contentDescription = null, tint = FieldTheme.colors.statusSuccess, modifier = Modifier.size(18.dp))
                    Text("Verified & submitted", style = FieldTheme.typography.body, color = FieldTheme.colors.statusSuccess)
                }
            }
            is UploadState.Failed -> {
                Text(submitState.message, style = FieldTheme.typography.body, color = FieldTheme.colors.statusDanger)
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(text = "Retry Verification", onClick = onVerifyClick)
            }
            else -> {
                val canSubmit = extractedName.isNotBlank() || extractedBvn.isNotBlank()
                PrimaryButton(
                    text = "Verify & Confirm",
                    onClick = onVerifyClick,
                    enabled = canSubmit
                )
                if (!canSubmit) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scan a document or fill in fields above to continue.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray500
                    )
                }
            }
        }
    }
}

@Composable
private fun OcrFieldConfidenceNotice(field: com.fieldcrm.android.data.api.OcrExtractedField?) {
    val confidence = field?.confidence ?: return
    val needsReview = confidence < 80f || field.is_critical && !field.verified
    if (needsReview) {
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = FieldIcons.AlertOutlined,
                contentDescription = "Needs review",
                tint = FieldTheme.colors.statusWarning,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Check this value — low confidence (${confidence.toInt()}%)",
                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                color = FieldTheme.colors.statusWarning
            )
        }
    } else {
        Text(
            text = "Extracted with ${confidence.toInt()}% confidence",
            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
            color = FieldTheme.colors.gray500,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// ── Previews ─────────────────────────────────────────────────

@Preview(name = "Compact", widthDp = 411, heightDp = 850)
@Composable
fun PreviewDocUploadCompact() {
    val demoBorrower = BorrowerModel(
        id = "1", org_id = "org_1", loan_officer_id = "LO_1",
        name = "Ngozi Eze", phone = "08012345678", bvn = "", nin = "",
        status = "Active", created_at = ""
    )
    FieldCRMTheme {
        DocumentUploadScreen(
            applicationId = "app_1",
            borrower = demoBorrower,
            onBackClick = {},
            onComplete = {}
        )
    }
}
