package com.fieldcrm.android.ui.roles.managingdirector.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.queue.MdQueueScreen
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun ManagingDirectorDecisionQueue(applications: List<LoanApplicationModel>, onBackClick: () -> Unit, onReviewApplication: (String) -> Unit) = MdQueueScreen(applications, onBackClick, onReviewApplication)
