package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun PermissionsPrimerScreen(
    role: UserRole?,
    onContinueClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Determine which permissions are required based on user role
    val currentRole = role ?: UserRole.LOAN_OFFICER
    val needsCamera = currentRole == UserRole.LOAN_OFFICER
    val needsLocation = currentRole == UserRole.LOAN_OFFICER || currentRole == UserRole.BRANCH_MANAGER
    val needsNotifications = true // all roles need notifications

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        if (isTablet) {
            // Tablet Layout: Centered Card
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                FieldCard {
                    PermissionsList(
                        needsCamera = needsCamera,
                        needsLocation = needsLocation,
                        needsNotifications = needsNotifications,
                        onContinueClick = onContinueClick,
                        isTablet = true
                    )
                }
            }
        } else {
            // Phone Layout: Full Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                PermissionsList(
                    needsCamera = needsCamera,
                    needsLocation = needsLocation,
                    needsNotifications = needsNotifications,
                    onContinueClick = onContinueClick,
                    isTablet = false
                )
            }
        }
    }
}

@Composable
fun PermissionsList(
    needsCamera: Boolean,
    needsLocation: Boolean,
    needsNotifications: Boolean,
    onContinueClick: () -> Unit,
    isTablet: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Before we start",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "To allow FieldCRM to perform credit operations properly, we require the following permissions:",
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (needsCamera) {
            PermissionRow(
                emoji = "📷",
                title = "Camera",
                description = "To photograph loan applications and guarantor forms"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (needsLocation) {
            PermissionRow(
                emoji = "📍",
                title = "Location",
                description = "To log field visit coordinates accurately for verification"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (needsNotifications) {
            PermissionRow(
                emoji = "🔔",
                title = "Notifications",
                description = "To alert you instantly when a loan application requires your action"
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (!isTablet) {
            Spacer(modifier = Modifier.weight(1f))
        }

        PrimaryButton(
            text = "Continue",
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}

@Composable
fun PermissionRow(emoji: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 16.dp, top = 2.dp)
        )
        Column {
            Text(
                text = title,
                style = FieldTheme.typography.bodyStrong.copy(fontSize = 15.sp),
                color = FieldTheme.colors.gray100
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                color = FieldTheme.colors.gray400
            )
        }
    }
}
