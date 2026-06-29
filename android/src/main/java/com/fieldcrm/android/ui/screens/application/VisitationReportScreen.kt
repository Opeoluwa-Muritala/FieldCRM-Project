package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun VisitationReportScreen(
    onBackClick: () -> Unit,
    onSubmit: () -> Unit
) {
    var remarks by remember { mutableStateOf("Visually confirmed business operational stock and evaluated trade kiosk.") }
    var locationState by remember { mutableStateOf("Lat: 6.5244° N, Lon: 3.3792° E (± 4m Accuracy)") }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Field Verification Report",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                            text = "Site Visitation Audit Report",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        
                        // GPS Stamped Card
                        FieldCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = "GPS Location",
                                    tint = FieldTheme.colors.purple400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "GPS STAMPED COORDINATES",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = locationState,
                                        style = FieldTheme.typography.mono.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.purple200
                                    )
                                }
                            }
                        }
                        
                        // Camera capture mock
                        FieldCard {
                            Text(
                                text = "VISITATION PHOTO CAPTURE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(FieldTheme.colors.gray800, RoundedCornerShape(8.dp))
                                    .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "[ CameraX Preview Screen Mock ]",
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray400
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "GPS Metadata will be embedded in exif format",
                                        style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                        color = FieldTheme.colors.gray500
                                    )
                                }
                            }
                        }
                        
                        // Remarks Form
                        FieldCard {
                            Text(
                                text = "FIELD OBSERVATION LOG",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldTextField(
                                value = remarks,
                                onValueChange = { remarks = it },
                                label = "Verification Remarks",
                                isRequired = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Notes,
                                        contentDescription = "Remarks",
                                        tint = FieldTheme.colors.gray500
                                    )
                                }
                            )
                        }

                        // Verifying Officer Signature
                        FieldCard {
                            Text(
                                text = "OFFICER ATTESTATION SIGNATURE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldSignaturePad(
                                onConfirm = {},
                                onClear = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PrimaryButton(
                            text = "Submit Field Verification Dossier",
                            onClick = onSubmit
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

@Preview(name = "Compact Phone Visitation", widthDp = 411, heightDp = 850)
@Composable
fun PreviewVisitationCompact() {
    FieldCRMTheme {
        VisitationReportScreen(onBackClick = {}, onSubmit = {})
    }
}
