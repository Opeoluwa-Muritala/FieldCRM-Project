package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldDivider
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "Chidi Okafor",
    userEmail: String = "chidi@mmfb.com",
    role: UserRole? = UserRole.LOAN_OFFICER,
    onBackClick: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    var faceIdEnabled by remember { mutableStateOf(true) }
    var pushEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    if (showSignOutConfirmation) {
        // Full-page sign-out confirmation (not a popup)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FieldTheme.colors.gray950),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                ) {
                    FieldCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Sign Out",
                                style = FieldTheme.typography.display.copy(fontSize = 20.sp),
                                color = FieldTheme.colors.gray100,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Are you sure you want to sign out? Any unsynced data will be saved and sent when you sign back in.",
                                style = FieldTheme.typography.body,
                                color = FieldTheme.colors.gray400,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            PrimaryButton(
                                text = "Sign Out",
                                onClick = onSignOutClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showSignOutConfirmation = false },
                                shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = FieldTheme.colors.gray400
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = FieldTheme.typography.bodyStrong,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Profile & Settings",
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
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
                            // User Info Card
                            FieldCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val initials = userName.split(" ")
                                        .mapNotNull { it.firstOrNull()?.toString() }
                                        .joinToString("")
                                        .uppercase()

                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(FieldTheme.colors.purple900, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            style = FieldTheme.typography.title.copy(fontSize = 18.sp),
                                            color = FieldTheme.colors.purple400,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = userName,
                                            style = FieldTheme.typography.title,
                                            color = FieldTheme.colors.gray100
                                        )
                                        Text(
                                            text = "${role?.displayName ?: "Loan Officer"} · Ikeja Branch",
                                            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                            color = FieldTheme.colors.gray400
                                        )
                                    }
                                }
                            }

                            // Account Section
                            FieldCard {
                                Text(
                                    text = "ACCOUNT",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SettingsRow(label = "Change Password") {}
                                FieldDivider()
                                SettingsRow(label = "Update Phone Number") {}
                                FieldDivider()
                                SettingsRow(label = "Offline Sync Queue") { onNavigateToOfflineQueue() }
                            }

                            // Preferences Section
                            FieldCard {
                                Text(
                                    text = "PREFERENCES",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Enable Face ID",
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray300
                                    )
                                    Switch(
                                        checked = faceIdEnabled,
                                        onCheckedChange = { faceIdEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = FieldTheme.colors.purple600,
                                            uncheckedThumbColor = FieldTheme.colors.gray500,
                                            uncheckedTrackColor = FieldTheme.colors.gray800
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Push Notifications",
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray300
                                    )
                                    Switch(
                                        checked = pushEnabled,
                                        onCheckedChange = { pushEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = FieldTheme.colors.purple600,
                                            uncheckedThumbColor = FieldTheme.colors.gray500,
                                            uncheckedTrackColor = FieldTheme.colors.gray800
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Dark Mode",
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray300
                                    )
                                    Switch(
                                        checked = darkModeEnabled,
                                        onCheckedChange = { darkModeEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = FieldTheme.colors.purple600,
                                            uncheckedThumbColor = FieldTheme.colors.gray500,
                                            uncheckedTrackColor = FieldTheme.colors.gray800
                                        )
                                    )
                                }
                            }

                            // Support Section
                            FieldCard {
                                Text(
                                    text = "SUPPORT",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsRow(label = "Help Center") {}
                                FieldDivider()
                                SettingsRow(label = "Contact IT Support") {}
                                FieldDivider()
                                SettingsRow(label = "Report a Problem") {}
                            }

                            // Version Label
                            Text(
                                text = "App Version 2.4.1",
                                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                color = FieldTheme.colors.gray500,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )

                            // Sign Out Button (Danger-tinted ghost button)
                            Button(
                                onClick = { showSignOutConfirmation = true },
                                shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = FieldTheme.colors.statusDanger
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FieldTheme.colors.statusDanger.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Sign Out",
                                    style = FieldTheme.typography.bodyStrong,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp), // 8-point grid padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = FieldTheme.typography.bodyStrong,
            color = FieldTheme.colors.gray300
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = FieldTheme.colors.gray500,
            modifier = Modifier.size(24.dp) // 24dp size target
        )
    }
}
