package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit
) {
    var biometricEnabled by remember { mutableStateOf(true) }
    var pinningEnabled by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf("15 Minutes") }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Console Settings",
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
                        // User Profile Card
                        FieldCard {
                            Text("USER PROFILE DETAILS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(FieldTheme.colors.purple900, RoundedCornerShape(22.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("CO", color = FieldTheme.colors.purple400, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Chidi Okafor", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                                    Text("chidi@mainstreetbank.com", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                RoleBadge(role = "Credit Officer")
                            }
                        }
                        
                        // Device Security
                        FieldCard {
                            Text("DEVICE SECURITY CREDENTIALS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Biometric Key Unlock", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                    Text("Use fingerprint scanner for fast authentication.", style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray400)
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { biometricEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = FieldTheme.colors.purple400,
                                        checkedTrackColor = FieldTheme.colors.purple600
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("HTTPS Certificate Pinning", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                    Text("Enforce secure connection to Mainstreet server keys.", style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray400)
                                }
                                Switch(
                                    checked = pinningEnabled,
                                    onCheckedChange = { pinningEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = FieldTheme.colors.purple400,
                                        checkedTrackColor = FieldTheme.colors.purple600
                                    )
                                )
                            }
                        }

                        // WorkManager background sync setup
                        FieldCard {
                            Text("BACKGROUND SYNCHRONISATION", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            FieldDropdown(
                                value = selectedInterval,
                                options = listOf("15 Minutes", "30 Minutes", "1 Hour", "4 Hours"),
                                onOptionSelected = { selectedInterval = it },
                                label = "Worker Sync Interval"
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Offline Mutation Queue", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                Text(
                                    text = "VIEW QUEUE",
                                    style = FieldTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                                    color = FieldTheme.colors.purple500,
                                    modifier = Modifier.clickable(onClick = onNavigateToOfflineQueue)
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

@Preview(name = "Compact Phone Settings", widthDp = 411, heightDp = 850)
@Composable
fun PreviewSettingsCompact() {
    FieldCRMTheme {
        SettingsScreen(onBackClick = {}, onNavigateToOfflineQueue = {})
    }
}
