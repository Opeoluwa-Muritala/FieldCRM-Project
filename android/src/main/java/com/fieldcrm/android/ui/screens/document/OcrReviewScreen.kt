package com.fieldcrm.android.ui.screens.document

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.LoanApplicationModel

private data class OcrField(
    val label: String,
    val initialValue: String,
    val confidence: Float
)

private val ocrFields = listOf(
    OcrField("Applicant Full Name", "Adaeze Okonkwo", 0.88f),
    OcrField("Loan Amount", "250000", 0.75f),
    OcrField("BVN", "22244455567", 0.42f),
    OcrField("Guarantor Signature", "Present", 0.61f)
)

@Composable
fun OcrReviewScreen(
    application: LoanApplicationModel,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
    onReturnForReupload: () -> Unit
) {
    val fieldValues = remember {
        mutableStateMapOf<String, String>().apply {
            ocrFields.forEach { put(it.label, it.initialValue) }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "OCR Review",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .background(
                                FieldTheme.colors.gray800,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .border(
                                0.5.dp,
                                FieldTheme.colors.gray700,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "REF: ${application.id.take(8).uppercase()}",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.gray400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Card 1: Document Image Preview
            item {
                FieldCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DOCUMENT IMAGE",
                        style = FieldTheme.typography.label.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FieldTheme.colors.purple400
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                FieldTheme.colors.gray800,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .border(
                                0.5.dp,
                                FieldTheme.colors.gray700,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = FieldIcons.InfoOutlined,
                                contentDescription = "Document Preview",
                                tint = FieldTheme.colors.gray600,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Document image preview",
                                style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                color = FieldTheme.colors.gray500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Card 2: OCR Extracted Fields
            item {
                FieldCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "OCR EXTRACTED FIELDS",
                        style = FieldTheme.typography.label.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FieldTheme.colors.purple400
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ocrFields.forEachIndexed { index, field ->
                        val currentValue = fieldValues[field.label] ?: field.initialValue

                        FieldTextField(
                            value = currentValue,
                            onValueChange = { fieldValues[field.label] = it },
                            label = field.label
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Confidence progress bar using LinearProgressIndicator
                        Column {
                            LinearProgressIndicator(
                                progress = { field.confidence },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = when {
                                    field.confidence >= 0.8f -> FieldTheme.colors.statusSuccess
                                    field.confidence >= 0.5f -> FieldTheme.colors.statusWarning
                                    else -> FieldTheme.colors.statusDanger
                                },
                                trackColor = FieldTheme.colors.gray700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Confidence: ${(field.confidence * 100).toInt()}%",
                                    style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                    color = when {
                                        field.confidence >= 0.8f -> FieldTheme.colors.statusSuccess
                                        field.confidence >= 0.5f -> FieldTheme.colors.statusWarning
                                        else -> FieldTheme.colors.statusDanger
                                    }
                                )
                                SourceTag(source = "OCR")
                            }
                        }

                        if (index < ocrFields.size - 1) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(FieldTheme.colors.gray700.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }

            // Bottom action buttons
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryButton(
                        text = "Mark as Verified",
                        onClick = onVerified
                    )
                    PrimaryButton(
                        text = "Save Corrections",
                        onClick = { /* corrections saved — wired separately */ }
                    )
                    SecondaryButton(
                        text = "Return for Re-upload",
                        onClick = onReturnForReupload
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Preview(name = "OCR Review Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewOcrReviewScreen() {
    val demoApp = LoanApplicationModel(
        id = "app_preview_001",
        org_id = "org_1",
        borrower_id = "borrower_1",
        current_stage = 2,
        current_owner_id = "LO_1",
        status = "OCR Review",
        amount = 250000.0,
        tenure = 6,
        product_type = "SME Loan",
        interest_rate = 15.0,
        repayment_frequency = "Monthly",
        created_at = "2026-07-01"
    )
    FieldCRMTheme {
        OcrReviewScreen(
            application = demoApp,
            onBackClick = {},
            onVerified = {},
            onReturnForReupload = {}
        )
    }
}
