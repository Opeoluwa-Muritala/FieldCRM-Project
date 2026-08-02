package com.fieldcrm.android.ui.roles.managingdirector.review

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.review.MdApprovalScreen
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun ManagingDirectorDecisionReview(application: LoanApplicationModel, isSubmitting: Boolean, onApprove: () -> Unit, onReturnToEd: (String) -> Unit, onAddBoardReferral: (String, String, String) -> Unit, onBack: () -> Unit) =
    MdApprovalScreen(application, isSubmitting, onApprove, onReturnToEd, onAddBoardReferral, onBack)
