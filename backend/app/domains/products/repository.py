import json


class ProductRepository:
    def __init__(self, conn): self.conn = conn

    async def draft_version(self, version_id, org_id):
        return await self.conn.fetchrow("SELECT id FROM configuration_versions WHERE id=$1 AND org_id=$2 AND status='draft'", version_id, org_id)

    async def create(self, org_id, version_id, product):
        code = f"{str(org_id).replace('-','')[:12]}_{product.code}"
        row = await self.conn.fetchrow(
            """INSERT INTO loan_products(code,name,description,family,customer_segment,active,min_amount,max_amount,
               min_tenor_months,max_tenor_months,repayment_frequency,interest_calculation_type,collateral_required,
               guarantor_required,workflow_stages,cbs_enabled,org_id,configuration_version_id,interest_parameters,
               guarantor_count,collateral_rules,approval_limits,visit_requirements,credit_checks,sla_hours)
               VALUES($1,$2,$3,$4,$5,FALSE,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18::jsonb,$19,$20::jsonb,$21::jsonb,$22::jsonb,$23::jsonb,$24)
               RETURNING *""",
            code, product.name, product.description, product.family, product.customer_segment,
            product.min_amount, product.max_amount, product.min_tenor_months, product.max_tenor_months,
            product.repayment_frequency, str(product.interest_parameters.get("calculation_type", "flat")),
            product.collateral_required, product.guarantor_count > 0, ",".join(product.workflow_stages), product.cbs_enabled,
            org_id, version_id, json.dumps(product.interest_parameters), product.guarantor_count,
            json.dumps(product.collateral_rules), json.dumps(product.approval_limits), json.dumps(product.visit_requirements),
            json.dumps(product.credit_checks), product.sla_hours,
        )
        for section in product.sections:
            await self.conn.execute("""INSERT INTO product_section_requirements(org_id,configuration_version_id,product_code,section_key,requirement)
                                     VALUES($1,$2,$3,$4,$5)""", org_id, version_id, code, section.section_key, section.requirement)
        for document in product.documents:
            await self.conn.execute("""INSERT INTO product_document_requirements(product_code,doc_type,is_mandatory,org_id,configuration_version_id,display_name)
                                     VALUES($1,$2,$3,$4,$5,$6)""", code, document.doc_type, document.mandatory, org_id, version_id, document.display_name)
        for field in product.fields:
            await self.conn.execute("""INSERT INTO product_form_fields(org_id,configuration_version_id,product_code,section_key,field_key,label,field_type,requirement,options,validation_rules,visibility_condition,help_text,display_order)
                                     VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9::jsonb,$10::jsonb,$11::jsonb,$12,$13)""",
                                    org_id, version_id, code, field.section_key, field.field_key, field.label, field.field_type,
                                    field.requirement, json.dumps(field.options), json.dumps(field.validation_rules),
                                    json.dumps(field.visibility_condition), field.help_text, field.display_order)
        return row

    async def effective(self, org_id):
        return await self.conn.fetch("""SELECT lp.* FROM loan_products lp
          LEFT JOIN configuration_versions cv ON cv.id=lp.configuration_version_id
          WHERE (lp.org_id IS NULL AND lp.active=TRUE) OR
                (lp.org_id=$1 AND cv.status='published' AND cv.effective_at<=NOW())
          ORDER BY lp.name""", org_id)

    async def definition(self, code, org_id):
        product = await self.conn.fetchrow("""SELECT lp.* FROM loan_products lp LEFT JOIN configuration_versions cv ON cv.id=lp.configuration_version_id
          WHERE lp.code=$1 AND ((lp.org_id IS NULL AND lp.active=TRUE) OR (lp.org_id=$2 AND cv.status='published' AND cv.effective_at<=NOW()))""", code, org_id)
        if not product: return None
        fields = await self.conn.fetch("SELECT * FROM product_form_fields WHERE product_code=$1 AND org_id=$2 ORDER BY display_order,id", code, org_id)
        sections = await self.conn.fetch("SELECT section_key,requirement FROM product_section_requirements WHERE product_code=$1 AND org_id=$2", code, org_id)
        documents = await self.conn.fetch("SELECT doc_type,display_name,is_mandatory FROM product_document_requirements WHERE product_code=$1 AND (org_id=$2 OR org_id IS NULL)", code, org_id)
        return {"product": dict(product), "fields": [dict(x) for x in fields], "sections": [dict(x) for x in sections], "documents": [dict(x) for x in documents]}
