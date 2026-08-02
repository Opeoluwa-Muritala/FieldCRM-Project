package com.fieldcrm.android.ui.roles.supervisor.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun SupervisorDashboard(userName: String, metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val data=metrics?.data;val m=data?.metrics;WorkspaceDashboardPage("Supervisor","Perform supervisory review of team-approved applications.",userName,"Supervisor",listOf(WorkspaceMetric("Supervisory reviews",m?.supervisory_reviews?.toString()?:"Not available"),WorkspaceMetric("Approved today",m?.approved_today?.toString()?:"Not available")),listOf(WorkspaceAction("Supervisory Review",Screen.CreditReviewQueue,data?.queue.orEmpty().workspaceLines()),WorkspaceAction("Search Supervised Applications",Screen.SearchResults,data?.queue.orEmpty().workspaceLines())),loading,error,onOpen,onNavigateToSettings)}
