package com.fieldcrm.android.ui.screens.application

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
import com.fieldcrm.android.ui.viewmodel.ConfigViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

private data class PledgeItem(
    val name: String = "",
    val qty: String = "1",
    val description: String = "",
    val value: String = ""
)

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
        onExecute = { witnessName, totalCollateralValue ->
            applicationViewModel.executePledge(
                id = application.id,
                witnessName = witnessName,
                collateralValue = totalCollateralValue,
                onSuccess = onSignComplete
            )
        }
    )
}

@Composable
fun PledgeTrustContent(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    onBackClick: () -> Unit,
    onExecute: (witnessName: String, totalCollateralValue: Double) -> Unit
) {
    val context = LocalContext.current
    var selectedAlternativeDocName by remember { mutableStateOf<String?>(null) }
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "document"
            } ?: "document"
            selectedAlternativeDocName = name
        }
    }

    val configViewModel: ConfigViewModel = koinViewModel()
    val configState by configViewModel.uiState.collectAsState()
    val pledgeFormCode = configState.config?.pledge_form_code ?: "MMFB/CRM/02"

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val pledgeAmountFigs = application.amount
    val pledgeAmountWords = remember(pledgeAmountFigs) { numberToWords(pledgeAmountFigs) }

    var pledgeLocation by remember { mutableStateOf("") }
    var pledgeObligor by remember { mutableStateOf(borrower?.name ?: "") }

    var pledgeItems by remember {
        mutableStateOf(listOf(PledgeItem(name = "", qty = "1", description = "", value = "")))
    }

    val totalPledgeValue = pledgeItems.sumOf { it.value.toDoubleOrNull() ?: 0.0 }

    var isLegalAcknowledged by remember { mutableStateOf(false) }
    var witnessName by remember { mutableStateOf("") }
    var witnessAddress by remember { mutableStateOf("") }

    val isFormValid = witnessName.isNotEmpty() &&
        pledgeLocation.isNotEmpty() &&
        pledgeItems.any { it.name.isNotEmpty() && (it.value.toDoubleOrNull() ?: 0.0) > 0 } &&
        isLegalAcknowledged

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Pledge & Trust Receipt",
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
                            text = "Legal Pledge & Trust Receipt — $pledgeFormCode",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )

                        // Auto-filled pledge header
                        FieldCard {
                            Text(
                                text = "FACILITY DETAILS",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DetailItem(label = "Date", value = today)
                            DetailItem(
                                label = "Borrower / Association Name",
                                value = borrower?.name ?: ""
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Facility amount — auto-calculated read-only
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(6.dp))
                                    .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(6.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "FACILITY AMOUNT IN FIGURES",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₦ ${String.format(Locale.US, "%,.2f", pledgeAmountFigs)}",
                                        style = FieldTheme.typography.mono.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = FieldTheme.colors.purple200
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(6.dp))
                                    .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(6.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "FACILITY AMOUNT IN WORDS",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pledgeAmountWords,
                                        style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                        color = FieldTheme.colors.gray300
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FieldTextField(
                                value = pledgeLocation,
                                onValueChange = { pledgeLocation = it },
                                label = "Shop / House Address (Where Goods Are Located)",
                                isRequired = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = pledgeObligor,
                                onValueChange = { pledgeObligor = it },
                                label = "Name of Obligor",
                                isRequired = true
                            )
                        }

                        // Pledged Item Schedule
                        FieldCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PLEDGED ITEM SCHEDULE",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // OCR Trigger Button
                                    var ocrScanning by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { ocrScanning = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                FieldTheme.colors.purple900.copy(alpha = 0.15f),
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.CameraOutlined,
                                            contentDescription = "Scan & OCR Extract",
                                            tint = FieldTheme.colors.purple400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            pledgeItems = pledgeItems + PledgeItem()
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                FieldTheme.colors.purple900.copy(alpha = 0.15f),
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.AddOutlined,
                                            contentDescription = "Add Item",
                                            tint = FieldTheme.colors.purple400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Upload alternative
                            var showUploadAlternative by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(6.dp))
                                    .clickable { showUploadAlternative = !showUploadAlternative }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = FieldIcons.DocumentOutlined,
                                    contentDescription = null,
                                    tint = FieldTheme.colors.purple400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (showUploadAlternative) "Hide upload option" else "Or upload collateral document instead",
                                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.purple400
                                )
                            }
                            if (showUploadAlternative) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FieldTheme.colors.gray900, RoundedCornerShape(6.dp))
                                        .border(0.5.dp, FieldTheme.colors.gray700.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = FieldIcons.DocumentOutlined,
                                            contentDescription = null,
                                            tint = if (selectedAlternativeDocName != null) FieldTheme.colors.purple400 else FieldTheme.colors.gray600,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = selectedAlternativeDocName?.let { "Selected file: $it" } ?: "Tap to upload collateral valuation report or inventory document",
                                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                            color = if (selectedAlternativeDocName != null) FieldTheme.colors.gray100 else FieldTheme.colors.gray500,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        SecondaryButton(
                                            text = if (selectedAlternativeDocName != null) "Change Document" else "Select Document",
                                            onClick = { documentPickerLauncher.launch("*/*") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Item", style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1.5f))
                                Text("Description", style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1.8f))
                                Text("Qty", style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(0.7f))
                                Text("Est. Value (₦)", style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1.5f))
                                Spacer(modifier = Modifier.width(28.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            pledgeItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        FieldTextField(
                                            value = item.name,
                                            onValueChange = { v ->
                                                pledgeItems = pledgeItems.toMutableList().also { list ->
                                                    list[index] = list[index].copy(name = v)
                                                }
                                            },
                                            label = "Item ${index + 1}",
                                            isRequired = true
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1.8f)) {
                                        FieldTextField(
                                            value = item.description,
                                            onValueChange = { v ->
                                                pledgeItems = pledgeItems.toMutableList().also { list ->
                                                    list[index] = list[index].copy(description = v)
                                                }
                                            },
                                            label = "Description"
                                        )
                                    }
                                    Column(modifier = Modifier.weight(0.7f)) {
                                        FieldTextField(
                                            value = item.qty,
                                            onValueChange = { v ->
                                                pledgeItems = pledgeItems.toMutableList().also { list ->
                                                    list[index] = list[index].copy(qty = v.filter { it.isDigit() })
                                                }
                                            },
                                            label = "Qty"
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        FieldAmountField(
                                            value = item.value,
                                            onValueChange = { v ->
                                                pledgeItems = pledgeItems.toMutableList().also { list ->
                                                    list[index] = list[index].copy(value = v)
                                                }
                                            },
                                            label = "Value"
                                        )
                                    }
                                    if (pledgeItems.size > 1) {
                                        IconButton(
                                            onClick = {
                                                pledgeItems = pledgeItems.toMutableList().also { it.removeAt(index) }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = FieldIcons.DeleteOutlined,
                                                contentDescription = "Remove",
                                                tint = FieldTheme.colors.statusDanger,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(28.dp))
                                    }
                                }
                                if (index < pledgeItems.size - 1) {
                                    FieldDivider()
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Auto-calculated total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL ESTIMATED VALUE",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray400
                                )
                                Text(
                                    text = "₦ ${String.format(Locale.US, "%,.2f", totalPledgeValue)}",
                                    style = FieldTheme.typography.mono.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (totalPledgeValue >= pledgeAmountFigs) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusWarning
                                )
                            }
                            if (totalPledgeValue > 0 && totalPledgeValue < pledgeAmountFigs) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Collateral value is below facility amount. Add more items to cover the pledge.",
                                    style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.statusWarning
                                )
                            }
                        }

                        // Legal Acknowledgement
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
                                Text(
                                    text = "${borrower?.name ?: "Borrower"} acknowledges the above pledge schedule and affirms that all goods are free from prior encumbrance.",
                                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.gray300,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = isLegalAcknowledged,
                                    onCheckedChange = { isLegalAcknowledged = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FieldTheme.colors.purple600,
                                        uncheckedColor = FieldTheme.colors.gray700
                                    )
                                )
                            }
                        }

                        // Witness Details
                        FieldCard {
                            Text(
                                text = "WITNESS ATTESTATION DETAILS",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldTextField(
                                value = witnessName,
                                onValueChange = { witnessName = it },
                                label = "Witness Full Name",
                                isRequired = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = witnessAddress,
                                onValueChange = { witnessAddress = it },
                                label = "Witness Address"
                            )
                        }

                        // Borrower Signature Pad
                        FieldCard {
                            Text(
                                text = "BORROWER SIGNATURE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldSignaturePad(
                                onConfirm = {},
                                onClear = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }

                        // Witness Signature Pad
                        FieldCard {
                            Text(
                                text = "WITNESS SIGNATURE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldSignaturePad(
                                onConfirm = {},
                                onClear = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PrimaryButton(
                            text = "Execute Pledge & Trust Receipt",
                            onClick = { onExecute(witnessName, totalPledgeValue) },
                            enabled = isFormValid
                        )
                    }
                }
            }
        }
    }
}

private fun numberToWords(amount: Double): String {
    val n = amount.toLong()
    if (n == 0L) return "Zero Naira Only"

    val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )
    val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    fun convert(num: Long): String = when {
        num < 20 -> units[num.toInt()]
        num < 100 -> tens[(num / 10).toInt()] + if (num % 10 != 0L) " ${units[(num % 10).toInt()]}" else ""
        num < 1000 -> "${units[(num / 100).toInt()]} Hundred${if (num % 100 != 0L) " and ${convert(num % 100)}" else ""}"
        num < 1_000_000 -> "${convert(num / 1000)} Thousand${if (num % 1000 != 0L) " ${convert(num % 1000)}" else ""}"
        num < 1_000_000_000 -> "${convert(num / 1_000_000)} Million${if (num % 1_000_000 != 0L) " ${convert(num % 1_000_000)}" else ""}"
        else -> "${convert(num / 1_000_000_000)} Billion${if (num % 1_000_000_000 != 0L) " ${convert(num % 1_000_000_000)}" else ""}"
    }

    return "${convert(n)} Naira Only"
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
        PledgeTrustContent(application = demoApp, borrower = demoBorrower, onBackClick = {}, onExecute = { _, _ -> })
    }
}
