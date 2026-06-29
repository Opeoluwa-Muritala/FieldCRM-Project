package com.fieldcrm.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.theme.FieldIcons

@Composable
fun ComponentsDemoScreen() {
    var textValue by remember { mutableStateOf("Adaeze Okonkwo") }
    var amountValue by remember { mutableStateOf("250000") }
    var dropdownValue by remember { mutableStateOf("Ikeja, Lagos State") }

    val navItems = listOf(
        NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
        NavigationItem("Borrowers", FieldIcons.SearchOutlined, FieldIcons.SearchFilled),
        NavigationItem("Applications", FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
        NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
    )
    var selectedNavIndex by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        FieldTopAppBar(
            title = "FieldCRM System Primitives",
            actions = {
                RoleBadge(role = "Branch Manager")
            }
        )

        Row(modifier = Modifier.weight(1f)) {
            // Show NavigationRail side-by-side if layout is wider (simulated in preview layout)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Section: Timeline
                Text(
                    text = "Timeline / Stepper",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                LoanStageTimeline(
                    stages = listOf("Intake", "OCR Review", "Credit Review", "BM Approval", "Disbursed"),
                    currentStageIndex = 2
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Section: Status and Badges
                Text(
                    text = "Status & Role Badges",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(variant = StatusChipVariant.Verified)
                    StatusChip(variant = StatusChipVariant.LowConfidence)
                    StatusChip(variant = StatusChipVariant.NeedsReview)
                    StatusChip(variant = StatusChipVariant.Missing)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleBadge(role = "Loan Officer")
                    RoleBadge(role = "Credit Officer")
                    RoleBadge(role = "Auditor")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Confidence and Tags
                Text(
                    text = "Confidence & Attribution",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConfidenceBar(percentage = 0.85f)
                Spacer(modifier = Modifier.height(4.dp))
                ConfidenceBar(percentage = 0.45f)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SourceTag(source = "ocr")
                    SourceTag(source = "corrected")
                    SourceTag(source = "approved")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Surfaces
                Text(
                    text = "Surfaces & Dividers",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                FieldCard {
                    Text(
                        text = "Primary Applicant Details",
                        style = FieldTheme.typography.title,
                        color = FieldTheme.colors.gray100
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BVN and NIN verified dynamically against credit registry records.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray300
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Officer Check: Emeka Chukwu (LGA: Gwale, Kano)",
                        style = FieldTheme.typography.bodyStrong,
                        color = FieldTheme.colors.purple200
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Inputs
                Text(
                    text = "Input Fields & Forms",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                FieldTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = "Applicant Full Name",
                    isRequired = true,
                    helperText = "As it appears on NIN/BVN cards"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldAmountField(
                    value = amountValue,
                    onValueChange = { amountValue = it },
                    label = "Requested Loan Amount",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldDropdown(
                    value = dropdownValue,
                    options = listOf("Ikeja, Lagos State", "Gwale, Kano State", "Enugu North, Enugu State"),
                    onOptionSelected = { dropdownValue = it },
                    label = "Branch / LGA Office"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Document and Uploads
                Text(
                    text = "Document Management",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                DocumentThumbnail(
                    fileName = "NIN_Slip_Adaeze_Okonkwo.pdf",
                    fileSize = "1.2 MB",
                    fileType = "pdf"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldUploadDropzone(
                    title = "Upload Land Registry Pledge Deed",
                    subtitle = "Supported formats: PDF, JPEG, PNG up to 10MB",
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Action Buttons
                Text(
                    text = "Standard Actions",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(text = "Approve Application", onClick = {})
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(text = "Return to Intake Officer", onClick = {})
                Spacer(modifier = Modifier.height(8.dp))
                DangerButton(text = "Reject & Flag Fraudulent", onClick = {})

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Feedback
                Text(
                    text = "Feedback & Shimmer",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                LoadingSkeleton(height = 60.dp, width = 200.dp, cornerRadius = 8.dp)
                Spacer(modifier = Modifier.height(12.dp))
                ErrorBanner(text = "Failed to sync loan dossier with central repository. Network timed out.", onRetry = {})
                Spacer(modifier = Modifier.height(12.dp))
                EmptyState(
                    text = "No pending compliance checklist logs found.",
                    linkText = "Create New Log",
                    onLinkClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Interactive Signature Pad
                Text(
                    text = "Interactive Signature Capture",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                FieldSignaturePad(onConfirm = {}, onClear = {})

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Audit Trail
                Text(
                    text = "Audit Log Entry",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                AuditTrailEntry(
                    timestamp = "2026-06-18 11:24:10",
                    actorName = "Fatima Al-Hassan",
                    actorRole = "Credit Officer",
                    action = "Reduced loan ceiling from ₦300,000 to ₦250,000 due to DTI recalculation",
                    diff = "- ApprovedAmount: 300,000\n+ ApprovedAmount: 250,000",
                    isCurrentUserAction = false
                )
                Spacer(modifier = Modifier.height(12.dp))
                AuditTrailEntry(
                    timestamp = "2026-06-18 12:05:43",
                    actorName = "Samuel Adebayo",
                    actorRole = "Branch Manager",
                    action = "Initiated final workspace sync and certificate review",
                    diff = null,
                    isCurrentUserAction = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        FieldBottomBar(
            items = navItems,
            selectedItemIndex = selectedNavIndex,
            onItemSelect = { selectedNavIndex = it }
        )
    }
}

// ==========================================
// PREVIEWS FOR RESPONSIVE BREAKPOINTS
// ==========================================

@Preview(name = "Compact Phone - 411dp", widthDp = 411, heightDp = 850)
@Composable
fun PreviewCompactPhone() {
    FieldCRMTheme {
        ComponentsDemoScreen()
    }
}

@Preview(name = "Small Phone - 320dp", widthDp = 320, heightDp = 640)
@Composable
fun PreviewSmallPhone() {
    FieldCRMTheme {
        ComponentsDemoScreen()
    }
}

@Preview(name = "Foldable Folded - 360dp", widthDp = 360, heightDp = 720)
@Composable
fun PreviewFoldableFolded() {
    FieldCRMTheme {
        ComponentsDemoScreen()
    }
}

@Preview(name = "Foldable Unfolded - 673dp", widthDp = 673, heightDp = 800)
@Composable
fun PreviewFoldableUnfolded() {
    FieldCRMTheme {
        Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
            FieldNavigationRail(
                items = listOf(
                    NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                    NavigationItem("Borrowers", FieldIcons.SearchOutlined, FieldIcons.SearchFilled),
                    NavigationItem("Applications", FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
                    NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
                ),
                selectedItemIndex = 1,
                onItemSelect = {}
            )
            Box(modifier = Modifier.weight(1f)) {
                ComponentsDemoScreen()
            }
        }
    }
}

@Preview(name = "Tablet - 1280dp", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewTablet() {
    FieldCRMTheme {
        Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
            FieldNavigationRail(
                items = listOf(
                    NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                    NavigationItem("Borrowers", FieldIcons.SearchOutlined, FieldIcons.SearchFilled),
                    NavigationItem("Applications", FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
                    NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
                ),
                selectedItemIndex = 1,
                onItemSelect = {}
            )
            Box(modifier = Modifier.weight(1f)) {
                ComponentsDemoScreen()
            }
        }
    }
}

@Preview(name = "Landscape Phone - 640x360dp", widthDp = 640, heightDp = 360)
@Composable
fun PreviewLandscapePhone() {
    FieldCRMTheme {
        ComponentsDemoScreen()
    }
}
