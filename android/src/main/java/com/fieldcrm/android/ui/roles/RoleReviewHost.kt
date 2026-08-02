package com.fieldcrm.android.ui.roles

import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.roles.crm.review.CrmDossierReview
import com.fieldcrm.android.ui.roles.headcrm.review.HeadCrmOversightReview
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun RoleCrmReviewHost(
    application: LoanApplicationModel,
    role: UserRole,
    isSubmitting: Boolean,
    savedChecklist: Map<String, Boolean>,
    onAdvanceToExecutive: (String, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onReturnToBranchManager: (String) -> Unit,
    onUploadDocument: () -> Unit,
    onBack: () -> Unit,
) {
    if (role == UserRole.HEAD_CRM) {
        HeadCrmOversightReview(application, isSubmitting, savedChecklist, onAdvanceToExecutive, onReturnToBranchManager, onUploadDocument, onBack)
    } else {
        CrmDossierReview(application, isSubmitting, savedChecklist, onAdvanceToExecutive, onReturnToBranchManager, onUploadDocument, onBack)
    }
}
