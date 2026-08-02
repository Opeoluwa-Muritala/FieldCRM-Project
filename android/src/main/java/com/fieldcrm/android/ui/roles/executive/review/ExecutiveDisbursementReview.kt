package com.fieldcrm.android.ui.roles.executive.review

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.review.ExecutiveApprovalScreen
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun ExecutiveDisbursementReview(application: LoanApplicationModel, documents: List<Map<String, Any>> = emptyList(), crmNotes: String = "", isSubmitting: Boolean, onIssueInstruction: () -> Unit, onBack: () -> Unit) =
    ExecutiveApprovalScreen(application, documents, crmNotes, isSubmitting, onIssueInstruction, onBack)
