package com.fieldcrm.android.ui.roles.creditanalyst.review

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.review.CreditOfficerReviewScreen
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun CreditAssessmentReview(application: LoanApplicationModel, borrower: BorrowerModel?, applicationViewModel: ApplicationViewModel, onBackClick: () -> Unit, onCompleteReview: () -> Unit) =
    CreditOfficerReviewScreen(application, borrower, applicationViewModel, onBackClick, onCompleteReview)
