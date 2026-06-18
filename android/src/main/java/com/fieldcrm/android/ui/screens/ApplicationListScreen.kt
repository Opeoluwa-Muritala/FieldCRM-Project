package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    // Quick stats pill
                    Box(
                        modifier = Modifier
                            .background(FieldTheme.colors.gray800, RoundedCornerShape(12.dp))
                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${filteredApps.size} DOSSIERS",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.purple400
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Application")
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Input Block
            FieldTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = "Filter Queue by Applicant Name or Reference",
                placeholder = "Search Emeka, Adaeze, or ref...",
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = FieldTheme.colors.gray500
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Horizontal Chips Scroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val stages = listOf("All Stages", "Intake", "OCR Review", "Credit Review", "BM Approved")
                stages.forEach { stage ->
                    val isSelected = selectedFilterStage == stage
                    val chipBg = if (isSelected) FieldTheme.colors.purple950 else FieldTheme.colors.gray850
                    val chipBorder = if (isSelected) FieldTheme.colors.purple400 else FieldTheme.colors.gray700
                    val chipText = if (isSelected) FieldTheme.colors.purple200 else FieldTheme.colors.gray400
                    
                    Box(
                        modifier = Modifier
                            .background(chipBg, RoundedCornerShape(6.dp))
                            .border(0.5.dp, chipBorder, RoundedCornerShape(6.dp))
                            .clickable { onFilterStageChange(stage) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stage.uppercase(Locale.getDefault()),
                            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                            color = chipText
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoading) {
                // Shimmer loading layout instead of spinning circles
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(6) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                .background(FieldTheme.colors.gray850)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 16.dp, width = 140.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 80.dp)
                                }
                                LoadingSkeleton(height = 20.dp, width = 60.dp, cornerRadius = 10.dp)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredApps) { app ->
                        val borrower = borrowers.find { it.id == app.borrower_id }
                        val name = borrower?.name ?: "Adaeze Okonkwo"
                        val stateCode = when (app.status.lowercase(Locale.getDefault())) {
                            "approved", "bm approved" -> StatusChipVariant.Approved
                            "intake" -> StatusChipVariant.Verified
                            "needs review" -> StatusChipVariant.NeedsReview
                            "low confidence" -> StatusChipVariant.LowConfidence
                            "returned" -> StatusChipVariant.Returned
                            else -> StatusChipVariant.NeedsReview
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                .background(FieldTheme.colors.gray850)
                                .clickable { onApplicationSelected(app) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₦ ${app.amount}",
                                            style = FieldTheme.typography.mono.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FieldTheme.colors.gray300
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "${app.tenure} MONTHS",
                                            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                }
                                StatusChip(variant = stateCode)
                            }
                        }
                    }
                }
            }
        }
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
