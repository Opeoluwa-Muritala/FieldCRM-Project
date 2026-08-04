package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.data.repository.ApplicationDetailResult
import com.fieldcrm.android.ui.components.LoadingSkeleton
import com.fieldcrm.android.ui.components.SectionCard
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun DatabaseDossierSections(detail: ApplicationDetailResult?, isLoading: Boolean) {
    if (isLoading) {
        SectionCard(title = "Loading dossier") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { LoadingSkeleton(height = 16.dp, width = 260.dp) }
            }
        }
        return
    }
    if (detail == null) {
        SectionCard(title = "Dossier details") {
            Text("Dossier details could not be loaded. Refresh the application and try again.", style = FieldTheme.typography.body, color = FieldTheme.colors.statusDanger)
        }
        return
    }

    DetailMapSection("Application intake", detail.intake)
    DetailMapSection("Readiness and compliance", detail.readiness)
    SectionCard(title = "Documents (${detail.documents.size})") {
        if (detail.documents.isEmpty()) {
            UnavailableText("No documents were returned for this application.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                detail.documents.forEachIndexed { index, document ->
                    val verified = (document["verified"] as? Boolean)?.let { if (it) "Verified" else "Not verified" } ?: "Not available"
                    DetailItem(label = "Document ${index + 1}", value = document["doc_type"].displayValue())
                    DetailItem(label = "Verification", value = verified)
                    if (index != detail.documents.lastIndex) Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
    DetailMapSection("Visitation", detail.visitation)
    SectionCard(title = "Workflow history (${detail.workflowEvents.size})") {
        if (detail.workflowEvents.isEmpty()) {
            UnavailableText("No workflow events were returned.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                detail.workflowEvents.forEachIndexed { index, event ->
                    DetailItem(label = "Event ${index + 1}", value = event["action"].displayValue())
                    event["actor_name"]?.let { DetailItem(label = "Actor", value = it.displayValue()) }
                    event["created_at"]?.let { DetailItem(label = "Time", value = it.displayValue()) }
                }
            }
        }
    }
}

@Composable
private fun DetailMapSection(title: String, values: Map<String, Any>) {
    SectionCard(title = title) {
        if (values.isEmpty()) {
            UnavailableText("No $title data was returned.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                flatten(values).forEach { (key, value) ->
                    DetailItem(label = key.displayLabel(), value = value.displayValue())
                }
            }
        }
    }
}

@Composable
private fun UnavailableText(message: String) =
    Text(message, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)

private fun String.displayLabel(): String = split('_').joinToString(" ") { word ->
    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun Any?.displayValue(): String = when (this) {
    null -> "Not available"
    is Boolean -> if (this) "Yes" else "No"
    else -> toString().takeIf { it.isNotBlank() } ?: "Not available"
}

private fun flatten(values: Map<String, Any>, prefix: String = ""): List<Pair<String, Any?>> =
    values.toSortedMap().flatMap { (key, value) ->
        val fullKey = if (prefix.isBlank()) key else "${prefix}_$key"
        @Suppress("UNCHECKED_CAST")
        when (value) {
            is Map<*, *> -> flatten(
                value.entries.mapNotNull { (nestedKey, nestedValue) ->
                    nestedValue?.let { nestedKey.toString() to it }
                }.toMap(),
                fullKey,
            )
            is List<*> -> value.mapIndexed { index, item -> "${fullKey}_${index + 1}" to item }
            else -> listOf(fullKey to value)
        }
    }
