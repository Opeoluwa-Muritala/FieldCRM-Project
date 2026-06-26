package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun DashboardScreenView(
    role: UserRole?,
    onNavigateToBorrowers: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearchResults: () -> Unit = {}
) {
    val resolvedRole = role ?: UserRole.LOAN_OFFICER
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        when (resolvedRole) {
            UserRole.LOAN_OFFICER -> LoanOfficerTabletDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults)
            UserRole.BRANCH_MANAGER -> BranchManagerTabletDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults)
            UserRole.CREDIT_OFFICER -> CreditOfficerTabletDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults)
            UserRole.AUDITOR -> AuditorTabletDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults)
            UserRole.ADMIN_MCR -> AdminTabletDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults)
        }
    } else {
        when (resolvedRole) {
            UserRole.LOAN_OFFICER -> LoanOfficerPhoneDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults, onNavigateToBorrowers, onNavigateToApplications)
            UserRole.BRANCH_MANAGER -> BranchManagerPhoneDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults, onNavigateToApplications)
            UserRole.CREDIT_OFFICER -> CreditOfficerPhoneDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults, onNavigateToApplications)
            UserRole.AUDITOR -> AuditorPhoneDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults, onNavigateToApplications)
            UserRole.ADMIN_MCR -> AdminPhoneDashboard(onLogout, onNavigateToOfflineQueue, onNavigateToNotifications, onNavigateToSearchResults, onNavigateToBorrowers)
        }
    }
}

// =========================================================================
// 1. LOAN OFFICER SCREEN GRAPHICS
// =========================================================================

@Composable
fun LoanOfficerPhoneDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit,
    onNavigateToBorrowers: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettingsInline by remember { mutableStateOf(false) }
    var showNewBottomSheet by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Queue", Icons.AutoMirrored.Outlined.List),
        NavigationItem("New", Icons.Outlined.Add), // Center distinct FAB
        NavigationItem("Upload", Icons.AutoMirrored.Outlined.Send),
        NavigationItem("Visits", Icons.Outlined.LocationOn)
    )

    Scaffold(
        bottomBar = {
            if (!showSettingsInline) {
                FieldBottomBar(
                    items = navigationItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = {
                        if (it == 2) {
                            showNewBottomSheet = true
                        } else {
                            selectedTab = it
                            showSettingsInline = false
                        }
                    }
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showSettingsInline) {
                SettingsScreen(
                    userName = "Chidi Okafor",
                    userEmail = "chidi@mmfb.com",
                    role = UserRole.LOAN_OFFICER,
                    onBackClick = { showSettingsInline = false },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        // Home Screen Layout
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Deep Purple greeting band (full-bleed, 110dp tall)
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(FieldTheme.colors.purple900) // Deep Purple
                                        .padding(horizontal = 24.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Good morning, Chidi",
                                        style = FieldTheme.typography.display.copy(fontSize = 22.sp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Loan Officer · Ikeja Branch",
                                        style = FieldTheme.typography.body,
                                        color = FieldTheme.colors.gray400
                                    )
                                }
                            }

                            // Content padding
                            item { Spacer(modifier = Modifier.height(16.dp)) }

                            // 2×2 metric grid (white chips, purple accent)
                            item {
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            MiniMetricCard("My Apps Today", "12", FieldTheme.colors.purple600)
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            MiniMetricCard("Pending Upload", "2", FieldTheme.colors.purple600)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            MiniMetricCard("Visits Due", "4", FieldTheme.colors.purple600)
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            MiniMetricCard("Missing Docs", "3", FieldTheme.colors.purple600)
                                        }
                                    }
                                }
                            }

                            // TODAY'S TASKS (priority list: Missing Doc -> Pending Upload -> Visit Due -> Draft App)
                            item {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text("TODAY'S TASKS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TaskRow("Missing Doc: Adaeze Kalu · MMFB-041", true) { onNavigateToApplications() }
                                    TaskRow("Pending Upload: CRM-01 Loan Form", false) { selectedTab = 3 }
                                    TaskRow("Visit Due: guarantor verification", false) { selectedTab = 4 }
                                    TaskRow("Draft App: MMFB-052", false) { onNavigateToApplications() }
                                }
                            }

                            // RECENT ACTIVITY (last 3)
                            item {
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    Text("RECENT ACTIVITY", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    RecentActivityCard("MMFB-041 (Adaeze Kalu) - Verified match with original land title.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RecentActivityCard("MMFB-039 (Bola T.) - Application returned for guarantor signature.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RecentActivityCard("MMFB-052 (Chidi Okafor) - Local database synched with server.")
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                    1 -> {
                        // Queue screen: Sticky search + horizontal filter chips
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                placeholder = { Text("Search Loan Officer Queue...") },
                                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = true, onClick = {}, label = { Text("All") })
                                FilterChip(selected = false, onClick = {}, label = { Text("Intake") })
                                FilterChip(selected = false, onClick = {}, label = { Text("OCR Review") })
                                FilterChip(selected = false, onClick = {}, label = { Text("Returned") })
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // Swipeable cards simulation
                            Text("Swipeable queue cards: Swipe Right to Upload Doc, Swipe Left to Return.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Adaeze Kalu · MMFB-041", "Bola T. · MMFB-039", "Chioma Eze · MMFB-022")) { app ->
                                    FieldCard {
                                        Text(app, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Swipe to take action inline", style = FieldTheme.typography.label, color = FieldTheme.colors.purple600)
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Upload Screen: Camera-first, 4 large tiles
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("UPLOAD DOCUMENTS", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                            Text("Camera-first documents submission queue.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    LargeTile("Take Photo", Icons.Outlined.AddCircle) {}
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    LargeTile("Choose File", Icons.Outlined.Search) {}
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    LargeTile("CRM-01 Form", Icons.Outlined.Info) {}
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    LargeTile("CRM-03 Guarantor", Icons.Outlined.Person) {}
                                }
                            }
                        }
                    }
                    4 -> {
                        // Visits screen: List of visits due, opening one-question survey
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            Text("VISITS DUE SURVEYS", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldCard {
                                Text("Visits due: guarantor verification", style = FieldTheme.typography.bodyStrong)
                                Spacer(modifier = Modifier.height(8.dp))
                                PrimaryButton(text = "Start One-Question Survey", onClick = {
                                    // Simulated survey wizard
                                })
                            }
                        }
                    }
                }
            }

            // Topbar icons
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onNavigateToSearchResults) {
                    Icon(Icons.Outlined.Search, "Search", tint = FieldTheme.colors.gray400)
                }
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(Icons.Outlined.Notifications, "Notifications", tint = FieldTheme.colors.gray400)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clickable { showSettingsInline = !showSettingsInline }) {
                    RoleBadge(role = "LO")
                }
            }

            // Wizard bottom sheet for "New" tab
            if (showNewBottomSheet) {
                AlertDialog(
                    onDismissRequest = { showNewBottomSheet = false },
                    title = { Text("New Application", style = FieldTheme.typography.title) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Choose the intake mode to proceed:")
                            PrimaryButton(text = "Fill Form on Phone", onClick = {
                                showNewBottomSheet = false
                                onNavigateToApplications()
                            })
                            SecondaryButton(text = "Upload Completed Form Scan", onClick = {
                                showNewBottomSheet = false
                                selectedTab = 3
                            })
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showNewBottomSheet = false }) { Text("Cancel") }
                    },
                    containerColor = FieldTheme.colors.gray900
                )
            }
        }
    }
}

@Composable
fun LoanOfficerTabletDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedQueueIndex by remember { mutableStateOf(0) }
    val sideRailItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Queue", Icons.AutoMirrored.Outlined.List),
        NavigationItem("New App", Icons.Outlined.AddCircle),
        NavigationItem("Upload", Icons.AutoMirrored.Outlined.Send),
        NavigationItem("Visits", Icons.Outlined.LocationOn),
        NavigationItem("Settings", Icons.Outlined.Settings)
    )

    Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
        // Persistent 72pt Deep Purple sidebar
        FieldNavigationRail(
            items = sideRailItems,
            selectedItemIndex = selectedTab,
            onItemSelect = { selectedTab = it }
        )

        if (selectedTab == 5) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SettingsScreen(
                    userName = "Chidi Okafor",
                    userEmail = "chidi@mmfb.com",
                    role = UserRole.LOAN_OFFICER,
                    onBackClick = { selectedTab = 0 },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            }
        } else {
            // Split view layout: Master list on Left (340dp), Details on Right
            Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // 340dp Master Queue list
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.gray900)
                        .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(0.dp))
                        .padding(16.dp)
                ) {
                    Text("LOAN QUEUE", style = FieldTheme.typography.title, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val itemsList = listOf("Adaeze Kalu", "Bola T. (Pending)", "Chioma Eze", "David Okoro")
                        itemsIndexed(itemsList) { idx, name ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx == selectedQueueIndex) FieldTheme.colors.purple950 else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedQueueIndex = idx }
                                    .padding(12.dp)
                            ) {
                                Text(name, style = FieldTheme.typography.bodyStrong, color = if (idx == selectedQueueIndex) Color.White else FieldTheme.colors.gray300)
                            }
                        }
                    }
                }

                // Application Detail: fluid right pane (forms, docs, visit, logs)
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text("APPLICATION DETAIL", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Adaeze Kalu · ₦500,000", style = FieldTheme.typography.display, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Detail Form fields 2-up in a grid (tablet feature)
                    Text("APPLICANT INFO", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            FieldTextField(value = "Adaeze Kalu", onValueChange = {}, label = "Full Name")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FieldTextField(value = "Lagos", onValueChange = {}, label = "Branch")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Side-by-side scanned document and OCR review
                    Text("SIDE-BY-SIDE OCR VERIFICATION", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Scan preview Left
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp)).border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Text("Scanned Form image", color = FieldTheme.colors.gray400)
                        }
                        // OCR fields Right
                        Column(modifier = Modifier.weight(1f).fillMaxHeight().background(FieldTheme.colors.gray900).padding(12.dp), verticalArrangement = Arrangement.Center) {
                            Text("BVN Match Confidence: 99%", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.statusSuccess)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Full Name OCR Match: 94%", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.statusSuccess)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Signature pad (200dp tall vs phone's 120dp/180dp)
                    Text("SIGNATURE pad (tablet size)", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldSignaturePad(modifier = Modifier.height(200.dp), onConfirm = {}, onClear = {})
                }
            }
        }
    }
}

// =========================================================================
// 2. BRANCH MANAGER SCREEN GRAPHICS
// =========================================================================

@Composable
fun BranchManagerPhoneDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettingsInline by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Awaiting Me", Icons.Outlined.Info), // clock-pending
        NavigationItem("Pipeline", Icons.AutoMirrored.Outlined.List),
        NavigationItem("Visits", Icons.Outlined.LocationOn),
        NavigationItem("Reports", Icons.Outlined.AccountBox)
    )

    Scaffold(
        bottomBar = {
            if (!showSettingsInline) {
                FieldBottomBar(
                    items = navigationItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = {
                        selectedTab = it
                        showSettingsInline = false
                    }
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showSettingsInline) {
                SettingsScreen(
                    userName = "Amaka Obi",
                    userEmail = "amaka@mmfb.com",
                    role = UserRole.BRANCH_MANAGER,
                    onBackClick = { showSettingsInline = false },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        // Home screen: Action Required sits ABOVE greeting
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Action Required Card (red-bordered, above greeting)
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, FieldTheme.colors.statusDanger, RoundedCornerShape(8.dp))
                                        .background(FieldTheme.colors.statusDanger.copy(alpha = 0.05f))
                                        .clickable { selectedTab = 1 }
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text("ACTION REQUIRED", style = FieldTheme.typography.label, color = FieldTheme.colors.statusDanger)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("3 applications awaiting your concurrence", style = FieldTheme.typography.bodyStrong, color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Review Now →", style = FieldTheme.typography.label, color = FieldTheme.colors.purple600)
                                    }
                                }
                            }

                            // 2. Greeting
                            item {
                                Text("Welcome back, Amaka", style = FieldTheme.typography.display, color = Color.White)
                                Text("Branch Manager · Lagos West", style = FieldTheme.typography.body, color = FieldTheme.colors.gray500)
                            }

                            // 3. Visitation Signoffs Pending card with button inline
                            item {
                                FieldCard {
                                    Text("VISITATION SIGNOFFS PENDING", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Officer: Chidi Okafor", style = FieldTheme.typography.bodyStrong)
                                            Text("Applicant: Adaeze Kalu", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                                        }
                                        Button(
                                            onClick = {},
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.purple600),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Sign Off", style = FieldTheme.typography.label)
                                        }
                                    }
                                }
                            }

                            // 4. Branch Pipeline (condensed chip row)
                            item {
                                Text("BRANCH PIPELINE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.background(FieldTheme.colors.gray900, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("Intake: 8", style = FieldTheme.typography.label, color = Color.White)
                                    }
                                    Box(modifier = Modifier.background(FieldTheme.colors.gray900, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("OCR: 4", style = FieldTheme.typography.label, color = Color.White)
                                    }
                                    Box(modifier = Modifier.background(FieldTheme.colors.gray900, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("Credit: 6", style = FieldTheme.typography.label, color = Color.White)
                                    }
                                }
                            }

                            // 5. Recent Approvals (last 5)
                            item {
                                Text("RECENT APPROVALS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Adaeze Okonkwo - Approved", "Emeka Chukwu - Approved", "Fatima Al-Hassan - Approved").forEach { app ->
                                        Box(modifier = Modifier.fillMaxWidth().background(FieldTheme.colors.gray900).padding(12.dp)) {
                                            Text(app, style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Awaiting Me screen: Inline buttons concur/return
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("AWAITING MY CONCURRENCE", style = FieldTheme.typography.title, color = Color.White)
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(listOf("MMFB-041 (Adaeze Kalu)", "MMFB-039 (Bola T.)")) { doc ->
                                    FieldCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(doc, style = FieldTheme.typography.bodyStrong)
                                                Text("Credit Recommendation: Approve", style = FieldTheme.typography.body, color = FieldTheme.colors.statusSuccess)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {},
                                                    colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.statusSuccess),
                                                    modifier = Modifier.height(36.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Concur", style = FieldTheme.typography.label)
                                                }
                                                OutlinedButton(
                                                    onClick = {},
                                                    modifier = Modifier.height(36.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, FieldTheme.colors.statusDanger),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldTheme.colors.statusDanger)
                                                ) {
                                                    Text("Return", style = FieldTheme.typography.label)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Manager operational utility screen", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                        }
                    }
                }
            }

            // Topbar icons
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onNavigateToSearchResults) {
                    Icon(Icons.Outlined.Search, "Search", tint = FieldTheme.colors.gray400)
                }
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(Icons.Outlined.Notifications, "Notifications", tint = FieldTheme.colors.gray400)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clickable { showSettingsInline = !showSettingsInline }) {
                    RoleBadge(role = "BM")
                }
            }
        }
    }
}

@Composable
fun BranchManagerTabletDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedQueueIndex by remember { mutableStateOf(0) }

    val sideRailItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Awaiting Me", Icons.Outlined.Info),
        NavigationItem("Pipeline", Icons.AutoMirrored.Outlined.List),
        NavigationItem("Visits", Icons.Outlined.LocationOn),
        NavigationItem("Reports", Icons.Outlined.AccountBox),
        NavigationItem("Settings", Icons.Outlined.Settings)
    )

    Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
        // Persistent side rail warning amber accent tint
        FieldNavigationRail(
            items = sideRailItems,
            selectedItemIndex = selectedTab,
            onItemSelect = { selectedTab = it }
        )

        if (selectedTab == 5) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SettingsScreen(
                    userName = "Amaka Obi",
                    userEmail = "amaka@mmfb.com",
                    role = UserRole.BRANCH_MANAGER,
                    onBackClick = { selectedTab = 0 },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            }
        } else {
            Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // 340dp pipeline master list
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.gray900)
                        .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(0.dp))
                        .padding(16.dp)
                ) {
                    Text("FULL PIPELINE", style = FieldTheme.typography.title, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val pipelineSteps = listOf("Intake (8)", "OCR Review (4)", "Credit Review (6)", "Branch Appr (3)", "Disbursed (2)")
                        itemsIndexed(pipelineSteps) { idx, name ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx == selectedQueueIndex) FieldTheme.colors.purple950 else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedQueueIndex = idx }
                                    .padding(12.dp)
                            ) {
                                Text(name, style = FieldTheme.typography.bodyStrong, color = if (idx == selectedQueueIndex) Color.White else FieldTheme.colors.gray300)
                            }
                        }
                    }
                }

                // Application Detail
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                    Text("CONCURRENCE STAGE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Credit Recommendation: Approve (Verified GSI)", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.statusSuccess)
                    Text("Readiness: 18/22 gates verified", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                    Spacer(modifier = Modifier.height(32.dp))

                    // Buttons live in detail pane, always visible
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {},
                            modifier = Modifier.width(180.dp).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.statusSuccess)
                        ) {
                            Text("Concur Approval", style = FieldTheme.typography.bodyStrong)
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.width(180.dp).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, FieldTheme.colors.statusDanger),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldTheme.colors.statusDanger)
                        ) {
                            Text("Return Application", style = FieldTheme.typography.bodyStrong)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. CREDIT OFFICER SCREEN GRAPHICS
// =========================================================================

@Composable
fun CreditOfficerPhoneDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettingsInline by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("My Reviews", Icons.AutoMirrored.Outlined.List),
        NavigationItem("OCR Queue", Icons.Outlined.Search),
        NavigationItem("Flags", Icons.Outlined.Warning),
        NavigationItem("Search", Icons.Outlined.Search)
    )

    Scaffold(
        bottomBar = {
            if (!showSettingsInline) {
                FieldBottomBar(
                    items = navigationItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = {
                        selectedTab = it
                        showSettingsInline = false
                    }
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showSettingsInline) {
                SettingsScreen(
                    userName = "Kemi Ade",
                    userEmail = "kemi@mmfb.com",
                    role = UserRole.CREDIT_OFFICER,
                    onBackClick = { showSettingsInline = false },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        // Home screen layout: Metric strip, list of overdue, exceptions list, upsell banner
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Metric strip
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(FieldTheme.colors.gray900).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Reviews: 7", style = FieldTheme.typography.label, color = Color.White)
                                    Text("High Risk: 3", style = FieldTheme.typography.label, color = FieldTheme.colors.statusDanger)
                                    Text("OCR Exceptions: 2", style = FieldTheme.typography.label, color = FieldTheme.colors.statusWarning)
                                }
                            }

                            // 2. Overdue Reviews list (age-sorted, amber/red urgency)
                            item {
                                Text("OVERDUE REVIEWS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DossierCard("Adaeze Okonkwo (Age: 5 days)", FieldTheme.colors.statusDanger)
                                    DossierCard("Emeka Chukwu (Age: 2 days)", FieldTheme.colors.statusWarning)
                                }
                            }

                            // 3. OCR Exceptions list: field + confidence + Flag/Accept inline (the ONLY edit action on phone)
                            item {
                                Text("OCR EXCEPTIONS (INLINE ACTIONS)", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    Column {
                                        Text("Field: guarantor_bvn", style = FieldTheme.typography.bodyStrong)
                                        Text("Confidence: 54%", style = FieldTheme.typography.body, color = FieldTheme.colors.statusWarning)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = {}, modifier = Modifier.height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.statusSuccess)) {
                                                Text("Accept", style = FieldTheme.typography.label)
                                            }
                                            OutlinedButton(onClick = {}, modifier = Modifier.height(32.dp), border = BorderStroke(1.dp, FieldTheme.colors.statusDanger), colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldTheme.colors.statusDanger)) {
                                                Text("Flag", style = FieldTheme.typography.label)
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Desktop/Tablet upsell banner
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FieldTheme.colors.purple950)
                                        .border(0.5.dp, FieldTheme.colors.purple600, RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Desktop / Tablet Upsell", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Full credit review is best done on a larger screen.",
                                            style = FieldTheme.typography.body,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            PrimaryButton(text = "Continue Anyway", onClick = { onNavigateToApplications() }, modifier = Modifier.weight(1f))
                                            SecondaryButton(text = "Switch Device", onClick = {}, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Credit Officer Reviews List", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                        }
                    }
                }
            }

            // Topbar icons
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onNavigateToSearchResults) {
                    Icon(Icons.Outlined.Search, "Search", tint = FieldTheme.colors.gray400)
                }
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(Icons.Outlined.Notifications, "Notifications", tint = FieldTheme.colors.gray400)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clickable { showSettingsInline = !showSettingsInline }) {
                    RoleBadge(role = "CO")
                }
            }
        }
    }
}

@Composable
fun CreditOfficerTabletDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedReviewIndex by remember { mutableStateOf(0) }

    val sideRailItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("My Reviews", Icons.AutoMirrored.Outlined.List),
        NavigationItem("OCR Exceptions", Icons.Outlined.Search),
        NavigationItem("Affordability", Icons.Outlined.Info),
        NavigationItem("Flags", Icons.Outlined.Warning),
        NavigationItem("Settings", Icons.Outlined.Settings)
    )

    Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
        // Persistent rail success green active tint
        FieldNavigationRail(
            items = sideRailItems,
            selectedItemIndex = selectedTab,
            onItemSelect = { selectedTab = it }
        )

        if (selectedTab == 5) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SettingsScreen(
                    userName = "Kemi Ade",
                    userEmail = "kemi@mmfb.com",
                    role = UserRole.CREDIT_OFFICER,
                    onBackClick = { selectedTab = 0 },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            }
        } else {
            // True 3-pane split view
            Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Column 1: Queue (280dp)
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.gray900)
                        .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(0.dp))
                        .padding(16.dp)
                ) {
                    Text("REVIEWS DUE", style = FieldTheme.typography.title, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val reviewsList = listOf("Adaeze Kalu (3d)", "Chioma Eze (1d)", "David Adio (5d)")
                        itemsIndexed(reviewsList) { idx, name ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx == selectedReviewIndex) FieldTheme.colors.purple950 else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedReviewIndex = idx }
                                    .padding(12.dp)
                            ) {
                                Text(name, style = FieldTheme.typography.bodyStrong, color = if (idx == selectedReviewIndex) Color.White else FieldTheme.colors.gray300)
                            }
                        }
                    }
                }

                // Column 2: Scanned Document view image (fluid middle)
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(16.dp)
                        .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
                        .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("[ Scanned land deed document scan view. Pinch to zoom ]", color = FieldTheme.colors.gray400)
                }

                // Column 3: Extracted OCR fields list (fluid right)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.gray900)
                        .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(0.dp))
                        .padding(16.dp)
                ) {
                    Text("EXTRACTED FIELDS", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Full Name Match", style = FieldTheme.typography.bodyStrong, color = Color.White)
                    Text("Adaeze Kalu - 94% confidence [OCR]", style = FieldTheme.typography.body, color = FieldTheme.colors.statusSuccess)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("BVN Status Match", style = FieldTheme.typography.bodyStrong, color = Color.White)
                    Text("22109841890 - 99% confidence [CRIT]", style = FieldTheme.typography.body, color = FieldTheme.colors.statusSuccess)
                }
            }
        }
    }
}

// =========================================================================
// 4. AUDITOR SCREEN GRAPHICS
// =========================================================================

@Composable
fun AuditorPhoneDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettingsInline by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Audit Trail", Icons.Outlined.Info),
        NavigationItem("Applications", Icons.AutoMirrored.Outlined.List),
        NavigationItem("Flags", Icons.Outlined.Warning),
        NavigationItem("Export", Icons.Outlined.Share)
    )

    Scaffold(
        bottomBar = {
            if (!showSettingsInline) {
                FieldBottomBar(
                    items = navigationItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = {
                        selectedTab = it
                        showSettingsInline = false
                    }
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showSettingsInline) {
                SettingsScreen(
                    userName = "Amadi Okafor",
                    userEmail = "amadi@mmfb.com",
                    role = UserRole.AUDITOR,
                    onBackClick = { showSettingsInline = false },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        // Home screen layout: Exception count category, quick search, read-only list, export shortcut
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Exception count cards
                            item {
                                Text("COMPLIANCE EXCEPTIONS BY CATEGORY", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f).background(FieldTheme.colors.gray900).clickable { selectedTab = 1 }.padding(8.dp)) {
                                        Text("Expired ID: 3", color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.label)
                                    }
                                    Box(modifier = Modifier.weight(1f).background(FieldTheme.colors.gray900).clickable { selectedTab = 1 }.padding(8.dp)) {
                                        Text("Unsigned: 2", color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.label)
                                    }
                                    Box(modifier = Modifier.weight(1f).background(FieldTheme.colors.gray900).clickable { selectedTab = 1 }.padding(8.dp)) {
                                        Text("Missing GSI: 1", color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.label)
                                    }
                                }
                            }

                            // 2. Quick search by reference
                            item {
                                OutlinedTextField(
                                    value = "",
                                    onValueChange = {},
                                    placeholder = { Text("Quick Reference Search...") },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            // 3. Read-only application cards (compliance status only, no swipe actions)
                            item {
                                Text("COMPLIANCE STATUS DOSSIERS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Adaeze Okonkwo - Approved", "Emeka Chukwu - Needs Review").forEach { app ->
                                        Box(modifier = Modifier.fillMaxWidth().background(FieldTheme.colors.gray900).padding(12.dp)) {
                                            Text(app, style = FieldTheme.typography.bodyStrong)
                                        }
                                    }
                                }
                            }

                            // 4. Export shortcut: Last 7 Days
                            item {
                                PrimaryButton(text = "Export Logs (Last 7 Days)", onClick = {})
                            }
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Auditor Trial View Logs", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                        }
                    }
                }
            }

            // Topbar icons
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onNavigateToSearchResults) {
                    Icon(Icons.Outlined.Search, "Search", tint = FieldTheme.colors.gray400)
                }
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(Icons.Outlined.Notifications, "Notifications", tint = FieldTheme.colors.gray400)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clickable { showSettingsInline = !showSettingsInline }) {
                    RoleBadge(role = "AUD")
                }
            }
        }
    }
}

@Composable
fun AuditorTabletDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedAuditIndex by remember { mutableStateOf(0) }

    val sideRailItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Audit Trail", Icons.Outlined.Info),
        NavigationItem("Applications", Icons.AutoMirrored.Outlined.List),
        NavigationItem("Compliance Flags", Icons.Outlined.Warning),
        NavigationItem("Settings", Icons.Outlined.Settings)
    )

    Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
        // Persistent slate active state side rail
        FieldNavigationRail(
            items = sideRailItems,
            selectedItemIndex = selectedTab,
            onItemSelect = { selectedTab = it }
        )

        if (selectedTab == 4) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SettingsScreen(
                    userName = "Amadi Okafor",
                    userEmail = "amadi@mmfb.com",
                    role = UserRole.AUDITOR,
                    onBackClick = { selectedTab = 0 },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            }
        } else {
            Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // 340dp master list of audit trail events
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.gray900)
                        .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(0.dp))
                        .padding(16.dp)
                ) {
                    Text("AUDIT TRAIL EVENTS", style = FieldTheme.typography.title, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val auditList = listOf("guarantor_bvn changed by Chidi Okafor", "loan_amount approved by Amaka Obi")
                        itemsIndexed(auditList) { idx, name ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx == selectedAuditIndex) FieldTheme.colors.purple950 else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedAuditIndex = idx }
                                    .padding(12.dp)
                            ) {
                                Text(name, style = FieldTheme.typography.bodyStrong, color = if (idx == selectedAuditIndex) Color.White else FieldTheme.colors.gray300)
                            }
                        }
                    }
                }

                // Detail event pane: before/after diffs
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                    Text("EVENT DETAILS & COMPLIANCE DIFFS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(16.dp))
                    FieldCard {
                        Text("Field: guarantor_bvn", style = FieldTheme.typography.bodyStrong)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Before: 2210•••••89", style = FieldTheme.typography.mono, color = FieldTheme.colors.statusDanger)
                        Text("After: 2210984••••89", style = FieldTheme.typography.mono, color = FieldTheme.colors.statusSuccess)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Actor Source: corrected by Chidi Okafor", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    PrimaryButton(text = "Export compliance reports CSV", onClick = {}, modifier = Modifier.width(280.dp))
                }
            }
        }
    }
}

// =========================================================================
// 5. SYSTEM ADMIN SCREEN GRAPHICS
// =========================================================================

@Composable
fun AdminPhoneDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit,
    onNavigateToBorrowers: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettingsInline by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Users", Icons.Outlined.Person),
        NavigationItem("System", Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            if (!showSettingsInline) {
                FieldBottomBar(
                    items = navigationItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = {
                        selectedTab = it
                        showSettingsInline = false
                    }
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showSettingsInline) {
                SettingsScreen(
                    userName = "Root Admin",
                    userEmail = "admin@mainstreet.com",
                    role = UserRole.ADMIN_MCR,
                    onBackClick = { showSettingsInline = false },
                    onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                    onSignOutClick = onLogout
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        // Home screen layout: System health, upsell banner, users read-only
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("SYSTEM OPERATIONS STATUS", style = FieldTheme.typography.title, color = Color.White)
                            FieldCard {
                                Text("Database Node Syncing: Online", color = FieldTheme.colors.statusSuccess)
                                Text("Local SQLite Conflicts: 0", color = FieldTheme.colors.gray300)
                                Text("Active Connected Devices: 128", color = FieldTheme.colors.gray300)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.purple950)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "User and configuration management is available on tablet and desktop.",
                                    style = FieldTheme.typography.body,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    1 -> {
                        // Users tab: Read-only users list
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            Text("USER REGISTRY (READ-ONLY ON PHONE)", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Chidi Okafor - Loan Officer", "Amaka Obi - Branch Manager", "Kemi Ade - Credit Officer")) { user ->
                                    Box(modifier = Modifier.fillMaxWidth().background(FieldTheme.colors.gray900).padding(12.dp)) {
                                        Text(user, style = FieldTheme.typography.bodyStrong)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            PrimaryButton(text = "Manage configurations", onClick = {}, enabled = false)
                        }
                    }
                }
            }

            // Topbar icons
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onNavigateToSearchResults) {
                    Icon(Icons.Outlined.Search, "Search", tint = FieldTheme.colors.gray400)
                }
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(Icons.Outlined.Notifications, "Notifications", tint = FieldTheme.colors.gray400)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clickable { showSettingsInline = !showSettingsInline }) {
                    RoleBadge(role = "ADM")
                }
            }
        }
    }
}

@Composable
fun AdminTabletDashboard(
    onLogout: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearchResults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedUserIndex by remember { mutableStateOf(0) }

    var userNameInput by remember { mutableStateOf("Chidi Okafor") }
    var roleInput by remember { mutableStateOf("Loan Officer") }

    val sideRailItems = listOf(
        NavigationItem("Home", Icons.Outlined.Home),
        NavigationItem("Users", Icons.Outlined.Person),
        NavigationItem("Organisation", Icons.Outlined.Home),
        NavigationItem("System", Icons.Outlined.Settings),
        NavigationItem("Audit", Icons.Outlined.Info)
    )

    Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
        // Persistent structural deep purple active active side rail
        FieldNavigationRail(
            items = sideRailItems,
            selectedItemIndex = selectedTab,
            onItemSelect = { selectedTab = it }
        )

        Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // 340dp User List master
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .background(FieldTheme.colors.gray900)
                    .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(0.dp))
                    .padding(16.dp)
            ) {
                Text("ALL OPERATIONAL USERS", style = FieldTheme.typography.title, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val userList = listOf("Chidi Okafor (Loan Officer)", "Amaka Obi (Branch Manager)")
                    itemsIndexed(userList) { idx, name ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx == selectedUserIndex) FieldTheme.colors.purple950 else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedUserIndex = idx
                                    if (idx == 0) {
                                        userNameInput = "Chidi Okafor"
                                        roleInput = "Loan Officer"
                                    } else {
                                        userNameInput = "Amaka Obi"
                                        roleInput = "Branch Manager"
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Text(name, style = FieldTheme.typography.bodyStrong, color = if (idx == selectedUserIndex) Color.White else FieldTheme.colors.gray300)
                        }
                    }
                }
            }

            // Edit User details form
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                Text("EDIT USER OPERATIONAL ACCOUNT", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(value = userNameInput, onValueChange = { userNameInput = it }, label = "Full Name")
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(value = roleInput, onValueChange = { roleInput = it }, label = "User Role")
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(text = "Save changes", onClick = {}, modifier = Modifier.width(160.dp))
                    OutlinedButton(onClick = {}, colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldTheme.colors.statusDanger), border = BorderStroke(1.dp, FieldTheme.colors.statusDanger), shape = RoundedCornerShape(8.dp), modifier = Modifier.width(160.dp).height(44.dp)) {
                        Text("Deactivate User", style = FieldTheme.typography.bodyStrong)
                    }
                }
            }
        }
    }
}

// =========================================================================
// WIDGET HELPERS FOR COMPACT VIEW LAYOUT
// =========================================================================

@Composable
fun MiniMetricCard(title: String, value: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(title.uppercase(Locale.getDefault()), style = FieldTheme.typography.label.copy(fontSize = 9.sp), color = FieldTheme.colors.gray500)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = FieldTheme.typography.mono.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

@Composable
fun TaskRow(text: String, isPriority: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPriority) FieldTheme.colors.purple950 else FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = FieldTheme.typography.bodyStrong, color = Color.White)
        Text("➔", color = FieldTheme.colors.purple400)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun RecentActivityCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
    }
}

@Composable
fun DossierCard(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
            .border(0.5.dp, color, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text, style = FieldTheme.typography.bodyStrong, color = Color.White)
    }
}

@Composable
fun LargeTile(label: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray900, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label, tint = FieldTheme.colors.purple400, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = FieldTheme.typography.bodyStrong, color = Color.White)
        }
    }
}
