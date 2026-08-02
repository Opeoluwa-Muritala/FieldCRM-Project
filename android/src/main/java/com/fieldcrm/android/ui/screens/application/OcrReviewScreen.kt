package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun OcrReviewScreen(
    application: LoanApplicationModel,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onVerified: () -> Unit
) {
    val uiState by applicationViewModel.uiState.collectAsState()
    val corrections = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(application.id) {
        applicationViewModel.loadOcrFields(application.id)
    }

    LaunchedEffect(uiState.ocrFields) {
        uiState.ocrFields.forEach { field ->
            if (!corrections.containsKey(field.field_name)) {
                corrections[field.field_name] = field.final_value ?: field.ocr_value ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "OCR Verification",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FieldCard {
                Text(
                    "Review auto-extracted fields below. Correct any errors before sending the application to Team Lead Review.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.ocrFields.isEmpty()) {
                    Text(
                        "No OCR fields loaded or processing...",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray500,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    uiState.ocrFields.forEachIndexed { index, field ->
                        val label = field.field_name.replace("_", " ").replaceFirstChar { it.uppercase() }
                        val currentValue = corrections[field.field_name] ?: field.final_value ?: field.ocr_value ?: ""

                        FieldTextField(
                            value = currentValue,
                            onValueChange = { corrections[field.field_name] = it },
                            label = label
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val conf = field.confidence ?: 0f
                        Column {
                            LinearProgressIndicator(
                                progress = { conf },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = when {
                                    conf >= 0.8f -> FieldTheme.colors.statusSuccess
                                    conf >= 0.5f -> FieldTheme.colors.statusWarning
                                    else -> FieldTheme.colors.statusDanger
                                },
                                trackColor = FieldTheme.colors.gray800
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Confidence: ${(conf * 100).toInt()}%",
                                    style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                    color = when {
                                        conf >= 0.8f -> FieldTheme.colors.statusSuccess
                                        conf >= 0.5f -> FieldTheme.colors.statusWarning
                                        else -> FieldTheme.colors.statusDanger
                                    }
                                )
                                Text(
                                    text = if (field.verified) "Verified" else "Unverified",
                                    style = FieldTheme.typography.body.copy(fontSize = 10.sp),
                                    color = if (field.verified) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusWarning
                                )
                            }
                        }

                        if (index < uiState.ocrFields.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(FieldTheme.colors.gray800)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            PrimaryButton(
                text = "Verify & Send to Team Lead",
                enabled = !uiState.isLoading && uiState.ocrFields.isNotEmpty(),
                onClick = {
                    applicationViewModel.submitOcrReview(
                        id = application.id,
                        corrections = corrections.toMap(),
                        onSuccess = onVerified
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryButton(
                text = "Back",
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
