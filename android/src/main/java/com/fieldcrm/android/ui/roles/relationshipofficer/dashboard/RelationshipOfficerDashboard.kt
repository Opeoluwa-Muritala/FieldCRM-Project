package com.fieldcrm.android.ui.roles.relationshipofficer.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun RelationshipOfficerDashboard(userName: String, metrics: DashboardMetrics?, loading: Boolean, error: String?, onOpen: (Screen)->Unit, onNavigateToSettings:()->Unit) {
    val data=metrics?.data
    val m=data?.metrics
    WorkspaceDashboardPage("Relationship Officer", "Prepare applications and follow up outstanding customer requirements.", userName, "Relationship Officer", listOf(
        WorkspaceMetric("My applications", m?.my_applications?.toString() ?: "Not available"), WorkspaceMetric("Draft applications", m?.drafts?.toString() ?: "Not available"), WorkspaceMetric("Visits due", m?.visits_due?.toString() ?: "Not available"), WorkspaceMetric("Returned for correction", m?.returned?.toString() ?: "Not available")
    ), listOf(WorkspaceAction("My Application Work", Screen.MyQueue, data?.tasks.orEmpty().taskWorkspaceLines()), WorkspaceAction("Visits Due", Screen.VisitsDue, data?.visits_due.orEmpty().visitWorkspaceLines()), WorkspaceAction("Find Customers and Applications", Screen.SearchResults, data?.tasks.orEmpty().taskWorkspaceLines())), loading,error,onOpen,onNavigateToSettings)
}
