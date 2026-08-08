-- Migration: 036_offer_letter_product_configs.sql
-- Attaches offer letter fees, securities, and boilerplate configurations to loan products (loan types).

-- 1. Create offer letter product configs table
CREATE TABLE IF NOT EXISTS offer_letter_product_configs (
    product_code                TEXT PRIMARY KEY REFERENCES loan_products(code) ON DELETE CASCADE,
    fees_template               JSONB NOT NULL,
    securities_template         JSONB NOT NULL,
    boilerplate_paragraphs      JSONB NOT NULL,
    conditions_precedent        JSONB NOT NULL
);

-- 2. Insert Save & Borrow Basic into product catalog (if not exists)
INSERT INTO loan_products (
    code, name, description, family, customer_segment, active,
    min_amount, max_amount, min_tenor_months, max_tenor_months,
    repayment_frequency, interest_calculation_type, collateral_required, guarantor_required
) VALUES (
    'save_n_borrow_basic', 'Save & Borrow Basic', 'Save and Borrow Basic Facility', 'savings_personal', 'individual', TRUE,
    50000.00, 2000000.00, 1, 12, 'weekly', 'flat', TRUE, TRUE
) ON CONFLICT (code) DO NOTHING;

-- 3. Map aliases for Save & Borrow Basic
INSERT INTO product_aliases (alias, product_code) VALUES
('save and borrow basic', 'save_n_borrow_basic'),
('save & borrow basic', 'save_n_borrow_basic')
ON CONFLICT (alias) DO NOTHING;

-- 4. Seed configs for Enterprise Loan (corporate_sme)
INSERT INTO offer_letter_product_configs (
    product_code, fees_template, securities_template, boilerplate_paragraphs, conditions_precedent
) VALUES (
    'corporate_sme',
    '[
        {"name": "Administrative Fee", "percentage": 1.0, "is_upfront": true},
        {"name": "Processing Fee", "percentage": 1.0, "is_upfront": true},
        {"name": "Risk Premium", "percentage": 1.0, "is_upfront": true},
        {"name": "Application Fee", "fixed_amount": 1000.0, "is_upfront": true},
        {"name": "Handling Charge", "percentage": 0.1, "is_upfront": true},
        {"name": "Credit Search Fee", "fixed_amount": 1000.0, "is_upfront": true},
        {"name": "Collateral Registry", "fixed_amount": 2000.0, "is_upfront": true},
        {"name": "Pre Liquidation fee", "percentage": 1.0, "is_upfront": false, "note": "on outstanding Principal"}
    ]'::jsonb,
    '[
        "Duly executed deeds of guarantee by {guarantors_list}.",
        "10% Cash Collateral ({cash_collateral_amount}).",
        "Stock hypothecation.",
        "Transfer of ownership of 42inches Samsung Tv, LG standing fridge and Elepaq Generator."
    ]'::jsonb,
    '[
        "In the event of failure by the borrower to pay any due instalment on the Facility, interest shall be calculated on the unpaid instalment(s) at the Bank’s default rate of additional 1% flat per month and 6% penalty rate on expiration of the loan monthly.",
        "A non-repayment of two (2) instalments amounts to a default of the entire facility agreement and such default entitles the bank to call in the facility and or take step as it may think fit to recover its funds.",
        "The bank shall be at liberty to review the rates applicable to this facility in line with prevailing money market conditions from time to time and such review shall be deemed acceptable to the borrower where the facility is not fully repaid immediately.",
        "All legal, statutory, regulatory and out of pocket expenses that may arise in the execution of this facility or in enforcing the terms and conditions in respect of same shall be for the account of the borrower.",
        "No failure or delay the bank in executing any remedy, power or right above shall operate as a waiver or impairment thereof nor shall it affect or impair any such remedies powers or rights of any such subsequent default.",
        "The bank reserves the right to alter, amend and vary the terms on which this offer is made without recourse to you.",
        "By signing this offer letter/loan agreement and by drawing on the loan, I covenant to repay the loan as and when due. In the event that I fail to repay the loan as agreed, and the loan becomes delinquent, the bank shall have the right to report the delinquent loan to the CBN through the Credit Risk Management System (CRMS) or by any other means, and request the CBN exercise its regulatory power to direct all banks and other financial institutions under its regulatory purview to set-off my indebtedness from any money standing to my credit in any bank account and from any other financial assets they may be holding for my benefit.",
        "I covenant and warrant that the bank shall have power to set-off my indebtedness under this loan agreement from all such monies and funds standing to my credit/benefit in any and all such accounts or from any other financial assets belonging to me and in the custody of any such bank.",
        "I hereby waive any right of confidentiality whether arising under common law or statue or in any other manner whatsoever and irrevocably agree that I shall not argue to the contrary before any court of law, tribunal, administrative authority or any other body acting in any judicial or quasi-judicial capacity."
    ]'::jsonb,
    '[
        "Submission of duly signed offer letter.",
        "Submission of Loan request form.",
        "Duly executed deed of Guarantors.",
        "Payment of all upfront charges."
    ]'::jsonb
) ON CONFLICT (product_code) DO UPDATE 
SET fees_template = EXCLUDED.fees_template,
    securities_template = EXCLUDED.securities_template,
    boilerplate_paragraphs = EXCLUDED.boilerplate_paragraphs,
    conditions_precedent = EXCLUDED.conditions_precedent;

-- 5. Seed configs for Save & Borrow Basic (save_n_borrow_basic)
INSERT INTO offer_letter_product_configs (
    product_code, fees_template, securities_template, boilerplate_paragraphs, conditions_precedent
) VALUES (
    'save_n_borrow_basic',
    '[
        {"name": "Administrative Fee", "percentage": 1.0, "is_upfront": true},
        {"name": "Processing Fee", "percentage": 1.0, "is_upfront": true},
        {"name": "Risk Premium", "percentage": 1.0, "is_upfront": true},
        {"name": "Handling Charge", "percentage": 0.1, "is_upfront": true},
        {"name": "Credit Search Fee", "fixed_amount": 500.0, "is_upfront": true}
    ]'::jsonb,
    '[
        "Duly executed deeds of guarantee by {guarantors_list}.",
        "20% cash collateral ({cash_collateral_amount})."
    ]'::jsonb,
    '[
        "In the event of failure by the borrower to pay any due instalment on the Facility, interest shall be calculated on the unpaid instalment(s) at the Bank’s default rate of additional 1% flat per month and 5.5% penalty rate on expiration of the loan monthly. A non repayment of two (2) instalments amounts to a default of the entire facility agreement and such default entitles the bank to call in the facility and or take step as it may think fit to recover its funds. The bank shall be at liberty to review the rates applicable to this facility in line with prevailing money market conditions from time to time and such review shall be deemed acceptable to the borrower where the facility is not fully repaid immediately. All legal, statutory, regulatory and out of pocket expenses that may arise in the execution of this facility or in enforcing the terms and conditions in respect of same shall be for the account of the borrower. No failure or delay the bank in executing any remedy, power or right above shall operate as a waiver or impairment thereof nor shall it affect or impair any such remedies powers or rights of any such subsequent default. The bank reserves the right to alter, amend and vary the terms on which this offer is made without recourse to you.",
        "By signing this offer letter/loan agreement and by drawing on the loan, I covenant to repay the loan as and when due. In the event that I fail to repay the loan as agreed, and the loan becomes delinquent, the bank shall have the right to report the delinquent loan to the CBN through the Credit Risk Management System (CRMS) or by any other means, and request the CBN exercise its regulatory power to direct all banks and other financial institutions under its regulatory purview to set-off my indebtedness from any money standing to my credit in any bank account and from any other financial assets they may be holding for my benefit. I covenant and warrant that the bank shall have power to set-off my indebtedness under this loan agreement from all such monies and funds standing to my credit/benefit in any and all such accounts or from any other financial assets belonging to me and in the custody of any such bank. I hereby waive any right of confidentiality whether arising under common law or statue or in any other manner whatsoever and irrevocably agree that I shall not argue to the contrary before any court of law, tribunal, administrative authority or any other body acting in any judicial or quasi-judicial capacity."
    ]'::jsonb,
    '[
        "Submission of duly signed offer letter.",
        "Submission of Loan request form",
        "Duly executed Deed of Guarantors",
        "Payment of all upfront charges."
    ]'::jsonb
) ON CONFLICT (product_code) DO UPDATE 
SET fees_template = EXCLUDED.fees_template,
    securities_template = EXCLUDED.securities_template,
    boilerplate_paragraphs = EXCLUDED.boilerplate_paragraphs,
    conditions_precedent = EXCLUDED.conditions_precedent;
