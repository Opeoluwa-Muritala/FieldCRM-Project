package com.fieldcrm.android.core.session

import com.fieldcrm.android.ui.viewmodel.Screen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAccessPolicyTest {
    private val role = UserRole.ACCOUNT_OFFICER

    @Test
    fun relationshipOfficerCanAccessPersonalWorkspaces() {
        assertTrue(RoleAccessPolicy.canAccess(role, Screen.MyQueue))
        assertTrue(RoleAccessPolicy.canAccess(role, Screen.VisitsDue))
        assertTrue(RoleAccessPolicy.canAccess(role, Screen.CreateApplication))
        assertTrue(RoleAccessPolicy.canAccess(role, Screen.DocumentUpload))
        assertTrue(RoleAccessPolicy.canAccess(role, Screen.OcrReview))
    }

    @Test
    fun relationshipOfficerCannotAccessReviewOrAdminWorkspaces() {
        assertFalse(RoleAccessPolicy.canAccess(role, Screen.AwaitingConcurrence))
        assertFalse(RoleAccessPolicy.canAccess(role, Screen.CreditReviewQueue))
        assertFalse(RoleAccessPolicy.canAccess(role, Screen.CrmQueue))
        assertFalse(RoleAccessPolicy.canAccess(role, Screen.ComplianceFlags))
        assertFalse(RoleAccessPolicy.canAccess(role, Screen.EdQueue))
        assertFalse(RoleAccessPolicy.canAccess(role, Screen.Users))
        assertFalse(RoleAccessPolicy.canAccess(role, Screen.Pipeline))
    }

    @Test
    fun teamLeadMatchesWebWorkspaceAccess() {
        val teamLead = UserRole.BRANCH_MANAGER
        assertTrue(RoleAccessPolicy.canAccess(teamLead, Screen.AwaitingConcurrence))
        assertTrue(RoleAccessPolicy.canAccess(teamLead, Screen.PendingSignoffs))
        assertTrue(RoleAccessPolicy.canAccess(teamLead, Screen.Pipeline))
        assertTrue(RoleAccessPolicy.canAccess(teamLead, Screen.BorrowerList))
        assertFalse(RoleAccessPolicy.canAccess(teamLead, Screen.CreditReviewQueue))
        assertFalse(RoleAccessPolicy.canAccess(teamLead, Screen.OcrExceptions))
        assertFalse(RoleAccessPolicy.canAccess(teamLead, Screen.CreateApplication))
    }

    @Test
    fun supervisorMatchesWebWorkspaceAccess() {
        val supervisor = UserRole.BRANCH_SUPERVISOR
        assertTrue(RoleAccessPolicy.canAccess(supervisor, Screen.CreditReviewQueue))
        assertTrue(RoleAccessPolicy.canAccess(supervisor, Screen.BorrowerList))
        assertTrue(RoleAccessPolicy.canAccess(supervisor, Screen.BranchManagerReview))
        assertFalse(RoleAccessPolicy.canAccess(supervisor, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(supervisor, Screen.PendingSignoffs))
        assertFalse(RoleAccessPolicy.canAccess(supervisor, Screen.CreateApplication))
        assertFalse(RoleAccessPolicy.canAccess(supervisor, Screen.CreditOfficerReview))
    }

    @Test
    fun creditAnalystMatchesWebWorkspaceAccess() {
        val analyst = UserRole.CREDIT_ANALYST
        assertTrue(RoleAccessPolicy.canAccess(analyst, Screen.CreditReviewQueue))
        assertTrue(RoleAccessPolicy.canAccess(analyst, Screen.OcrExceptions))
        assertTrue(RoleAccessPolicy.canAccess(analyst, Screen.BorrowerList))
        assertTrue(RoleAccessPolicy.canAccess(analyst, Screen.CreditOfficerReview))
        assertFalse(RoleAccessPolicy.canAccess(analyst, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(analyst, Screen.OcrReview))
        assertFalse(RoleAccessPolicy.canAccess(analyst, Screen.CreateApplication))
        assertFalse(RoleAccessPolicy.canAccess(analyst, Screen.BranchManagerReview))
    }

    @Test
    fun crmOfficerMatchesWebWorkspaceAccess() {
        val crm = UserRole.CRM
        assertTrue(RoleAccessPolicy.canAccess(crm, Screen.CrmQueue))
        assertTrue(RoleAccessPolicy.canAccess(crm, Screen.BorrowerList))
        assertTrue(RoleAccessPolicy.canAccess(crm, Screen.ParDashboard))
        assertTrue(RoleAccessPolicy.canAccess(crm, Screen.CrmReview))
        assertTrue(RoleAccessPolicy.canAccess(crm, Screen.DocumentUpload))
        assertFalse(RoleAccessPolicy.canAccess(crm, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(crm, Screen.CreditReviewQueue))
        assertFalse(RoleAccessPolicy.canAccess(crm, Screen.CreateApplication))
    }

    @Test
    fun headCrmMatchesWebWorkspaceAccess() {
        val headCrm = UserRole.HEAD_CRM
        assertTrue(RoleAccessPolicy.canAccess(headCrm, Screen.CrmQueue))
        assertTrue(RoleAccessPolicy.canAccess(headCrm, Screen.BorrowerList))
        assertTrue(RoleAccessPolicy.canAccess(headCrm, Screen.ParDashboard))
        assertTrue(RoleAccessPolicy.canAccess(headCrm, Screen.CrmReview))
        assertTrue(RoleAccessPolicy.canAccess(headCrm, Screen.DocumentUpload))
        assertFalse(RoleAccessPolicy.canAccess(headCrm, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(headCrm, Screen.ExecutiveApproval))
        assertFalse(RoleAccessPolicy.canAccess(headCrm, Screen.CreateApplication))
    }

    @Test
    fun auditorMatchesReadOnlyWebWorkspace() {
        val auditor = UserRole.AUDITOR
        assertTrue(RoleAccessPolicy.canAccess(auditor, Screen.ComplianceFlags))
        assertTrue(RoleAccessPolicy.canAccess(auditor, Screen.AuditTrail))
        assertTrue(RoleAccessPolicy.canAccess(auditor, Screen.BorrowerList))
        assertTrue(RoleAccessPolicy.canAccess(auditor, Screen.DocumentViewer))
        assertFalse(RoleAccessPolicy.canAccess(auditor, Screen.AuditorCompliance))
        assertFalse(RoleAccessPolicy.canAccess(auditor, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(auditor, Screen.CreateApplication))
    }

    @Test
    fun executiveDirectorMatchesWebWorkspaceAccess() {
        val ed = UserRole.ED
        assertTrue(RoleAccessPolicy.canAccess(ed, Screen.EdQueue))
        assertTrue(RoleAccessPolicy.canAccess(ed, Screen.ParDashboard))
        assertTrue(RoleAccessPolicy.canAccess(ed, Screen.BorrowerList))
        assertTrue(RoleAccessPolicy.canAccess(ed, Screen.EdApproval))
        assertFalse(RoleAccessPolicy.canAccess(ed, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(ed, Screen.MdApproval))
        assertFalse(RoleAccessPolicy.canAccess(ed, Screen.CreateApplication))
    }

    @Test
    fun managingDirectorMatchesWebWorkspaceAccess() {
        val md = UserRole.MD
        assertTrue(RoleAccessPolicy.canAccess(md, Screen.MdQueue))
        assertTrue(RoleAccessPolicy.canAccess(md, Screen.ParDashboard))
        assertTrue(RoleAccessPolicy.canAccess(md, Screen.BorrowerList))
        assertTrue(RoleAccessPolicy.canAccess(md, Screen.MdApproval))
        assertFalse(RoleAccessPolicy.canAccess(md, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(md, Screen.EdApproval))
        assertFalse(RoleAccessPolicy.canAccess(md, Screen.CreateApplication))
    }

    @Test
    fun legalMatchesWebWorkspaceAccess() {
        val legal = UserRole.LEGAL
        assertTrue(RoleAccessPolicy.canAccess(legal, Screen.LegalWorkspace))
        assertTrue(RoleAccessPolicy.canAccess(legal, Screen.ValuationEditor))
        assertTrue(RoleAccessPolicy.canAccess(legal, Screen.DocumentViewer))
        assertFalse(RoleAccessPolicy.canAccess(legal, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(legal, Screen.BorrowerList))
        assertFalse(RoleAccessPolicy.canAccess(legal, Screen.CreateApplication))
    }

    @Test
    fun systemAdminMatchesWebWorkspaceAccess() {
        val admin = UserRole.SYSTEM_ADMIN
        assertTrue(RoleAccessPolicy.canAccess(admin, Screen.Users))
        assertTrue(RoleAccessPolicy.canAccess(admin, Screen.SystemActivity))
        assertFalse(RoleAccessPolicy.canAccess(admin, Screen.Pipeline))
        assertFalse(RoleAccessPolicy.canAccess(admin, Screen.BorrowerList))
        assertFalse(RoleAccessPolicy.canAccess(admin, Screen.InterestPresets))
        assertFalse(RoleAccessPolicy.canAccess(admin, Screen.BranchManagement))
    }

    @Test
    fun retiredExecutiveCompatibilityRoleHasNoOperationalWorkspace() {
        val executive = UserRole.EXECUTIVE
        assertTrue(RoleAccessPolicy.canAccess(executive, Screen.Dashboard))
        assertFalse(RoleAccessPolicy.canAccess(executive, Screen.ExecutiveQueue))
        assertFalse(RoleAccessPolicy.canAccess(executive, Screen.ExecutiveApproval))
        assertFalse(RoleAccessPolicy.canAccess(executive, Screen.ParDashboard))
        assertFalse(RoleAccessPolicy.canAccess(executive, Screen.Pipeline))
    }
}
