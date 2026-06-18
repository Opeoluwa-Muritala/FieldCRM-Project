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
    val filteredBorrowers = remember(borrowers, searchQuery) {
        borrowers.filter { b ->
            b.name.contains(searchQuery, ignoreCase = true) ||
            b.phone.contains(searchQuery) ||
            b.bvn.contains(searchQuery)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Borrower Profiles",
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
                    Box(
                        modifier = Modifier
                            .background(FieldTheme.colors.gray800, RoundedCornerShape(12.dp))
                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${filteredBorrowers.size} REGISTERED",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.purple400
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Borrower")
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
            
            // Search Input field
            FieldTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = "Filter registered profiles by name/phone/BVN",
                placeholder = "Search Emeka, Adaeze, or BVN...",
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = FieldTheme.colors.gray500
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                .background(FieldTheme.colors.gray850)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 14.dp, width = 120.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 10.dp, width = 70.dp)
                                }
                                LoadingSkeleton(height = 18.dp, width = 50.dp, cornerRadius = 9.dp)
                            }
                        }
                    }
                }
            } else if (filteredBorrowers.isEmpty()) {
                EmptyState(
                    text = "No borrower profiles match query.",
                    linkText = "Register New Borrower Profile",
                    onLinkClick = onAddBorrower
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredBorrowers) { borrower ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                .background(FieldTheme.colors.gray850)
                                .clickable { onBorrowerSelected(borrower) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                StatusChip(
                                    variant = if (borrower.status.lowercase(Locale.getDefault()) == "active") {
                                        StatusChipVariant.Verified
                                    } else {
                                        StatusChipVariant.NeedsReview
                                    }
                                )
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
