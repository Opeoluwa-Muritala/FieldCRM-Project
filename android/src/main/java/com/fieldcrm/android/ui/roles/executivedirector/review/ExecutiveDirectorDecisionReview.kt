package com.fieldcrm.android.ui.roles.executivedirector.review

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.review.EdApprovalScreen
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun ExecutiveDirectorDecisionReview(application: LoanApplicationModel, isSubmitting: Boolean, onApprove: () -> Unit, onForwardToMd: () -> Unit, onBack: () -> Unit) =
    EdApprovalScreen(application, isSubmitting, onApprove, onForwardToMd, onBack)
