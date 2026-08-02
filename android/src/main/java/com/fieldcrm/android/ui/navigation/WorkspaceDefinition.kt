package com.fieldcrm.android.ui.navigation

import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.viewmodel.Screen

data class WorkspaceDestination(val label: String, val route: Screen)

data class QueueDefinition(
    val title: String,
    val endpoint: String,
    val emptyMessage: String,
    val destination: Screen
)

data class SearchDefinition(
    val title: String,
    val placeholder: String,
    val entities: Set<String>
)

data class ReviewDefinition(
    val title: String,
    val route: Screen,
    val allowedStages: Set<String>
)

data class WorkspaceDefinition(
    val role: UserRole,
    val canonicalRoleName: String,
    val primaryDestinations: List<WorkspaceDestination>,
    val allowedRoutes: Set<Screen>,
    val queues: List<QueueDefinition> = emptyList(),
    val search: SearchDefinition? = null,
    val reviews: List<ReviewDefinition> = emptyList(),
    val dossierSections: Set<String> = emptySet(),
    val mccEnabled: Boolean = false
)
