package com.fieldcrm.android.ui.roles.legal.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun LegalDashboard(userName: String, metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("Legal","Review collateral ownership, legal evidence, and valuations.",userName,"Legal",listOf(WorkspaceMetric("Legal review queue",m?.legal_queue?.toString()?:"Not available")),listOf(WorkspaceAction("Legal Review",Screen.LegalWorkspace),WorkspaceAction("Search Legal Dossiers",Screen.SearchResults)),loading,error,onOpen,onNavigateToSettings)}
