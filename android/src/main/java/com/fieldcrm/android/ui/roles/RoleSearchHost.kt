package com.fieldcrm.android.ui.roles
import androidx.compose.runtime.Composable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.roles.audit.search.AuditSearch
import com.fieldcrm.android.ui.roles.creditanalyst.search.CreditAnalystSearch
import com.fieldcrm.android.ui.roles.crm.search.CrmSearch
import com.fieldcrm.android.ui.roles.executivedirector.search.ExecutiveDirectorSearch
import com.fieldcrm.android.ui.roles.headcrm.search.HeadCrmSearch
import com.fieldcrm.android.ui.roles.legal.search.LegalSearch
import com.fieldcrm.android.ui.roles.managingdirector.search.ManagingDirectorSearch
import com.fieldcrm.android.ui.roles.relationshipofficer.search.RelationshipOfficerSearch
import com.fieldcrm.android.ui.roles.supervisor.search.SupervisorSearch
import com.fieldcrm.android.ui.roles.teamlead.search.TeamLeadSearch
@Composable fun RoleSearchHost(role:UserRole,onBack:()->Unit,onOpen:(String)->Unit){when(role){
    UserRole.ACCOUNT_OFFICER,UserRole.LOAN_OFFICER->RelationshipOfficerSearch(onBack,onOpen)
    UserRole.BRANCH_MANAGER->TeamLeadSearch(onBack,onOpen)
    UserRole.BRANCH_SUPERVISOR->SupervisorSearch(onBack,onOpen)
    UserRole.CREDIT_ANALYST->CreditAnalystSearch(onBack,onOpen)
    UserRole.CRM->CrmSearch(onBack,onOpen)
    UserRole.HEAD_CRM->HeadCrmSearch(onBack,onOpen)
    UserRole.AUDITOR->AuditSearch(onBack,onOpen)
    UserRole.ED->ExecutiveDirectorSearch(onBack,onOpen)
    UserRole.MD->ManagingDirectorSearch(onBack,onOpen)
    UserRole.LEGAL->LegalSearch(onBack,onOpen)
    UserRole.SYSTEM_ADMIN,UserRole.EXECUTIVE->Unit
}}
