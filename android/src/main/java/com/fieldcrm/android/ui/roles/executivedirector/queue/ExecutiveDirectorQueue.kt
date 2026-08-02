package com.fieldcrm.android.ui.roles.executivedirector.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.queue.EdQueueScreen
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun ExecutiveDirectorDecisionQueue(applications: List<LoanApplicationModel>, onBackClick: () -> Unit, onReviewApplication: (String) -> Unit) = EdQueueScreen(applications, onBackClick, onReviewApplication)
