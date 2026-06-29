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
fun PledgeTrustScreen(
    onBackClick: () -> Unit,
    onSignComplete: () -> Unit
) {
    var witnessName by remember { mutableStateOf("Samuel Adebayo") }
    var witnessBvn by remember { mutableStateOf("333444555") }

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
                            
                            // Rows
                            val instalments = listOf(
                                Triple("Month 1", "2026-07-18", "₦47,500"),
                                Triple("Month 2", "2026-08-18", "₦47,500"),
                                Triple("Month 3", "2026-09-18", "₦47,500"),
                                Triple("Month 4", "2026-10-18", "₦47,500")
                            )
                            
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
                        
                        // Witness Fields Card
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
                            onClick = onSignComplete
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

@Preview(name = "Compact Phone Pledge Agreement", widthDp = 411, heightDp = 850)
@Composable
fun PreviewPledgeCompact() {
    FieldCRMTheme {
        PledgeTrustScreen(onBackClick = {}, onSignComplete = {})
    }
}
