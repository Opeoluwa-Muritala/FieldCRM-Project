package com.fieldcrm.android.ui.roles.audit.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun AuditDashboard(userName: String, metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onNavigateToSettings:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("Audit","Inspect evidence, compliance exceptions, and immutable workflow history.",userName,"Auditor",listOf(WorkspaceMetric("Unverified documents",m?.unverified_documents?.toString()?:"Not available"),WorkspaceMetric("Critical OCR gaps",m?.critical_ocr_gaps?.toString()?:"Not available"),WorkspaceMetric("Workflow exceptions",m?.workflow_exceptions?.toString()?:"Not available"),WorkspaceMetric("Audit events today",m?.audit_events_today?.toString()?:"Not available")),listOf(WorkspaceAction("Compliance Exceptions",Screen.ComplianceFlags),WorkspaceAction("Search Auditable Records",Screen.SearchResults)),loading,error,onOpen,onNavigateToSettings)}
