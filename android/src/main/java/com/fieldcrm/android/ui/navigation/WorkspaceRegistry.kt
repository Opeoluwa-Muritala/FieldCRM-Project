package com.fieldcrm.android.ui.navigation

import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.viewmodel.Screen

object WorkspaceRegistry {
    private val sessionRoutes = setOf(
        Screen.Login, Screen.ForgotPassword, Screen.ResetPassword,
        Screen.PermissionsPrimer, Screen.Onboarding, Screen.Confirmation,
        Screen.Notifications, Screen.Dashboard
    )
    private val dossier = setOf(
        "identity", "application", "compliance", "documents", "guarantors",
        "collateral", "visitation", "workflow"
    )
    private fun workspace(
        role: UserRole, name: String, primary: List<WorkspaceDestination>,
        routes: Set<Screen>, queue: QueueDefinition? = null,
        search: SearchDefinition? = null, review: ReviewDefinition? = null,
        sections: Set<String> = dossier, mcc: Boolean = false
    ) = WorkspaceDefinition(
        role, name, primary, sessionRoutes + routes,
        queues = listOfNotNull(queue), search = search,
        reviews = listOfNotNull(review), dossierSections = sections, mccEnabled = mcc
    )

    private val relationship = workspace(
        UserRole.ACCOUNT_OFFICER, "Relationship Officer",
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("My Work", Screen.MyQueue), WorkspaceDestination("Search", Screen.SearchResults)),
        setOf(Screen.MyQueue, Screen.VisitsDue, Screen.CreateApplication, Screen.ApplicationDetail, Screen.LoanApplicationForm, Screen.GuarantorsForm, Screen.PledgeTrust, Screen.VisitationReport, Screen.DocumentUpload, Screen.DocumentViewer, Screen.OcrReview, Screen.SearchResults, Screen.OfflineQueue),
        QueueDefinition("My Application Work", "loan-officer", "You have no application work requiring attention.", Screen.ApplicationDetail),
        SearchDefinition("Find Customers and Applications", "Name, phone, BVN, NIN, or reference", setOf("borrower", "application")),
        ReviewDefinition("Prepare and Submit Application", Screen.LoanApplicationForm, setOf("intake", "returned"))
    )
    private val teamLead = workspace(
        UserRole.BRANCH_MANAGER, "Team Lead",
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("Concurrence", Screen.AwaitingConcurrence), WorkspaceDestination("Search", Screen.SearchResults)),
        setOf(Screen.AwaitingConcurrence, Screen.PendingSignoffs, Screen.Pipeline, Screen.ApplicationDetail, Screen.BranchManagerReview, Screen.VisitationReport, Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.SearchResults),
        QueueDefinition("Awaiting Concurrence", "awaiting-concurrence", "No applications are awaiting concurrence.", Screen.BranchManagerReview),
        SearchDefinition("Search Team Applications", "Customer, officer, or application reference", setOf("application", "relationship_officer")),
        ReviewDefinition("Completeness and Concurrence Review", Screen.BranchManagerReview, setOf("branch_manager_review"))
    )
    private val supervisor = workspace(
        UserRole.BRANCH_SUPERVISOR, "Supervisor",
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("Supervisory Review", Screen.CreditReviewQueue), WorkspaceDestination("Search", Screen.SearchResults)),
        setOf(Screen.CreditReviewQueue, Screen.ApplicationDetail, Screen.BranchManagerReview, Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.SearchResults),
        QueueDefinition("Supervisory Review", "branch-supervisor-review", "No applications require supervisory review.", Screen.BranchManagerReview),
        SearchDefinition("Search Supervised Applications", "Customer or application reference", setOf("application")),
        ReviewDefinition("Supervisor Review", Screen.BranchManagerReview, setOf("branch_supervisor_review"))
    )
    private val credit = workspace(
        UserRole.CREDIT_ANALYST, "Credit Analyst",
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("Credit Assessments", Screen.CreditReviewQueue), WorkspaceDestination("Exceptions", Screen.OcrExceptions), WorkspaceDestination("Search", Screen.SearchResults)),
        setOf(Screen.CreditReviewQueue, Screen.OcrExceptions, Screen.ApplicationDetail, Screen.CreditOfficerReview, Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.SearchResults),
        QueueDefinition("Credit Assessments", "credit-analyst-review", "No dossiers are awaiting credit assessment.", Screen.CreditOfficerReview),
        SearchDefinition("Search Credit Dossiers", "Application reference, customer, or exception", setOf("credit_dossier", "ocr_exception")),
        ReviewDefinition("Credit Assessment and Recommendation", Screen.CreditOfficerReview, setOf("credit_analyst_review")),
        dossier + setOf("credit_assessment", "recommendations")
    )
    private fun crm(role: UserRole, name: String) = workspace(
        role, name,
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("CRM Review", Screen.CrmQueue), WorkspaceDestination("Search", Screen.SearchResults)),
        setOf(Screen.CrmQueue, Screen.ApplicationDetail, Screen.CrmReview, Screen.DocumentUpload, Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.ParDashboard, Screen.SearchResults),
        QueueDefinition(if (role == UserRole.HEAD_CRM) "Head CRM Oversight" else "CRM Review and Disbursement", if (role == UserRole.HEAD_CRM) "head-crm-review" else "crm-review", "No dossiers are awaiting CRM action.", Screen.CrmReview),
        SearchDefinition("Search CRM Records", "Application, customer, or payment reference", setOf("crm_dossier", "disbursement")),
        ReviewDefinition(if (role == UserRole.HEAD_CRM) "Head CRM Oversight Review" else "CRM Review and Disbursement Preparation", Screen.CrmReview, setOf("crm_review", "head_crm_review", "disbursement_ready")),
        dossier + setOf("approval_conditions", "disbursement")
    )
    private val audit = workspace(
        UserRole.AUDITOR, "Audit",
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("Compliance Exceptions", Screen.ComplianceFlags), WorkspaceDestination("Audit Search", Screen.SearchResults)),
        setOf(Screen.ComplianceFlags, Screen.AuditTrail, Screen.ApplicationDetail, Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.SearchResults),
        QueueDefinition("Compliance Exceptions", "compliance-flags", "No compliance exceptions require inspection.", Screen.AuditTrail),
        SearchDefinition("Search Auditable Records", "Actor, action, resource, or reference", setOf("audit_event", "document", "application")),
        sections = dossier + setOf("audit_history")
    )
    private fun executive(role: UserRole, name: String, queueTitle: String, queueRoute: Screen, reviewRoute: Screen, endpoint: String, mcc: Boolean) = workspace(
        role, name,
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination(queueTitle, queueRoute), WorkspaceDestination("Decision Search", Screen.SearchResults)),
        setOf(queueRoute, reviewRoute, Screen.ApplicationDetail, Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.ParDashboard, Screen.SearchResults) + if (mcc) setOf(Screen.MccWorkspace) else emptySet(),
        QueueDefinition(queueTitle, endpoint, "No dossiers are awaiting your decision.", reviewRoute),
        SearchDefinition("Search Decision Dossiers", "Customer or application reference", setOf("decision_dossier", "historical_decision")),
        ReviewDefinition("$name Decision", reviewRoute, if (role == UserRole.ED) setOf("ed_approval") else setOf("md_approval")),
        dossier + setOf("credit_assessment", "recommendations", "approval_conditions", "mcc_decisions"), mcc
    )
    private val legal = workspace(
        UserRole.LEGAL, "Legal",
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("Legal Review", Screen.LegalWorkspace), WorkspaceDestination("Search", Screen.SearchResults)),
        setOf(Screen.LegalWorkspace, Screen.ValuationEditor, Screen.ApplicationDetail, Screen.DocumentViewer, Screen.WorkflowEventAudit, Screen.SearchResults),
        QueueDefinition("Legal Review", "legal", "No dossiers require legal review.", Screen.LegalWorkspace),
        SearchDefinition("Search Legal Dossiers", "Customer, application, or collateral reference", setOf("legal_dossier", "collateral")),
        ReviewDefinition("Legal and Valuation Review", Screen.LegalWorkspace, setOf("branch_manager_review", "credit_analyst_review", "crm_review"))
    )
    private val admin = WorkspaceDefinition(
        role = UserRole.SYSTEM_ADMIN,
        canonicalRoleName = "System Admin",
        primaryDestinations = listOf(WorkspaceDestination("Dashboard", Screen.Dashboard), WorkspaceDestination("Users", Screen.Users), WorkspaceDestination("System Activity", Screen.SystemActivity)),
        allowedRoutes = setOf(Screen.Login, Screen.ForgotPassword, Screen.ResetPassword, Screen.PermissionsPrimer, Screen.Onboarding, Screen.Dashboard, Screen.Users, Screen.SystemActivity),
        dossierSections = emptySet()
    )
    private val executiveReadOnly = workspace(
        UserRole.EXECUTIVE, "Executive",
        listOf(WorkspaceDestination("Dashboard", Screen.Dashboard)), emptySet(), sections = emptySet()
    )

    fun forRole(role: UserRole): WorkspaceDefinition = when (role) {
        UserRole.ACCOUNT_OFFICER, UserRole.LOAN_OFFICER -> relationship
        UserRole.BRANCH_MANAGER -> teamLead
        UserRole.BRANCH_SUPERVISOR -> supervisor
        UserRole.CREDIT_ANALYST -> credit
        UserRole.CRM -> crm(UserRole.CRM, "CRM Officer")
        UserRole.HEAD_CRM -> crm(UserRole.HEAD_CRM, "Head CRM")
        UserRole.AUDITOR -> audit
        UserRole.ED -> executive(UserRole.ED, "Executive Director", "Executive Director Decisions", Screen.EdQueue, Screen.EdApproval, "ed-approval", true)
        UserRole.MD -> executive(UserRole.MD, "Managing Director", "Managing Director Decisions", Screen.MdQueue, Screen.MdApproval, "md-approval", true)
        UserRole.LEGAL -> legal
        UserRole.SYSTEM_ADMIN -> admin
        UserRole.EXECUTIVE -> executiveReadOnly
    }
}
