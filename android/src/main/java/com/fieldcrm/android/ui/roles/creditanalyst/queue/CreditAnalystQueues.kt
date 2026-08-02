package com.fieldcrm.android.ui.roles.creditanalyst.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.screens.queue.CreditReviewQueueScreen
import com.fieldcrm.android.ui.screens.queue.OcrExceptionsScreen
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun CreditAssessmentQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBack: () -> Unit, onOpenAssessment: (String) -> Unit) = CreditReviewQueueScreen(applications, borrowers, UserRole.CREDIT_ANALYST, onBack, onOpenAssessment)
@Composable
fun CreditOcrExceptionQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: () -> Unit, onResolveException: (String) -> Unit) = OcrExceptionsScreen(applications, borrowers, onBackClick, onResolveException)
