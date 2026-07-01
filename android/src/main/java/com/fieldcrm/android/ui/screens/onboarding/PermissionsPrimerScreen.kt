package com.fieldcrm.android.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun PermissionsPrimerScreen(
    role: UserRole?,
    onContinueClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val currentRole = role ?: UserRole.LOAN_OFFICER
    val needsCamera = currentRole == UserRole.LOAN_OFFICER
    val needsLocation = currentRole == UserRole.LOAN_OFFICER || currentRole == UserRole.BRANCH_MANAGER
    val needsNotifications = true

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
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                FieldCard {
                    PermissionsContent(
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
                PermissionsContent(
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
fun PermissionsContent(
    needsCamera: Boolean,
    needsLocation: Boolean,
    needsNotifications: Boolean,
    onContinueClick: () -> Unit,
    isTablet: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Brand Mark
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FieldIcons.ShieldOutlined,
                contentDescription = "Brand Mark",
                tint = FieldTheme.colors.purple600,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Before we start",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "FieldCRM needs a few permissions to help you manage your daily operations securely and efficiently.",
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (needsCamera) {
            PermissionRow(
                icon = FieldIcons.CameraOutlined,
                title = "Camera Access",
                description = "Required for secure document scanning and client identity verification in the field."
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (needsLocation) {
            PermissionRow(
                icon = FieldIcons.LocationOutlined,
                title = "Location Services",
                description = "Enables accurate field verification, geotagging visits, and optimizing your daily route."
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (needsNotifications) {
            PermissionRow(
                icon = FieldIcons.BellFilled,
                title = "Push Notifications",
                description = "Stay updated with real-time task assignments, crucial alerts, and schedule changes."
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (!isTablet) {
            Spacer(modifier = Modifier.weight(1f))
        }

        PrimaryButton(
            text = "Continue",
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(
            text = "Skip for now",
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PermissionRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(48.dp)
                .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = FieldTheme.colors.purple600,
                modifier = Modifier.size(24.dp)
            )
        }
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

