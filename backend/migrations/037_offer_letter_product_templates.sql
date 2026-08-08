-- Give every catalog product an explicit offer-letter configuration.
-- Product-specific fees/securities remain editable in the config table while
-- the family determines the initial template category and fallback shape.
ALTER TABLE offer_letter_product_configs
    ADD COLUMN IF NOT EXISTS template_category TEXT;

UPDATE offer_letter_product_configs c
SET template_category = CASE
    WHEN p.family = 'corporate_business' THEN 'corporate_business'
    WHEN p.family = 'education' THEN 'education'
    WHEN p.family = 'salary_personal' THEN 'salary_personal'
    WHEN p.family = 'savings_personal' THEN 'savings_personal'
    ELSE 'general'
END
FROM loan_products p
WHERE p.code = c.product_code;

-- Seed missing products from the closest maintained family template. This
-- creates one row per product while preserving the catalog product name and
-- terms in the generated letter.
INSERT INTO offer_letter_product_configs (
    product_code, fees_template, securities_template, boilerplate_paragraphs,
    conditions_precedent, template_category
)
SELECT
    p.code,
    base.fees_template,
    base.securities_template,
    base.boilerplate_paragraphs,
    base.conditions_precedent,
    CASE
        WHEN p.family = 'corporate_business' THEN 'corporate_business'
        WHEN p.family = 'education' THEN 'education'
        WHEN p.family = 'salary_personal' THEN 'salary_personal'
        WHEN p.family = 'savings_personal' THEN 'savings_personal'
        ELSE 'general'
    END
FROM loan_products p
JOIN offer_letter_product_configs base
  ON base.product_code = CASE
      WHEN p.family = 'corporate_business' THEN 'corporate_sme'
      ELSE 'save_n_borrow_basic'
  END
ON CONFLICT (product_code) DO UPDATE
SET template_category = EXCLUDED.template_category;
