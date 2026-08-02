package com.fieldcrm.android.ui.roles.managingdirector.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun ManagingDirectorDashboard(metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onSignOut:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("Managing Director","Make final decisions using executive recommendations and MCC evidence.",listOf(WorkspaceMetric("Awaiting final decision",m?.md_queue?.toString()?:"Not available"),WorkspaceMetric("Decisions signed",metrics?.decisions_signed?.toString()?:"Not available")),listOf(WorkspaceAction("Managing Director Decisions",Screen.MdQueue),WorkspaceAction("MCC",Screen.MccWorkspace),WorkspaceAction("Decision Search",Screen.SearchResults)),loading,error,onOpen,onSignOut)}
