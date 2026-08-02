package com.fieldcrm.android.ui.navigation

import com.fieldcrm.android.core.session.UserRole

object RoleResolver {
    fun resolve(identifier: String): UserRole = UserRole.fromServerRole(identifier)
    fun canonicalName(role: UserRole): String = WorkspaceRegistry.forRole(role).canonicalRoleName
}
