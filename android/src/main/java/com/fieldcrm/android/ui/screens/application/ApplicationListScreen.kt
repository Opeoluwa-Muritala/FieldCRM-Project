package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationUiState
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@Composable
fun ApplicationListScreenView(
    viewModel: ApplicationViewModel,
    borrowers: List<BorrowerModel>,
    onApplicationSelected: (LoanApplicationModel) -> Unit,
    onAddApplication: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterStage by remember { mutableStateOf("All Stages") }

    ApplicationListContent(
        isLoading = state.isLoading,
        applications = state.applications,
        borrowers = borrowers,
        searchQuery = searchQuery,
        selectedFilterStage = selectedFilterStage,
        onSearchQueryChange = { searchQuery = it },
        onFilterStageChange = { selectedFilterStage = it },
        onApplicationSelected = onApplicationSelected,
        onAddApplication = onAddApplication,
        onBackClick = onBackClick
    )
}

@Composable
fun ApplicationListContent(
    isLoading: Boolean,
    applications: List<LoanApplicationModel>,
    borrowers: List<BorrowerModel>,
    searchQuery: String,
    selectedFilterStage: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterStageChange: (String) -> Unit,
    onApplicationSelected: (LoanApplicationModel) -> Unit,
    onAddApplication: () -> Unit,
    onBackClick: () -> Unit
) {
    // Filter and search logic
    val filteredApps = remember(applications, borrowers, searchQuery, selectedFilterStage) {
        applications.filter { app ->
            val borrower = borrowers.find { it.id == app.borrower_id }
            val nameMatches = borrower?.name?.contains(searchQuery, ignoreCase = true) ?: false
            val idMatches = app.id.contains(searchQuery, ignoreCase = true)
            
            val stageMatches = selectedFilterStage == "All Stages" || app.status.equals(selectedFilterStage, ignoreCase = true)
            
            (nameMatches || idMatches) && stageMatches
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Application Queue",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddApplication,
                containerColor = FieldTheme.colors.purple600,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(FieldIcons.AddOutlined, contentDescription = "Create Application")
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pipeline Stats Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                    .border(width = 0.5.dp, color = FieldTheme.colors.purple600.copy(alpha = 0.1f))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Pipeline Overview",
                    style = FieldTheme.typography.title.copy(fontSize = 28.sp),
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PipelineStatBox(
                        label = "TOTAL DOSSIERS",
                        value = "${applications.size}",
                        color = FieldTheme.colors.purple400
                    )
                    PipelineStatBox(
                        label = "IN REVIEW",
                        value = "${applications.count { it.status.lowercase(Locale.getDefault()).contains("review") }}",
                        color = FieldTheme.colors.statusWarning
                    )
                    PipelineStatBox(
                        label = "APPROVED",
                        value = "${applications.count { it.status.lowercase(Locale.getDefault()).contains("approved") }}",
                        color = FieldTheme.colors.statusSuccess
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Input Block
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                FieldTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = "",
                    placeholder = "Search applicant or ref...",
                    trailingIcon = {
                        Icon(
                            imageVector = FieldIcons.SearchOutlined,
                            contentDescription = "Search",
                            tint = FieldTheme.colors.gray500
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val stages = listOf("All Stages", "Intake", "OCR Review", "Credit Review", "BM Approved")
                stages.forEach { stage ->
                    val isSelected = selectedFilterStage == stage
                    val chipBg = if (isSelected) FieldTheme.colors.purple600.copy(alpha = 0.2f) else FieldTheme.colors.gray900
                    val chipBorder = if (isSelected) FieldTheme.colors.purple400 else FieldTheme.colors.gray700
                    val chipText = if (isSelected) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                    
                    Box(
                        modifier = Modifier
                            .background(chipBg, RoundedCornerShape(16.dp))
                            .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                            .clickable { onFilterStageChange(stage) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stage,
                            style = FieldTheme.typography.bodyStrong.copy(fontSize = 13.sp),
                            color = chipText
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                // Shimmer loading layout
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(6) {
                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LoadingSkeleton(height = 48.dp, width = 48.dp, cornerRadius = 24.dp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 16.dp, width = 140.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 80.dp)
                                }
                                LoadingSkeleton(height = 24.dp, width = 80.dp, cornerRadius = 12.dp)
                            }
                        }
                    }
                }
            } else if (filteredApps.isEmpty()) {
                EmptyState(
                    text = "No lending applications found matching filters.",
                    linkText = "Register New Loan Intake",
                    onLinkClick = onAddApplication
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredApps) { app ->
                        val borrower = borrowers.find { it.id == app.borrower_id }
                        val name = borrower?.name ?: "Unknown Profile"
                        val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                        
                        val stateCode = when (app.status.lowercase(Locale.getDefault())) {
                            "approved", "bm approved" -> StatusChipVariant.Approved
                            "intake" -> StatusChipVariant.Verified
                            "needs review", "ocr review", "credit review" -> StatusChipVariant.NeedsReview
                            "low confidence" -> StatusChipVariant.LowConfidence
                            "returned" -> StatusChipVariant.Returned
                            else -> StatusChipVariant.NeedsReview
                        }

                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onApplicationSelected(app) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(FieldTheme.colors.purple600.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                        .border(1.dp, FieldTheme.colors.purple600.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        style = FieldTheme.typography.title.copy(fontSize = 18.sp),
                                        color = FieldTheme.colors.purple400
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 16.sp),
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₦${String.format(Locale.US, "%,.0f", app.amount)}",
                                            style = FieldTheme.typography.mono.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FieldTheme.colors.purple200
                                        )
                                        Text(
                                            text = " • ${app.product_type}",
                                            style = FieldTheme.typography.label.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Status
                                StatusChip(variant = stateCode)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PipelineStatBox(label: String, value: String, color: Color) {
    Column {
        Text(
            text = value,
            style = FieldTheme.typography.title.copy(fontSize = 24.sp),
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
            color = FieldTheme.colors.gray500
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone - List", widthDp = 411, heightDp = 850)
@Composable
fun PreviewApplicationListCompact() {
    val demoBorrowers = listOf(
        BorrowerModel(
            id = "1", org_id = "org_1", loan_officer_id = "LO_1",
            name = "Adaeze Okonkwo", phone = "08012345678", bvn = "222333444", nin = "111222333",
            status = "Active", created_at = "2026-06-18"
        ),
        BorrowerModel(
            id = "2", org_id = "org_1", loan_officer_id = "LO_1",
            name = "Emeka Chukwu", phone = "08087654321", bvn = "555666777", nin = "999888777",
            status = "Active", created_at = "2026-06-18"
        )
    )
    val demoApps = listOf(
        LoanApplicationModel(
            id = "app_1", org_id = "org_1", borrower_id = "1",
            current_stage = 1, current_owner_id = "LO_1", status = "Intake",
            amount = 250000.0, tenure = 6, product_type = "SME Loan",
            interest_rate = 15.0, repayment_frequency = "Monthly",
            created_at = "2026-06-18"
        ),
        LoanApplicationModel(
            id = "app_2", org_id = "org_1", borrower_id = "2",
            current_stage = 2, current_owner_id = "LO_1", status = "Needs Review",
            amount = 1200000.0, tenure = 12, product_type = "Asset Loan",
            interest_rate = 12.0, repayment_frequency = "Monthly",
            created_at = "2026-06-18"
        )
    )

    FieldCRMTheme {
        ApplicationListContent(
            isLoading = false,
            applications = demoApps,
            borrowers = demoBorrowers,
            searchQuery = "",
            selectedFilterStage = "All Stages",
            onSearchQueryChange = {},
            onFilterStageChange = {},
            onApplicationSelected = {},
            onAddApplication = {},
            onBackClick = {}
        )
    }
}
