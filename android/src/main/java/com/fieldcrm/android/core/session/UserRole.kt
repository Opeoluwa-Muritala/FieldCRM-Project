package com.fieldcrm.android.core.session

enum class UserRole {
    LOAN_OFFICER,
    BRANCH_MANAGER,
    CREDIT_OFFICER,
    AUDITOR,
    SYSTEM_ADMIN,
    CRM,
    EXECUTIVE;

    val displayName: String
        get() = when (this) {
            LOAN_OFFICER   -> "Loan Officer"
            BRANCH_MANAGER -> "Branch Manager"
            CREDIT_OFFICER -> "Credit Officer"
            AUDITOR        -> "Auditor"
            SYSTEM_ADMIN   -> "System Admin"
            CRM            -> "CRM Officer"
            EXECUTIVE      -> "Executive"
        }

    /** True for roles that participate in the loan approval workflow. */
    val isBusinessRole: Boolean
        get() = this != SYSTEM_ADMIN

    companion object {
        fun fromServerRole(role: String): UserRole = when (role.trim().lowercase()) {
            "branch_manager", "branchmanager"  -> BRANCH_MANAGER
            "credit_officer", "creditofficer"  -> CREDIT_OFFICER
            "auditor"                          -> AUDITOR
            "system_admin", "admin", "admin_mcr", "mcr" -> SYSTEM_ADMIN
            "crm"                              -> CRM
            "md", "ed", "executive"            -> EXECUTIVE
            else                               -> LOAN_OFFICER
        }

        @Deprecated("Use fromServerRole — role must come from the API, not the email")
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
