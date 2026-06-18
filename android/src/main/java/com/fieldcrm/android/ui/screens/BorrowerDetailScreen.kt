package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcrm.shared.model.BorrowerModel

@Composable
fun BorrowerDetailScreenView(
    borrower: BorrowerModel,
    onBackClick: () -> Unit,
    onCreateApplication: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Borrower Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                DetailField("Name", borrower.name)
                DetailField("Phone", borrower.phone)
                DetailField("BVN", borrower.bvn)
                DetailField("NIN", borrower.nin)
                DetailField("Status", borrower.status)
                borrower.physical_address?.let { DetailField("Address", it) }
                borrower.employment_status?.let { DetailField("Employment", it) }
                borrower.employer_name?.let { DetailField("Employer", it) }
                borrower.monthly_income?.let { DetailField("Monthly Income", "NGN $it") }
                borrower.bank_name?.let { DetailField("Bank", it) }
                borrower.account_number?.let { DetailField("Account", it) }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateApplication,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Application")
                }
            }
        }
    }
}
