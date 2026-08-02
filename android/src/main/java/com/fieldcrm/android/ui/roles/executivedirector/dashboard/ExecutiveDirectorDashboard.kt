package com.fieldcrm.android.ui.roles.executivedirector.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun ExecutiveDirectorDashboard(userName:String,metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val d=metrics?.data;val m=d?.metrics;val records=d?.ed_queue.orEmpty().workspaceLines();WorkspaceDashboardPage("Executive Director","Review executive-ready dossiers, recommendations, and MCC evidence.",userName,"Executive Director",listOf(WorkspaceMetric("Awaiting decision",m?.ed_queue?.toString()?:"Not available"),WorkspaceMetric("PAR-30",m?.par30_pct?.let{"$it%"}?:"Not available")),listOf(WorkspaceAction("Executive Director Decisions",Screen.EdQueue,records),WorkspaceAction("MCC",Screen.MccWorkspace,records),WorkspaceAction("Decision Search",Screen.SearchResults,records)),loading,error,onOpen,onNavigateToSettings)}
