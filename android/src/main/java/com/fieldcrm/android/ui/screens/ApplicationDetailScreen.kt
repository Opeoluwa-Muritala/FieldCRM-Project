package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun ApplicationDetailScreenView(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Details") },
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
                Text(
                    text = borrower?.name ?: "Unknown",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                DetailField("Loan Amount", "NGN ${application.amount}")
                DetailField("Tenure", "${application.tenure} months")
                DetailField("Product Type", application.product_type)
                DetailField("Interest Rate", "${application.interest_rate}%")
                DetailField("Status", application.status)
                DetailField("Current Stage", "Stage ${application.current_stage}")
                DetailField("Repayment Frequency", application.repayment_frequency)

                application.collateral_desc?.let {
                    DetailField("Collateral", it)
                }
                application.collateral_value?.let {
                    DetailField("Collateral Value", "NGN $it")
                }
                application.officer_recommendation?.let {
                    DetailField("Officer Recommendation", it)
                }
            }
        }
    }
}
