package com.fieldcrm.android.core.session

enum class UserRole {
    ACCOUNT_OFFICER,
    BRANCH_SUPERVISOR,
    CREDIT_ANALYST,
    HEAD_CRM,

    /** Legacy mobile roles retained only to restore sessions written by older app versions. */
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
            ACCOUNT_OFFICER -> "Account Officer"
            BRANCH_SUPERVISOR -> "Branch Supervisor"
            CREDIT_ANALYST -> "Credit Analyst"
            HEAD_CRM -> "Head CRM"
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

    /**
     * Temporary presentation bridge while the mobile endpoints complete their
     * migration from the former Committee/Executive flow to the website's
     * Branch Supervisor/Credit Analyst/Head CRM workflow.
     */
    val legacyUiRole: UserRole
        get() = when (this) {
            ACCOUNT_OFFICER -> LOAN_OFFICER
            BRANCH_SUPERVISOR -> BRANCH_MANAGER
            CREDIT_ANALYST -> COMMITTEE
            HEAD_CRM -> EXECUTIVE
            else -> this
        }

    companion object {
        fun fromServerRole(role: String): UserRole = when (role.trim().lowercase()) {
            "account_officer", "accountofficer" -> ACCOUNT_OFFICER
            "loan_officer", "loanofficer" -> ACCOUNT_OFFICER
            "branch_manager", "branchmanager" -> BRANCH_MANAGER
            "branch_supervisor", "branchsupervisor" -> BRANCH_SUPERVISOR
            "credit_analyst", "creditanalyst" -> CREDIT_ANALYST
            "auditor"                          -> AUDITOR
            "system_admin", "admin", "admin_mcr", "mcr" -> SYSTEM_ADMIN
            "crm"                              -> CRM
            "head_crm", "headcrm" -> HEAD_CRM
            // Restored sessions from versions that predate the website workflow.
            "committee" -> COMMITTEE
            "ed"                               -> ED
            "md"                               -> MD
            "executive"                        -> EXECUTIVE
            else -> ACCOUNT_OFFICER
        }

        @Deprecated("Use fromServerRole — role must come from the API, not the email")
        fun fromLoginIdentifier(identifier: String): UserRole {
            val normalized = identifier.trim().lowercase()
            return when {
                "adebayo" in normalized -> BRANCH_MANAGER
                "samuel" in normalized -> AUDITOR
                "admin" in normalized -> SYSTEM_ADMIN
                else -> ACCOUNT_OFFICER
            }
        }
    }
}
