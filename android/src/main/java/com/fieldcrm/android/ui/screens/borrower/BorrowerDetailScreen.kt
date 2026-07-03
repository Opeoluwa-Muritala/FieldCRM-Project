package com.fieldcrm.android.ui.screens.borrower

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.common.DetailItem
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
    val initials = remember(borrower.name) {
        borrower.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    }
    
    val isActive = borrower.status.lowercase(Locale.getDefault()) == "active"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Client Profile",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit Action */ }) {
                        Icon(
                            imageVector = FieldIcons.PenOutlined,
                            contentDescription = "Edit Profile",
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
                    .background(FieldTheme.colors.gray900)
                    .border(width = 0.5.dp, color = FieldTheme.colors.gray800)
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    text = "Initiate Loan Application",
                    onClick = onCreateApplication,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // High-Impact Profile Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                color = if (isActive) FieldTheme.colors.purple600.copy(alpha = 0.15f) else FieldTheme.colors.gray800,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(48.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = if (isActive) FieldTheme.colors.purple600 else FieldTheme.colors.gray700,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(48.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = FieldTheme.typography.title.copy(fontSize = 36.sp),
                            color = if (isActive) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = borrower.name,
                        style = FieldTheme.typography.title.copy(fontSize = 24.sp),
                        color = FieldTheme.colors.gray100
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    StatusChip(
                        variant = if (isActive) StatusChipVariant.Verified else StatusChipVariant.NeedsReview
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Quick Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionItem(icon = FieldIcons.PhoneOutlined, label = "Call")
                        QuickActionItem(icon = FieldIcons.CheckCircleOutlined, label = "Verify ID")
                        QuickActionItem(icon = FieldIcons.ShieldOutlined, label = "Documents")
                    }
                }
            }

            // Identity Trust Score
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                    Text(
                        text = "IDENTITY VERIFICATION CREDENTIALS",
                        style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FieldIcons.ShieldFilled, // Safe fallback
                                contentDescription = null,
                                tint = FieldTheme.colors.statusSuccess,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("High Confidence Profile", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
                                Spacer(modifier = Modifier.height(6.dp))
                                ConfidenceBar(percentage = 0.96f)
                            }
                        }
                    }
                }
            }

            // Structured Details Cards
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailSectionCard(
                        title = "Personal Profile",
                        items = listOf(
                            Triple("Phone Number", borrower.phone, false),
                            Triple("BVN Reference", borrower.bvn, true),
                            Triple("NIN Reference", borrower.nin, true),
                            Triple("Home Address", borrower.physical_address ?: "Lagos LGA", false)
                        )
                    )

                    DetailSectionCard(
                        title = "Employment & Income",
                        items = listOf(
                            Triple("Status", borrower.employment_status ?: "Self Employed", false),
                            Triple("Employer/Business", borrower.employer_name ?: "Private Trade", false),
                            Triple("Monthly Income", "₦ ${borrower.monthly_income ?: 0.0}", true)
                        )
                    )

                    DetailSectionCard(
                        title = "Banking Settlement",
                        items = listOf(
                            Triple("Bank Name", borrower.bank_name ?: "Unknown Bank", false),
                            Triple("Account Number", borrower.account_number ?: "0000000000", true)
                        )
                    )

                    DetailSectionCard(
                        title = "Emergency Contact",
                        items = listOf(
                            Triple("Guarantor Name", borrower.guarantor_name ?: "Unspecified", false),
                            Triple("Guarantor Phone", borrower.guarantor_phone ?: "Unspecified", false)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = FieldTheme.colors.gray800,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = FieldTheme.colors.gray700,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = FieldTheme.colors.gray100,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = FieldTheme.typography.label, color = FieldTheme.colors.gray400)
    }
}

@Composable
fun DetailSectionCard(title: String, items: List<Triple<String, String, Boolean>>) {
    FieldCard {
        Text(
            text = title,
            style = FieldTheme.typography.title,
            color = FieldTheme.colors.gray100
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        items.chunked(2).forEachIndexed { index, rowItems ->
            if (index > 0) Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { (label, value, isMono) ->
                    Column(modifier = Modifier.weight(1f)) {
                        DetailItem(label = label, value = value, isMono = isMono)
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
