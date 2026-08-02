package com.fieldcrm.android.ui.roles.supervisor.review

import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.screens.review.BranchManagerReviewScreen
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun SupervisorReview(application: LoanApplicationModel, borrower: BorrowerModel?, applicationViewModel: ApplicationViewModel, onBackClick: () -> Unit, onDecisionSubmitted: () -> Unit) =
    BranchManagerReviewScreen(application, borrower, UserRole.BRANCH_SUPERVISOR, applicationViewModel, onBackClick, onDecisionSubmitted)
