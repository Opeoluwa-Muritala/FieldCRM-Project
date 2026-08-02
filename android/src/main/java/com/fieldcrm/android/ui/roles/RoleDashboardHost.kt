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
    metrics: DashboardMetrics?,
    isLoading: Boolean,
    error: String?,
    onOpen: (Screen) -> Unit,
    onSignOut: () -> Unit
) {
    when (role) {
        UserRole.ACCOUNT_OFFICER, UserRole.LOAN_OFFICER -> RelationshipOfficerDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.BRANCH_MANAGER -> TeamLeadDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.BRANCH_SUPERVISOR -> SupervisorDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.CREDIT_ANALYST -> CreditAnalystDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.CRM -> CrmDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.HEAD_CRM -> HeadCrmDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.AUDITOR -> AuditDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.ED -> ExecutiveDirectorDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.MD -> ManagingDirectorDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.LEGAL -> LegalDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.SYSTEM_ADMIN -> SystemAdminDashboard(metrics,isLoading,error,onOpen,onSignOut)
        UserRole.EXECUTIVE -> ExecutiveDashboard(metrics,isLoading,error,onOpen,onSignOut)
    }
}
