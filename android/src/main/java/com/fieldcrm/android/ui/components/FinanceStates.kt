package com.fieldcrm.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun FinanceListSkeleton(modifier: Modifier = Modifier, rows: Int = 5) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items((0 until rows).toList()) {
            FieldCard(Modifier.fillMaxWidth()) {
                LoadingSkeleton(height = 18.dp, width = 180.dp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LoadingSkeleton(height = 14.dp, width = 96.dp)
                    LoadingSkeleton(height = 14.dp, width = 72.dp)
                }
            }
        }
    }
}

@Composable
fun FinanceEmptyState(title: String, explanation: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
        Spacer(Modifier.height(8.dp))
        Text(explanation, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
    }
}

@Composable
fun FinanceErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Unable to load this workspace", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
        Spacer(Modifier.height(8.dp))
        Text(message, style = FieldTheme.typography.body, color = FieldTheme.colors.statusDanger)
        Spacer(Modifier.height(20.dp))
        SecondaryButton("Try again", onRetry)
    }
}
