package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun CreateApplicationScreenView(
    viewModel: ApplicationViewModel,
    borrowers: List<BorrowerModel>,
    onApplicationCreated: (LoanApplicationModel) -> Unit,
    onBackClick: () -> Unit
) {
    var expandedBorrower by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Application") },
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
                Box {
                    OutlinedTextField(
                        value = viewModel.selectedBorrowerForApp.value?.name ?: "",
                        onValueChange = {},
                        label = { Text("Select Borrower") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedBorrower = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = expandedBorrower,
                        onDismissRequest = { expandedBorrower = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        borrowers.forEach { borrower ->
                            DropdownMenuItem(
                                text = { Text(borrower.name) },
                                onClick = {
                                    viewModel.setSelectedBorrowerForApp(borrower)
                                    expandedBorrower = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.newAppAmount.value,
                    onValueChange = { viewModel.setNewAppAmount(it) },
                    label = { Text("Loan Amount (NGN)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.newAppTenure.value,
                    onValueChange = { viewModel.setNewAppTenure(it) },
                    label = { Text("Tenure (months)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.newAppInterestRate.value,
                    onValueChange = { viewModel.setNewAppInterestRate(it) },
                    label = { Text("Interest Rate (%)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (viewModel.errorMessage.value != null) {
                    Text(
                        text = viewModel.errorMessage.value!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        viewModel.createApplication { newApp ->
                            onApplicationCreated(newApp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.selectedBorrowerForApp.value != null &&
                        viewModel.newAppAmount.value.isNotEmpty() &&
                        viewModel.newAppTenure.value.isNotEmpty() &&
                        !viewModel.isLoading.value
                ) {
                    if (viewModel.isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Application")
                    }
                }
            }
        }
    }
}
