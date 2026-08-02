package com.fieldcrm.android.ui.features.dossier

import androidx.compose.runtime.Immutable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.shared.model.LoanApplicationModel

@Immutable
data class DossierCapabilities(
    val allowedActions: Set<String> = emptySet(),
    val missingGates: List<String> = emptyList(),
    val canSubmit: Boolean = false
)

@Immutable
data class DossierUiState(
    val application: LoanApplicationModel? = null,
    val intake: Map<String, Any?> = emptyMap(),
    val borrowerIdentity: Map<String, Any?> = emptyMap(),
    val readiness: Map<String, Any?> = emptyMap(),
    val documents: List<Map<String, Any?>> = emptyList(),
    val guarantors: List<Map<String, Any?>> = emptyList(),
    val collateral: List<Map<String, Any?>> = emptyList(),
    val visitation: Map<String, Any?> = emptyMap(),
    val creditAssessment: Map<String, Any?> = emptyMap(),
    val workflowHistory: List<Map<String, Any?>> = emptyList(),
    val mccDecisions: List<Map<String, Any?>> = emptyList(),
    val capabilities: DossierCapabilities = DossierCapabilities(),
    val visibleSections: Set<String> = emptySet(),
    val loadedForRole: UserRole? = null,
    val isLoading: Boolean = false,
    val isCached: Boolean = false,
    val error: String? = null
)

fun Map<String, Any?>.displayValue(key: String): String =
    this[key]?.toString()?.takeIf { it.isNotBlank() } ?: "Not available"
