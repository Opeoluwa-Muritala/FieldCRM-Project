package com.fieldcrm.android.ui.roles.creditanalyst.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun CreditAnalystDashboard(metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onSignOut:()->Unit){val m=metrics?.data?.metrics;WorkspaceDashboardPage("Credit Analyst","Assess affordability, bureau evidence, OCR exceptions, and credit risk.",listOf(WorkspaceMetric("Assessments due",m?.reviews_due?.toString()?:"Not available"),WorkspaceMetric("OCR exceptions",m?.ocr_exceptions?.toString()?:"Not available"),WorkspaceMetric("Reviewed today",m?.reviewed_today?.toString()?:"Not available")),listOf(WorkspaceAction("Credit Assessments",Screen.CreditReviewQueue),WorkspaceAction("OCR Exceptions",Screen.OcrExceptions),WorkspaceAction("Search Credit Dossiers",Screen.SearchResults)),loading,error,onOpen,onSignOut)}
