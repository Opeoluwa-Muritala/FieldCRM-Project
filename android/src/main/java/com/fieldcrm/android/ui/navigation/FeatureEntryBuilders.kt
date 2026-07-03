package com.fieldcrm.android.ui.navigation.impl

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.fieldcrm.android.ui.viewmodel.Screen

internal fun EntryProviderScope<NavKey>.authEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.Login> { content(it) }
    entry<Screen.ForgotPassword> { content(it) }
    entry<Screen.ResetPassword> { content(it) }
    entry<Screen.PasscodeSetup> { content(it) }
    entry<Screen.PasscodeLogin> { content(it) }
    entry<Screen.BiometricEnrollment> { content(it) }
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
    entry<Screen.Notifications> { content(it) }
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
    entry<Screen.ApplicationDetail> { content(it) }
    entry<Screen.CreateApplication> { content(it) }
    entry<Screen.LoanApplicationForm> { content(it) }
    entry<Screen.GuarantorsForm> { content(it) }
    entry<Screen.PledgeTrust> { content(it) }
    entry<Screen.VisitationReport> { content(it) }
    entry<Screen.OfflineQueue> { content(it) }
    entry<Screen.OcrReview> { content(it) }
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
    entry<Screen.BranchManagerReview> { content(it) }
    entry<Screen.CreditOfficerReview> { content(it) }
    entry<Screen.AuditorCompliance> { content(it) }
    entry<Screen.AdminMcrApproval> { content(it) }
    entry<Screen.WorkflowEventAudit> { content(it) }
    entry<Screen.AuditTrail> { content(it) }
    entry<Screen.ComplianceFlags> { content(it) }
}

internal fun EntryProviderScope<NavKey>.queueEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.MyQueue> { content(it) }
    entry<Screen.VisitsDue> { content(it) }
    entry<Screen.AwaitingConcurrence> { content(it) }
    entry<Screen.PendingSignoffs> { content(it) }
    entry<Screen.CreditReviewQueue> { content(it) }
    entry<Screen.OcrExceptions> { content(it) }
    entry<Screen.Pipeline> { content(it) }
}

internal fun EntryProviderScope<NavKey>.adminEntryBuilder(
    content: @Composable (Screen) -> Unit
) {
    entry<Screen.Users> { content(it) }
    entry<Screen.SystemActivity> { content(it) }
}
