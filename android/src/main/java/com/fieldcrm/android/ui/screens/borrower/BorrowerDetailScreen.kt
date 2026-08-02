package com.fieldcrm.android.ui.screens.borrower

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerDetailScreenView(
    borrower: BorrowerModel,
    applications: List<LoanApplicationModel>,
    role: UserRole?,
    onBackClick: () -> Unit,
    onViewApplication: (String) -> Unit,
    onCreateApplication: () -> Unit
) {
    val initials = remember(borrower.name) {
        borrower.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    }
    
    val isActive = borrower.status.lowercase(Locale.getDefault()) == "active"
    val isAuthorizedToEdit = role == UserRole.LOAN_OFFICER || role == UserRole.SYSTEM_ADMIN // Edit allowed for Loan Officer and Admin

    // Filter applications belonging to this borrower
    val borrowerApps = remember(applications, borrower) {
        applications.filter {
            it.bvn == borrower.bvn || it.phone == borrower.phone || it.applicant_name.equals(borrower.name, ignoreCase = true)
        }
    }

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
                    if (isAuthorizedToEdit) {
                        IconButton(onClick = { /* Edit Action */ }) {
                            Icon(
                                imageVector = FieldIcons.PenOutlined,
                                contentDescription = "Edit Profile",
                                tint = FieldTheme.colors.gray400
                            )
                        }
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
            // Z2: Summary Band (high-impact profile header)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldTheme.colors.purple900.copy(alpha = 0.08f))
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = if (isActive) FieldTheme.colors.purple600.copy(alpha = 0.15f) else FieldTheme.colors.gray800,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isActive) FieldTheme.colors.purple600 else FieldTheme.colors.gray700,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = FieldTheme.typography.title.copy(fontSize = 28.sp),
                            color = if (isActive) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = borrower.name,
                        style = FieldTheme.typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = FieldTheme.colors.gray100
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = borrower.phone,
                        style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                        color = FieldTheme.colors.gray400
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    StatusChip(
                        variant = if (isActive) StatusChipVariant.Verified else StatusChipVariant.NeedsReview
                    )
                }
            }

            // Z4 Content: Applications
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        text = "LOAN APPLICATIONS",
                        style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (borrowerApps.isEmpty()) {
                        FieldCard {
                            Text(
                                text = "No active or historical loan applications found for this client profile.",
                                style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                color = FieldTheme.colors.gray500,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            items(borrowerApps) { app ->
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                    FieldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewApplication(app.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = app.ref_no.ifBlank { "Ref: ${app.id.take(8)}" },
                                    style = FieldTheme.typography.bodyStrong,
                                    color = FieldTheme.colors.gray100
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Amount: ₦${app.amount ?: 0.0}",
                                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.gray400
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = app.displayStatus,
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.purple400
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = FieldIcons.ChevronRightOutlined,
                                    contentDescription = "View details",
                                    tint = FieldTheme.colors.gray600,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Trust Credentials Card
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        text = "IDENTITY & TRUST METRICS",
                        style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FieldIcons.ShieldFilled,
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

            // Contact & Professional Profile Sections
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailSectionCard(
                        title = "Personal & Contact Profile",
                        items = listOf(
                            Triple("BVN Reference", borrower.bvn, true),
                            Triple("NIN Reference", borrower.nin, true),
                            Triple("Physical Address", borrower.physical_address ?: "Lagos LGA", false)
                        )
                    )

                    DetailSectionCard(
                        title = "Employment & Income Profile",
                        items = listOf(
                            Triple("Employment Status", borrower.employment_status ?: "Self Employed", false),
                            Triple("Employer/Business Name", borrower.employer_name ?: "Private Retail", false),
                            Triple("Monthly Net Income", "₦ ${borrower.monthly_income ?: 0.0}", true)
                        )
                    )

                    DetailSectionCard(
                        title = "Emergency Contact & Guarantee",
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
                .size(52.dp)
                .background(
                    color = FieldTheme.colors.gray850,
                    shape = CircleShape
                )
                .border(
                    width = 0.5.dp,
                    color = FieldTheme.colors.gray800,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = FieldTheme.colors.gray100,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = FieldTheme.typography.label, color = FieldTheme.colors.gray400)
    }
}

@Composable
fun DetailSectionCard(title: String, items: List<Triple<String, String, Boolean>>) {
    FieldCard {
        Text(
            text = title,
            style = FieldTheme.typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
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
