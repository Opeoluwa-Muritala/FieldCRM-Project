package com.fieldcrm.android.ui.screens.borrower

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.BorrowerUiState
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel
import com.fieldcrm.shared.model.BorrowerModel
import java.util.Locale

@Composable
fun BorrowerListScreenView(
    viewModel: BorrowerViewModel,
    onBorrowerSelected: (BorrowerModel) -> Unit,
    onAddBorrower: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    BorrowerListContent(
        isLoading = state.isLoading,
        borrowers = state.borrowers,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onBorrowerSelected = onBorrowerSelected,
        onAddBorrower = onAddBorrower,
        onBackClick = onBackClick
    )
}

@Composable
fun BorrowerListContent(
    isLoading: Boolean,
    borrowers: List<BorrowerModel>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBorrowerSelected: (BorrowerModel) -> Unit,
    onAddBorrower: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filteredBorrowers = remember(borrowers, searchQuery, selectedFilter) {
        borrowers.filter { b ->
            val matchesSearch = b.name.contains(searchQuery, ignoreCase = true) ||
                b.phone.contains(searchQuery) ||
                b.bvn.contains(searchQuery)
            
            val matchesFilter = when (selectedFilter) {
                "Active" -> b.status.equals("active", ignoreCase = true)
                "Review" -> !b.status.equals("active", ignoreCase = true)
                else -> true
            }
            
            matchesSearch && matchesFilter
        }.sortedByDescending { it.created_at } // Sort recent first
    }

    val activeCount = borrowers.count { it.status.equals("active", ignoreCase = true) }
    val reviewCount = borrowers.size - activeCount

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Client Lineup",
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
                onClick = onAddBorrower,
                containerColor = FieldTheme.colors.purple600,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(FieldIcons.AddOutlined, contentDescription = "Add Client")
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats & Filters Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray900)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(label = "Total Lineup", value = borrowers.size.toString(), color = FieldTheme.colors.gray100)
                    StatBox(label = "Active", value = activeCount.toString(), color = FieldTheme.colors.statusSuccess)
                    StatBox(label = "Needs Review", value = reviewCount.toString(), color = FieldTheme.colors.statusWarning)
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                FieldTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = "Search Lineup",
                    placeholder = "Name, Phone, or BVN...",
                    trailingIcon = {
                        Icon(FieldIcons.SearchOutlined, contentDescription = "Search", tint = FieldTheme.colors.gray500)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All", style = FieldTheme.typography.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FieldTheme.colors.purple600,
                            selectedLabelColor = Color.White,
                            containerColor = FieldTheme.colors.gray800,
                            labelColor = FieldTheme.colors.gray400
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == "Active",
                        onClick = { selectedFilter = "Active" },
                        label = { Text("Active", style = FieldTheme.typography.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FieldTheme.colors.purple600,
                            selectedLabelColor = Color.White,
                            containerColor = FieldTheme.colors.gray800,
                            labelColor = FieldTheme.colors.gray400
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == "Review",
                        onClick = { selectedFilter = "Review" },
                        label = { Text("Needs Review", style = FieldTheme.typography.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FieldTheme.colors.purple600,
                            selectedLabelColor = Color.White,
                            containerColor = FieldTheme.colors.gray800,
                            labelColor = FieldTheme.colors.gray400
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
            
            // List Area
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(5) {
                            FieldCard(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LoadingSkeleton(height = 40.dp, width = 40.dp, cornerRadius = 20.dp)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        LoadingSkeleton(height = 14.dp, width = 120.dp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LoadingSkeleton(height = 10.dp, width = 70.dp)
                                    }
                                }
                            }
                        }
                    }
                } else if (filteredBorrowers.isEmpty()) {
                    EmptyState(
                        text = if (searchQuery.isNotEmpty()) "No clients match your search." else "Lineup is empty.",
                        linkText = "Add Client to Lineup",
                        onLinkClick = onAddBorrower
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredBorrowers) { borrower ->
                            BorrowerLineupCard(
                                borrower = borrower,
                                onClick = { onBorrowerSelected(borrower) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = FieldTheme.typography.title.copy(fontSize = 20.sp), color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, style = FieldTheme.typography.label.copy(fontSize = 10.sp), color = FieldTheme.colors.gray500)
    }
}

@Composable
fun BorrowerLineupCard(borrower: BorrowerModel, onClick: () -> Unit) {
    val initials = remember(borrower.name) {
        borrower.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    }
    
    val isActive = borrower.status.equals("active", ignoreCase = true)
    
    FieldCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isActive) FieldTheme.colors.purple600.copy(alpha = 0.2f) else FieldTheme.colors.gray800,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) FieldTheme.colors.purple600 else FieldTheme.colors.gray700,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 16.sp),
                    color = if (isActive) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = borrower.name,
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = borrower.phone,
                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                        color = FieldTheme.colors.gray400
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "BVN: ${borrower.bvn}",
                        style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                        color = FieldTheme.colors.gray500
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Status & Chevron
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(
                    variant = if (isActive) StatusChipVariant.Verified else StatusChipVariant.NeedsReview
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = FieldIcons.ChevronRightOutlined,
                    contentDescription = "View Profile",
                    tint = FieldTheme.colors.gray600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone List", widthDp = 411, heightDp = 850)
@Composable
fun PreviewBorrowerListCompact() {
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
    
    FieldCRMTheme {
        BorrowerListContent(
            isLoading = false,
            borrowers = demoBorrowers,
            searchQuery = "",
            onSearchQueryChange = {},
            onBorrowerSelected = {},
            onAddBorrower = {},
            onBackClick = {}
        )
    }
}
