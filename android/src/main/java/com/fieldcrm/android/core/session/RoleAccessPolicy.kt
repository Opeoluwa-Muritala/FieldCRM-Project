package com.fieldcrm.android.core.session

import com.fieldcrm.android.ui.viewmodel.Screen
import com.fieldcrm.android.ui.navigation.RouteGuard

/** Client-side visibility guard. The API remains the authorization boundary. */
object RoleAccessPolicy {
    private val common = setOf(
        Screen.Login, Screen.ForgotPassword, Screen.ResetPassword,
        Screen.PermissionsPrimer, Screen.Onboarding,
        Screen.PasscodeSetup, Screen.PasscodeLogin,
        Screen.Dashboard, Screen.Settings, Screen.Notifications, Screen.SearchResults,
        Screen.OfflineQueue, Screen.Confirmation
    )

    private val relationshipOfficer = common + setOf(
        Screen.MyQueue, Screen.VisitsDue, Screen.CreateApplication,
        Screen.ApplicationDetail, Screen.LoanApplicationForm, Screen.GuarantorsForm,
        Screen.PledgeTrust, Screen.VisitationReport, Screen.DocumentUpload,
        Screen.DocumentViewer, Screen.OcrReview
    )

    private val teamLead = common + setOf(
        Screen.AwaitingConcurrence, Screen.PendingSignoffs, Screen.Pipeline,
        Screen.BorrowerList, Screen.BorrowerDetail, Screen.ApplicationDetail,
        Screen.BranchManagerReview, Screen.VisitationReport, Screen.DocumentViewer,
        Screen.WorkflowEventAudit
    )

    private val supervisor = common + setOf(
        Screen.CreditReviewQueue, Screen.BorrowerList, Screen.BorrowerDetail,
        Screen.ApplicationDetail, Screen.BranchManagerReview,
        Screen.DocumentViewer, Screen.WorkflowEventAudit
    )

    private val creditAnalyst = common + setOf(
        Screen.CreditReviewQueue, Screen.OcrExceptions, Screen.BorrowerList,
        Screen.BorrowerDetail, Screen.ApplicationDetail, Screen.CreditOfficerReview,
        Screen.DocumentViewer, Screen.WorkflowEventAudit
    )

    private val crmOfficer = common + setOf(
        Screen.CrmQueue, Screen.BorrowerList, Screen.BorrowerDetail,
        Screen.ApplicationDetail, Screen.CrmReview, Screen.DocumentUpload,
        Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.ParDashboard
    )

    private val audit = common + setOf(
        Screen.ComplianceFlags, Screen.AuditTrail, Screen.BorrowerList,
        Screen.BorrowerDetail, Screen.ApplicationDetail, Screen.DocumentViewer,
        Screen.WorkflowEventAudit
    )

    private val executiveDirector = common + setOf(
        Screen.EdQueue, Screen.BorrowerList, Screen.BorrowerDetail,
        Screen.ApplicationDetail, Screen.EdApproval, Screen.ParDashboard,
        Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.MccWorkspace
    )

    private val managingDirector = common + setOf(
        Screen.MdQueue, Screen.BorrowerList, Screen.BorrowerDetail,
        Screen.ApplicationDetail, Screen.MdApproval, Screen.ParDashboard,
        Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.MccWorkspace
    )

    private val legal = common + setOf(
        Screen.LegalWorkspace, Screen.ValuationEditor, Screen.ApplicationDetail,
        Screen.DocumentViewer, Screen.WorkflowEventAudit
    )

    private val systemAdmin = common + setOf(Screen.Users, Screen.SystemActivity)

    fun canAccess(role: UserRole, screen: Screen): Boolean = RouteGuard.canOpen(role, screen)

    fun canCreateApplications(role: UserRole): Boolean =
        role == UserRole.ACCOUNT_OFFICER || role == UserRole.LOAN_OFFICER

}
