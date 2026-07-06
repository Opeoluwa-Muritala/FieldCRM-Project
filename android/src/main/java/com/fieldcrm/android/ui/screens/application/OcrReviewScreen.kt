package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "OCR Verification — Step 2 of 9",
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
                    "Review auto-extracted fields below. Correct any errors before advancing to Credit Review.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(16.dp))

                listOf(
                    "applicant_name" to application.applicant_name,
                    "amount" to (application.amount?.toInt()?.toString() ?: ""),
                    "tenor_months" to (application.tenor_months?.toString() ?: ""),
                    "loan_type" to application.loan_type
                ).forEach { (key, autoValue) ->
                    val label = key.replace("_", " ")
                        .replaceFirstChar { it.uppercase() }
                    FieldTextField(
                        value = corrections[key] ?: autoValue,
                        onValueChange = { corrections[key] = it },
                        label = label
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            PrimaryButton(
                text = "Verify & Advance to Credit Review",
                enabled = !uiState.isLoading,
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
