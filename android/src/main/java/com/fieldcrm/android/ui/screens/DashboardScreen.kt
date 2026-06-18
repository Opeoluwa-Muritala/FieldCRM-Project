package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fieldcrm.android.sync.AndroidSyncWorker

@Composable
fun DashboardScreenView(
    onNavigateToBorrowers: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FieldCRM Dashboard") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Welcome to FieldCRM",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            DashboardCard(
                title = "Borrowers",
                description = "Manage borrower profiles",
                onClick = onNavigateToBorrowers
            )

            Spacer(modifier = Modifier.height(16.dp))

            DashboardCard(
                title = "Applications",
                description = "View and manage loan applications",
                onClick = onNavigateToApplications
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    WorkManager.getInstance().enqueueUniqueWork(
                        "field_crm_sync",
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequestBuilder<AndroidSyncWorker>().build()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync with Server")
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
