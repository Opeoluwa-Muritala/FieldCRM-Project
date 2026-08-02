package com.fieldcrm.android.ui.navigation

import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.viewmodel.Screen

object RouteGuard {
    fun canOpen(role: UserRole, route: Screen): Boolean =
        route in WorkspaceRegistry.forRole(role).allowedRoutes
}
