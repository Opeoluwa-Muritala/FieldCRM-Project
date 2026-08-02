package com.fieldcrm.android.ui.roles.executive.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.queue.ExecutiveQueueScreen
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun ExecutiveDisbursementQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: (() -> Unit)? = null, onReviewApplication: (String) -> Unit) = ExecutiveQueueScreen(applications, borrowers, onBackClick, onReviewApplication)
