from typing import Optional, List, Any, Dict
from pydantic import BaseModel, Field

class FieldWithMeta(BaseModel):
    value: Any
    source: str = "manual"  # manual, ocr, corrected, approved
    confidence: Optional[float] = None

class LoanApplicationFormModel(BaseModel):
    # Section 1
    applicant_full_name: FieldWithMeta
    gender: FieldWithMeta
    date_of_birth: FieldWithMeta
    marital_status: FieldWithMeta
    nationality: FieldWithMeta
    residential_address: FieldWithMeta
    business_address: FieldWithMeta
    phone_numbers: FieldWithMeta
    email_address: FieldWithMeta
    bvn: FieldWithMeta
    means_of_identification: FieldWithMeta
    id_number: FieldWithMeta
    id_issue_date: FieldWithMeta
    id_expiry_date: FieldWithMeta
    
    # Section 2
    spouse_name: Optional[FieldWithMeta] = None
    spouse_phone_number: Optional[FieldWithMeta] = None
    spouse_occupation: Optional[FieldWithMeta] = None
    spouse_employer: Optional[FieldWithMeta] = None
    spouse_address: Optional[FieldWithMeta] = None
    
    # Section 3
    employer_name: Optional[FieldWithMeta] = None
    employer_address: Optional[FieldWithMeta] = None
    employment_status: Optional[FieldWithMeta] = None
    position: Optional[FieldWithMeta] = None
    staff_number: Optional[FieldWithMeta] = None
    date_employed: Optional[FieldWithMeta] = None
    monthly_salary: Optional[FieldWithMeta] = None
    other_income: Optional[FieldWithMeta] = None
    salary_account_details: Optional[FieldWithMeta] = None
    
    # Section 4
    business_name: Optional[FieldWithMeta] = None
    nature_of_business: Optional[FieldWithMeta] = None
    business_registration_number: Optional[FieldWithMeta] = None
    years_in_operation: Optional[FieldWithMeta] = None
    monthly_sales: Optional[FieldWithMeta] = None
    monthly_expenses: Optional[FieldWithMeta] = None
    average_monthly_turnover: Optional[FieldWithMeta] = None

    # Section 6
    loan_type: FieldWithMeta
    loan_amount_requested: FieldWithMeta
    loan_purpose: FieldWithMeta
    tenor: FieldWithMeta
    repayment_frequency: FieldWithMeta
    repayment_method: FieldWithMeta
    proposed_disbursement_account: FieldWithMeta
    proposed_disbursement_date: FieldWithMeta
    
    # Section 8
    credit_bureau_consent: FieldWithMeta
    cheque_authorisation: FieldWithMeta
    gsi_mandate: FieldWithMeta
    terms_acceptance: FieldWithMeta
    
    # Section 9
    applicant_signature: FieldWithMeta
    signature_date: FieldWithMeta
    witness_name: FieldWithMeta
    witness_signature: FieldWithMeta
    witness_date: FieldWithMeta

class GuarantorsFormModel(BaseModel):
    guarantor_full_name: FieldWithMeta
    relationship_to_applicant: FieldWithMeta
    gender: FieldWithMeta
    marital_status: FieldWithMeta
    phone_number: FieldWithMeta
    email_address: FieldWithMeta
    bvn: FieldWithMeta
    residential_address: FieldWithMeta
    
    id_type: FieldWithMeta
    id_number: FieldWithMeta
    issue_date: FieldWithMeta
    expiry_date: FieldWithMeta
    
    employer_name: Optional[FieldWithMeta] = None
    employer_address: Optional[FieldWithMeta] = None
    position: Optional[FieldWithMeta] = None
    monthly_income: Optional[FieldWithMeta] = None
    business_name: Optional[FieldWithMeta] = None
    nature_of_business: Optional[FieldWithMeta] = None
    business_address: Optional[FieldWithMeta] = None
    
    maximum_guarantee_limit: FieldWithMeta
    bank_name: FieldWithMeta
    account_number: FieldWithMeta
    account_name: FieldWithMeta
    cheque_number: FieldWithMeta
    
    declaration_text: FieldWithMeta
    guarantor_signature: FieldWithMeta
    signature_date: FieldWithMeta
    witness_name: FieldWithMeta
    witness_signature: FieldWithMeta
    witness_date: FieldWithMeta

class PledgeTrustReceiptFormModel(BaseModel):
    borrower_name: FieldWithMeta
    association_name: FieldWithMeta
    facility_amount: FieldWithMeta
    shop_address: FieldWithMeta
    house_address: FieldWithMeta
    
    expected_sales_proceeds: FieldWithMeta
    collection_method: FieldWithMeta
    deposit_account: FieldWithMeta
    
    borrower_signature: FieldWithMeta
    signature_date: FieldWithMeta
    witness_name: FieldWithMeta
    witness_signature: FieldWithMeta
    witness_date: FieldWithMeta
