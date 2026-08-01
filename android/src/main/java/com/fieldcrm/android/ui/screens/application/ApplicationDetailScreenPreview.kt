package com.fieldcrm.android.ui.screens.application

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Preview(name = "Compact — Loan Officer", widthDp = 411, heightDp = 850)
@Composable
fun PreviewApplicationDetailLoanOfficer() {
    val demoBorrower = BorrowerModel(
        id = "1",
        org_id = "org_1",
        loan_officer_id = "LO_1",
        name = "Adaeze Okonkwo",
        phone = "08012345678",
        bvn = "222333444",
        nin = "111222333",
        status = "Active",
        created_at = "2026-06-18"
    )
    val demoApp = LoanApplicationModel(
        id = "app_1", org_id = "org_1", ref_no = "MMFB-001",
        stage = "ocr_review", loan_type = "enterprise", customer_type = "new",
        applicant_name = "Adaeze Okonkwo", created_by = "LO_1", current_owner_id = "LO_1",
        amount = 250000.0, tenor_months = 6,
        interest_rate = 15.0, repayment_frequency = "monthly",
        purpose = "Deed of pledge over local shop stock",
        crm_notes = "Approved with high shop confidence metrics",
        created_at = "2026-06-18"
    )
    FieldCRMTheme {
        ApplicationDetailScreenView(
            application = demoApp,
            borrower = demoBorrower,
            role = UserRole.LOAN_OFFICER,
            onBackClick = {}
        )
    }
}

@Preview(name = "Compact — Branch Manager", widthDp = 411, heightDp = 850)
@Composable
fun PreviewApplicationDetailBranchManager() {
    val demoBorrower = BorrowerModel(
        id = "1",
        org_id = "org_1",
        loan_officer_id = "LO_1",
        name = "Emeka Obi",
        phone = "08098765432",
        bvn = "333444555",
        nin = "222333444",
        status = "Active",
        created_at = "2026-06-20"
    )
    val demoApp = LoanApplicationModel(
        id = "app_2", org_id = "org_1", ref_no = "MMFB-002",
        stage = "credit_review", loan_type = "msef", customer_type = "existing",
        applicant_name = "Emeka Obi", created_by = "LO_1", current_owner_id = "BM_1",
        amount = 500000.0, tenor_months = 12,
        interest_rate = 12.0, repayment_frequency = "monthly",
        purpose = "Land title document and registry check verification",
        created_at = "2026-06-20"
    )
    FieldCRMTheme {
        ApplicationDetailScreenView(
            application = demoApp,
            borrower = demoBorrower,
            role = UserRole.BRANCH_MANAGER,
            onBackClick = {}
        )
    }
}

@Preview(name = "Tablet — Branch Manager", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewApplicationDetailTablet() {
    val demoBorrower = BorrowerModel(
        id = "1",
        org_id = "org_1",
        loan_officer_id = "LO_1",
        name = "Ngozi Eze",
        phone = "08011223344",
        bvn = "444555666",
        nin = "333444555",
        status = "Active",
        created_at = "2026-06-18"
    )
    val demoApp = LoanApplicationModel(
        id = "app_3", org_id = "org_1", ref_no = "MMFB-003",
        stage = "branch_approval", loan_type = "msef", customer_type = "existing",
        applicant_name = "Ngozi Eze", created_by = "LO_1", current_owner_id = "BM_1",
        amount = 1200000.0, tenor_months = 12,
        interest_rate = 12.0, repayment_frequency = "monthly",
        purpose = "Land title document and registry check verification",
        created_at = "2026-06-18"
    )
    FieldCRMTheme {
        ApplicationDetailScreenView(
            application = demoApp,
            borrower = demoBorrower,
            role = UserRole.BRANCH_MANAGER,
            onBackClick = {}
        )
    }
}
