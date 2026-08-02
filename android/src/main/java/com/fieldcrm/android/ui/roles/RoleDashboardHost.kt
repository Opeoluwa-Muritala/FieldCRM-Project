package com.fieldcrm.android.ui.roles

import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.audit.dashboard.AuditDashboard
import com.fieldcrm.android.ui.roles.creditanalyst.dashboard.CreditAnalystDashboard
import com.fieldcrm.android.ui.roles.crm.dashboard.CrmDashboard
import com.fieldcrm.android.ui.roles.executive.dashboard.ExecutiveDashboard
import com.fieldcrm.android.ui.roles.executivedirector.dashboard.ExecutiveDirectorDashboard
import com.fieldcrm.android.ui.roles.headcrm.dashboard.HeadCrmDashboard
import com.fieldcrm.android.ui.roles.legal.dashboard.LegalDashboard
import com.fieldcrm.android.ui.roles.managingdirector.dashboard.ManagingDirectorDashboard
import com.fieldcrm.android.ui.roles.relationshipofficer.dashboard.RelationshipOfficerDashboard
import com.fieldcrm.android.ui.roles.supervisor.dashboard.SupervisorDashboard
import com.fieldcrm.android.ui.roles.systemadmin.dashboard.SystemAdminDashboard
import com.fieldcrm.android.ui.roles.teamlead.dashboard.TeamLeadDashboard
import com.fieldcrm.android.ui.viewmodel.Screen

@Composable
fun RoleDashboardHost(
    role: UserRole,
    userName: String,
    metrics: DashboardMetrics?,
    isLoading: Boolean,
    error: String?,
    onOpen: (Screen) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    when (role) {
        UserRole.ACCOUNT_OFFICER, UserRole.LOAN_OFFICER -> RelationshipOfficerDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.BRANCH_MANAGER -> TeamLeadDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.BRANCH_SUPERVISOR -> SupervisorDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.CREDIT_ANALYST -> CreditAnalystDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.CRM -> CrmDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.HEAD_CRM -> HeadCrmDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.AUDITOR -> AuditDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.ED -> ExecutiveDirectorDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.MD -> ManagingDirectorDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.LEGAL -> LegalDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.SYSTEM_ADMIN -> SystemAdminDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
        UserRole.EXECUTIVE -> ExecutiveDashboard(userName,metrics,isLoading,error,onOpen,onNavigateToSettings)
    }
}
