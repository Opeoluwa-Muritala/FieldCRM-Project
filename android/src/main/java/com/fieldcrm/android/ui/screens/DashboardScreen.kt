package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun DashboardScreenView(
    role: UserRole?,
    onNavigateToBorrowers: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val navigationItems = listOf(
        NavigationItem("Dashboard", Icons.Outlined.Home),
        NavigationItem("Borrowers", Icons.Outlined.Person),
        NavigationItem("Applications", Icons.Outlined.List),
        NavigationItem("Settings", Icons.Outlined.Settings)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isWide = maxWidth >= 600.dp
        
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                // Expanded/Medium width strategy: Navigation Rail on the left
                FieldNavigationRail(
                    items = navigationItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = { selectedTab = it }
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Header Top Bar
                FieldTopAppBar(
                    title = "FieldCRM Operational Console",
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoleBadge(role = role?.displayName ?: "Loan Officer")
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(onClick = onLogout) {
                                Icon(
                                    imageVector = Icons.Outlined.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = FieldTheme.colors.gray400
                                )
                            }
                        }
                    }
                )
                
                // Screen content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> DashboardHomeContent(
                            role = role ?: UserRole.LOAN_OFFICER,
                            onNavigateToBorrowers = onNavigateToBorrowers,
                            onNavigateToApplications = onNavigateToApplications
                        )
                        1 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PrimaryButton(
                                text = "View Borrowers Queue",
                                onClick = onNavigateToBorrowers,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                        2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PrimaryButton(
                                text = "View Applications Queue",
                                onClick = onNavigateToApplications,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                        else -> SettingsScreen(
                            onBackClick = { selectedTab = 0 },
                            onNavigateToOfflineQueue = onNavigateToOfflineQueue
                        )
                    }
                }
                
                if (!isWide) {
                    // Compact width strategy: Bottom Bar
                    FieldBottomBar(
                        items = navigationItems,
                        selectedItemIndex = selectedTab,
                        onItemSelect = { selectedTab = it }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardHomeContent(
    role: UserRole,
    onNavigateToBorrowers: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    // Generate role specific statistics
    val colors = FieldTheme.colors
    val stats = remember(role, colors) { getStatsForRole(role, colors) }
    val modules = remember(role) { getModulesForRole(role) }
    val recentItems = remember(role) { getRecentItemsForRole(role) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good day, Field Agent",
                        style = FieldTheme.typography.display,
                        color = FieldTheme.colors.gray100
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Organizational Unit: Lagos West MMFB",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400
                    )
                }
                
                // Server Sync Trigger
                Box(
                    modifier = Modifier
                        .border(0.5.dp, FieldTheme.colors.purple500.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .background(FieldTheme.colors.purple950)
                        .clickable {
                            // Run server sync via context
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Sync",
                            tint = FieldTheme.colors.purple400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SYNC NOW",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.purple200
                        )
                    }
                }
            }
        }

        // Stats Row (Compact reflow: horizontal scroll or grids)
        item {
            Column {
                Text(
                    text = "LENDING STATS",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    stats.forEach { stat ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                .background(FieldTheme.colors.gray850)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = stat.title.uppercase(Locale.getDefault()),
                                    style = FieldTheme.typography.label.copy(fontSize = 9.sp),
                                    color = FieldTheme.colors.gray400
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stat.value,
                                    style = FieldTheme.typography.mono.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = stat.color
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modules Matrix / Grid (Grid flow based on space)
        item {
            Column {
                Text(
                    text = "WORKFLOW ENGINES",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modules.chunked(2).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            chunk.forEach { module ->
                                val clickAction = if (module.targetScreen == "borrowers") onNavigateToBorrowers else onNavigateToApplications
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                        .background(FieldTheme.colors.gray850)
                                        .clickable(onClick = clickAction)
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(FieldTheme.colors.purple950, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = module.icon,
                                                contentDescription = module.title,
                                                tint = FieldTheme.colors.purple400,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = module.title,
                                                style = FieldTheme.typography.bodyStrong,
                                                color = FieldTheme.colors.gray100
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = module.desc,
                                                style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                                color = FieldTheme.colors.gray500
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity / Queue items
        item {
            Column {
                Text(
                    text = "RECENT DOSSIERS IN QUEUE",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                .background(FieldTheme.colors.gray850)
                                .clickable { onNavigateToApplications() }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.borrowerName,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.amount,
                                            style = FieldTheme.typography.mono.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.gray400
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "•  ${item.lga}",
                                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                }
                                StatusChip(variant = item.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ROLE BASED DEMO GENERATION
// ==========================================

data class StatItem(val title: String, val value: String, val color: Color)
data class ModuleItem(val title: String, val desc: String, val icon: ImageVector, val targetScreen: String)
data class RecentDossier(val borrowerName: String, val amount: String, val lga: String, val status: StatusChipVariant)

private fun getStatsForRole(role: UserRole, colors: com.fieldcrm.android.ui.theme.FieldColors): List<StatItem> {
    val success = colors.statusSuccess
    val warning = colors.statusWarning
    val primary = colors.purple400

    return when (role) {
        UserRole.LOAN_OFFICER -> listOf(
            StatItem("Pending Visitations", "4", warning),
            StatItem("Intake Queue", "12", primary),
            StatItem("Disbursed Total", "₦2.4M", success)
        )
        UserRole.BRANCH_MANAGER -> listOf(
            StatItem("Approvals Queue", "3", warning),
            StatItem("Active Officers", "8", primary),
            StatItem("Monthly Target", "84%", success)
        )
        UserRole.CREDIT_OFFICER -> listOf(
            StatItem("Pending Scores", "7", warning),
            StatItem("Calculations Done", "41", primary),
            StatItem("Avg Risk Confidence", "92%", success)
        )
        UserRole.AUDITOR -> listOf(
            StatItem("Gates Checklists", "18", primary),
            StatItem("Returned Files", "2", warning),
            StatItem("Compliance Rate", "98%", success)
        )
        UserRole.ADMIN_MCR -> listOf(
            StatItem("Active Sync Syncing", "Online", success),
            StatItem("Failed Retries", "0", primary),
            StatItem("Org Nodes", "12", primary)
        )
    }
}

private fun getModulesForRole(role: UserRole): List<ModuleItem> {
    return when (role) {
        UserRole.LOAN_OFFICER -> listOf(
            ModuleItem("Borrower Intake", "Register profiles", Icons.Outlined.Person, "borrowers"),
            ModuleItem("Visitation Report", "Capture GPS and Photo", Icons.Outlined.LocationOn, "applications"),
            ModuleItem("Guarantors Upload", "Collapsible verification", Icons.Outlined.Share, "applications"),
            ModuleItem("Pledges & Trusts", "Signature pads", Icons.Outlined.Edit, "applications")
        )
        UserRole.BRANCH_MANAGER -> listOf(
            ModuleItem("Review Queue", "Dossier audit & decision", Icons.Outlined.List, "applications"),
            ModuleItem("Active Sync Status", "WorkManager state", Icons.Outlined.Refresh, "applications"),
            ModuleItem("Team Operations", "Branch analytics", Icons.Outlined.AccountBox, "borrowers"),
            ModuleItem("Disbursements", "Disbursement ready gates", Icons.Outlined.ThumbUp, "applications")
        )
        UserRole.CREDIT_OFFICER -> listOf(
            ModuleItem("Credit Scores", "DTI & bureau matrices", Icons.Outlined.Create, "applications"),
            ModuleItem("OCR Checks", "Verify extracted NIN/BVN", Icons.Outlined.Search, "applications"),
            ModuleItem("Risk Analytics", "Borrower scoring ratios", Icons.Outlined.Star, "applications"),
            ModuleItem("Compliance Gates", "Organization checklists", Icons.Outlined.Done, "applications")
        )
        UserRole.AUDITOR -> listOf(
            ModuleItem("Compliance Log", "Tick checklists & verification", Icons.Outlined.Done, "applications"),
            ModuleItem("Immutable Audit Log", "Actor action trail", Icons.Outlined.Info, "applications"),
            ModuleItem("OCR Mismatch Checks", "Manual corrections list", Icons.Outlined.Warning, "applications"),
            ModuleItem("Dossier Exporters", "Download bank certs", Icons.Outlined.List, "applications")
        )
        UserRole.ADMIN_MCR -> listOf(
            ModuleItem("Certificate Pinning", "HTTPS security settings", Icons.Outlined.Lock, "applications"),
            ModuleItem("Queue Monitor", "Offline cache mutations", Icons.Outlined.ShoppingCart, "applications"),
            ModuleItem("System Logging", "Organization audit trails", Icons.Outlined.Info, "applications"),
            ModuleItem("Sync Conflicts", "Resolve sqlite conflicts", Icons.Outlined.Warning, "applications")
        )
    }
}

private fun getRecentItemsForRole(role: UserRole): List<RecentDossier> {
    return when (role) {
        UserRole.LOAN_OFFICER -> listOf(
            RecentDossier("Adaeze Okonkwo", "₦250,000", "Ikeja", StatusChipVariant.Verified),
            RecentDossier("Emeka Chukwu", "₦1,200,000", "Gwale", StatusChipVariant.NeedsReview),
            RecentDossier("Fatima Al-Hassan", "₦450,000", "Enugu North", StatusChipVariant.LowConfidence)
        )
        UserRole.BRANCH_MANAGER -> listOf(
            RecentDossier("Emeka Chukwu", "₦1,200,000", "Gwale", StatusChipVariant.NeedsReview),
            RecentDossier("Babatunde Olatunji", "₦600,000", "Surulere", StatusChipVariant.Returned),
            RecentDossier("Chinedu Eze", "₦350,000", "Onitsha", StatusChipVariant.Approved)
        )
        UserRole.CREDIT_OFFICER -> listOf(
            RecentDossier("Adaeze Okonkwo", "₦250,000", "Ikeja", StatusChipVariant.Verified),
            RecentDossier("Fatima Al-Hassan", "₦450,000", "Enugu North", StatusChipVariant.LowConfidence),
            RecentDossier("Tunde Bakare", "₦900,000", "Ibadan", StatusChipVariant.Missing)
        )
        UserRole.AUDITOR -> listOf(
            RecentDossier("Chinedu Eze", "₦350,000", "Onitsha", StatusChipVariant.Signed),
            RecentDossier("Babatunde Olatunji", "₦600,000", "Surulere", StatusChipVariant.Returned),
            RecentDossier("Adaeze Okonkwo", "₦250,000", "Ikeja", StatusChipVariant.Approved)
        )
        UserRole.ADMIN_MCR -> listOf(
            RecentDossier("Emeka Chukwu", "₦1,200,000", "Gwale", StatusChipVariant.NeedsReview),
            RecentDossier("Adaeze Okonkwo", "₦250,000", "Ikeja", StatusChipVariant.Verified),
            RecentDossier("Tunde Bakare", "₦900,000", "Ibadan", StatusChipVariant.Missing)
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Loan Officer - Compact", widthDp = 411, heightDp = 850)
@Composable
fun PreviewDashboardLO() {
    FieldCRMTheme {
        DashboardScreenView(
            role = UserRole.LOAN_OFFICER,
            onNavigateToBorrowers = {},
            onNavigateToApplications = {},
            onNavigateToOfflineQueue = {},
            onLogout = {}
        )
    }
}

@Preview(name = "Branch Manager - Tablet", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewDashboardBM() {
    FieldCRMTheme {
        DashboardScreenView(
            role = UserRole.BRANCH_MANAGER,
            onNavigateToBorrowers = {},
            onNavigateToApplications = {},
            onNavigateToOfflineQueue = {},
            onLogout = {}
        )
    }
}
