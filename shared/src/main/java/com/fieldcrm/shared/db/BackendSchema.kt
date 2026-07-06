package com.fieldcrm.shared.db

/**
 * Backend PostgreSQL schema metadata mirrored from backend/migrations.
 *
 * This file is intentionally declarative. It is useful for API mapping,
 * validation, sync tooling, and avoiding client assumptions about column names.
 */
object BackendSchema {
    data class Column(
        val name: String,
        val type: String,
        val nullable: Boolean = true,
        val primaryKey: Boolean = false,
        val unique: Boolean = false,
        val defaultValue: String? = null,
        val references: String? = null,
        val check: String? = null,
        val generated: String? = null
    )

    data class Index(
        val name: String,
        val columns: List<String>,
        val unique: Boolean = false,
        val where: String? = null
    )

    data class Table(
        val name: String,
        val columns: List<Column>,
        val indexes: List<Index> = emptyList(),
        val uniqueConstraints: List<List<String>> = emptyList()
    )

    object Organisations {
        const val TABLE = "organisations"
        const val ID = "id"
        const val NAME = "name"
        const val CODE = "code"
        const val ACTIVE = "active"
        const val CREATED_AT = "created_at"
    }

    object Users {
        const val TABLE = "users"
        const val ID = "id"
        const val ORG_ID = "org_id"
        const val FULL_NAME = "full_name"
        const val EMAIL = "email"
        const val PASSWORD_HASH = "password_hash"
        const val ROLE = "role"
        const val ACTIVE = "active"
        const val LAST_LOGIN_AT = "last_login_at"
        const val CREATED_AT = "created_at"
    }

    object LoanApplications {
        const val TABLE = "loan_applications"
        const val ID = "id"
        const val ORG_ID = "org_id"
        const val REF_NO = "ref_no"
        const val CUSTOMER_TYPE = "customer_type"
        const val LOAN_TYPE = "loan_type"
        const val STAGE = "stage"
        const val APPLICANT_NAME = "applicant_name"
        const val BVN = "bvn"
        const val PHONE = "phone"
        const val AMOUNT = "amount"
        const val TENOR_MONTHS = "tenor_months"
        const val PURPOSE = "purpose"
        const val REPAYMENT_MODE = "repayment_mode"
        const val CREATED_BY = "created_by"
        const val CURRENT_OWNER_ID = "current_owner_id"
        const val CREDIT_OFFICER_ID = "credit_officer_id"
        const val BRANCH_MANAGER_ID = "branch_manager_id"
        const val RETURN_REASON = "return_reason"
        const val RETURNED_AT = "returned_at"
        const val APPROVED_BY = "approved_by"
        const val APPROVED_AT = "approved_at"
        const val DISBURSED_AT = "disbursed_at"
        const val DELETED_AT = "deleted_at"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
        const val DISBURSEMENT_REF = "disbursement_ref"
        const val DISBURSEMENT_METHOD = "disbursement_method"
        const val DISBURSEMENT_MEMO_PATH = "disbursement_memo_path"
        const val DISBURSED_AMOUNT = "disbursed_amount"
        const val DISBURSED_BANK_REF = "disbursed_bank_ref"
        const val AUDIT_ARCHIVED_AT = "audit_archived_at"
        const val AUDIT_PACKAGE_PATH = "audit_package_path"
        const val EXECUTIVE_APPROVED_BY = "executive_approved_by"
        const val EXECUTIVE_APPROVED_AT = "executive_approved_at"
        const val CRM_REVIEWED_BY = "crm_reviewed_by"
        const val CRM_REVIEWED_AT = "crm_reviewed_at"
        const val CRM_NOTES = "crm_notes"
        const val CLASSIFICATION = "classification"
        const val DAYS_PAST_DUE = "days_past_due"
        const val CLASSIFICATION_UPDATED_AT = "classification_updated_at"
        const val SECTOR = "sector"
        const val INTEREST_RATE = "interest_rate"
        const val REPAYMENT_FREQUENCY = "repayment_frequency"
        const val SCHEDULE_METHOD = "schedule_method"
        const val CREDIT_BUREAU_1_DATE = "credit_bureau_1_date"
        const val CREDIT_BUREAU_2_DATE = "credit_bureau_2_date"
        const val CRMS_SEARCHED = "crms_searched"
        const val CRMS_SEARCH_DATE = "crms_search_date"
    }

    object StageData {
        const val TABLE = "stage_data"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val STAGE = "stage"
        const val DATA_JSON = "data_json"
        const val SAVED_BY = "saved_by"
        const val SAVED_AT = "saved_at"
    }

    object Guarantors {
        const val TABLE = "guarantors"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val ORG_ID = "org_id"
        const val SLOT = "slot"
        const val FULL_NAME = "full_name"
        const val RELATIONSHIP_TO_CLIENT = "relationship_to_client"
        const val BVN = "bvn"
        const val PHONE = "phone"
        const val HOME_ADDRESS = "home_address"
        const val EMPLOYMENT_TYPE = "employment_type"
        const val MONTHLY_SALARY = "monthly_salary"
        const val MAX_GUARANTEE_AMOUNT = "max_guarantee_amount"
        const val MAX_GUARANTEE_AMOUNT_WORDS = "max_guarantee_amount_words"
        const val BANK_NAME = "bank_name"
        const val ACCOUNT_NUMBER = "account_number"
        const val CHEQUE_NUMBER = "cheque_number"
        const val FORM_STAGE = "form_stage"
        const val SIGNATURE_DETECTED = "signature_detected"
        const val WITNESS_SIGNATURE_DETECTED = "witness_signature_detected"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
    }

    object PledgedItems {
        const val TABLE = "pledged_items"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val ITEM_NUMBER = "item_number"
        const val ITEM_NAME = "item_name"
        const val SERIAL_NUMBER = "serial_number"
        const val DESCRIPTION = "description"
        const val ESTIMATED_VALUE = "estimated_value"
        const val CREATED_AT = "created_at"
        const val NCR_REG_NUMBER = "ncr_reg_number"
    }

    object Documents {
        const val TABLE = "documents"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val ORG_ID = "org_id"
        const val GUARANTOR_ID = "guarantor_id"
        const val DOC_TYPE = "doc_type"
        const val FORM_CODE = "form_code"
        const val ORIGINAL_NAME = "original_name"
        const val STORED_PATH = "stored_path"
        const val MIME_TYPE = "mime_type"
        const val SIZE_BYTES = "size_bytes"
        const val QUALITY_STATUS = "quality_status"
        const val VERIFIED = "verified"
        const val VERIFIED_BY = "verified_by"
        const val VERIFIED_AT = "verified_at"
        const val UPLOADED_BY = "uploaded_by"
        const val UPLOADED_AT = "uploaded_at"
        const val DELETED_AT = "deleted_at"
        const val ZOHO_FILE_ID = "zoho_file_id"
        const val OCR_STATUS = "ocr_status"
        const val CLOUD_PUBLIC_ID = "cloud_public_id"
        const val CLOUD_PREVIEW_URL = "cloud_preview_url"
    }

    object OcrResults {
        const val TABLE = "ocr_results"
        const val ID = "id"
        const val DOCUMENT_ID = "document_id"
        const val LOAN_ID = "loan_id"
        const val FORM_TYPE = "form_type"
        const val OVERALL_CONFIDENCE = "overall_confidence"
        const val RAW_EXTRACTION = "raw_extraction"
        const val CREATED_AT = "created_at"
    }

    object OcrFields {
        const val TABLE = "ocr_fields"
        const val ID = "id"
        const val OCR_RESULT_ID = "ocr_result_id"
        const val LOAN_ID = "loan_id"
        const val FIELD_NAME = "field_name"
        const val OCR_VALUE = "ocr_value"
        const val CORRECTED_VALUE = "corrected_value"
        const val FINAL_VALUE = "final_value"
        const val CONFIDENCE = "confidence"
        const val SOURCE = "source"
        const val IS_CRITICAL = "is_critical"
        const val VERIFIED = "verified"
        const val VERIFIED_BY = "verified_by"
        const val VERIFIED_AT = "verified_at"
        const val CORRECTED_BY = "corrected_by"
        const val CORRECTED_AT = "corrected_at"
        const val CREATED_AT = "created_at"
        const val PAGE_NUMBER = "page_number"
    }

    object WorkflowEvents {
        const val TABLE = "workflow_events"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val ORG_ID = "org_id"
        const val EVENT_TYPE = "event_type"
        const val FROM_STAGE = "from_stage"
        const val TO_STAGE = "to_stage"
        const val TRIGGERED_BY = "triggered_by"
        const val TRIGGERED_ROLE = "triggered_role"
        const val NOTES = "notes"
        const val CREATED_AT = "created_at"
    }

    object VisitationReports {
        const val TABLE = "visitation_reports"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val ORG_ID = "org_id"
        const val VISIT_DATE = "visit_date"
        const val MET_WITH = "met_with"
        const val PREMISES_DESCRIPTION = "premises_description"
        const val DIRECTION_FROM_BRANCH = "direction_from_branch"
        const val BUSINESS_CONDITION = "business_condition"
        const val VISITING_OFFICER_ID = "visiting_officer_id"
        const val VISITING_OFFICER_SIGNATURE = "visiting_officer_signature"
        const val ACCOUNT_OFFICER_ID = "account_officer_id"
        const val MANAGER_CONCURRENCE = "manager_concurrence"
        const val MANAGER_ID = "manager_id"
        const val MANAGER_NOTES = "manager_notes"
        const val MANAGER_CONCURRED_AT = "manager_concurred_at"
        const val STATUS = "status"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
    }

    object Notifications {
        const val TABLE = "notifications"
        const val ID = "id"
        const val USER_ID = "user_id"
        const val ORG_ID = "org_id"
        const val APPLICATION_ID = "application_id"
        const val TITLE = "title"
        const val MESSAGE = "message"
        const val TYPE = "type"
        const val IS_READ = "is_read"
        const val CREATED_AT = "created_at"
    }

    object AuditEntries {
        const val TABLE = "audit_entries"
        const val ID = "id"
        const val ORG_ID = "org_id"
        const val ENTITY_TYPE = "entity_type"
        const val ENTITY_ID = "entity_id"
        const val ACTION = "action"
        const val USER_ID = "user_id"
        const val USER_ROLE = "user_role"
        const val FIELD_NAME = "field_name"
        const val OLD_VALUE = "old_value"
        const val NEW_VALUE = "new_value"
        const val SOURCE = "source"
        const val NOTES = "notes"
        const val REQUEST_ID = "request_id"
        const val CREATED_AT = "created_at"
    }

    object PasswordResetTokens {
        const val TABLE = "password_reset_tokens"
        const val ID = "id"
        const val USER_ID = "user_id"
        const val TOKEN = "token"
        const val EXPIRES_AT = "expires_at"
        const val USED_AT = "used_at"
        const val CREATED_AT = "created_at"
    }

    object RepaymentSchedule {
        const val TABLE = "repayment_schedule"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val ORG_ID = "org_id"
        const val INSTALLMENT_NO = "installment_no"
        const val DUE_DATE = "due_date"
        const val PRINCIPAL_DUE = "principal_due"
        const val INTEREST_DUE = "interest_due"
        const val TOTAL_DUE = "total_due"
        const val CREATED_AT = "created_at"
    }

    object RepaymentRecords {
        const val TABLE = "repayment_records"
        const val ID = "id"
        const val LOAN_ID = "loan_id"
        const val ORG_ID = "org_id"
        const val PAYMENT_DATE = "payment_date"
        const val AMOUNT_PAID = "amount_paid"
        const val CHANNEL = "channel"
        const val BANK_REF = "bank_ref"
        const val RECORDED_BY = "recorded_by"
        const val CREATED_AT = "created_at"
    }

    val allTables: List<Table> = listOf(
        Table(
            name = Organisations.TABLE,
            columns = listOf(
                Column(Organisations.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(Organisations.NAME, "TEXT", nullable = false),
                Column(Organisations.CODE, "TEXT", nullable = false, unique = true),
                Column(Organisations.ACTIVE, "BOOLEAN", nullable = false, defaultValue = "TRUE"),
                Column(Organisations.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            )
        ),
        Table(
            name = Users.TABLE,
            columns = listOf(
                Column(Users.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(Users.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(Users.FULL_NAME, "TEXT", nullable = false),
                Column(Users.EMAIL, "TEXT", nullable = false),
                Column(Users.PASSWORD_HASH, "TEXT", nullable = false),
                Column(Users.ROLE, "TEXT", nullable = false, check = "loan_officer, credit_officer, branch_manager, auditor, system_admin, crm, md, ed"),
                Column(Users.ACTIVE, "BOOLEAN", nullable = false, defaultValue = "TRUE"),
                Column(Users.LAST_LOGIN_AT, "TIMESTAMPTZ"),
                Column(Users.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(Index("ix_users_org_role", listOf(Users.ORG_ID, Users.ROLE))),
            uniqueConstraints = listOf(listOf(Users.ORG_ID, Users.EMAIL))
        ),
        Table(
            name = LoanApplications.TABLE,
            columns = listOf(
                Column(LoanApplications.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(LoanApplications.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(LoanApplications.REF_NO, "TEXT", nullable = false),
                Column(LoanApplications.CUSTOMER_TYPE, "TEXT", nullable = false, check = "new, existing"),
                Column(LoanApplications.LOAN_TYPE, "TEXT", nullable = false, check = "enterprise, msef, payee, other"),
                Column(LoanApplications.STAGE, "TEXT", nullable = false, defaultValue = "intake", check = "intake, ocr_review, credit_review, branch_approval, crm_review, executive_approval, disbursement_ready, disbursed, returned, rejected"),
                Column(LoanApplications.APPLICANT_NAME, "TEXT", nullable = false),
                Column(LoanApplications.BVN, "TEXT"),
                Column(LoanApplications.PHONE, "TEXT"),
                Column(LoanApplications.AMOUNT, "NUMERIC(15,2)", check = "> 0"),
                Column(LoanApplications.TENOR_MONTHS, "INTEGER", check = "> 0"),
                Column(LoanApplications.PURPOSE, "TEXT"),
                Column(LoanApplications.REPAYMENT_MODE, "TEXT", check = "cheque, standing_order, direct_debit, cash_deposit"),
                Column(LoanApplications.CREATED_BY, "UUID", nullable = false, references = "${Users.TABLE}(${Users.ID})"),
                Column(LoanApplications.CURRENT_OWNER_ID, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(LoanApplications.CREDIT_OFFICER_ID, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(LoanApplications.BRANCH_MANAGER_ID, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(LoanApplications.RETURN_REASON, "TEXT"),
                Column(LoanApplications.RETURNED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.APPROVED_BY, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(LoanApplications.APPROVED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.DISBURSED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.DELETED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()"),
                Column(LoanApplications.UPDATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()"),
                Column(LoanApplications.DISBURSEMENT_REF, "TEXT"),
                Column(LoanApplications.DISBURSEMENT_METHOD, "TEXT", check = "bank_transfer, cheque, cash, direct_debit"),
                Column(LoanApplications.DISBURSEMENT_MEMO_PATH, "TEXT"),
                Column(LoanApplications.DISBURSED_AMOUNT, "NUMERIC(15,2)"),
                Column(LoanApplications.DISBURSED_BANK_REF, "TEXT"),
                Column(LoanApplications.AUDIT_ARCHIVED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.AUDIT_PACKAGE_PATH, "TEXT"),
                Column(LoanApplications.EXECUTIVE_APPROVED_BY, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(LoanApplications.EXECUTIVE_APPROVED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.CRM_REVIEWED_BY, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(LoanApplications.CRM_REVIEWED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.CRM_NOTES, "TEXT"),
                Column(LoanApplications.CLASSIFICATION, "TEXT", defaultValue = "current", check = "current, olem, substandard, doubtful, lost"),
                Column(LoanApplications.DAYS_PAST_DUE, "INTEGER", nullable = false, defaultValue = "0"),
                Column(LoanApplications.CLASSIFICATION_UPDATED_AT, "TIMESTAMPTZ"),
                Column(LoanApplications.SECTOR, "TEXT"),
                Column(LoanApplications.INTEREST_RATE, "NUMERIC(5,2)"),
                Column(LoanApplications.REPAYMENT_FREQUENCY, "TEXT", check = "daily, weekly, biweekly, monthly"),
                Column(LoanApplications.SCHEDULE_METHOD, "TEXT", defaultValue = "flat_rate", check = "flat_rate, reducing_balance"),
                Column(LoanApplications.CREDIT_BUREAU_1_DATE, "DATE"),
                Column(LoanApplications.CREDIT_BUREAU_2_DATE, "DATE"),
                Column(LoanApplications.CRMS_SEARCHED, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(LoanApplications.CRMS_SEARCH_DATE, "DATE")
            ),
            indexes = listOf(
                Index("ix_loan_org_stage", listOf(LoanApplications.ORG_ID, LoanApplications.STAGE), where = "${LoanApplications.DELETED_AT} IS NULL"),
                Index("ix_loan_org_officer", listOf(LoanApplications.ORG_ID, LoanApplications.CREATED_BY), where = "${LoanApplications.DELETED_AT} IS NULL"),
                Index("ix_loan_org_updated", listOf(LoanApplications.ORG_ID, "${LoanApplications.UPDATED_AT} DESC"), where = "${LoanApplications.DELETED_AT} IS NULL")
            ),
            uniqueConstraints = listOf(listOf(LoanApplications.ORG_ID, LoanApplications.REF_NO))
        ),
        Table(
            name = StageData.TABLE,
            columns = listOf(
                Column(StageData.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(StageData.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID}) ON DELETE CASCADE"),
                Column(StageData.STAGE, "TEXT", nullable = false),
                Column(StageData.DATA_JSON, "JSONB", nullable = false, defaultValue = "{}"),
                Column(StageData.SAVED_BY, "UUID", nullable = false, references = "${Users.TABLE}(${Users.ID})"),
                Column(StageData.SAVED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(Index("ix_stage_data_loan", listOf(StageData.LOAN_ID, StageData.STAGE)))
        ),
        Table(
            name = Guarantors.TABLE,
            columns = listOf(
                Column(Guarantors.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(Guarantors.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID}) ON DELETE CASCADE"),
                Column(Guarantors.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(Guarantors.SLOT, "INTEGER", nullable = false, check = "1, 2"),
                Column(Guarantors.FULL_NAME, "TEXT"),
                Column(Guarantors.RELATIONSHIP_TO_CLIENT, "TEXT"),
                Column(Guarantors.BVN, "TEXT"),
                Column(Guarantors.PHONE, "TEXT"),
                Column(Guarantors.HOME_ADDRESS, "TEXT"),
                Column(Guarantors.EMPLOYMENT_TYPE, "TEXT"),
                Column(Guarantors.MONTHLY_SALARY, "NUMERIC(15,2)"),
                Column(Guarantors.MAX_GUARANTEE_AMOUNT, "NUMERIC(15,2)"),
                Column(Guarantors.MAX_GUARANTEE_AMOUNT_WORDS, "TEXT"),
                Column(Guarantors.BANK_NAME, "TEXT"),
                Column(Guarantors.ACCOUNT_NUMBER, "TEXT"),
                Column(Guarantors.CHEQUE_NUMBER, "TEXT"),
                Column(Guarantors.FORM_STAGE, "TEXT", nullable = false, defaultValue = "draft", check = "draft, submitted, ocr_review, verified, returned"),
                Column(Guarantors.SIGNATURE_DETECTED, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(Guarantors.WITNESS_SIGNATURE_DETECTED, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(Guarantors.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()"),
                Column(Guarantors.UPDATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(Index("ix_guarantor_loan", listOf(Guarantors.LOAN_ID))),
            uniqueConstraints = listOf(listOf(Guarantors.LOAN_ID, Guarantors.SLOT))
        ),
        Table(
            name = PledgedItems.TABLE,
            columns = listOf(
                Column(PledgedItems.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(PledgedItems.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID}) ON DELETE CASCADE"),
                Column(PledgedItems.ITEM_NUMBER, "INTEGER", nullable = false),
                Column(PledgedItems.ITEM_NAME, "TEXT", nullable = false),
                Column(PledgedItems.SERIAL_NUMBER, "TEXT"),
                Column(PledgedItems.DESCRIPTION, "TEXT"),
                Column(PledgedItems.ESTIMATED_VALUE, "NUMERIC(15,2)", check = ">= 0"),
                Column(PledgedItems.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()"),
                Column(PledgedItems.NCR_REG_NUMBER, "TEXT")
            ),
            indexes = listOf(Index("ix_pledged_loan", listOf(PledgedItems.LOAN_ID)))
        ),
        Table(
            name = Documents.TABLE,
            columns = listOf(
                Column(Documents.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(Documents.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID}) ON DELETE CASCADE"),
                Column(Documents.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(Documents.GUARANTOR_ID, "UUID", references = "${Guarantors.TABLE}(${Guarantors.ID})"),
                Column(Documents.DOC_TYPE, "TEXT", nullable = false),
                Column(Documents.FORM_CODE, "TEXT"),
                Column(Documents.ORIGINAL_NAME, "TEXT", nullable = false),
                Column(Documents.STORED_PATH, "TEXT", nullable = false),
                Column(Documents.MIME_TYPE, "TEXT", nullable = false),
                Column(Documents.SIZE_BYTES, "INTEGER", nullable = false),
                Column(Documents.QUALITY_STATUS, "TEXT", nullable = false, defaultValue = "pending", check = "pending, clear, blurry, cropped, unreadable"),
                Column(Documents.VERIFIED, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(Documents.VERIFIED_BY, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(Documents.VERIFIED_AT, "TIMESTAMPTZ"),
                Column(Documents.UPLOADED_BY, "UUID", nullable = false, references = "${Users.TABLE}(${Users.ID})"),
                Column(Documents.UPLOADED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()"),
                Column(Documents.DELETED_AT, "TIMESTAMPTZ"),
                Column(Documents.ZOHO_FILE_ID, "TEXT"),
                Column(Documents.OCR_STATUS, "TEXT", nullable = false, defaultValue = "pending", check = "pending, processing, done, failed, skipped"),
                Column(Documents.CLOUD_PUBLIC_ID, "TEXT"),
                Column(Documents.CLOUD_PREVIEW_URL, "TEXT")
            ),
            indexes = listOf(
                Index("ix_doc_loan", listOf(Documents.LOAN_ID), where = "${Documents.DELETED_AT} IS NULL"),
                Index("ix_doc_loan_type", listOf(Documents.LOAN_ID, Documents.DOC_TYPE), where = "${Documents.DELETED_AT} IS NULL"),
                Index("ix_doc_unverified", listOf(Documents.LOAN_ID), where = "${Documents.VERIFIED} = FALSE AND ${Documents.DELETED_AT} IS NULL")
            )
        ),
        Table(
            name = OcrResults.TABLE,
            columns = listOf(
                Column(OcrResults.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(OcrResults.DOCUMENT_ID, "UUID", nullable = false, references = "${Documents.TABLE}(${Documents.ID}) ON DELETE CASCADE"),
                Column(OcrResults.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID})"),
                Column(OcrResults.FORM_TYPE, "TEXT", nullable = false, check = "loan_application, guarantor, pledge_receipt"),
                Column(OcrResults.OVERALL_CONFIDENCE, "NUMERIC(5,2)"),
                Column(OcrResults.RAW_EXTRACTION, "JSONB", nullable = false, defaultValue = "{}"),
                Column(OcrResults.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(
                Index("ix_ocr_document", listOf(OcrResults.DOCUMENT_ID)),
                Index("ix_ocr_loan", listOf(OcrResults.LOAN_ID))
            )
        ),
        Table(
            name = OcrFields.TABLE,
            columns = listOf(
                Column(OcrFields.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(OcrFields.OCR_RESULT_ID, "UUID", nullable = false, references = "${OcrResults.TABLE}(${OcrResults.ID}) ON DELETE CASCADE"),
                Column(OcrFields.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID})"),
                Column(OcrFields.FIELD_NAME, "TEXT", nullable = false),
                Column(OcrFields.OCR_VALUE, "TEXT"),
                Column(OcrFields.CORRECTED_VALUE, "TEXT"),
                Column(OcrFields.FINAL_VALUE, "TEXT", generated = "COALESCE(corrected_value, ocr_value) STORED"),
                Column(OcrFields.CONFIDENCE, "NUMERIC(5,2)"),
                Column(OcrFields.SOURCE, "TEXT", nullable = false, defaultValue = "ocr", check = "ocr, manual, corrected, approved"),
                Column(OcrFields.IS_CRITICAL, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(OcrFields.VERIFIED, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(OcrFields.VERIFIED_BY, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(OcrFields.VERIFIED_AT, "TIMESTAMPTZ"),
                Column(OcrFields.CORRECTED_BY, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(OcrFields.CORRECTED_AT, "TIMESTAMPTZ"),
                Column(OcrFields.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()"),
                Column(OcrFields.PAGE_NUMBER, "INTEGER")
            ),
            indexes = listOf(
                Index("ix_ocr_fields_result", listOf(OcrFields.OCR_RESULT_ID)),
                Index("ix_ocr_fields_loan", listOf(OcrFields.LOAN_ID)),
                Index("ix_ocr_fields_low_conf", listOf(OcrFields.LOAN_ID), where = "${OcrFields.CONFIDENCE} < 70 AND ${OcrFields.VERIFIED} = FALSE"),
                Index("ix_ocr_fields_unverified", listOf(OcrFields.LOAN_ID), where = "${OcrFields.IS_CRITICAL} = TRUE AND ${OcrFields.VERIFIED} = FALSE")
            )
        ),
        Table(
            name = WorkflowEvents.TABLE,
            columns = listOf(
                Column(WorkflowEvents.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(WorkflowEvents.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID})"),
                Column(WorkflowEvents.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(WorkflowEvents.EVENT_TYPE, "TEXT", nullable = false),
                Column(WorkflowEvents.FROM_STAGE, "TEXT"),
                Column(WorkflowEvents.TO_STAGE, "TEXT"),
                Column(WorkflowEvents.TRIGGERED_BY, "UUID", nullable = false, references = "${Users.TABLE}(${Users.ID})"),
                Column(WorkflowEvents.TRIGGERED_ROLE, "TEXT", nullable = false),
                Column(WorkflowEvents.NOTES, "TEXT"),
                Column(WorkflowEvents.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(
                Index("ix_workflow_loan", listOf(WorkflowEvents.LOAN_ID, "${WorkflowEvents.CREATED_AT} DESC")),
                Index("ix_workflow_org_date", listOf(WorkflowEvents.ORG_ID, "${WorkflowEvents.CREATED_AT} DESC"))
            )
        ),
        Table(
            name = VisitationReports.TABLE,
            columns = listOf(
                Column(VisitationReports.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(VisitationReports.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID})"),
                Column(VisitationReports.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(VisitationReports.VISIT_DATE, "DATE"),
                Column(VisitationReports.MET_WITH, "TEXT"),
                Column(VisitationReports.PREMISES_DESCRIPTION, "TEXT"),
                Column(VisitationReports.DIRECTION_FROM_BRANCH, "TEXT"),
                Column(VisitationReports.BUSINESS_CONDITION, "TEXT"),
                Column(VisitationReports.VISITING_OFFICER_ID, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(VisitationReports.VISITING_OFFICER_SIGNATURE, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(VisitationReports.ACCOUNT_OFFICER_ID, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(VisitationReports.MANAGER_CONCURRENCE, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(VisitationReports.MANAGER_ID, "UUID", references = "${Users.TABLE}(${Users.ID})"),
                Column(VisitationReports.MANAGER_NOTES, "TEXT"),
                Column(VisitationReports.MANAGER_CONCURRED_AT, "TIMESTAMPTZ"),
                Column(VisitationReports.STATUS, "TEXT", nullable = false, defaultValue = "pending", check = "pending, submitted, concurred, returned"),
                Column(VisitationReports.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()"),
                Column(VisitationReports.UPDATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(Index("ix_visitation_loan", listOf(VisitationReports.LOAN_ID), unique = true))
        ),
        Table(
            name = Notifications.TABLE,
            columns = listOf(
                Column(Notifications.ID, "TEXT", nullable = false, primaryKey = true, defaultValue = "'notif_' || replace(gen_random_uuid()::text, '-', '')"),
                Column(Notifications.USER_ID, "UUID", nullable = false, references = "${Users.TABLE}(${Users.ID}) ON DELETE CASCADE"),
                Column(Notifications.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID}) ON DELETE CASCADE"),
                Column(Notifications.APPLICATION_ID, "UUID", references = "${LoanApplications.TABLE}(${LoanApplications.ID}) ON DELETE CASCADE"),
                Column(Notifications.TITLE, "TEXT", nullable = false),
                Column(Notifications.MESSAGE, "TEXT", nullable = false),
                Column(Notifications.TYPE, "TEXT", nullable = false),
                Column(Notifications.IS_READ, "BOOLEAN", nullable = false, defaultValue = "FALSE"),
                Column(Notifications.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(
                Index("ix_notifications_user_date", listOf(Notifications.USER_ID, "${Notifications.CREATED_AT} DESC")),
                Index("ix_notifications_user_unread", listOf(Notifications.USER_ID, Notifications.IS_READ, "${Notifications.CREATED_AT} DESC")),
                Index("ix_notifications_application", listOf(Notifications.APPLICATION_ID))
            )
        ),
        Table(
            name = AuditEntries.TABLE,
            columns = listOf(
                Column(AuditEntries.ID, "BIGSERIAL", nullable = false, primaryKey = true),
                Column(AuditEntries.ORG_ID, "UUID", nullable = false),
                Column(AuditEntries.ENTITY_TYPE, "TEXT", nullable = false),
                Column(AuditEntries.ENTITY_ID, "UUID", nullable = false),
                Column(AuditEntries.ACTION, "TEXT", nullable = false),
                Column(AuditEntries.USER_ID, "UUID", nullable = false),
                Column(AuditEntries.USER_ROLE, "TEXT", nullable = false),
                Column(AuditEntries.FIELD_NAME, "TEXT"),
                Column(AuditEntries.OLD_VALUE, "TEXT"),
                Column(AuditEntries.NEW_VALUE, "TEXT"),
                Column(AuditEntries.SOURCE, "TEXT"),
                Column(AuditEntries.NOTES, "TEXT"),
                Column(AuditEntries.REQUEST_ID, "TEXT"),
                Column(AuditEntries.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(
                Index("ix_audit_entity", listOf(AuditEntries.ENTITY_TYPE, AuditEntries.ENTITY_ID, "${AuditEntries.CREATED_AT} DESC")),
                Index("ix_audit_org_date", listOf(AuditEntries.ORG_ID, "${AuditEntries.CREATED_AT} DESC")),
                Index("ix_audit_user", listOf(AuditEntries.USER_ID, "${AuditEntries.CREATED_AT} DESC"))
            )
        ),
        Table(
            name = PasswordResetTokens.TABLE,
            columns = listOf(
                Column(PasswordResetTokens.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(PasswordResetTokens.USER_ID, "UUID", nullable = false, references = "${Users.TABLE}(${Users.ID}) ON DELETE CASCADE"),
                Column(PasswordResetTokens.TOKEN, "TEXT", nullable = false, unique = true),
                Column(PasswordResetTokens.EXPIRES_AT, "TIMESTAMPTZ", nullable = false),
                Column(PasswordResetTokens.USED_AT, "TIMESTAMPTZ"),
                Column(PasswordResetTokens.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(
                Index("idx_prt_token", listOf(PasswordResetTokens.TOKEN)),
                Index("idx_prt_user_id", listOf(PasswordResetTokens.USER_ID))
            )
        ),
        Table(
            name = RepaymentSchedule.TABLE,
            columns = listOf(
                Column(RepaymentSchedule.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(RepaymentSchedule.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID}) ON DELETE CASCADE"),
                Column(RepaymentSchedule.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(RepaymentSchedule.INSTALLMENT_NO, "INTEGER", nullable = false),
                Column(RepaymentSchedule.DUE_DATE, "DATE", nullable = false),
                Column(RepaymentSchedule.PRINCIPAL_DUE, "NUMERIC(15,2)", nullable = false),
                Column(RepaymentSchedule.INTEREST_DUE, "NUMERIC(15,2)", nullable = false),
                Column(RepaymentSchedule.TOTAL_DUE, "NUMERIC(15,2)", nullable = false),
                Column(RepaymentSchedule.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(Index("ix_schedule_loan", listOf(RepaymentSchedule.LOAN_ID, RepaymentSchedule.INSTALLMENT_NO))),
            uniqueConstraints = listOf(listOf(RepaymentSchedule.LOAN_ID, RepaymentSchedule.INSTALLMENT_NO))
        ),
        Table(
            name = RepaymentRecords.TABLE,
            columns = listOf(
                Column(RepaymentRecords.ID, "UUID", nullable = false, primaryKey = true, defaultValue = "gen_random_uuid()"),
                Column(RepaymentRecords.LOAN_ID, "UUID", nullable = false, references = "${LoanApplications.TABLE}(${LoanApplications.ID}) ON DELETE CASCADE"),
                Column(RepaymentRecords.ORG_ID, "UUID", nullable = false, references = "${Organisations.TABLE}(${Organisations.ID})"),
                Column(RepaymentRecords.PAYMENT_DATE, "DATE", nullable = false),
                Column(RepaymentRecords.AMOUNT_PAID, "NUMERIC(15,2)", nullable = false, check = "> 0"),
                Column(RepaymentRecords.CHANNEL, "TEXT", nullable = false, check = "bank_transfer, cheque, cash, direct_debit, pos"),
                Column(RepaymentRecords.BANK_REF, "TEXT"),
                Column(RepaymentRecords.RECORDED_BY, "UUID", nullable = false, references = "${Users.TABLE}(${Users.ID})"),
                Column(RepaymentRecords.CREATED_AT, "TIMESTAMPTZ", nullable = false, defaultValue = "NOW()")
            ),
            indexes = listOf(Index("ix_repayment_loan", listOf(RepaymentRecords.LOAN_ID, "${RepaymentRecords.PAYMENT_DATE} DESC")))
        )
    )

    val tablesByName: Map<String, Table> = allTables.associateBy { it.name }
}
