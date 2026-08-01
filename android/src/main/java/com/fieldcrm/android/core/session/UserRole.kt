package com.fieldcrm.android.core.session

enum class UserRole {
    ACCOUNT_OFFICER,
    BRANCH_SUPERVISOR,
    CREDIT_ANALYST,
    HEAD_CRM,
    LEGAL,

    /** Legacy mobile roles retained only to restore sessions written by older app versions. */
    LOAN_OFFICER,
    BRANCH_MANAGER,
    AUDITOR,
    SYSTEM_ADMIN,
    CRM,
    ED,
    MD,
    EXECUTIVE;

    val displayName: String
        get() = when (this) {
            ACCOUNT_OFFICER -> "Relationship Officer"
            BRANCH_SUPERVISOR -> "Supervisor"
            CREDIT_ANALYST -> "Credit Analyst"
            HEAD_CRM -> "Head CRM"
            LEGAL -> "Legal"
            LOAN_OFFICER   -> "Relationship Officer"
            BRANCH_MANAGER -> "Team Lead"
            AUDITOR        -> "Audit"
            SYSTEM_ADMIN   -> "System Admin"
            CRM            -> "CRM Officer"
            ED             -> "Executive Director"
            MD             -> "Managing Director"
            EXECUTIVE      -> "Executive"
        }

    /** True for roles that participate in the loan approval workflow. */
    val isBusinessRole: Boolean
        get() = this != SYSTEM_ADMIN

    companion object {
        fun fromServerRole(role: String): UserRole = when (role.trim().lowercase()) {
            "account_officer", "accountofficer" -> ACCOUNT_OFFICER
            "loan_officer", "loanofficer" -> ACCOUNT_OFFICER
            "branch_manager", "branchmanager" -> BRANCH_MANAGER
            "branch_supervisor", "branchsupervisor" -> BRANCH_SUPERVISOR
            "credit_analyst", "creditanalyst" -> CREDIT_ANALYST
            "auditor"                          -> AUDITOR
            "system_admin", "admin" -> SYSTEM_ADMIN
            "crm"                              -> CRM
            "head_crm", "headcrm" -> HEAD_CRM
            "legal" -> LEGAL
            "ed"                               -> ED
            "md"                               -> MD
            "executive"                        -> EXECUTIVE
            else -> EXECUTIVE
        }

    }
}
