# Loan Forms, Fields, Categories, and Flow

This document describes the loan origination forms currently implemented in FieldCRM across the web intake wizard, generated dashboard form workspace, document upload/OCR screens, and review workflow.

Primary implementation references:

- `frontend/templates/shared/new_application.html`
- `frontend/templates/shared/application_wizard.html`
- `frontend/templates/shared/guarantor_wizard.html`
- `frontend/templates/shared/upload_document.html`
- `frontend/templates/shared/ocr_review.html`
- `frontend/templates/shared/visitation.html`
- `frontend/templates/shared/credit_review.html`
- `frontend/templates/shared/approve.html`
- `frontend/templates/shared/return_page.html`
- `frontend/static/js/dashboard.js`
- `backend/app/domains/documents/service.py`
- `backend/migrations/001_full_schema.sql`

## Loan Form Categories

### Loan Categories

The application starts with one customer type and one loan category.

Customer types:

- `new`: New Customer
- `existing`: Existing Customer

Loan categories:

- `enterprise`: Enterprise Loan
- `msef`: MSEF
- `payee`: PAYEE
- `other`: Other Option, with `other_loan_desc`

### Coded Bank Forms

The backend maps uploaded form categories to these form codes:

| Form | Category key | Form code | Purpose |
| --- | --- | --- | --- |
| Loan Application Form | `loan_application_form` | `MMFB/CRM/01` | Main borrower application, disclosures, loan request, and consents |
| Pledge and Trust Receipt | `pledge_form` | `MMFB/CRM/02` | Collateral pledge details, pledged item schedule, acknowledgements, and signatures |
| Guarantor Form | `guarantor_form` | `MMFB/CRM/03` | Guarantor identity, financial capacity, declaration, guarantee limit, bank details, and signatures |

### Supporting Document Categories

The upload page supports these visible categories:

- `payslip`: Payslip
- `id`: Identity Document
- `statement`: Bank Statement
- `guarantor`: Guarantor Document
- `other`: Other Supporting

The Loan Officer application detail also exposes upload shortcuts for:

- `passport_photo`: Passport Photo
- `id_card`: Valid ID Card
- `utility_bill`: Utility Bill

Documents are stored with `doc_type`, optional `form_code`, file name/path, MIME type, file size, quality status, verification status, uploader, and timestamps.

## Main Intake Application Form

The web application intake wizard has 9 steps. Data is merged into `stage_data` under the `intake` stage. Step 9 advances the application from `intake` to `ocr_review`.

### Step 1: Applicant Details

Category: borrower identity and contact.

Fields:

- `full_name`: Full Name, required
- `id_type`: Means of Identification, radio options: National ID, Voters Card, Drivers License, Passport
- `id_number`: ID Number, required
- `id_expiry`: Expiry Date, required
- `phone`: Telephone Number, required
- `bvn`: Bank Verification Number, required, 11 digits
- `marital_status`: Single or Married, required
- `dob`: Date of Birth, required
- `state_of_origin`: State of Origin
- `lga`: LGA
- `home_address`: Home Address, required
- `landmark`: Nearest Landmark / Bus Stop
- `photo_url`: Passport photograph data/reference

Core values copied to `loan_applications` from this step include applicant name, phone, and BVN.

### Step 2: Spousal Consent

Category: spouse awareness and consent. The UI describes this as required for married applicants.

Fields:

- `spouse_name`: Name of Spouse, required
- `spouse_phone`: Spouse Telephone Number, required
- `spouse_children`: Number of Children
- `spouse_dependants`: Number of Dependants
- `spouse_business_address`: Spouse Business Address
- `spouse_signature`: Captured spouse signature, required

### Step 3: Guarantors List

Category: guarantor slots and status.

Each application has two guarantor slots.

Guarantor 1 fields:

- `guarantor_1_name`: Full Name, required
- `guarantor_1_relationship`: Relationship to Applicant, required
- `guarantor_1_phone`: Phone Number, required
- `guarantor_1_status`: Pending, Submitted, or Verified display state

Guarantor 2 fields:

- `guarantor_2_name`: Full Name, required
- `guarantor_2_relationship`: Relationship to Applicant, required
- `guarantor_2_phone`: Phone Number, required
- `guarantor_2_status`: Pending, Submitted, or Verified display state

Each slot opens its own 8-step guarantor wizard.

### Step 4: Employment and Business Details

Category: income source and supporting evidence.

Common field:

- `employment_type`: Full-time, Part-time, Contract Staff, Public Servant, or Self-employed

Salary-earner fields:

- `industry`: Industry/Sector
- `years_employed`: Years in Employment
- `employer_name`: Employer Name, required when not self-employed
- `monthly_salary`: Monthly Salary, required when not self-employed
- `employer_address`: Employer Address, required when not self-employed

Self-employed fields:

- `business_type`: Type of Business
- `years_in_business`: Years in Business
- `monthly_sales`: Average Monthly Sales, required when self-employed
- `monthly_turnover`: Average Monthly Credit Turnover, required when self-employed
- `business_address`: Business Address

Document guidance changes by employment type: salary earners see payslip/employer-letter evidence; self-employed applicants see business registration and turnover evidence.

### Step 5: Existing Facilities

Category: current obligations and education.

Repeatable existing facility fields:

- `facility_bank[]`: Bank Name
- `facility_amount[]`: Amount
- `facility_tenor[]`: Tenor in months

Additional field:

- `education`: Educational Background

### Step 6: Loan Request Details

Category: requested facility, purpose, security, and repayment.

Fields:

- `loan_purpose`: Loan Purpose, required
- `loan_purpose_other`: Specify Purpose, required when purpose is other
- `amount`: Loan Amount, required
- `tenor`: Loan Tenor in months, required
- `collateral_security[]`: Type of Security/Collateral, required
- `repayment_mode`: Mode of Repayment, required

Repayment modes are normalized in the backend to:

- `cheque`
- `standing_order`
- `direct_debit`
- `cash_deposit`

If selected collateral includes pledgeable security such as shop stock, household appliances, business proceeds, or property documents, the UI warns that the Pledge and Trust Receipt is required in Step 8.

### Step 7: Disbursement Account

Category: payout destination.

Fields:

- `account_name`: Account Name, required
- `account_number`: Account Number, required, max 10 characters
- `bank_name`: Bank Name, required
- `sort_code`: Sort Code

### Step 8: Pledge and Trust Receipt

Category: collateral pledge. This is form `MMFB/CRM/02`.

The user can either fill the form in app or upload a completed signed pledge form for OCR review.

Facility and pledger fields:

- `pledge_date`: Date, required in fill mode
- `pledge_borrower`: Name of Borrower/Association, required in fill mode
- `pledge_amount_figs`: Facility Amount in Figures, required in fill mode
- `pledge_amount_words`: Facility Amount in Words, required in fill mode
- `pledge_location`: Shop/House Address where goods are located, required in fill mode
- `pledge_obligor`: Name of Obligor, required in fill mode

Pledged item schedule:

- `pledge_item_name[]`: Item name, required
- `pledge_item_qty[]`: Quantity, required
- `pledge_item_desc[]`: Description
- `pledge_item_val[]`: Estimated value, required

Execution fields:

- `pledge_legal_ack`: Legal acknowledgement checkbox, required
- `borrower_pledge_signature`: Borrower Signature, required
- `witness_pledge_signature`: Witness Signature, required
- `witness_name`: Witness Name
- `witness_address`: Witness Address

Upload mode accepts a pledge file and directs the user to OCR review for `doc=pledge`.

### Step 9: Declarations and Consents

Category: legal consent and final borrower declaration.

Fields:

- `consent_credit_bureau`: Credit Bureau Disclosure consent, required
- `consent_credit_check`: Credit Check Authorisation, required
- `consent_cheque`: Cheque Recovery Authorisation, required
- `consent_gsi`: Global Standing Instruction mandate, required
- `applicant_signature`: Applicant Signature, required

Submitting this step records the intake submission and advances the application to OCR review.

## Guarantor Form

The guarantor wizard is used for each guarantor slot: `/applications/{id}/guarantors/{1-2}/step/{1-8}`. Completing Step 8 submits the guarantor record and returns to the main intake Step 3. This form maps to `MMFB/CRM/03`.

### Step 1: Identity

- `name`: Full Name, required
- `relationship`: Relationship to Applicant, required
- `phone`: Phone Number, required
- `bvn`: BVN, required, max 11 characters
- `dob`: Date of Birth, required
- `origin_lga`: State of Origin / LGA
- `home_address`: Home Address, required

### Step 2: Obligations

- `existing_loans`: Existing loans or active guarantees

### Step 3: Family and Marital Details

- `marital_status`: Marital Status
- `dependants`: Number of Dependants
- `spouse_info`: Spouse Name and Address

### Step 4: Employment Details

- `employer_name`: Employer Name
- `monthly_salary`: Monthly Salary
- `employer_address`: Workplace Address

### Step 5: Business and Supporting Documents

- `business_sector`: Guarantor Business Sector
- `business_turnover`: Monthly Turnover
- Required supporting documents are shown in the UI for guarantor evidence.

### Step 6: Declaration

- `declaration_accept`: Guarantor declaration acceptance, required

### Step 7: Guarantee Limits and Bank Details

- `max_guarantee`: Maximum Guarantee Limit, required
- `cheque_number`: Cheque Number, required
- `bank_name`: Bank Name, required
- `account_number`: Account Number, required, max 10 characters

### Step 8: Signatures and Witnesses

- `guarantor_signature`: Guarantor Signature, required
- `witness_signature`: Witness Signature, required
- `witness_name`: Witness Name
- `witness_date`: Witness Date, required

Persisted guarantor columns include slot, full name, relationship, BVN, phone, home address, employment type, monthly salary, maximum guarantee amount, bank name, account number, cheque number, form stage, and signature detection flags.

## Dashboard Generated Forms

The dashboard JavaScript can open a form workspace for Loan Officers. Each form supports either manual entry or upload/OCR mode.

### Loan Application Form

Category: generated `MMFB/CRM/01`-style workspace.

Applicant and Identity:

- `full_name`: Full Name
- `phone`: Phone Number
- `bvn`: BVN
- `means_of_id`: Means of ID
- `id_number`: ID Number
- `marital_status`: Single, Married, Separated, Divorced

Loan and Security:

- `loan_purpose`: Loan Purpose
- `loan_amount`: Loan Amount
- `loan_tenor`: Loan Tenor
- `collateral_description`: Collateral / Security
- `repayment_method`: Cheque, Standing Order, Direct Debit, Cash Deposit
- `disbursement_account`: Disbursement Account Number

Consents and Signature:

- `credit_bureau_consent`: Credit bureau consent accepted
- `cheque_authorization`: Cheque authorization accepted
- `gsi_mandate`: GSI mandate accepted
- `applicant_signature`: Applicant Signature
- `signature_date`: Date

### Guarantors Form

Guarantor Identity:

- `guarantor_name`: Guarantor Full Name
- `relationship`: Relationship to Client
- `phone`: Phone Number
- `bvn`: BVN
- `date_of_birth`: Date of Birth
- `home_address`: Home Address

Employment and Guarantee:

- `employment_status`: Employed, Self Employed, Public Servant, Other
- `employer_or_business`: Employer / Business Name
- `maximum_guarantee`: Maximum Guarantee Amount
- `bank_account`: Bank Account Number
- `cheque_number`: Cheque Number
- `pledged_items`: Pledged Items

Declaration and Witness:

- `declaration_accepted`: Declaration accepted
- `guarantor_signature`: Guarantor Signature
- `witness_signature`: Witness Signature
- `witness_date`: Witness Date

### Pledge and Trust Receipt

Facility and Pledger:

- `receipt_date`: Date
- `borrower_or_association`: Borrower / Association Name
- `facility_amount_figures`: Facility Amount in Figures
- `facility_amount_words`: Facility Amount in Words
- `shop_or_house_address`: Shop / House Address
- `obligor_name`: Obligor Name

Pledged Asset Schedule:

- `pledged_goods`: Pledged Goods / Stock / Assets
- `quantity`: Quantity
- `description`: Description
- `estimated_value`: Estimated Value

Execution and Witness:

- `borrower_signature`: Borrower Signature
- `witness_signature`: Witness Signature
- `witness_name`: Witness Name
- `witness_address`: Witness Address
- `witness_occupation`: Witness Occupation

Upload/OCR mode accepts PDF, JPG, JPEG, and PNG and shows a preview of extracted fields such as name, BVN, loan or guarantee amount, and signature confidence.

## OCR Review Form

Category: extraction correction and verification.

Fields shown for review:

- `full_name`: Applicant Full Name
- `amount`: Loan Amount
- `signature`: Guarantor Signature
- `bvn`: Bank Verification Number

Actions:

- Save All Corrections
- Mark as Verified
- Return Document for Re-upload

OCR data is stored as an OCR result per document and one row per extracted field. OCR fields track original value, corrected value, final value, confidence, source, critical flag, verification state, verifier, correction user, and timestamps.

## Visitation Report

Category: field verification and branch-manager concurrence.

Loan Officer fields:

- `visit_date`: Date of Visitation, required
- `visit_time`: Time of Arrival
- `person_met`: Person Met, required
- `relationship`: Relationship to Applicant
- `premises_description`: Premises Description, required
- `direction_from_branch`: Direction from Branch, required
- `business_condition`: Business Condition Observed
- `visiting_officer`: Visiting Officer Name, read-only
- `account_officer`: Account Officer, required
- `visiting_officer_sig`: Visiting Officer Signature, required
- `account_officer_sig`: Account Officer Signature

Branch Manager concurrence fields:

- `bm_name`: Branch Manager Name, read-only
- `concurrence`: Concurred or Returned for Correction, required
- `concurrence_return_reason`: Reason for Return, required when returned
- `bm_notes`: Concurrence Notes / Observations
- `bm_sig`: Manager Signature, required

Persisted visitation status values are `pending`, `submitted`, `concurred`, and `returned`.

## Credit Review Form

Category: underwriting review and recommendation.

Read-only borrower/profile fields:

- Borrower Name
- Requested Amount
- Loan Tenor
- Product Type
- Collateral Pledged

Editable review fields:

- `affordability_notes`: Credit Officer Override Notes
- `ocr_override_1`: OCR exception decision
- `ocr_override_2`: OCR exception decision
- `recommendation_decision`: Recommend Approval, Recommend Rejection, or Return for Correction
- `recommendation_notes`: Decision Reason / Notes, required

The credit review screen also presents document evidence categories and OCR discrepancy checks.

## Approval Readiness Form

Category: branch/system approval gate.

The approval screen is a readiness checklist rather than a detailed data-capture form. It checks gates such as:

- Loan Application Form submitted
- Pledge and Trust Receipt submitted
- Guarantor Form submitted
- OCR verified
- Required guarantors verified
- Visitation/concurrence completed
- Critical documents available

Actions:

- Return to Loan Officer
- Return to Credit Officer
- Approve for Disbursement

The approval action posts `force_approve=1`.

## Return Application Form

Category: correction routing.

Fields:

- `reason_category`: Missing Documents, Incorrect Information, Low Confidence OCR, Unsigned Form, Policy Exception, or Other
- `corrections[]`: ID Expiry proof, Spousal consent signature, Bank Statement, Guarantor Cheque, Visitation Concurrence
- `notes`: Checklist Details / Instruction Notes, required

Submitting this form moves the application to the returned state and provides correction instructions.

## End-to-End Flow

1. User creates a new application from `/applications/new`.
2. User selects customer type and loan category.
3. Backend creates a loan draft with stage `intake`.
4. User completes the 9-step intake wizard.
5. User completes two guarantor forms from intake Step 3.
6. User uploads supporting documents where required.
7. User fills or uploads the Pledge and Trust Receipt if collateral requires it.
8. On intake Step 9, user accepts all consents and submits.
9. Backend advances the application to `ocr_review`.
10. OCR review validates extracted critical fields and signatures.
11. Verified OCR advances the application toward `credit_review`.
12. Credit Officer reviews borrower profile, document evidence, OCR exceptions, collateral, guarantors, and affordability.
13. Credit Officer recommends approval, rejection, or return for correction.
14. Branch Manager reviews approval readiness and visitation concurrence.
15. If ready, the loan is approved for disbursement and moves toward `disbursement_ready`.
16. If problems are found at OCR, credit, or approval, the application is returned with a reason and correction checklist.
17. Final terminal states include `disbursed` and `rejected`.

## Workflow Stages

The `loan_applications.stage` values are:

- `intake`
- `ocr_review`
- `credit_review`
- `branch_approval`
- `disbursement_ready`
- `disbursed`
- `returned`
- `rejected`

Workflow activity is stored as immutable events with event type, source stage, destination stage, triggering user, role, notes, and timestamp.

