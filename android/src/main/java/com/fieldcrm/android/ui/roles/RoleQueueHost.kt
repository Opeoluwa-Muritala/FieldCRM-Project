package com.fieldcrm.android.ui.roles

import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.roles.creditanalyst.queue.CreditAssessmentQueue
import com.fieldcrm.android.ui.roles.supervisor.queue.SupervisorReviewQueue
import com.fieldcrm.android.ui.roles.crm.queue.CrmReviewQueue
import com.fieldcrm.android.ui.roles.headcrm.queue.HeadCrmOversightQueue
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun RoleReviewQueueHost(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, role: UserRole?, onBackClick: () -> Unit, onReviewApplication: (String) -> Unit) {
    if (role == UserRole.CREDIT_ANALYST) {
        CreditAssessmentQueue(applications, borrowers, onBackClick, onReviewApplication)
    } else {
        SupervisorReviewQueue(applications, borrowers, onBackClick, onReviewApplication)
    }
}

@Composable
fun RoleCrmQueueHost(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, role: UserRole?, onBackClick: () -> Unit, onReviewApplication: (String) -> Unit) {
    if (role == UserRole.HEAD_CRM) HeadCrmOversightQueue(applications, borrowers, onBackClick, onReviewApplication)
    else CrmReviewQueue(applications, borrowers, onBackClick, onReviewApplication)
}
