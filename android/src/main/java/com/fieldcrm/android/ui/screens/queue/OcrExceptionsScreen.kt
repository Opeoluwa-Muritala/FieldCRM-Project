package com.fieldcrm.android.ui.screens.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

private data class OcrExceptionItem(
    val fieldName: String,
    val applicantName: String,
    val confidencePct: Int,
    val exceptionId: String
)

private val placeholderOcrExceptions = listOf(
    OcrExceptionItem("BVN", "Adaeze Okonkwo", 42, ""),
    OcrExceptionItem("Guarantor Signature", "Emeka Chukwu", 28, ""),
    OcrExceptionItem("Loan Amount", "Ngozi Adeyemi", 55, ""),
    OcrExceptionItem("Applicant Full Name", "Fatima Bello", 38, ""),
    OcrExceptionItem("NIN", "Chukwuemeka Eze", 47, "")
)

@Composable
fun OcrExceptionsScreen(
    applications: List<LoanApplicationModel> = emptyList(),
    borrowers: List<BorrowerModel> = emptyList(),
    onBackClick: () -> Unit,
    onResolveException: (String) -> Unit = {}
) {
    val exceptions = remember(applications, borrowers) {
        if (applications.isNotEmpty()) {
            applications.map { app ->
                val borrower = borrowers.find { it.id == app.id }
                OcrExceptionItem(
                    fieldName = "Document Review",
                    applicantName = borrower?.name ?: "Unknown Applicant",
                    confidencePct = 45,
                    exceptionId = app.id
                )
            }
        } else placeholderOcrExceptions
    }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "OCR Exceptions",
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
                                FieldTheme.colors.statusDanger.copy(alpha = 0.15f),
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .border(
                                0.5.dp,
                                FieldTheme.colors.statusDanger.copy(alpha = 0.4f),
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${exceptions.size} EXCEPTIONS",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.statusDanger
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(5) {
                        FieldCard(modifier = Modifier.fillMaxWidth().height(88.dp)) {
                            Column {
                                LoadingSkeleton(height = 16.dp, width = 140.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                LoadingSkeleton(height = 6.dp, width = 200.dp, cornerRadius = 3.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                LoadingSkeleton(height = 28.dp, width = 90.dp, cornerRadius = 6.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(exceptions) { exc ->
                        val confidenceFraction = exc.confidencePct / 100f
                        FieldCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exc.fieldName,
                                            style = FieldTheme.typography.bodyStrong,
                                            color = FieldTheme.colors.gray100
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = exc.applicantName,
                                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.gray400
                                        )
                                    }
                                    StatusChip(variant = StatusChipVariant.LowConfidence)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                ConfidenceBar(percentage = confidenceFraction)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            FieldTheme.colors.statusDanger.copy(alpha = 0.12f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            0.5.dp,
                                            FieldTheme.colors.statusDanger.copy(alpha = 0.4f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable(enabled = exc.exceptionId.isNotEmpty()) { onResolveException(exc.exceptionId) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "RESOLVE",
                                        style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                                        color = FieldTheme.colors.statusDanger
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "OCR Exceptions Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewOcrExceptionsScreen() {
    FieldCRMTheme {
        OcrExceptionsScreen(onBackClick = {})
    }
}
