package com.fieldcrm.android.ui.roles.supervisor.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.screens.queue.CreditReviewQueueScreen
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun SupervisorReviewQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBack: () -> Unit, onOpenReview: (String) -> Unit) = CreditReviewQueueScreen(applications, borrowers, UserRole.BRANCH_SUPERVISOR, onBack, onOpenReview)
