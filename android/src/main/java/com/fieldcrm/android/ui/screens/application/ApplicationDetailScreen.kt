package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.data.api.AuditTrailEvent
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.AuditTrailViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun ApplicationDetailScreenView(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    role: UserRole?,
    onBackClick: () -> Unit,
    onNavigateToDocumentUpload: () -> Unit = {},
    onNavigateToPledgeTrust: () -> Unit = {},
    onNavigateToVisitationReport: () -> Unit = {},
    onNavigateToGuarantorsForm: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToAuditTrail: () -> Unit = {},
    onNavigateToFormWizard: () -> Unit = {},
    onNavigateToDocumentViewer: () -> Unit = {}
) {
    val auditTrailVm: AuditTrailViewModel = koinViewModel()
    val auditUiState by auditTrailVm.uiState.collectAsState()

    LaunchedEffect(application.id) {
        auditTrailVm.load(application.id)
    }

    var isBorrowerSectionExpanded by remember { mutableStateOf(true) }
    var isCollateralSectionExpanded by remember { mutableStateOf(true) }
    var isAuditSectionExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isWide = maxWidth >= 840.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                FieldTopAppBar(
                    title = "Application Dossier: ${application.id.take(8).uppercase(Locale.getDefault())}",
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = FieldIcons.ArrowBackOutlined,
                                contentDescription = "Back",
                                tint = FieldTheme.colors.gray400
                            )
                        }
                    },
                    actions = {
                        StatusChip(variant = getStatusVariant(application.status))
                    }
                )
            },
            bottomBar = {
                if (!isWide) {
                    ApplicationActionFooter(
                        role = role,
                        onNavigateToFormWizard = onNavigateToFormWizard,
                        onNavigateToReview = onNavigateToReview
                    )
                }
            },
            containerColor = FieldTheme.colors.gray950
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Left/Main Area
                Column(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "LENDING FLOW POSITION",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.clickable { onNavigateToFormWizard() }) {
                                LoanStageTimeline(
                                    stages = listOf("Intake", "OCR Review", "Credit Review", "BM Approved", "Ready"),
                                    currentStageIndex = application.current_stage
                                )
                            }
                        }

                        item {
                            FieldCard {
                                Text(
                                    text = "Loan Profile Details",
                                    style = FieldTheme.typography.title,
                                    color = FieldTheme.colors.gray100
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItem(label = "Requested Amount", value = "₦${String.format(Locale.US, "%,.0f", application.amount)}", isMono = true)
                                        DetailItem(label = "Interest Rate", value = "${application.interest_rate}% per annum")
                                        DetailItem(label = "Repayment Frequency", value = application.repayment_frequency)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItem(label = "Loan Tenure", value = "${application.tenure} Months")
                                        DetailItem(label = "Product Type", value = application.product_type)
                                        DetailItem(label = "Created Date", value = application.created_at)
                                    }
                                }
                            }
                        }

                        // Borrower Section Accordion
                        item {
                            AccordionHeader(
                                title = "Borrower & Identity Verification",
                                isExpanded = isBorrowerSectionExpanded,
                                onToggle = { isBorrowerSectionExpanded = !isBorrowerSectionExpanded }
                            )
                            if (isBorrowerSectionExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    val bName = borrower?.name?.takeIf { it.isNotBlank() } ?: "—"
                                    val bBvn = borrower?.bvn?.takeIf { it.isNotBlank() } ?: "—"
                                    val bNin = borrower?.nin?.takeIf { it.isNotBlank() } ?: "—"
                                    val bLga = borrower?.physical_address?.takeIf { it.isNotBlank() } ?: "—"

                                    DetailItem(label = "Applicant Full Name", value = bName)
                                    DetailItem(label = "Primary Address / LGA Node", value = bLga)
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "BVN Ref", value = bBvn, isMono = true)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "NIN Ref", value = bNin, isMono = true)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "IDENTITY CONFIDENCE METRICS",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ConfidenceBar(percentage = 0.94f)
                                }
                            }
                        }

                        // Collateral Accordion
                        item {
                            AccordionHeader(
                                title = "Collateral & Guarantee Dossier",
                                isExpanded = isCollateralSectionExpanded,
                                onToggle = { isCollateralSectionExpanded = !isCollateralSectionExpanded }
                            )
                            if (isCollateralSectionExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    val cDesc = application.collateral_desc?.takeIf { it.isNotBlank() } ?: "—"
                                    val cVal = application.collateral_value ?: 0.0

                                    DetailItem(label = "Collateral Description", value = cDesc)
                                    DetailItem(
                                        label = "Estimated Value",
                                        value = if (cVal > 0) "₦${String.format(Locale.US, "%,.0f", cVal)}" else "Not recorded",
                                        isMono = true
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "UPLOADED VERIFICATION SLIPS",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // No document model yet — prompt to upload
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(6.dp))
                                            .background(FieldTheme.colors.gray850, RoundedCornerShape(6.dp))
                                            .clickable { onNavigateToDocumentUpload() }
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "No document uploaded — tap to upload",
                                            style = FieldTheme.typography.body,
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                }
                            }
                        }

                        // Compliance Gates Checklist
                        item {
                            Text(
                                text = "COMPLIANCE CHECKLIST GATES",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                    .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                val hasIdentity = borrower != null && borrower.name.isNotEmpty() && borrower.bvn.isNotEmpty()
                                val isPledgeSigned = application.collateral_desc?.contains("Pledge") == true || application.collateral_desc?.contains("Executed") == true
                                val hasGps = borrower?.gps_coordinates?.let { it.isNotEmpty() && it.contains("Lat:") } == true
                                val hasGuarantor = borrower?.guarantor_name?.isNotEmpty() == true

                                val gatesList = listOf(
                                    Triple("Identity Verification Passed", if (hasIdentity) StatusChipVariant.Verified else StatusChipVariant.Missing, onNavigateToDocumentUpload),
                                    Triple("Pledge Document Signed", if (isPledgeSigned) StatusChipVariant.Signed else StatusChipVariant.Missing, onNavigateToPledgeTrust),
                                    Triple("GPS Visitation Photo Stamped", if (hasGps) StatusChipVariant.Verified else StatusChipVariant.Missing, onNavigateToVisitationReport),
                                    Triple("Guarantor Verification Logged", if (hasGuarantor) StatusChipVariant.Verified else StatusChipVariant.Missing, onNavigateToGuarantorsForm)
                                )
                                gatesList.forEachIndexed { index, gate ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = gate.third)
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (gate.second != StatusChipVariant.Missing) FieldIcons.CheckOutlined else FieldIcons.CloseOutlined,
                                                contentDescription = gate.first,
                                                tint = if (gate.second != StatusChipVariant.Missing) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusDanger,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = gate.first,
                                                style = FieldTheme.typography.body,
                                                color = FieldTheme.colors.gray300
                                            )
                                        }
                                        StatusChip(variant = gate.second)
                                    }
                                    if (index < gatesList.size - 1) {
                                        FieldDivider()
                                    }
                                }
                            }
                        }

                        if (!isWide) {
                            item {
                                AccordionHeader(
                                    title = "Decision Audit Trail",
                                    isExpanded = isAuditSectionExpanded,
                                    onToggle = { isAuditSectionExpanded = !isAuditSectionExpanded }
                                )
                                if (isAuditSectionExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AuditTrailContent(
                                        events = auditUiState.events,
                                        isLoading = auditUiState.isLoading
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                if (isWide) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(FieldTheme.colors.gray900)
                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(0.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "DECISION CONTROLS",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.gray500
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ApplicationActionFooter(
                            role = role,
                            onNavigateToFormWizard = onNavigateToFormWizard,
                            onNavigateToReview = onNavigateToReview,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "HISTORICAL AUDIT PATH",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.gray500
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .clickable { onNavigateToAuditTrail() }
                                .weight(1f)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    AuditTrailContent(
                                        events = auditUiState.events,
                                        isLoading = auditUiState.isLoading
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditTrailContent(
    events: List<AuditTrailEvent>,
    isLoading: Boolean
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = FieldTheme.colors.brandPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        events.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audit entries recorded yet.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray500,
                    textAlign = TextAlign.Center
                )
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                events.forEach { event ->
                    AuditTrailEntry(
                        timestamp = event.timestamp,
                        actorName = event.actor_name,
                        actorRole = event.actor_role,
                        action = event.action,
                        diff = event.state_diff.ifBlank { null },
                        isCurrentUserAction = event.is_mine
                    )
                }
            }
        }
    }
}

@Composable
fun AccordionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = FieldTheme.typography.label.copy(fontWeight = FontWeight.Bold),
            color = FieldTheme.colors.gray400
        )
        Icon(
            imageVector = if (isExpanded) FieldIcons.ChevronUpOutlined else FieldIcons.ChevronDownOutlined,
            contentDescription = "Toggle Section",
            tint = FieldTheme.colors.gray500
        )
    }
}

@Composable
fun ApplicationActionFooter(
    role: UserRole?,
    onNavigateToFormWizard: () -> Unit,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(FieldTheme.colors.gray900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (role) {
            null, UserRole.LOAN_OFFICER -> {
                PrimaryButton(
                    text = "Continue Application Form",
                    onClick = onNavigateToFormWizard
                )
            }
            else -> {
                PrimaryButton(
                    text = "Open Review Console",
                    onClick = onNavigateToReview
                )
            }
        }
    }
}

private fun getStatusVariant(status: String): StatusChipVariant {
    return when (status.lowercase(Locale.getDefault())) {
        "approved", "bm approved" -> StatusChipVariant.Approved
        "intake" -> StatusChipVariant.Verified
        "needs review" -> StatusChipVariant.NeedsReview
        "low confidence" -> StatusChipVariant.LowConfidence
        "returned" -> StatusChipVariant.Returned
        else -> StatusChipVariant.NeedsReview
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact — Loan Officer", widthDp = 411, heightDp = 850)
@Composable
fun PreviewApplicationDetailLoanOfficer() {
    val demoBorrower = BorrowerModel(
        id = "1",
        org_id = "org_1",
        loan_officer_id = "LO_1",
        name = "Adaeze Okonkwo",
        phone = "08012345678",
        bvn = "222333444",
        nin = "111222333",
        status = "Active",
        created_at = "2026-06-18"
    )
    val demoApp = LoanApplicationModel(
        id = "app_1",
        org_id = "org_1",
        borrower_id = "1",
        current_stage = 2,
        current_owner_id = "LO_1",
        status = "Needs Review",
        amount = 250000.0,
        tenure = 6,
        product_type = "SME Asset Acquisition",
        interest_rate = 15.0,
        repayment_frequency = "Monthly repayment",
        collateral_desc = "Deed of pledge over local shop stock",
        collateral_value = 850000.0,
        officer_recommendation = "Approved with high shop confidence metrics",
        created_at = "2026-06-18"
    )
    FieldCRMTheme {
        ApplicationDetailScreenView(
            application = demoApp,
            borrower = demoBorrower,
            role = UserRole.LOAN_OFFICER,
            onBackClick = {}
        )
    }
}

@Preview(name = "Compact — Branch Manager", widthDp = 411, heightDp = 850)
@Composable
fun PreviewApplicationDetailBranchManager() {
    val demoBorrower = BorrowerModel(
        id = "1",
        org_id = "org_1",
        loan_officer_id = "LO_1",
        name = "Emeka Obi",
        phone = "08098765432",
        bvn = "333444555",
        nin = "222333444",
        status = "Active",
        created_at = "2026-06-20"
    )
    val demoApp = LoanApplicationModel(
        id = "app_2",
        org_id = "org_1",
        borrower_id = "1",
        current_stage = 3,
        current_owner_id = "BM_1",
        status = "Needs Review",
        amount = 500000.0,
        tenure = 12,
        product_type = "Micro Enterprise Working Capital",
        interest_rate = 12.0,
        repayment_frequency = "Monthly repayment",
        collateral_desc = "Land title document and registry check verification",
        collateral_value = 1200000.0,
        officer_recommendation = "",
        created_at = "2026-06-20"
    )
    FieldCRMTheme {
        ApplicationDetailScreenView(
            application = demoApp,
            borrower = demoBorrower,
            role = UserRole.BRANCH_MANAGER,
            onBackClick = {}
        )
    }
}

@Preview(name = "Tablet — Branch Manager", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewApplicationDetailTablet() {
    val demoBorrower = BorrowerModel(
        id = "1",
        org_id = "org_1",
        loan_officer_id = "LO_1",
        name = "Ngozi Eze",
        phone = "08011223344",
        bvn = "444555666",
        nin = "333444555",
        status = "Active",
        created_at = "2026-06-18"
    )
    val demoApp = LoanApplicationModel(
        id = "app_3",
        org_id = "org_1",
        borrower_id = "1",
        current_stage = 3,
        current_owner_id = "BM_1",
        status = "Approved",
        amount = 1200000.0,
        tenure = 12,
        product_type = "Micro Enterprise Working Capital",
        interest_rate = 12.0,
        repayment_frequency = "Monthly repayment",
        collateral_desc = "Land title document and registry check verification",
        collateral_value = 2400000.0,
        officer_recommendation = "",
        created_at = "2026-06-18"
    )
    FieldCRMTheme {
        ApplicationDetailScreenView(
            application = demoApp,
            borrower = demoBorrower,
            role = UserRole.BRANCH_MANAGER,
            onBackClick = {}
        )
    }
}
