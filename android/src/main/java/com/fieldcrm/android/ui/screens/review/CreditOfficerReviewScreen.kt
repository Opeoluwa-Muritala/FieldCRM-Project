package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
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
import java.util.Locale

@Composable
fun CreditOfficerReviewScreen(
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

    val isDtiLimitExceeded = dtiRatio > 0.40f

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Risk Underwriting Center",
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
            val isWide = maxWidth >= 600.dp
            
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
                            text = "Credit Risk Verification Matrix",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )

                        Text(
                            text = "Verify applicant leverage metrics and guarantor signatures prior to manager recommendation.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        
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
                                            imageVector = Icons.Outlined.Warning,
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
                                Triple("Tunde Bakare", "Matched", StatusChipVariant.Verified),
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
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PrimaryButton(
                            text = "Submit Credit Evaluation",
                            onClick = onCompleteReview,
                            enabled = !isDtiLimitExceeded && creditScores[creditScoreIndex].second != StatusChipVariant.Missing
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Compact Phone Credit Review", widthDp = 411, heightDp = 850)
@Composable
fun PreviewCreditReviewCompact() {
    FieldCRMTheme {
        CreditOfficerReviewScreen(onBackClick = {}, onCompleteReview = {})
    }
}
