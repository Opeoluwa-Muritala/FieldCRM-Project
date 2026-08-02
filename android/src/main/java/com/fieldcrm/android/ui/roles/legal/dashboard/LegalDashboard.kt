package com.fieldcrm.android.ui.roles.legal.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun LegalDashboard(metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onSignOut:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("Legal","Review collateral ownership, legal evidence, and valuations.",listOf(WorkspaceMetric("Legal review queue",m?.legal_queue?.toString()?:"Not available")),listOf(WorkspaceAction("Legal Review",Screen.LegalWorkspace),WorkspaceAction("Search Legal Dossiers",Screen.SearchResults)),loading,error,onOpen,onSignOut)}
