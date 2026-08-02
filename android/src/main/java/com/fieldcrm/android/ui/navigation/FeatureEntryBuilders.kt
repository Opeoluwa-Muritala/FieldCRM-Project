package com.fieldcrm.android.ui.navigation.impl

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.fieldcrm.android.ui.viewmodel.Screen
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.SupportingPaneSceneStrategy

internal fun EntryProviderScope<NavKey>.authEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.Login> { content(it) }
    entry<Screen.ForgotPassword> { content(it) }
    entry<Screen.ResetPassword> { content(it) }
    entry<Screen.PasscodeSetup> { content(it) }
    entry<Screen.PasscodeLogin> { content(it) }
}

internal fun EntryProviderScope<NavKey>.onboardingEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.PermissionsPrimer> { content(it) }
    entry<Screen.Onboarding> { content(it) }
}

internal fun EntryProviderScope<NavKey>.shellEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.Dashboard> { content(it) }
    entry<Screen.Settings> { content(it) }
    entry<Screen.Notifications>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.SearchResults> { content(it) }
    entry<Screen.Confirmation> { content(it) }
}

internal fun EntryProviderScope<NavKey>.borrowerEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.BorrowerList> { content(it) }
    entry<Screen.BorrowerDetail> { content(it) }
    entry<Screen.CreateBorrower> { content(it) }
}

internal fun EntryProviderScope<NavKey>.applicationEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.ApplicationDetail>(
        metadata = ListDetailSceneStrategy.detailPane() + SupportingPaneSceneStrategy.mainPane()
    ) { content(it) }
    entry<Screen.CreateApplication> { content(it) }
    entry<Screen.LoanApplicationForm> { content(it) }
    entry<Screen.GuarantorsForm> { content(it) }
    entry<Screen.PledgeTrust> { content(it) }
    entry<Screen.VisitationReport> { content(it) }
    entry<Screen.OfflineQueue> { content(it) }
    entry<Screen.OcrReview> { content(it) }
    entry<Screen.RepaymentSchedule>(metadata = ListDetailSceneStrategy.detailPane("servicing")) { content(it) }
    entry<Screen.ParDashboard>(metadata = ListDetailSceneStrategy.listPane("servicing")) { content(it) }
}

internal fun EntryProviderScope<NavKey>.documentEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.DocumentUpload> { content(it) }
    entry<Screen.DocumentViewer> { content(it) }
}

internal fun EntryProviderScope<NavKey>.reviewEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.BranchManagerReview>(metadata = SupportingPaneSceneStrategy.supportingPane()) { content(it) }
    entry<Screen.CreditOfficerReview>(metadata = SupportingPaneSceneStrategy.supportingPane()) { content(it) }
    entry<Screen.LegalWorkspace> { content(it) }
    entry<Screen.ValuationEditor> { content(it) }
    entry<Screen.MccWorkspace>(metadata = ListDetailSceneStrategy.listPane("mcc")) { content(it) }
    entry<Screen.InterestPresets> { content(it) }
    entry<Screen.BranchManagement> { content(it) }
    entry<Screen.WorkflowEventAudit> { content(it) }
    entry<Screen.AuditTrail>(metadata = ListDetailSceneStrategy.listPane("audit")) { content(it) }
    entry<Screen.ComplianceFlags>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.CrmReview>(metadata = SupportingPaneSceneStrategy.supportingPane()) { content(it) }
    entry<Screen.EdApproval>(metadata = SupportingPaneSceneStrategy.supportingPane()) { content(it) }
    entry<Screen.MdApproval>(metadata = SupportingPaneSceneStrategy.supportingPane()) { content(it) }
    entry<Screen.ExecutiveApproval>(metadata = SupportingPaneSceneStrategy.supportingPane()) { content(it) }
}

internal fun EntryProviderScope<NavKey>.executiveEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.EdQueue> { content(it) }
    entry<Screen.MdQueue> { content(it) }
}

internal fun EntryProviderScope<NavKey>.queueEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.MyQueue>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.VisitsDue>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.AwaitingConcurrence>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.PendingSignoffs>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.CreditReviewQueue>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.OcrExceptions>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.Pipeline>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.CrmQueue>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
    entry<Screen.ExecutiveQueue>(metadata = ListDetailSceneStrategy.listPane()) { content(it) }
}

internal fun EntryProviderScope<NavKey>.adminEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.Users>(metadata = ListDetailSceneStrategy.listPane("users")) { content(it) }
    entry<Screen.SystemActivity>(metadata = ListDetailSceneStrategy.listPane("activity")) { content(it) }
}
