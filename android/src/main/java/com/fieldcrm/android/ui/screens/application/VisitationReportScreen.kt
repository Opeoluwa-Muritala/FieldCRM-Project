package com.fieldcrm.android.ui.screens.application

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun VisitationReportScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    borrowerViewModel: BorrowerViewModel,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit
) {
    VisitationReportContent(
        application = application,
        borrower = borrower,
        onBackClick = onBackClick,
        onSubmitComplete = { locationState, remarks, metWith, premises, direction ->
            val updatedBorrower = borrower?.copy(gps_coordinates = locationState)
            val updatedApp = application.copy(officer_recommendation = remarks)
            if (updatedBorrower != null) {
                borrowerViewModel.updateBorrowerLocal(updatedBorrower) {}
            }
            applicationViewModel.updateApplicationLocal(updatedApp) {}
            applicationViewModel.submitVisitationReport(
                id = application.id,
                metWith = metWith,
                premises = premises,
                direction = direction,
                onSuccess = onSubmit
            )
        }
    )
}

@Composable
fun VisitationReportContent(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    onBackClick: () -> Unit,
    onSubmitComplete: (locationState: String, remarks: String, metWith: String, premises: String, direction: String) -> Unit
) {
    val context = LocalContext.current
    var remarks by remember { mutableStateOf(application.officer_recommendation ?: "") }
    var locationState by remember { mutableStateOf(borrower?.gps_coordinates ?: "Click refresh to lock GPS coordinates") }
    var isRefreshingGPS by remember { mutableStateOf(false) }
    var visitDate by remember { mutableStateOf("") }
    var visitTime by remember { mutableStateOf("") }
    var personMet by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var premisesDescription by remember { mutableStateOf("") }
    var directionFromBranch by remember { mutableStateOf("") }
    var businessCondition by remember { mutableStateOf("") }
    var accountOfficer by remember { mutableStateOf("") }

    var showCameraScanner by remember { mutableStateOf(false) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }

    // Rotate refresh icon animation
    val infiniteTransition = rememberInfiniteTransition(label = "gpsRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gpsRotation"
    )

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                querySystemGps(context) { locationState = it }
            } else {
                locationState = "Location Permission Denied"
            }
        }
    )

    LaunchedEffect(isRefreshingGPS) {
        if (isRefreshingGPS) {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                querySystemGps(context) {
                    locationState = it
                    isRefreshingGPS = false
                }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                isRefreshingGPS = false
            }
        }
    }

    if (showCameraScanner) {
        CameraOcrScanner(
            mode = "PHOTO",
            onTextScanned = { showCameraScanner = false },
            onPhotoCaptured = { path ->
                capturedPhotoPath = path
                showCameraScanner = false
            },
            onDismiss = { showCameraScanner = false }
        )
    } else {
        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Field Verification Report",
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = FieldIcons.ArrowBackOutlined,
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
                            val borrowerName = borrower?.name ?: "Applicant"
                            Text(
                                text = "Site Verification Audit: $borrowerName",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            
                            // GPS Stamped Card
                            FieldCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = FieldIcons.LocationOutlined,
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
                                    
                                    IconButton(
                                        onClick = { isRefreshingGPS = true },
                                        enabled = !isRefreshingGPS,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(4.dp))
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.RefreshOutlined,
                                            contentDescription = "Refresh GPS",
                                            tint = FieldTheme.colors.gray400,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .graphicsLayer(rotationZ = if (isRefreshingGPS) rotation else 0f)
                                        )
                                    }
                                }
                            }
                            
                            // Camera capture block
                            FieldCard {
                                Text(
                                    text = "VISITATION PHOTO CAPTURE",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val localBitmap = remember(capturedPhotoPath) {
                                    capturedPhotoPath?.let { path ->
                                        BitmapFactory.decodeFile(path)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(FieldTheme.colors.gray800)
                                        .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (localBitmap != null) {
                                        Image(
                                            bitmap = localBitmap.asImageBitmap(),
                                            contentDescription = "Visitation Site Capture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Overlaid retake button
                                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                                            PrimaryButton(
                                                text = "Retake",
                                                onClick = { showCameraScanner = true }
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = FieldIcons.CameraOutlined,
                                                contentDescription = "Camera",
                                                tint = FieldTheme.colors.purple400,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Capture a real site verification photo to attach to application dossier.",
                                                style = FieldTheme.typography.bodyStrong.copy(fontSize = 13.sp),
                                                color = FieldTheme.colors.gray300,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            PrimaryButton(
                                                text = "Take Site Photo",
                                                onClick = { showCameraScanner = true }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Visit Details Form
                            FieldCard {
                                Text(
                                    text = "VISIT DETAILS",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                FieldTextField(
                                    value = visitDate,
                                    onValueChange = { visitDate = it },
                                    label = "Date of Visitation",
                                    isRequired = true,
                                    placeholder = "YYYY-MM-DD"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = visitTime,
                                    onValueChange = { visitTime = it },
                                    label = "Time of Arrival",
                                    placeholder = "e.g. 10:30 AM"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = personMet,
                                    onValueChange = { personMet = it },
                                    label = "Person Met",
                                    isRequired = true,
                                    placeholder = "e.g. Customer / Spouse / Landlord"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = relationship,
                                    onValueChange = { relationship = it },
                                    label = "Relationship to Applicant"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = premisesDescription,
                                    onValueChange = { premisesDescription = it },
                                    label = "Premises Description",
                                    isRequired = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = directionFromBranch,
                                    onValueChange = { directionFromBranch = it },
                                    label = "Direction from Branch",
                                    isRequired = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = businessCondition,
                                    onValueChange = { businessCondition = it },
                                    label = "Business Condition Observed"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                FieldTextField(
                                    value = accountOfficer,
                                    onValueChange = { accountOfficer = it },
                                    label = "Account Officer",
                                    isRequired = true
                                )
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
                                            imageVector = FieldIcons.PenOutlined,
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
                                onClick = { onSubmitComplete(locationState, remarks, personMet, premisesDescription, directionFromBranch) },
                                enabled = remarks.isNotEmpty() && visitDate.isNotEmpty() && personMet.isNotEmpty() && !isRefreshingGPS
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun querySystemGps(context: Context, onResult: (String) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        onResult("Location Permission Denied")
        return
    }
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    try {
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        val provider = when {
            isGpsEnabled -> android.location.LocationManager.GPS_PROVIDER
            isNetworkEnabled -> android.location.LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider != null) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                onResult(String.format(Locale.US, "Lat: %.6f° N, Lon: %.6f° E (Real GPS)", location.latitude, location.longitude))
            } else {
                onResult("Lat: 6.524451° N, Lon: 3.379219° E (Mock: Searching...)")
            }
        } else {
            onResult("GPS Disabled on Device")
        }
    } catch (e: Exception) {
        onResult("GPS Error: ${e.localizedMessage}")
    }
}

@Preview(name = "Compact Phone Visitation", widthDp = 411, heightDp = 850)
@Composable
fun PreviewVisitationCompact() {
    val demoApp = LoanApplicationModel(
        id = "app_1", org_id = "org_1", borrower_id = "1",
        current_stage = 1, current_owner_id = "LO_1", status = "intake",
        amount = 180000.0, tenure = 4, product_type = "Working Capital",
        interest_rate = 18.5, repayment_frequency = "MONTHLY", created_at = ""
    )
    val demoBorrower = BorrowerModel(
        id = "1", org_id = "org_1", loan_officer_id = "LO_1",
        name = "Adaeze Okonkwo", phone = "08012345678", bvn = "222333444", nin = "111222333",
        status = "Active", created_at = ""
    )
    FieldCRMTheme {
        VisitationReportContent(application = demoApp, borrower = demoBorrower, onBackClick = {}, onSubmitComplete = { _, _, _, _, _ -> })
    }
}
