package com.fieldcrm.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun UserRow(name: String, email: String, role: String, status: String, onClick: () -> Unit = {}) =
    DomainRow(name, "$email | $role", status, onClick)

@Composable
fun SystemActivityRow(action: String, actor: String, timestamp: String, onClick: () -> Unit = {}) =
    DomainRow(action, actor, timestamp, onClick)

@Composable
fun ApplicationQueueRow(customer: String, reference: String, readiness: String, onClick: () -> Unit = {}) =
    DomainRow(customer, reference, readiness, onClick)

@Composable
fun DocumentRow(name: String, uploadState: String, ocrState: String, onClick: () -> Unit = {}) =
    DomainRow(name, uploadState, ocrState, onClick)

@Composable
fun ComplianceGateRow(label: String, value: String, status: String) = DomainRow(label, value, status)

@Composable
fun GuarantorRow(name: String, contact: String, verification: String, onClick: () -> Unit = {}) =
    DomainRow(name, contact, verification, onClick)

@Composable
fun CollateralRow(description: String, value: String, valuationState: String, onClick: () -> Unit = {}) =
    DomainRow(description, value, valuationState, onClick)

@Composable
fun WorkflowEventRow(action: String, actor: String, timestamp: String, onClick: () -> Unit = {}) =
    DomainRow(action, actor, timestamp, onClick)

@Composable
fun MccDecisionRow(member: String, recommendation: String, amount: String, onClick: () -> Unit = {}) =
    DomainRow(member, recommendation, amount, onClick)

@Composable
private fun DomainRow(title: String, detail: String, trailing: String, onClick: () -> Unit = {}) {
    FieldCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
                Text(detail.ifBlank { "Not available" }, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
            }
            Text(trailing.ifBlank { "Not available" }, style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
        }
    }
}
