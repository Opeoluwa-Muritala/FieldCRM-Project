package com.fieldcrm.android.ui.roles.headcrm.review

import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.screens.review.CrmReviewScreen
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun HeadCrmOversightReview(application: LoanApplicationModel, isSubmitting: Boolean, savedChecklist: Map<String, Boolean>, onAdvanceToExecutive: (String, Boolean, Boolean, Boolean, Boolean) -> Unit, onReturnToBranchManager: (String) -> Unit, onUploadDocument: () -> Unit, onBack: () -> Unit) =
    CrmReviewScreen(application, UserRole.HEAD_CRM, isSubmitting, savedChecklist, onAdvanceToExecutive, onReturnToBranchManager, onUploadDocument, onBack)
