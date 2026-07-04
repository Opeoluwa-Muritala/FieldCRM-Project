package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
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
import com.fieldcrm.android.data.api.PaymentRecord
import com.fieldcrm.android.data.api.RepaymentScheduleRow
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldTheme

/**
 * Repayment schedule screen — shows installment table with Paid / Partial / Due
 * status per row, and full payment history below.
 */
@Composable
fun RepaymentScheduleScreen(
    applicantName: String,
    refNo: String,
    schedule: List<RepaymentScheduleRow>,
    payments: List<PaymentRecord>,
    totalDue: Double,
    totalPaid: Double,
    outstanding: Double,
    classification: String = "current",
    daysPastDue: Int = 0,
    canRecordPayment: Boolean = false,
    onRecordPayment: () -> Unit = {},
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            FieldTopAppBar(title = "Repayment Schedule", onBackClick = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary header
            SectionCard(title = "$applicantName — $refNo") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMini("Total Due",  "₦%,.2f".format(totalDue),  FieldTheme.colors.gray100)
                    MetricMini("Total Paid", "₦%,.2f".format(totalPaid), Color(0xFF2E7D32))
                    MetricMini(
                        "Outstanding",
                        "₦%,.2f".format(outstanding),
                        if (outstanding > 0) FieldTheme.colors.statusDanger else Color(0xFF2E7D32)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusChip(
                        label = classification.uppercase(),
                        isPositive = classification == "current"
                    )
                    if (daysPastDue > 0) {
                        Text(
                            "$daysPastDue days overdue",
                            color = FieldTheme.colors.statusDanger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (canRecordPayment) {
                PrimaryButton(
                    text = "Record Payment",
                    onClick = onRecordPayment,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Installment table
            if (schedule.isNotEmpty()) {
                SectionCard(title = "Repayment Schedule") {
                    // Header row
                    ScheduleHeaderRow()
                    HorizontalDivider()

                    var cumulative = 0.0
                    schedule.forEach { row ->
                        cumulative += row.total_due
                        val rowStatus = when {
                            totalPaid >= cumulative              -> "paid"
                            totalPaid > (cumulative - row.total_due) -> "partial"
                            else                                 -> "due"
                        }
                        ScheduleRow(row, rowStatus)
                        HorizontalDivider(color = FieldTheme.colors.gray800)
                    }
                }
            } else {
                SectionCard(title = "Repayment Schedule") {
                    Text(
                        "No schedule generated yet.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Payment history
            if (payments.isNotEmpty()) {
                SectionCard(title = "Payment History") {
                    payments.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(p.payment_date, style = FieldTheme.typography.body, fontWeight = FontWeight.Medium)
                                Text(
                                    p.channel.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray400
                                )
                            }
                            Text(
                                "₦%,.2f".format(p.amount_paid),
                                style = FieldTheme.typography.body,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        HorizontalDivider(color = FieldTheme.colors.gray800)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeaderRow() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("#", "Due Date", "Principal", "Interest", "Total", "Status").forEach { col ->
            Text(
                col,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = FieldTheme.colors.gray400,
                modifier = Modifier.weight(if (col == "Due Date") 2f else 1f)
            )
        }
    }
}

@Composable
private fun ScheduleRow(row: RepaymentScheduleRow, status: String) {
    val bg = when (status) {
        "paid"    -> Color(0xFFF1F8E9)
        "partial" -> Color(0xFFFFF8E1)
        else      -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().background(bg).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${row.installment_no}", fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(row.due_date, fontSize = 11.sp, modifier = Modifier.weight(2f))
        Text("₦%,.0f".format(row.principal_due), fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("₦%,.0f".format(row.interest_due),  fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("₦%,.0f".format(row.total_due),      fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(Modifier.weight(1f)) {
            StatusChip(
                label = status.replaceFirstChar { it.uppercase() },
                isPositive = status == "paid",
                small = true
            )
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
        Text(label, fontSize = 11.sp, color = FieldTheme.colors.gray400)
    }
}
