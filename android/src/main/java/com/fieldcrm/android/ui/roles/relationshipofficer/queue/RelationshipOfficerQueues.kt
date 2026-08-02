package com.fieldcrm.android.ui.roles.relationshipofficer.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.queue.MyQueueScreen
import com.fieldcrm.android.ui.screens.queue.VisitsDueScreen
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun RelationshipOfficerApplicationQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: (() -> Unit)? = null, onViewApplication: (String) -> Unit) =
    MyQueueScreen(applications, borrowers, onBackClick, onViewApplication)

@Composable
fun RelationshipOfficerVisitsQueue(applications: List<LoanApplicationModel>, borrowers: List<BorrowerModel>, onBackClick: (() -> Unit)? = null, onStartVisit: (String) -> Unit) =
    VisitsDueScreen(applications, borrowers, onBackClick, onStartVisit)
