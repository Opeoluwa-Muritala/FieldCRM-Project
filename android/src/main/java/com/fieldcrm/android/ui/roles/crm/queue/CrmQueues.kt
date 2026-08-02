package com.fieldcrm.android.ui.roles.crm.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.queue.CrmQueueScreen
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun CrmReviewQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: () -> Unit, onReviewApplication: (String) -> Unit) = CrmQueueScreen(applications, borrowers, onBackClick, onReviewApplication)
