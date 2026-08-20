class DynamicReadinessService:
    def __init__(self, conn): self.conn = conn

    async def calculate(self, application_id, org_id):
        app = await self.conn.fetchrow("SELECT loan_type FROM loan_applications WHERE id=$1 AND org_id=$2", application_id, org_id)
        if not app: return None
        fields = await self.conn.fetch("""SELECT id,field_key,label,requirement FROM product_form_fields
          WHERE product_code=$1 AND org_id=$2 AND requirement='required' ORDER BY display_order""", app["loan_type"], org_id)
        docs = await self.conn.fetch("""SELECT doc_type,COALESCE(display_name,doc_type) display_name FROM product_document_requirements
          WHERE product_code=$1 AND (org_id=$2 OR org_id IS NULL) AND is_mandatory=TRUE""", app["loan_type"], org_id)
        values = {str(row["field_id"]) for row in await self.conn.fetch(
            "SELECT field_id FROM application_dynamic_values WHERE application_id=$1 AND org_id=$2 AND value_json NOT IN ('null'::jsonb,'\"\"'::jsonb)", application_id, org_id)}
        uploaded = {row["doc_type"] for row in await self.conn.fetch(
            "SELECT DISTINCT doc_type FROM documents WHERE loan_id=$1 AND org_id=$2 AND deleted_at IS NULL", application_id, org_id)}
        items = ([{"key": field["field_key"], "label": field["label"], "satisfied": str(field["id"]) in values, "type": "field"} for field in fields] +
                 [{"key": doc["doc_type"], "label": doc["display_name"], "satisfied": doc["doc_type"] in uploaded, "type": "document"} for doc in docs])
        satisfied = sum(1 for item in items if item["satisfied"])
        return {"percentage": round(100 * satisfied / len(items)) if items else 100,
                "ready": satisfied == len(items), "items": items,
                "missing": [item for item in items if not item["satisfied"]]}

    async def require_ready(self, application_id, org_id):
        readiness = await self.calculate(application_id, org_id)
        if readiness and not readiness["ready"]:
            from fastapi import HTTPException
            raise HTTPException(status_code=409, detail={"message": "Application is incomplete.", **readiness})
        return readiness
