package com.fieldcrm.android.ui.roles.headcrm.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun HeadCrmDashboard(userName: String, metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("Head CRM","Oversee CRM review quality, conditions, and disbursement readiness.",userName,"Head CRM",listOf(WorkspaceMetric("Oversight queue",m?.crm_queue?.toString()?:"Not available"),WorkspaceMetric("Unverified documents",m?.unverified_documents?.toString()?:"Not available"),WorkspaceMetric("Workflow exceptions",m?.workflow_exceptions?.toString()?:"Not available")),listOf(WorkspaceAction("Head CRM Oversight",Screen.CrmQueue),WorkspaceAction("Portfolio at Risk",Screen.ParDashboard),WorkspaceAction("Search CRM Records",Screen.SearchResults)),loading,error,onOpen,onNavigateToSettings)}
