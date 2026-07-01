package com.fieldcrm.android.ui.screens.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme

private data class VisitDueItem(
    val borrowerName: String,
    val address: String,
    val visitId: String
)

private val placeholderVisits = listOf(
    VisitDueItem("Adaeze Okonkwo", "12 Adeola Odeku St, Victoria Island, Lagos", "visit_001"),
    VisitDueItem("Emeka Chukwu", "45 Allen Avenue, Ikeja, Lagos", "visit_002"),
    VisitDueItem("Ngozi Adeyemi", "7 Wuse Zone 5, Abuja", "visit_003"),
    VisitDueItem("Fatima Bello", "22 Awolowo Road, Ikoyi, Lagos", "visit_004")
)

@Composable
fun VisitsDueScreen(
    onBackClick: () -> Unit,
    onStartVisit: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Visits Due Today",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .background(
                                FieldTheme.colors.gray800,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .border(
                                0.5.dp,
                                FieldTheme.colors.gray700,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${placeholderVisits.size} DUE",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.statusWarning
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(4) {
                        FieldCard(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                            Column {
                                LoadingSkeleton(height = 16.dp, width = 160.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LoadingSkeleton(height = 12.dp, width = 220.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                LoadingSkeleton(height = 32.dp, width = 110.dp, cornerRadius = 6.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(placeholderVisits) { visit ->
                        FieldCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = visit.borrowerName,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = visit.address,
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray400
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                FieldTheme.colors.purple950,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                FieldTheme.colors.purple400,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { onStartVisit(visit.visitId) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "START VISIT",
                                            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                                            color = FieldTheme.colors.purple200
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = FieldIcons.InfoOutlined,
                                    contentDescription = "Location",
                                    tint = FieldTheme.colors.gray600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Visits Due Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewVisitsDueScreen() {
    FieldCRMTheme {
        VisitsDueScreen(onBackClick = {})
    }
}
