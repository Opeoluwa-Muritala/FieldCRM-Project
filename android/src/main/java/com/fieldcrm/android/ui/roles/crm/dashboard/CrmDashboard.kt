package com.fieldcrm.android.ui.roles.crm.dashboard

import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen

@Composable
fun CrmDashboard(userName: String, metrics: DashboardMetrics?, loading: Boolean, error: String?, onOpen: (Screen) -> Unit, onNavigateToSettings: () -> Unit) {
    val data = metrics?.data
    val m = data?.metrics
    val records = data?.crm_queue.orEmpty().workspaceLines()
    WorkspaceDashboardPage(
        "CRM Officer", "Review approval conditions and prepare eligible applications for disbursement.", userName, "CRM Officer",
        listOf(
            WorkspaceMetric("CRM review queue", m?.crm_queue?.toString() ?: "Not available"),
            WorkspaceMetric("Ready amount", m?.ready_amount?.let { "₦%,.0f".format(it) } ?: "Not available"),
            WorkspaceMetric("PAR-30", m?.par30_pct?.let { "$it%" } ?: "Not available")
        ),
        listOf(
            WorkspaceAction("CRM Review and Disbursement", Screen.CrmQueue, records),
            WorkspaceAction("Portfolio at Risk", Screen.ParDashboard, workspaceSummary("Ready amount" to (m?.ready_amount?.let { "₦%,.0f".format(it) } ?: "Not available"), "PAR-30" to (m?.par30_pct?.let { "$it%" } ?: "Not available"))),
            WorkspaceAction("Search CRM Records", Screen.SearchResults, records)
        ), loading, error, onOpen, onNavigateToSettings
    )
}
