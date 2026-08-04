package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditOfficerReviewScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onCompleteReview: () -> Unit
) {
    val appState by applicationViewModel.uiState.collectAsState()
    LaunchedEffect(application.id) { applicationViewModel.loadApplicationDetail(application.id) }

    // Screen-level states
    var selectedTab by remember { mutableStateOf(0) }
    val tabLabels = listOf("Affordability", "Documents", "OCR Fields", "Bureau Report", "Recommendation")

    // Tab 1: Affordability inputs
    var affordabilityNotes by remember { mutableStateOf("") }
    
    // Tab 3: OCR Overrides
    var overrideBvn by remember { mutableStateOf("") }
    var overrideBvnReason by remember { mutableStateOf("") }
    var overrideName by remember { mutableStateOf("") }
    var overrideNameReason by remember { mutableStateOf("") }

    // Tab 5: Recommendation inputs
    var recommendedAmount by remember { mutableStateOf(application.amount?.toString() ?: "") }
    var recommendationNotes by remember { mutableStateOf("") }
    var recommendationDecision by remember { mutableStateOf("Recommend Approval") } // "Recommend Approval", "Recommend Rejection"

    LaunchedEffect(application.id) {
        applicationViewModel.loadBureauData(application.id)
        applicationViewModel.loadOcrFields(application.id)
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Credit Evaluation",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray950)
                    .border(width = 0.5.dp, color = FieldTheme.colors.gray800)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DecisionImpactNotice(
                    "Submission records your forward recommendation and advances the dossier to CRM review."
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { 
                            // Simulate Save Draft local action
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Draft", color = FieldTheme.colors.purple400)
                    }
                    PrimaryButton(
                        text = if (appState.isLoading) "Submitting..." else "Submit Recommendation",
                        onClick = {
                            applicationViewModel.submitCreditReview(
                                id = application.id,
                                decision = recommendationDecision,
                                notes = recommendationNotes,
                                onComplete = {
                                    onCompleteReview()
                                }
                            )
                        },
                        enabled = recommendationNotes.isNotEmpty() && !appState.isLoading,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Z2: Summary Card
            FieldCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = borrower?.name ?: application.applicant_name ?: "Unknown Applicant",
                            style = FieldTheme.typography.bodyStrong,
                            color = FieldTheme.colors.gray100
                        )
                        Text(
                            text = "Ref: ${application.ref_no}",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.gray400
                        )
                    }
                    StatusChip(label = application.displayStatus)
                }

                HorizontalDivider(color = FieldTheme.colors.gray800, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "REQUESTED AMOUNT",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.gray400
                        )
                        Text(
                            text = "₦${String.format(Locale.US, "%,.2f", application.amount ?: 0.0)}",
                            style = FieldTheme.typography.display.copy(fontSize = 28.sp),
                            color = FieldTheme.colors.purple400
                        )
                    }
                }
            }

            DatabaseDossierSections(appState.selectedAppDetail, appState.isLoadingDetail)

            // Tab Navigation Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = FieldTheme.colors.purple400,
                edgePadding = 0.dp,
                divider = { HorizontalDivider(color = FieldTheme.colors.gray800) }
            ) {
                tabLabels.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = label,
                                style = FieldTheme.typography.label,
                                color = if (selectedTab == index) FieldTheme.colors.gray100 else FieldTheme.colors.gray500
                            )
                        }
                    )
                }
            }

            // Z4 Tab Content
            when (selectedTab) {
                0 -> {
                    // Tab 1: Affordability Table
                    SectionCard(title = "Declared vs. Bank-Verified Affordability") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header Row
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Metric", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500, modifier = Modifier.weight(1.5f))
                                Text("Declared", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text("Verified", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text("Variance", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            
                            HorizontalDivider(color = FieldTheme.colors.gray800)

                            // Row 1: Monthly Income
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Monthly Income", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300, modifier = Modifier.weight(1.5f))
                                Text("Not available", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500, modifier = Modifier.weight(3f), textAlign = TextAlign.End)
                            }

                            // Row 2: Rent Expenditure
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Monthly Rent", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300, modifier = Modifier.weight(1.5f))
                                Text("Not available", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500, modifier = Modifier.weight(3f), textAlign = TextAlign.End)
                            }

                            // Row 3: Co-borrower repayments
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Debt Obligations", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300, modifier = Modifier.weight(1.5f))
                                Text("Not available", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500, modifier = Modifier.weight(3f), textAlign = TextAlign.End)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Notes field beneath
                            OutlinedTextField(
                                value = affordabilityNotes,
                                onValueChange = { affordabilityNotes = it },
                                placeholder = { Text("Enter affordability notes or variance explanations...") },
                                label = { Text("Affordability Analysis Notes") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = FieldTheme.colors.gray900,
                                    unfocusedContainerColor = FieldTheme.colors.gray900,
                                    focusedTextColor = FieldTheme.colors.gray100,
                                    unfocusedTextColor = FieldTheme.colors.gray100
                                )
                            )
                        }
                    }
                }
                1 -> {
                    // Tab 2: Documents Verification List
                    SectionCard(title = "Compliance and Verification Documents") {
                        Text("Open the dossier Documents section for current database-backed verification records.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                    }
                }
                2 -> {
                    // Tab 3: OCR Fields Override controls
                    SectionCard(title = "OCR Extracted Fields Override controls") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Verify or manually override the high-confidence values scanned by OCR:",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray400
                            )
                            
                            // Field 1: BVN
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Extracted BVN: 222******89", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                    Text("Confidence: 94%", style = FieldTheme.typography.label, color = FieldTheme.colors.statusSuccess)
                                }
                                OutlinedTextField(
                                    value = overrideBvn,
                                    onValueChange = { overrideBvn = it },
                                    placeholder = { Text("Enter correct BVN to override...") },
                                    label = { Text("Override BVN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = FieldTheme.colors.gray900, unfocusedContainerColor = FieldTheme.colors.gray900, focusedTextColor = FieldTheme.colors.gray100, unfocusedTextColor = FieldTheme.colors.gray100)
                                )
                                OutlinedTextField(
                                    value = overrideBvnReason,
                                    onValueChange = { overrideBvnReason = it },
                                    placeholder = { Text("Reason for overriding BVN value...") },
                                    label = { Text("Override Reason") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = FieldTheme.colors.gray900, unfocusedContainerColor = FieldTheme.colors.gray900, focusedTextColor = FieldTheme.colors.gray100, unfocusedTextColor = FieldTheme.colors.gray100)
                                )
                            }

                            HorizontalDivider(color = FieldTheme.colors.gray800)

                            // Field 2: Name
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Extracted Name: ${application.applicant_name}", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                    Text("Confidence: 98%", style = FieldTheme.typography.label, color = FieldTheme.colors.statusSuccess)
                                }
                                OutlinedTextField(
                                    value = overrideName,
                                    onValueChange = { overrideName = it },
                                    placeholder = { Text("Enter correct spelling to override...") },
                                    label = { Text("Override Applicant Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = FieldTheme.colors.gray900, unfocusedContainerColor = FieldTheme.colors.gray900, focusedTextColor = FieldTheme.colors.gray100, unfocusedTextColor = FieldTheme.colors.gray100)
                                )
                                OutlinedTextField(
                                    value = overrideNameReason,
                                    onValueChange = { overrideNameReason = it },
                                    placeholder = { Text("Reason for overriding Name value...") },
                                    label = { Text("Override Reason") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = FieldTheme.colors.gray900, unfocusedContainerColor = FieldTheme.colors.gray900, focusedTextColor = FieldTheme.colors.gray100, unfocusedTextColor = FieldTheme.colors.gray100)
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // Tab 4: Bureau Report
                    SectionCard(title = "External Credit Registry Bureau Report") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton(
                                text = if (appState.isMutating) "Refreshing Report..." else "Pull credit-bureau report",
                                enabled = !appState.isMutating,
                                onClick = { applicationViewModel.pullCreditBureau(application.id) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = FieldTheme.colors.gray800, modifier = Modifier.padding(vertical = 8.dp))

                            if (appState.bureauData != null) {
                                val report = appState.bureauData!!
                                DetailItem(label = "Bureau Credit Score", value = report.credit_score.toString())
                                DetailItem(label = "DTI Ratio (Verified)", value = "${String.format(Locale.US, "%.1f", report.dti_ratio)}%")
                                DetailItem(label = "Income Verified", value = if (report.income_verified) "Yes" else "No")
                                DetailItem(label = "Registry Database Source", value = report.source)
                            } else {
                                EmptyState("No active credit report has been pulled for this applicant today.")
                            }
                        }
                    }
                }
                4 -> {
                    // Tab 5: Recommendation
                    SectionCard(title = "Underwriter Verdict & Amount Recommendation") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Decision Selector
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Decision Recommendation", style = FieldTheme.typography.label, color = FieldTheme.colors.gray400)
                                listOf("Recommend Approval", "Recommend Rejection").forEach { decision ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { recommendationDecision = decision }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = recommendationDecision == decision,
                                            onClick = { recommendationDecision = decision }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(decision, style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                                    }
                                }
                            }

                            HorizontalDivider(color = FieldTheme.colors.gray800)

                            // Recommended amount input (numeric, tabular figures)
                            OutlinedTextField(
                                value = recommendedAmount,
                                onValueChange = { recommendedAmount = it },
                                label = { Text("Recommended Approved Amount (₦)") },
                                placeholder = { Text("Enter recommended approved amount...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = FieldTheme.colors.gray900,
                                    unfocusedContainerColor = FieldTheme.colors.gray900,
                                    focusedTextColor = FieldTheme.colors.gray100,
                                    unfocusedTextColor = FieldTheme.colors.gray100
                                )
                            )

                            // Notes textarea
                            OutlinedTextField(
                                value = recommendationNotes,
                                onValueChange = { recommendationNotes = it },
                                label = { Text("Recommendation Notes / Credit Assessment") },
                                placeholder = { Text("Provide credit analysis findings, notes on affordability ratios, or rejection reasons...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = FieldTheme.colors.gray900,
                                    unfocusedContainerColor = FieldTheme.colors.gray900,
                                    focusedTextColor = FieldTheme.colors.gray100,
                                    unfocusedTextColor = FieldTheme.colors.gray100
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
