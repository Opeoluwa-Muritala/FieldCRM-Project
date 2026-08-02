package com.fieldcrm.android.ui.roles.teamlead.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.queue.AwaitingConcurrenceScreen
import com.fieldcrm.android.ui.screens.queue.PendingSignoffsScreen
import com.fieldcrm.android.ui.screens.queue.PipelineScreen
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun TeamLeadConcurrenceQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: () -> Unit, onViewApplication: (String) -> Unit) = AwaitingConcurrenceScreen(applications, borrowers, onBackClick, onViewApplication)
@Composable
fun TeamLeadSignoffQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: () -> Unit, onViewReport: (String) -> Unit) = PendingSignoffsScreen(applications, borrowers, onBackClick, onViewReport)
@Composable
fun TeamLeadPipeline(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: () -> Unit, onViewApplication: (String) -> Unit) = PipelineScreen(applications, borrowers, onBackClick, onViewApplication)
