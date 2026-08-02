package com.fieldcrm.android.ui.roles.managingdirector.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun ManagingDirectorDashboard(userName:String,metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val d=metrics?.data;val m=d?.metrics;val records=d?.md_queue.orEmpty().workspaceLines();WorkspaceDashboardPage("Managing Director","Make final decisions using executive recommendations and MCC evidence.",userName,"Managing Director",listOf(WorkspaceMetric("Awaiting final decision",m?.md_queue?.toString()?:"Not available"),WorkspaceMetric("Decisions signed",metrics?.decisions_signed?.toString()?:"Not available")),listOf(WorkspaceAction("Managing Director Decisions",Screen.MdQueue,records),WorkspaceAction("MCC",Screen.MccWorkspace,records),WorkspaceAction("Decision Search",Screen.SearchResults,records)),loading,error,onOpen,onNavigateToSettings)}
