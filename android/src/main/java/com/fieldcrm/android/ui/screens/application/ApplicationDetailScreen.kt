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
import com.fieldcrm.android.data.repository.ApplicationDetailResult
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
    appDetail: ApplicationDetailResult? = null,
    isLoadingDetail: Boolean = false,
    onBackClick: () -> Unit,
    onNavigateToDocumentUpload: () -> Unit = {},
    onNavigateToPledgeTrust: () -> Unit = {},
    onNavigateToVisitationReport: () -> Unit = {},
    onNavigateToGuarantorsForm: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToAuditTrail: () -> Unit = {},
    onNavigateToFormWizard: () -> Unit = {},
    onNavigateToDocumentViewer: (url: String, name: String) -> Unit = { _, _ -> },
    onNavigateToOcrReview: () -> Unit = {}
) {
    val auditTrailVm: AuditTrailViewModel = koinViewModel()
    val auditUiState by auditTrailVm.uiState.collectAsState()

    LaunchedEffect(application.id) {
        auditTrailVm.load(application.id)
    }

    var isBorrowerSectionExpanded by remember { mutableStateOf(true) }
    var isCollateralSectionExpanded by remember { mutableStateOf(true) }
    var isAuditSectionExpanded by remember { mutableStateOf(true) }

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
                        StatusChip(variant = getStatusVariant(application.stage))
                    }
                )
            },
            bottomBar = {
                if (!isWide) {
                    ApplicationActionFooter(
                        role = role,
                        onNavigateToFormWizard = onNavigateToFormWizard,
                        onNavigateToReview = onNavigateToReview,
                        application = application,
                        onNavigateToOcrReview = onNavigateToOcrReview
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                                    .border(width = 0.5.dp, color = FieldTheme.colors.purple600.copy(alpha = 0.1f))
                                    .padding(horizontal = 24.dp, vertical = 24.dp)
                            ) {
                                Text(
                                    text = "Application Dossier Master",
                                    style = FieldTheme.typography.title.copy(fontSize = 28.sp),
                                    color = FieldTheme.colors.gray100
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Comprehensive review of the lending application profile, borrower identity, collateral, and compliance gates.",
                                    style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                                    color = FieldTheme.colors.gray400
                                )
                            }
                        }

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
                                    currentStageIndex = application.stageIndex
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
                                        DetailItem(label = "Requested Amount", value = "₦${String.format(Locale.US, "%,.0f", application.amount ?: 0.0)}", isMono = true)
                                        DetailItem(label = "Interest Rate", value = "${application.interest_rate ?: "—"}% per annum")
                                        DetailItem(label = "Repayment Frequency", value = application.repayment_frequency ?: "—")
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItem(label = "Loan Tenor", value = "${application.tenor_months ?: "—"} Months")
                                        DetailItem(label = "Loan Type", value = application.loan_type.replaceFirstChar { it.uppercase() })
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
                                if (isLoadingDetail) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FieldCard { DetailSkeletonBlock() }
                                    return@item
                                }
                                val i = appDetail?.intake ?: emptyMap()
                                fun intakeStr(vararg keys: String): String {
                                    for (k in keys) {
                                        val v = i[k]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                                        if (v != null) return v
                                    }
                                    return "—"
                                }

                                val bName = intakeStr("applicant_full_name", "full_name", "applicant_name")
                                    .takeIf { it != "—" } ?: (borrower?.name?.takeIf { it.isNotBlank() } ?: application.applicant_name)
                                val bPhone = intakeStr("phone", "phone_numbers", "phone_number")
                                    .takeIf { it != "—" } ?: (borrower?.phone?.takeIf { it.isNotBlank() } ?: "—")
                                val bBvn = intakeStr("bvn")
                                    .takeIf { it != "—" } ?: (borrower?.bvn?.takeIf { it.isNotBlank() } ?: "—")
                                val bNin = intakeStr("nin")
                                    .takeIf { it != "—" } ?: (borrower?.nin?.takeIf { it.isNotBlank() } ?: "—")
                                val bAddress = intakeStr("physical_address", "address", "home_address", "residential_address")
                                    .takeIf { it != "—" } ?: (borrower?.physical_address?.takeIf { it.isNotBlank() } ?: "—")
                                val bDob = intakeStr("dob", "date_of_birth")
                                val bMarital = intakeStr("marital_status")
                                val bEmployment = intakeStr("employment_status")
                                    .takeIf { it != "—" } ?: (borrower?.employment_status?.takeIf { it.isNotBlank() } ?: "—")
                                val bEmployer = intakeStr("employer_name", "business_name")
                                    .takeIf { it != "—" } ?: (borrower?.employer_name?.takeIf { it.isNotBlank() } ?: "—")
                                val bIncome = intakeStr("monthly_income", "monthly_salary")
                                    .takeIf { it != "—" }
                                    ?.toDoubleOrNull()
                                    ?.let { "₦${String.format(Locale.US, "%,.0f", it)}" }
                                    ?: (borrower?.monthly_income?.let { "₦${String.format(Locale.US, "%,.0f", it)}" } ?: "—")
                                val bBank = intakeStr("bank_name")
                                    .takeIf { it != "—" } ?: (borrower?.bank_name?.takeIf { it.isNotBlank() } ?: "—")
                                val bAccount = intakeStr("account_number")
                                    .takeIf { it != "—" } ?: (borrower?.account_number?.takeIf { it.isNotBlank() } ?: "—")
                                val bGps = intakeStr("gps_coordinates")
                                    .takeIf { it != "—" } ?: (borrower?.gps_coordinates?.takeIf { it.isNotBlank() } ?: "—")
                                val g1Name = intakeStr("guarantor_1_name")
                                val g2Name = intakeStr("guarantor_2_name")

                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    Text("PERSONAL INFORMATION", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    DetailItem(label = "Full Name", value = bName)
                                    DetailItem(label = "Phone Number", value = bPhone)
                                    DetailItem(label = "Physical Address", value = bAddress)
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "Date of Birth", value = bDob)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "Marital Status", value = bMarital)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("IDENTITY", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "BVN", value = bBvn, isMono = true)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "NIN", value = bNin, isMono = true)
                                        }
                                    }
                                    if (bGps != "—") DetailItem(label = "GPS Coordinates", value = bGps, isMono = true)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("EMPLOYMENT & INCOME", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "Employment Status", value = bEmployment)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "Monthly Income", value = bIncome, isMono = true)
                                        }
                                    }
                                    DetailItem(label = "Employer / Business Name", value = bEmployer)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("BANK DETAILS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "Bank Name", value = bBank)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            DetailItem(label = "Account Number", value = bAccount, isMono = true)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("IDENTITY CONFIDENCE METRICS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ConfidenceBar(percentage = 0.94f)
                                }

                                if (g1Name != "—" || g2Name != "—") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldCard {
                                        Text("GUARANTOR INFORMATION", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (g1Name != "—") {
                                            Text("Guarantor 1", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            DetailItem(label = "Full Name", value = g1Name)
                                            DetailItem(label = "Phone", value = intakeStr("guarantor_1_phone"))
                                            DetailItem(label = "Address", value = intakeStr("guarantor_1_address"))
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    DetailItem(label = "BVN", value = intakeStr("guarantor_1_bvn"), isMono = true)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    DetailItem(label = "NIN", value = intakeStr("guarantor_1_nin"), isMono = true)
                                                }
                                            }
                                            val g1Status = intakeStr("guarantor_1_status")
                                            if (g1Status != "—") DetailItem(label = "Status", value = g1Status)
                                        }
                                        if (g1Name != "—" && g2Name != "—") {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            FieldDivider()
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                        if (g2Name != "—") {
                                            Text("Guarantor 2", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            DetailItem(label = "Full Name", value = g2Name)
                                            DetailItem(label = "Phone", value = intakeStr("guarantor_2_phone"))
                                            DetailItem(label = "Address", value = intakeStr("guarantor_2_address"))
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    DetailItem(label = "BVN", value = intakeStr("guarantor_2_bvn"), isMono = true)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    DetailItem(label = "NIN", value = intakeStr("guarantor_2_nin"), isMono = true)
                                                }
                                            }
                                            val g2Status = intakeStr("guarantor_2_status")
                                            if (g2Status != "—") DetailItem(label = "Status", value = g2Status)
                                        }
                                    }
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
                                if (isLoadingDetail) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FieldCard { DetailSkeletonBlock(rows = 3) }
                                    return@item
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    val cDesc = application.purpose?.takeIf { it.isNotBlank() } ?: "—"

                                    DetailItem(label = "Loan Purpose / Collateral Description", value = cDesc)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "UPLOADED VERIFICATION SLIPS",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val docs = appDetail?.documents ?: emptyList()
                                    if (docs.isEmpty()) {
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
                                    } else {
                                        docs.forEach { doc ->
                                            val docType = (doc["doc_type"] as? String ?: "Document")
                                                .replace("_", " ").replaceFirstChar { it.uppercase() }
                                            val verified = doc["verified"] as? Boolean ?: false
                                            val docUrl = doc["secure_url"] as? String ?: doc["file_url"] as? String ?: ""
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onNavigateToDocumentViewer(docUrl, docType) }
                                                    .padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(docType, style = FieldTheme.typography.body,
                                                    color = FieldTheme.colors.gray300, modifier = Modifier.weight(1f))
                                                StatusChip(
                                                    label = if (verified) "Verified" else "Pending",
                                                    isPositive = verified
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, FieldTheme.colors.purple600.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .clickable { onNavigateToDocumentUpload() }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("+ Add document", style = FieldTheme.typography.body,
                                                color = FieldTheme.colors.purple400)
                                        }
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
                                val r = appDetail?.readiness ?: emptyMap()
                                val hasIdentity = (r["loan_form_submitted"] as? Boolean)
                                    ?: (borrower != null && borrower.bvn.isNotEmpty())
                                val isPledgeSigned = (r["pledge_form_submitted"] as? Boolean)
                                    ?: (application.purpose?.contains("Pledge") == true)
                                val hasGps = (r["visitation_status"] as? String)?.let { it == "completed" || it == "concurred" }
                                    ?: (borrower?.gps_coordinates?.isNotEmpty() == true)
                                val hasGuarantor = (r["guarantor_form_submitted"] as? Boolean)
                                    ?: (borrower?.guarantor_name?.isNotEmpty() == true)
                                val docs = appDetail?.documents ?: emptyList()
                                val hasDocuments = ((r["verified_docs"] as? Int) ?: docs.count { it["verified"] == true }) > 0

                                val firstDocUrl = docs.firstOrNull()
                                    ?.let { it["secure_url"] as? String ?: it["file_url"] as? String ?: "" } ?: ""
                                val firstDocName = docs.firstOrNull()
                                    ?.let { (it["doc_type"] as? String ?: "Document")
                                        .replace("_", " ").replaceFirstChar { c -> c.uppercase() } } ?: "Document"

                                val gatesList: List<Triple<String, StatusChipVariant, () -> Unit>> = listOf(
                                    Triple("Identity Verification Passed", if (hasIdentity) StatusChipVariant.Verified else StatusChipVariant.Missing, onNavigateToDocumentUpload),
                                    Triple("Pledge Document Signed", if (isPledgeSigned) StatusChipVariant.Signed else StatusChipVariant.Missing, onNavigateToPledgeTrust),
                                    Triple("GPS Visitation Completed", if (hasGps) StatusChipVariant.Verified else StatusChipVariant.Missing, onNavigateToVisitationReport),
                                    Triple("Guarantor Verification Logged", if (hasGuarantor) StatusChipVariant.Verified else StatusChipVariant.Missing, onNavigateToGuarantorsForm),
                                    Triple(
                                        "Documents Uploaded & Verified",
                                        if (hasDocuments) StatusChipVariant.Verified else StatusChipVariant.Missing,
                                        if (hasDocuments) {
                                            { onNavigateToDocumentViewer(firstDocUrl, firstDocName) }
                                        } else {
                                            onNavigateToDocumentUpload
                                        }
                                    )
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

                        // Visitation Report
                        item {
                            val v = appDetail?.visitation ?: emptyMap()
                            val metWith = v["met_with"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                            val premises = v["premises_description"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                            val direction = v["direction_from_branch"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                            val visitDate = v["visit_date"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                            val visitStatus = v["status"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                            val managerNotes = v["manager_notes"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                            val businessCondition = v["business_condition"]?.toString()?.takeIf { it.isNotBlank() && it != "null" }

                            if (metWith != null || premises != null || direction != null) {
                                Text(
                                    text = "VISITATION REPORT",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    if (visitDate != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Visit Date", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                            Text(visitDate, style = FieldTheme.typography.mono.copy(fontSize = 12.sp), color = FieldTheme.colors.gray300)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    if (metWith != null) DetailItem(label = "Met With", value = metWith)
                                    if (premises != null) DetailItem(label = "Premises Description", value = premises)
                                    if (direction != null) DetailItem(label = "Direction from Branch", value = direction)
                                    if (businessCondition != null) DetailItem(label = "Business Condition", value = businessCondition)
                                    if (managerNotes != null) DetailItem(label = "Manager Notes", value = managerNotes)
                                    if (visitStatus != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Status", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                            StatusChip(
                                                label = visitStatus.replaceFirstChar { it.uppercase() },
                                                isPositive = visitStatus == "concurred" || visitStatus == "completed"
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Officer Recommendation
                        item {
                            val rec = application.crm_notes?.takeIf { it.isNotBlank() }
                            if (rec != null) {
                                Text(
                                    text = "CRM NOTES",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    Text(
                                        text = rec,
                                        style = FieldTheme.typography.body,
                                        color = FieldTheme.colors.gray500
                                    )
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
                            application = application,
                            onNavigateToOcrReview = onNavigateToOcrReview,
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
private fun DetailSkeletonBlock(rows: Int = 6) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingSkeleton(height = 12.dp, width = 90.dp)
                LoadingSkeleton(height = 14.dp, width = 160.dp)
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
    modifier: Modifier = Modifier,
    application: LoanApplicationModel? = null,
    onNavigateToOcrReview: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(FieldTheme.colors.gray900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val stage = application?.stage

        when (role) {
            null, UserRole.LOAN_OFFICER -> when (stage) {
                "intake", "returned", null -> PrimaryButton(
                    text = "Continue Application Form",
                    onClick = onNavigateToFormWizard,
                    modifier = Modifier.fillMaxWidth()
                )
                "ocr_review" -> PrimaryButton(
                    text = "Verify OCR & Advance to Credit",
                    onClick = onNavigateToOcrReview,
                    modifier = Modifier.fillMaxWidth()
                )
                else -> ReadOnlyBanner()
            }

            UserRole.BRANCH_MANAGER -> if (stage in setOf("credit_review", "branch_approval", "returned")) {
                PrimaryButton(
                    text = "Open Review Console",
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ReadOnlyBanner()
            }

            UserRole.CRM -> if (stage == "crm_review") {
                PrimaryButton(
                    text = "Open Review Console",
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ReadOnlyBanner()
            }

            UserRole.EXECUTIVE -> if (stage == "executive_approval") {
                PrimaryButton(
                    text = "Open Review Console",
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ReadOnlyBanner()
            }

            UserRole.COMMITTEE -> if (stage == "committee_review") {
                PrimaryButton(
                    text = "Open Review Console",
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ReadOnlyBanner()
            }

            UserRole.ED -> if (stage == "ed_approval") {
                PrimaryButton(
                    text = "Open Review Console",
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ReadOnlyBanner()
            }

            UserRole.MD -> if (stage == "md_approval") {
                PrimaryButton(
                    text = "Open Review Console",
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ReadOnlyBanner()
            }

            UserRole.AUDITOR, UserRole.SYSTEM_ADMIN -> PrimaryButton(
                text = "Open Review Console",
                onClick = onNavigateToReview,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReadOnlyBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = FieldIcons.SearchOutlined,
            contentDescription = null,
            tint = FieldTheme.colors.gray500,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Read-only — application is not at your action stage",
            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
            color = FieldTheme.colors.gray500
        )
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
        id = "app_1", org_id = "org_1", ref_no = "MMFB-001",
        stage = "ocr_review", loan_type = "enterprise", customer_type = "new",
        applicant_name = "Adaeze Okonkwo", created_by = "LO_1", current_owner_id = "LO_1",
        amount = 250000.0, tenor_months = 6,
        interest_rate = 15.0, repayment_frequency = "monthly",
        purpose = "Deed of pledge over local shop stock",
        crm_notes = "Approved with high shop confidence metrics",
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
        id = "app_2", org_id = "org_1", ref_no = "MMFB-002",
        stage = "credit_review", loan_type = "msef", customer_type = "existing",
        applicant_name = "Emeka Obi", created_by = "LO_1", current_owner_id = "BM_1",
        amount = 500000.0, tenor_months = 12,
        interest_rate = 12.0, repayment_frequency = "monthly",
        purpose = "Land title document and registry check verification",
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
        id = "app_3", org_id = "org_1", ref_no = "MMFB-003",
        stage = "branch_approval", loan_type = "msef", customer_type = "existing",
        applicant_name = "Ngozi Eze", created_by = "LO_1", current_owner_id = "BM_1",
        amount = 1200000.0, tenor_months = 12,
        interest_rate = 12.0, repayment_frequency = "monthly",
        purpose = "Land title document and registry check verification",
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
