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
import com.fieldcrm.android.data.api.ParSummary
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldTheme

/**
 * PAR (Portfolio at Risk) dashboard screen.
 * Shows CBN classification breakdown and full active loan portfolio.
 */
@Composable
fun ParDashboardScreen(
    par: ParSummary?,
    loans: List<Map<String, Any>> = emptyList(),
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onOpenSchedule: (loanId: String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            FieldTopAppBar(title = "Portfolio at Risk (PAR)", onBackClick = onBack)
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FieldTheme.colors.purple700)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (par != null) {
                // PAR metric cards
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParMetricCard(
                        label = "Total Loans",
                        value = "${par.total_loans}",
                        sub = "₦%,.0f".format(par.total_portfolio),
                        bg = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    ParMetricCard(
                        label = "PAR-1",
                        value = "${par.par1_pct}%",
                        sub = "${par.par1_count} loans",
                        bg = Color(0xFFE8F5E9),
                        valueColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParMetricCard(
                        label = "PAR-30",
                        value = "${par.par30_pct}%",
                        sub = "${par.par30_count} loans",
                        bg = Color(0xFFFFF8E1),
                        valueColor = Color(0xFFF57F17),
                        modifier = Modifier.weight(1f)
                    )
                    ParMetricCard(
                        label = "PAR-90",
                        value = "${par.par90_pct}%",
                        sub = "${par.par90_count} loans",
                        bg = Color(0xFFFCE4EC),
                        valueColor = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )
                }

                // CBN Classification table
                SectionCard(title = "CBN Classification Breakdown") {
                    val currentCount = par.total_loans - par.olem_count -
                            par.substandard_count - par.doubtful_count - par.lost_count

                    ClassificationRow("CURRENT",     "0",       "1%",   currentCount,        isGood = true)
                    ClassificationRow("OLEM",        "1–30",    "5%",   par.olem_count)
                    ClassificationRow("SUBSTANDARD", "31–90",   "20%",  par.substandard_count)
                    ClassificationRow("DOUBTFUL",    "91–180",  "50%",  par.doubtful_count)
                    ClassificationRow("LOST",        "181+",    "100%", par.lost_count,       isDanger = true)
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "No disbursed loans found for this organisation.",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = FieldTheme.colors.gray400
                    )
                }
            }

            // Active loan portfolio
            if (loans.isNotEmpty()) {
                SectionCard(title = "Active Loan Portfolio") {
                    // Header
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        listOf("Borrower", "Amount", "Class", "DPD").forEach { h ->
                            Text(h, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                color = FieldTheme.colors.gray400,
                                modifier = Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider()
                    loans.forEach { loan ->
                        val classification = loan["classification"] as? String ?: "current"
                        val dpd = (loan["days_past_due"] as? Number)?.toInt() ?: 0
                        val amount = (loan["disbursed_amount"] as? Number)?.toDouble()
                        val loanId = loan["id"] as? String ?: ""
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                loan["applicant_name"] as? String ?: "—",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                amount?.let { "₦%,.0f".format(it) } ?: "—",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(Modifier.weight(1f)) {
                                StatusChip(
                                    label = classification.uppercase(),
                                    isPositive = classification == "current",
                                    small = true
                                )
                            }
                            Text("$dpd", fontSize = 12.sp, modifier = Modifier.weight(1f),
                                color = if (dpd > 0) FieldTheme.colors.statusDanger else FieldTheme.colors.gray100)
                        }
                        HorizontalDivider(color = FieldTheme.colors.gray800)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParMetricCard(
    label: String,
    value: String,
    sub: String,
    bg: Color,
    valueColor: Color = FieldTheme.colors.gray100,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, fontSize = 11.sp, color = FieldTheme.colors.gray400)
            Text(sub, fontSize = 11.sp, color = FieldTheme.colors.gray400)
        }
    }
}

@Composable
private fun ClassificationRow(
    name: String,
    daysRange: String,
    provision: String,
    count: Int,
    isGood: Boolean = false,
    isDanger: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(label = name, isPositive = isGood, isDanger = isDanger, modifier = Modifier.weight(2f))
        Text(daysRange,  fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(provision,  fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("$count",   fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
    HorizontalDivider(color = FieldTheme.colors.gray800)
}
