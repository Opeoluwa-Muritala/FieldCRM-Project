package com.fieldcrm.android.ui.roles.teamlead.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun TeamLeadDashboard(metrics: DashboardMetrics?, loading:Boolean,error:String?,onOpen:(Screen)->Unit,onSignOut:()->Unit){ val m=metrics?.data?.metrics; WorkspaceDashboardPage("Team Lead","Review completeness, visitation evidence, and concurrence.",listOf(WorkspaceMetric("Awaiting concurrence",m?.awaiting_concurrence?.toString()?:"Not available"),WorkspaceMetric("Pending sign-offs",m?.pending_signoffs?.toString()?:"Not available"),WorkspaceMetric("Returned this week",m?.returned_this_week?.toString()?:"Not available")),listOf(WorkspaceAction("Awaiting Concurrence",Screen.AwaitingConcurrence),WorkspaceAction("Pending Sign-offs",Screen.PendingSignoffs),WorkspaceAction("Search Team Applications",Screen.SearchResults)),loading,error,onOpen,onSignOut)}
