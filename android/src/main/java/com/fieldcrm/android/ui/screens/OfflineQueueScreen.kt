package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun OfflineQueueScreen(
    onBackClick: () -> Unit
) {
    var syncRetrying by remember { mutableStateOf(false) }
    var mockQueueSize by remember { mutableIntStateOf(2) }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Offline Cache Mutations Queue",
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
                            text = "SQLite Local Outbox Storage",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        
                        Text(
                            text = "These records were captured while network connectivity was disconnected. The WorkManager scheduler will automatically synchronise them when network drops end.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        
                        if (mockQueueSize > 0) {
                            // Mutated element 1
                            FieldCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CREATE BORROWER PROFILE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    StatusChip(variant = StatusChipVariant.NeedsReview)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                DetailItem(label = "Applicant", value = "Babatunde Olatunji")
                                DetailItem(label = "LGA Region Node", value = "Surulere, Lagos")
                            }
                            
                            // Mutated element 2 with conflict
                            FieldCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CREATE LOAN DOSSIER", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    StatusChip(variant = StatusChipVariant.Returned)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                DetailItem(label = "Target Borrower ID", value = "borrower_84", isMono = true)
                                DetailItem(label = "Amount", value = "₦ 600,000", isMono = true)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("SYNC DATABASE CONFLICT DETECTED", style = FieldTheme.typography.label, color = FieldTheme.colors.statusDanger)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("A borrower dossier with ID borrower_84 has newer edits on Mainstreet server.", style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray400)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PrimaryButton(
                                        text = "Keep Local",
                                        onClick = { mockQueueSize-- },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SecondaryButton(
                                        text = "Merge Server",
                                        onClick = { mockQueueSize-- },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            EmptyState(text = "No pending sync mutation operations in local SQLite outbox.")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PrimaryButton(
                            text = if (syncRetrying) "Syncing ledgers..." else "Force Synchronisation Check",
                            onClick = {
                                syncRetrying = true
                                // Simulate retry
                            },
                            enabled = !syncRetrying && mockQueueSize > 0
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

@Preview(name = "Compact Phone Offline Queue", widthDp = 411, heightDp = 850)
@Composable
fun PreviewOfflineQueueCompact() {
    FieldCRMTheme {
        OfflineQueueScreen(onBackClick = {})
    }
}
