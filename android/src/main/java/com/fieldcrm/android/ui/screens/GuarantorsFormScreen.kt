package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun GuarantorsFormScreen(
    onBackClick: () -> Unit,
    onSave: () -> Unit
) {
    var isGuarantor1Expanded by remember { mutableStateOf(true) }
    var isGuarantor2Expanded by remember { mutableStateOf(false) }

    var g1Name by remember { mutableStateOf("Tunde Bakare") }
    var g1Bvn by remember { mutableStateOf("22244455588") }
    var g1Phone by remember { mutableStateOf("08033344455") }

    var g2Name by remember { mutableStateOf("Adaeze Okonkwo") }
    var g2Bvn by remember { mutableStateOf("22233344499") }
    var g2Phone by remember { mutableStateOf("08099988877") }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Co-Guarantor Declarations",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                            text = "Guarantors Form",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        Text(
                            text = "Ensure each guarantor signature is physically matched and verified with state identity registries.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        
                        // Guarantor 1 Card
                        Column {
                            AccordionHeader(
                                title = "Guarantor 1: ${g1Name.ifEmpty { "New Profile" }}",
                                isExpanded = isGuarantor1Expanded,
                                onToggle = { isGuarantor1Expanded = !isGuarantor1Expanded }
                            )
                            if (isGuarantor1Expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Identity Parameters", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                        StatusChip(variant = StatusChipVariant.Verified)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    FieldTextField(
                                        value = g1Name,
                                        onValueChange = { g1Name = it },
                                        label = "Full Legal Name",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g1Phone,
                                        onValueChange = { g1Phone = it },
                                        label = "Primary Phone",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g1Bvn,
                                        onValueChange = { g1Bvn = it },
                                        label = "BVN Identifier Number",
                                        isRequired = true
                                    )
                                }
                            }
                        }

                        // Guarantor 2 Card
                        Column {
                            AccordionHeader(
                                title = "Guarantor 2: ${g2Name.ifEmpty { "New Profile" }}",
                                isExpanded = isGuarantor2Expanded,
                                onToggle = { isGuarantor2Expanded = !isGuarantor2Expanded }
                            )
                            if (isGuarantor2Expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Identity Parameters", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                        StatusChip(variant = StatusChipVariant.NeedsReview)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    FieldTextField(
                                        value = g2Name,
                                        onValueChange = { g2Name = it },
                                        label = "Full Legal Name",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g2Phone,
                                        onValueChange = { g2Phone = it },
                                        label = "Primary Phone",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g2Bvn,
                                        onValueChange = { g2Bvn = it },
                                        label = "BVN Identifier Number",
                                        isRequired = true
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        PrimaryButton(
                            text = "Save Guarantors Configuration",
                            onClick = onSave
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

@Preview(name = "Compact Phone Guarantors", widthDp = 411, heightDp = 850)
@Composable
fun PreviewGuarantorsCompact() {
    FieldCRMTheme {
        GuarantorsFormScreen(onBackClick = {}, onSave = {})
    }
}
