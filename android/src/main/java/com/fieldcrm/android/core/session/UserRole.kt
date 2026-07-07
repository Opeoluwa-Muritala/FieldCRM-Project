package com.fieldcrm.android.core.session

enum class UserRole {
    LOAN_OFFICER,
    BRANCH_MANAGER,
    AUDITOR,
    SYSTEM_ADMIN,
    CRM,
    COMMITTEE,
    ED,
    MD,
    EXECUTIVE;

    val displayName: String
        get() = when (this) {
            LOAN_OFFICER   -> "Loan Officer"
            BRANCH_MANAGER -> "Branch Manager"
            AUDITOR        -> "Auditor"
            SYSTEM_ADMIN   -> "System Admin"
            CRM            -> "CRM Officer"
            COMMITTEE      -> "Committee Member"
            ED             -> "Executive Director"
            MD             -> "Managing Director"
            EXECUTIVE      -> "Executive"
        }

    /** True for roles that participate in the loan approval workflow. */
    val isBusinessRole: Boolean
        get() = this != SYSTEM_ADMIN

    companion object {
        fun fromServerRole(role: String): UserRole = when (role.trim().lowercase()) {
            "branch_manager", "branchmanager"  -> BRANCH_MANAGER
            "auditor"                          -> AUDITOR
            "system_admin", "admin", "admin_mcr", "mcr" -> SYSTEM_ADMIN
            "crm"                              -> CRM
            "committee"                        -> COMMITTEE
            "ed"                               -> ED
            "md"                               -> MD
            "executive"                        -> EXECUTIVE
            else                               -> LOAN_OFFICER
        }

        @Deprecated("Use fromServerRole — role must come from the API, not the email")
        fun fromLoginIdentifier(identifier: String): UserRole {
            val normalized = identifier.trim().lowercase()
            return when {
                "adebayo" in normalized -> BRANCH_MANAGER
                "samuel" in normalized -> AUDITOR
                "admin" in normalized -> SYSTEM_ADMIN
                else -> LOAN_OFFICER
            }
        }
    }
}
