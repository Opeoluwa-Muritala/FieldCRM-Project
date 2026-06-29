package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
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
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@Composable
fun PledgeTrustScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onSignComplete: () -> Unit
) {
    PledgeTrustContent(
        application = application,
        borrower = borrower,
        onBackClick = onBackClick,
        onExecute = { witnessName ->
            val updatedApp = application.copy(
                collateral_desc = "Pledge & Trust Receipt Executed (Witness: $witnessName)",
                collateral_value = application.amount * 1.5
            )
            applicationViewModel.updateApplicationLocal(updatedApp) {
                onSignComplete()
            }
        }
    )
}

@Composable
fun PledgeTrustContent(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    onBackClick: () -> Unit,
    onExecute: (witnessName: String) -> Unit
) {
    var witnessName by remember { mutableStateOf("") }
    var witnessBvn by remember { mutableStateOf("") }
    var isTermsAccepted by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }

    val isFormValid = witnessName.isNotEmpty() && witnessBvn.isNotEmpty() && isTermsAccepted

    if (showCameraScanner) {
        CameraOcrScanner(
            onTextScanned = { text ->
                // Parse witness name from first non-empty scanned line
                val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    witnessName = lines.first()
                }
                // Parse BVN reference (find 9 to 11 contiguous digits)
                val bvnRegex = "\\b\\d{9,11}\\b".toRegex()
                val match = bvnRegex.find(text)
                if (match != null) {
                    witnessBvn = match.value
                }
                showCameraScanner = false
            },
            onDismiss = { showCameraScanner = false }
        )
    } else {
        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Pledge & Trust Receipt",
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = FieldTheme.colors.gray400
                            )
                        }
                    }
                )
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
                            Text(
                                text = "Legal Pledge & Trust Receipt Agreement",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            
                            // Schedule Table
                            FieldCard {
                                Text(
                                    text = "AMORTISATION & REPAYMENT TERMS",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Amortisation header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Instalment No.", style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1f))
                                    Text("Due Date", style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1.5f))
                                    Text("Amount", style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1f))
                                }
                                
                                // Dynamically generate instalments based on application details
                                val tenor = if (application.tenure > 0) application.tenure else 4
                                val monthlyPayment = if (tenor > 0) application.amount / tenor else 0.0
                                val instalments = (1..tenor).map { month ->
                                    val calendar = java.util.Calendar.getInstance()
                                    calendar.add(java.util.Calendar.MONTH, month)
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                                    Triple("Month $month", dateStr, "₦" + String.format(Locale.US, "%,.0f", monthlyPayment))
                                }
                                
                                instalments.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.first, style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray300, modifier = Modifier.weight(1f))
                                        Text(item.second, style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray300, modifier = Modifier.weight(1.5f))
                                        Text(item.third, style = FieldTheme.typography.mono.copy(fontSize = 12.sp), color = FieldTheme.colors.purple200, modifier = Modifier.weight(1f))
                                    }
                                    if (index < instalments.size - 1) {
                                        FieldDivider()
                                    }
                                }
                            }

                            // Agreement Attestation Checkbox Card
                            FieldCard {
                                Text(
                                    text = "LEGAL ACCEPTANCE",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val borrowerName = borrower?.name ?: "Borrower"
                                    Text(
                                        text = "$borrowerName accepts amortization schedule & pledges specified collateral inventory goods.",
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray300,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = isTermsAccepted,
                                        onCheckedChange = { isTermsAccepted = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = FieldTheme.colors.purple600,
                                            uncheckedColor = FieldTheme.colors.gray700
                                        )
                                    )
                                }
                            }
                            
                            // Witness Fields Card
                            FieldCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "WITNESS ATTESTATION DETAILS",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    TextButton(
                                        onClick = { showCameraScanner = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.CameraOutlined,
                                            contentDescription = "Scan Witness ID",
                                            modifier = Modifier.size(16.dp),
                                            tint = FieldTheme.colors.purple400
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Scan ID",
                                            style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.purple400
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                FieldTextField(
                                    value = witnessName,
                                    onValueChange = { witnessName = it },
                                    label = "Witness Full Name",
                                    isRequired = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = witnessBvn,
                                    onValueChange = { witnessBvn = it },
                                    label = "Witness BVN Reference",
                                    isRequired = true
                                )
                            }

                            // Signature Pad Card
                            FieldCard {
                                Text(
                                    text = "BORROWER SIGNATURE PAD",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldSignaturePad(
                                    onConfirm = {},
                                    onClear = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            PrimaryButton(
                                text = "Execute Legal Agreement",
                                onClick = { onExecute(witnessName) },
                                enabled = isFormValid
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Compact Phone Pledge Agreement", widthDp = 411, heightDp = 850)
@Composable
fun PreviewPledgeCompact() {
    val demoApp = LoanApplicationModel(
        id = "app_1", org_id = "org_1", borrower_id = "1",
        current_stage = 1, current_owner_id = "LO_1", status = "intake",
        amount = 180000.0, tenure = 4, product_type = "Working Capital",
        interest_rate = 18.5, repayment_frequency = "MONTHLY", created_at = ""
    )
    val demoBorrower = BorrowerModel(
        id = "1", org_id = "org_1", loan_officer_id = "LO_1",
        name = "Adaeze Okonkwo", phone = "08012345678", bvn = "222333444", nin = "111222333",
        status = "Active", created_at = ""
    )
    FieldCRMTheme {
        PledgeTrustContent(application = demoApp, borrower = demoBorrower, onBackClick = {}, onExecute = {})
    }
}
