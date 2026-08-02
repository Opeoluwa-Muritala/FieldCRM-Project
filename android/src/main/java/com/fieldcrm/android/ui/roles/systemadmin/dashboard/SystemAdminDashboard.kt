package com.fieldcrm.android.ui.roles.systemadmin.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun SystemAdminDashboard(userName: String, metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("System Admin","Manage users and inspect system activity. Lending data is not available in this workspace.",userName,"System Admin",listOf(WorkspaceMetric("Active users",m?.active_users?.toString()?:"Not available"),WorkspaceMetric("System events",m?.system_events?.toString()?:"Not available"),WorkspaceMetric("Failed jobs",m?.failed_jobs?.toString()?:"Not available"),WorkspaceMetric("Configuration alerts",m?.config_alerts?.toString()?:"Not available")),listOf(WorkspaceAction("Users",Screen.Users),WorkspaceAction("System Activity",Screen.SystemActivity)),loading,error,onOpen,onNavigateToSettings)}
