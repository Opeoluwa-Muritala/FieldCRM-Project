package com.fieldcrm.android.core.session

enum class UserRole {
    LOAN_OFFICER,
    BRANCH_MANAGER,
    CREDIT_OFFICER,
    AUDITOR,
    ADMIN_MCR;

    val displayName: String
        get() = when (this) {
            LOAN_OFFICER -> "Loan Officer"
            BRANCH_MANAGER -> "Branch Manager"
            CREDIT_OFFICER -> "Credit Officer"
            AUDITOR -> "Auditor"
            ADMIN_MCR -> "Admin/MCR"
        }

    companion object {
        fun fromLoginIdentifier(identifier: String): UserRole {
            val normalized = identifier.trim().lowercase()
            return when {
                "adebayo" in normalized -> BRANCH_MANAGER
                "fatima" in normalized -> CREDIT_OFFICER
                "samuel" in normalized -> AUDITOR
                "admin" in normalized -> ADMIN_MCR
                else -> LOAN_OFFICER
            }
        }
    }
}
