package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@Composable
fun CreditOfficerReviewScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onCompleteReview: () -> Unit
) {
    var creditScoreIndex by remember { mutableIntStateOf(0) }
    val creditScores = listOf(
        Pair("740 (Excellent)", StatusChipVariant.Verified),
        Pair("680 (Good)", StatusChipVariant.Verified),
        Pair("580 (Fair)", StatusChipVariant.NeedsReview),
        Pair("450 (Poor)", StatusChipVariant.Missing)
    )
    
    var incomeStatement by remember { mutableStateOf("Verified Bank Statement (Lagos Node)") }
    var dtiRatio by remember { mutableFloatStateOf(0.32f) } // 32%
    var recommendationDecision by remember { mutableStateOf("Recommend Approval") } // "Recommend Approval", "Recommend Rejection", "Return for Correction"
    var recommendationNotes by remember { mutableStateOf("Applicant leverage index fits normal limits. Strong guarantor signature match verified.") }

    val isDtiLimitExceeded = dtiRatio > 0.40f
    val appState by applicationViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Risk Underwriting Center",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray950)
                    .border(width = 0.5.dp, color = FieldTheme.colors.gray800)
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    text = if (appState.isLoading) "Submitting Verdict..." else "Submit Credit Evaluation",
                    onClick = {
                        applicationViewModel.submitCreditReview(application.id, recommendationDecision, recommendationNotes) {
                            onCompleteReview()
                        }
                    },
                    enabled = !isDtiLimitExceeded && creditScores[creditScoreIndex].second != StatusChipVariant.Missing && recommendationNotes.isNotEmpty() && !appState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp) // extra padding for bottom bar
            ) {
                // High-End Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                        .border(width = 0.5.dp, color = FieldTheme.colors.purple600.copy(alpha = 0.1f))
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Credit Risk Matrix",
                        style = FieldTheme.typography.title.copy(fontSize = 28.sp),
                        color = FieldTheme.colors.gray100
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verify applicant leverage metrics and guarantor signatures prior to manager recommendation.",
                        style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                        color = FieldTheme.colors.gray400
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Application Summary Card
                        FieldCard {
                            Text(
                                text = "APPLICATION SUMMARY",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailItem(label = "Applicant", value = borrower?.name ?: application.applicant_name ?: "—")
                            DetailItem(label = "BVN", value = borrower?.bvn ?: "—")
                            DetailItem(label = "Phone", value = borrower?.phone ?: "—")
                            FieldDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            DetailItem(label = "Product Type", value = application.product_type)
                            DetailItem(
                                label = "Loan Amount",
                                value = "₦ ${String.format(Locale.US, "%,.2f", application.amount)}"
                            )
                            DetailItem(label = "Tenure", value = "${application.tenure} months")
                            DetailItem(label = "Interest Rate", value = "${application.interest_rate}% p.a.")
                            DetailItem(label = "Repayment Frequency", value = application.repayment_frequency)
                            DetailItem(label = "Application Status", value = application.status)
                        }

                        // DTI Calculator Card
                        FieldCard {
                            Text(
                                text = "DEBT-TO-INCOME (DTI) EVALUATION",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DTI Ratio Calculator",
                                    style = FieldTheme.typography.bodyStrong,
                                    color = FieldTheme.colors.gray300
                                )
                                Text(
                                    text = "${(dtiRatio * 100).toInt()}%",
                                    style = FieldTheme.typography.mono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                    color = if (isDtiLimitExceeded) FieldTheme.colors.statusDanger else FieldTheme.colors.statusSuccess
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Slider(
                                value = dtiRatio,
                                onValueChange = { dtiRatio = it },
                                valueRange = 0.1f..0.7f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isDtiLimitExceeded) FieldTheme.colors.statusDanger else FieldTheme.colors.purple600,
                                    activeTrackColor = if (isDtiLimitExceeded) FieldTheme.colors.statusDanger else FieldTheme.colors.purple600,
                                    inactiveTrackColor = FieldTheme.colors.gray800
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            ConfidenceBar(percentage = 1f - dtiRatio) // Lower ratio = higher confidence bar
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (isDtiLimitExceeded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FieldTheme.colors.statusDanger.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, FieldTheme.colors.statusDanger, RoundedCornerShape(4.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = FieldIcons.AlertOutlined,
                                            contentDescription = "Warning",
                                            tint = FieldTheme.colors.statusDanger,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CRITICAL LIMIT: DTI exceeds the maximum permissible limit of 40% per annum.",
                                            style = FieldTheme.typography.body.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = FieldTheme.colors.statusDanger
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Mainstreet MMFB maximum permissible DTI ratio is 40.0% per annum.",
                                    style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.gray500
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Affordability Table
                            Text(
                                text = "AFFORDABILITY TABLE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val monthlyIncome = application.amount / application.tenure.coerceAtLeast(1)
                            val monthlyInstalment = monthlyIncome * (1 + application.interest_rate / 100 / 12)
                            val estimatedExpenses = monthlyIncome * 0.30
                            val netDisposable = monthlyIncome - estimatedExpenses - monthlyInstalment
                            val affordabilityRows = listOf(
                                Triple("Monthly Income (Est.)", "₦ ${String.format(Locale.US, "%,.0f", monthlyIncome * 3)}", FieldTheme.colors.statusSuccess),
                                Triple("Monthly Instalment", "₦ ${String.format(Locale.US, "%,.0f", monthlyInstalment)}", FieldTheme.colors.statusWarning),
                                Triple("Est. Living Expenses (30%)", "₦ ${String.format(Locale.US, "%,.0f", estimatedExpenses * 3)}", FieldTheme.colors.gray400),
                                Triple("Net Disposable Income", "₦ ${String.format(Locale.US, "%,.0f", netDisposable * 3)}", if (netDisposable > 0) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusDanger)
                            )
                            affordabilityRows.forEachIndexed { idx, (label, value, color) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (idx % 2 == 0) FieldTheme.colors.gray900 else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray400
                                    )
                                    Text(
                                        text = value,
                                        style = FieldTheme.typography.mono.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                        color = color
                                    )
                                }
                            }
                        }
                        
                        // Bureau Pull Card
                        FieldCard {
                            Text(
                                text = "EXTERNAL CREDIT REGISTRY BUREAU",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Select Bureau Credit Assessment",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.gray300
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Horizontal chip choices for Bureau scores
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                creditScores.forEachIndexed { index, item ->
                                    val isSelected = index == creditScoreIndex
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) FieldTheme.colors.purple900.copy(alpha = 0.2f) else FieldTheme.colors.gray900,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                if (isSelected) FieldTheme.colors.purple600 else FieldTheme.colors.gray800,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable { creditScoreIndex = index }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.first.split(" ")[0],
                                            style = FieldTheme.typography.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = if (isSelected) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DetailItem(label = "Selected Score Tier", value = creditScores[creditScoreIndex].first)
                                StatusChip(variant = creditScores[creditScoreIndex].second)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailItem(label = "Income Statement Verification", value = incomeStatement)
                        }

                        // Guarantor Matrix Card
                        FieldCard {
                            Text(
                                text = "GUARANTOR STRENGTH MATRIX",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Matrix header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Guarantor",
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.gray400,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = "BVN Match",
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.gray400,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Risk Level",
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.gray400,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            
                            // Matrix rows
                            val guarantors = listOf(
                                Triple(borrower?.guarantor_name ?: "Tunde Bakare", "Matched", StatusChipVariant.Verified),
                                Triple("Adaeze Okonkwo", "High Confidence", StatusChipVariant.Approved)
                            )
                            
                            guarantors.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.first,
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray300,
                                        modifier = Modifier.weight(1.5f)
                                    )
                                    Text(
                                        text = item.second,
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.statusSuccess,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusChip(
                                        variant = item.third,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                if (index < guarantors.size - 1) {
                                    FieldDivider()
                                }
                            }
                        }

                        // Document Verification Checklist
                        FieldCard {
                            Text(
                                text = "DOCUMENT VERIFICATION CHECKLIST",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val docChecklist = remember {
                                mutableStateListOf(
                                    Pair("National ID / International Passport", true),
                                    Pair("Utility Bill (Not older than 3 months)", true),
                                    Pair("Bank Statement (6 months)", true),
                                    Pair("Business Registration / CAC Certificate", false),
                                    Pair("Guarantor ID Document", true),
                                    Pair("Signed Loan Application Form", true),
                                    Pair("Passport Photograph", true),
                                    Pair("Collateral Documentation", false)
                                )
                            }

                            docChecklist.forEachIndexed { index, (docName, isVerified) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { docChecklist[index] = Pair(docName, !isVerified) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isVerified) FieldIcons.CheckCircleOutlined else FieldIcons.AlertOutlined,
                                        contentDescription = null,
                                        tint = if (isVerified) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = docName,
                                        style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                        color = if (isVerified) FieldTheme.colors.gray300 else FieldTheme.colors.gray500,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusChip(variant = if (isVerified) StatusChipVariant.Verified else StatusChipVariant.Missing)
                                }
                                if (index < docChecklist.size - 1) FieldDivider()
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            val missingCount = docChecklist.count { !it.second }
                            if (missingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FieldTheme.colors.statusWarning.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, FieldTheme.colors.statusWarning.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "$missingCount document(s) still pending. Tap to mark as verified.",
                                        style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                        color = FieldTheme.colors.statusWarning
                                    )
                                }
                            }
                        }

                        // OCR Exceptions Table
                        FieldCard {
                            Text(
                                text = "OCR CONFIDENCE EXCEPTIONS",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Document", style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(2f))
                                Text("Confidence", style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1f))
                                Text("Action", style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1f))
                            }

                            val ocrExceptions = listOf(
                                Triple("CAC Certificate", "42%", StatusChipVariant.Missing),
                                Triple("Collateral Doc", "61%", StatusChipVariant.NeedsReview)
                            )

                            if (ocrExceptions.isEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No OCR exceptions — all documents passed confidence threshold.",
                                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.statusSuccess,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                ocrExceptions.forEachIndexed { index, (docName, confidence, variant) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = docName,
                                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.gray300,
                                            modifier = Modifier.weight(2f)
                                        )
                                        Text(
                                            text = confidence,
                                            style = FieldTheme.typography.mono.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                            color = FieldTheme.colors.statusDanger,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(modifier = Modifier.weight(1f)) {
                                            StatusChip(variant = variant)
                                        }
                                    }
                                    if (index < ocrExceptions.size - 1) FieldDivider()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Low-confidence documents require manual verification or re-upload before approval.",
                                    style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.gray500
                                )
                            }
                        }

                        // Decision Card
                        FieldCard {
                            Text(
                                text = "UNDERWRITING DECISION VERDICT",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            FieldDropdown(
                                value = recommendationDecision,
                                options = listOf(
                                    "Recommend Approval",
                                    "Recommend Rejection",
                                    "Return for Correction"
                                ),
                                onOptionSelected = { recommendationDecision = it },
                                label = "Recommendation Decision"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            FieldTextField(
                                value = recommendationNotes,
                                onValueChange = { recommendationNotes = it },
                                label = "Underwriter Review Notes",
                                isRequired = true
                            )
                        }
                        
                        }
                    }
                }
            }
        }
    }
}
