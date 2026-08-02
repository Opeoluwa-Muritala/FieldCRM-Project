package com.fieldcrm.android.ui.roles.crm.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun CrmDashboard(userName: String, metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("CRM Officer","Review approval conditions and prepare eligible applications for disbursement.",userName,"CRM Officer",listOf(WorkspaceMetric("CRM review queue",m?.crm_queue?.toString()?:"Not available"),WorkspaceMetric("Ready amount",m?.ready_amount?.let{"₦%,.0f".format(it)}?:"Not available"),WorkspaceMetric("PAR-30",m?.par30_pct?.let{"$it%"}?:"Not available")),listOf(WorkspaceAction("CRM Review and Disbursement",Screen.CrmQueue),WorkspaceAction("Portfolio at Risk",Screen.ParDashboard),WorkspaceAction("Search CRM Records",Screen.SearchResults)),loading,error,onOpen,onNavigateToSettings)}
