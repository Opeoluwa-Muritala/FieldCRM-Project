package com.fieldcrm.android.ui.screens

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun CreditOfficerReviewScreen(
    onBackClick: () -> Unit,
    onCompleteReview: () -> Unit
) {
    var creditScore by remember { mutableStateOf("740 (Excellent)") }
    var incomeStatement by remember { mutableStateOf("Verified Bank Statement") }
    var dtiRatio by remember { mutableFloatStateOf(0.32f) } // 32%

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Underwriting Analysis",
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
                        
                        // DTI Calculator Card
                        FieldCard {
                            Text("DEBT-TO-INCOME (DTI) EVALUATION", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("DTI Ratio", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                Text("${(dtiRatio * 100).toInt()}%", style = FieldTheme.typography.mono.copy(fontSize = 16.sp), color = FieldTheme.colors.purple200)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            ConfidenceBar(percentage = 1f - dtiRatio) // Lower ratio = higher confidence
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Lagos state MMFB maximum permissible DTI ratio is 40.0% per annum.",
                                style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                color = FieldTheme.colors.gray500
                            )
                        }
                        
                        // Bureau Pull Card
                        FieldCard {
                            Text("EXTERNAL CREDIT REGISTRY BUREAU", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DetailItem(label = "FirstCentral Score", value = creditScore)
                                StatusChip(variant = StatusChipVariant.Verified)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailItem(label = "Income Statement Verification", value = incomeStatement)
                        }

                        // Guarantor Matrix Card
                        FieldCard {
                            Text("GUARANTOR STRENGTH MATRIX", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Matrix header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Guarantor", style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1.5f))
                                Text("BVN Match", style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1f))
                                Text("Risk Level", style = FieldTheme.typography.bodyStrong.copy(fontSize = 11.sp), color = FieldTheme.colors.gray400, modifier = Modifier.weight(1.2f))
                            }
                            
                            // Matrix rows
                            val guarantors = listOf(
                                Pair("Tunde Bakare", "Matched"),
                                Pair("Adaeze Okonkwo", "High Confidence")
                            )
                            
                            guarantors.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.first, style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray300, modifier = Modifier.weight(1.5f))
                                    Text(item.second, style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.statusSuccess, modifier = Modifier.weight(1f))
                                    StatusChip(variant = StatusChipVariant.Verified, modifier = Modifier.weight(1.2f))
                                }
                                if (index < guarantors.size - 1) {
                                    FieldDivider()
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PrimaryButton(
                            text = "Submit Credit Evaluation",
                            onClick = onCompleteReview
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone Credit Review", widthDp = 411, heightDp = 850)
@Composable
fun PreviewCreditReviewCompact() {
    FieldCRMTheme {
        CreditOfficerReviewScreen(onBackClick = {}, onCompleteReview = {})
    }
}
