package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel
import java.util.Locale

@Composable
fun BorrowerDetailScreenView(
    borrower: BorrowerModel,
    onBackClick: () -> Unit,
    onCreateApplication: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = borrower.name,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    StatusChip(
                        variant = if (borrower.status.lowercase(Locale.getDefault()) == "active") {
                            StatusChipVariant.Verified
                        } else {
                            StatusChipVariant.NeedsReview
                        }
                    )
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
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "IDENTITY VERIFICATION CREDENTIALS",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.gray500
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ConfidenceBar(percentage = 0.96f)
                    }

                    // Card 1: Primary Profile Information
                    item {
                        FieldCard {
                            Text(
                                text = "Personal Profile",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Phone Number", value = borrower.phone)
                                    DetailItem(label = "BVN Reference (Mono)", value = borrower.bvn, isMono = true)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "NIN Reference (Mono)", value = borrower.nin, isMono = true)
                                    DetailItem(label = "Home Address", value = borrower.physical_address ?: "Lagos LGA")
                                }
                            }
                        }
                    }

                    // Card 2: Employment & Income Information
                    item {
                        FieldCard {
                            Text(
                                text = "Employment & Income Details",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Status", value = borrower.employment_status ?: "Self Employed")
                                    DetailItem(label = "Monthly Income Estimation", value = "₦ ${borrower.monthly_income ?: 0.0}", isMono = true)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Employer/Business", value = borrower.employer_name ?: "Private Trade")
                                }
                            }
                        }
                    }

                    // Card 3: Banking Account
                    item {
                        FieldCard {
                            Text(
                                text = "Banking Settlement Account",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Settlement Bank", value = borrower.bank_name ?: "Unknown Bank")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Account Number", value = borrower.account_number ?: "0000000000", isMono = true)
                                }
                            }
                        }
                    }
                    
                    // Card 4: Guarantor details
                    item {
                        FieldCard {
                            Text(
                                text = "Emergency Contact / Guarantor",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Guarantor Full Name", value = borrower.guarantor_name ?: "Unspecified")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Guarantor Phone", value = borrower.guarantor_phone ?: "Unspecified")
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Initiate New Loan Application",
                            onClick = onCreateApplication
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone Details", widthDp = 411, heightDp = 850)
@Composable
fun PreviewBorrowerDetailCompact() {
    val demoBorrower = BorrowerModel(
        id = "1",
        org_id = "org_1",
        loan_officer_id = "LO_1",
        name = "Adaeze Okonkwo",
        phone = "08012345678",
        bvn = "222333444",
        nin = "111222333",
        status = "Active",
        physical_address = "Ikeja, Lagos State",
        employment_status = "Micro Enterprise Owner",
        employer_name = "Self Employed Retail",
        monthly_income = 250000.0,
        bank_name = "Zenith Bank PLC",
        account_number = "1012345678",
        guarantor_name = "Emeka Chukwu",
        guarantor_phone = "08087654321",
        created_at = "2026-06-18"
    )

    FieldCRMTheme {
        BorrowerDetailScreenView(
            borrower = demoBorrower,
            onBackClick = {},
            onCreateApplication = {}
        )
    }
}
