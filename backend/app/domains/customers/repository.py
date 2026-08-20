from __future__ import annotations

from uuid import UUID


class CustomerRepository:
    def __init__(self, conn):
        self.conn = conn

    async def duplicate_candidates(self, org_id: UUID, values: dict) -> list[dict]:
        rows = await self.conn.fetch(
            """
            SELECT c.*,
                   EXISTS(
                     SELECT 1 FROM customer_accounts ca
                     WHERE ca.customer_id=c.id AND ca.org_id=c.org_id
                       AND $8 IS NOT NULL AND ca.account_number_lookup_hash=$8
                   ) AS account_match
            FROM customers c
            WHERE c.org_id=$1 AND c.active=TRUE AND (
              ($2 IS NOT NULL AND c.bvn_lookup_hash=$2) OR
              ($3 IS NOT NULL AND c.nin_lookup_hash=$3) OR
              ($4 IS NOT NULL AND c.phone_lookup_hash=$4) OR
              ($5 IS NOT NULL AND c.email_lookup_hash=$5) OR
              ($6 IS NOT NULL AND c.date_of_birth=$6) OR
              ($7 IS NOT NULL AND c.normalized_address=$7) OR
              ($9 IS NOT NULL AND c.external_customer_id=$9 AND c.cbs_provider=$10) OR
              EXISTS(SELECT 1 FROM customer_accounts ca WHERE ca.customer_id=c.id AND ca.org_id=c.org_id AND $8 IS NOT NULL AND ca.account_number_lookup_hash=$8)
            )
            ORDER BY c.updated_at DESC LIMIT 200
            """,
            org_id,
            values.get("bvn_hash"), values.get("nin_hash"), values.get("phone_hash"),
            values.get("email_hash"), values.get("date_of_birth"), values.get("normalized_address"),
            values.get("account_hash"), values.get("external_customer_id"), values.get("cbs_provider"),
        )
        return [dict(row) for row in rows]

    async def create(self, values: dict) -> dict:
        row = await self.conn.fetchrow(
            """
            INSERT INTO customers (
              org_id,customer_number,legal_name,normalized_name,name_signature,date_of_birth,
              phone_encrypted,phone_lookup_hash,email_encrypted,email_lookup_hash,
              bvn_encrypted,bvn_lookup_hash,nin_encrypted,nin_lookup_hash,
              residential_address,normalized_address,business_name,external_customer_id,cbs_provider,
              relationship_officer_id,branch_id,created_by
            ) VALUES (
              $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$20
            ) RETURNING *
            """,
            values["org_id"], values["customer_number"], values["legal_name"],
            values["normalized_name"], values["name_signature"], values.get("date_of_birth"),
            values.get("phone_encrypted"), values.get("phone_hash"),
            values.get("email_encrypted"), values.get("email_hash"),
            values.get("bvn_encrypted"), values.get("bvn_hash"),
            values.get("nin_encrypted"), values.get("nin_hash"),
            values.get("residential_address"), values.get("normalized_address"),
            values.get("business_name"), values.get("external_customer_id"), values.get("cbs_provider"),
            values["created_by"], values.get("branch_id"),
        )
        return dict(row)

    async def add_account(self, *, customer_id: UUID, org_id: UUID, encrypted: str, lookup_hash: str, bank_name: str | None, source: str) -> None:
        await self.conn.execute(
            """INSERT INTO customer_accounts
               (customer_id,org_id,account_number_encrypted,account_number_lookup_hash,bank_name,is_primary,source)
               VALUES ($1,$2,$3,$4,$5,TRUE,$6)""",
            customer_id, org_id, encrypted, lookup_hash, bank_name, source,
        )

    async def add_override(self, *, org_id: UUID, customer_id: UUID, duplicate_id: UUID, rules: list[str], reason: str, actor_id: UUID) -> None:
        await self.conn.execute(
            """INSERT INTO customer_duplicate_overrides
               (org_id,customer_id,probable_duplicate_id,matched_rules,override_reason,overridden_by)
               VALUES ($1,$2,$3,$4,$5,$6)""",
            org_id, customer_id, duplicate_id, rules, reason, actor_id,
        )

    async def add_activity(self, *, org_id: UUID, customer_id: UUID, application_id: UUID | None, event_type: str, actor_id: UUID | None, source: str, summary: str) -> None:
        await self.conn.execute(
            """INSERT INTO customer_activity
               (org_id,customer_id,application_id,event_type,actor_id,source,summary)
               VALUES ($1,$2,$3,$4,$5,$6,$7)""",
            org_id, customer_id, application_id, event_type, actor_id, source, summary[:500],
        )

    async def get(self, customer_id: UUID, org_id: UUID) -> dict | None:
        row = await self.conn.fetchrow("SELECT * FROM customers WHERE id=$1 AND org_id=$2 AND active=TRUE", customer_id, org_id)
        return dict(row) if row else None

    async def accounts(self, customer_id: UUID, org_id: UUID) -> list[dict]:
        rows = await self.conn.fetch("SELECT * FROM customer_accounts WHERE customer_id=$1 AND org_id=$2 ORDER BY is_primary DESC,created_at", customer_id, org_id)
        return [dict(row) for row in rows]

    async def link_application(self, *, customer_id: UUID, application_id: UUID, org_id: UUID, actor_id: UUID) -> bool:
        result = await self.conn.execute(
            """UPDATE loan_applications SET customer_id=$1, updated_at=NOW()
               WHERE id=$2 AND org_id=$3 AND deleted_at IS NULL""",
            customer_id, application_id, org_id,
        )
        if result.startswith("UPDATE"):
            await self.add_activity(
                org_id=org_id, customer_id=customer_id, application_id=application_id,
                event_type="application_linked", actor_id=actor_id, source="manual_web",
                summary="Application linked to customer profile",
            )
            return True
        return False

    async def search(self, *, org_id: UUID, query: str, hashes: dict, role: str, user_id: UUID, branch_id: UUID | None, limit: int = 50) -> list[dict]:
        rows = await self.conn.fetch(
            """
            SELECT DISTINCT c.id,c.customer_number,c.legal_name,c.business_name,c.external_customer_id,
                   c.phone_encrypted,c.bvn_encrypted,c.nin_encrypted,c.relationship_officer_id,c.branch_id
            FROM customers c
            LEFT JOIN customer_accounts ca ON ca.customer_id=c.id AND ca.org_id=c.org_id
            LEFT JOIN loan_applications la ON la.customer_id=c.id AND la.org_id=c.org_id AND la.deleted_at IS NULL
            WHERE c.org_id=$1 AND c.active=TRUE
              AND ($2 NOT IN ('account_officer','branch_manager')
                   OR ($2='account_officer' AND (c.relationship_officer_id=$3 OR c.created_by=$3))
                   OR ($2='branch_manager' AND c.branch_id=$4))
              AND (
                c.legal_name ILIKE '%' || $5 || '%' OR c.customer_number ILIKE '%' || $5 || '%'
                OR coalesce(c.business_name,'') ILIKE '%' || $5 || '%'
                OR coalesce(c.external_customer_id,'') ILIKE '%' || $5 || '%'
                OR coalesce(la.ref_no,'') ILIKE '%' || $5 || '%'
                OR coalesce(la.external_loan_id,'') ILIKE '%' || $5 || '%'
                OR ($6 IS NOT NULL AND c.phone_lookup_hash=$6)
                OR ($7 IS NOT NULL AND c.bvn_lookup_hash=$7)
                OR ($8 IS NOT NULL AND c.nin_lookup_hash=$8)
                OR ($9 IS NOT NULL AND ca.account_number_lookup_hash=$9)
              )
            ORDER BY c.legal_name LIMIT $10
            """,
            org_id, role, user_id, branch_id, query,
            hashes.get("phone_hash"), hashes.get("bvn_hash"), hashes.get("nin_hash"), hashes.get("account_hash"), limit,
        )
        return [dict(row) for row in rows]

    async def dossier(self, customer_id: UUID, org_id: UUID) -> dict:
        applications = [dict(row) for row in await self.conn.fetch(
            "SELECT * FROM loan_applications WHERE customer_id=$1 AND org_id=$2 AND deleted_at IS NULL ORDER BY created_at DESC",
            customer_id, org_id,
        )]
        visits = [dict(row) for row in await self.conn.fetch(
            """SELECT vr.* FROM visitation_reports vr JOIN loan_applications la ON la.id=vr.loan_id
               WHERE la.customer_id=$1 AND vr.org_id=$2 ORDER BY vr.created_at DESC""", customer_id, org_id,
        )]
        documents = [dict(row) for row in await self.conn.fetch(
            """SELECT d.* FROM documents d JOIN loan_applications la ON la.id=d.loan_id
               WHERE la.customer_id=$1 AND d.org_id=$2 AND d.deleted_at IS NULL ORDER BY d.created_at DESC""", customer_id, org_id,
        )]
        guarantors = [dict(row) for row in await self.conn.fetch(
            """SELECT g.* FROM guarantors g JOIN loan_applications la ON la.id=g.loan_id
               WHERE la.customer_id=$1 AND g.org_id=$2 ORDER BY g.created_at DESC""", customer_id, org_id,
        )]
        local_repayments = [dict(row) for row in await self.conn.fetch(
            """SELECT rr.*, 'FieldCRM' AS source FROM repayment_records rr JOIN loan_applications la ON la.id=rr.loan_id
               WHERE la.customer_id=$1 AND rr.org_id=$2 ORDER BY rr.payment_date DESC""", customer_id, org_id,
        )]
        cbs_repayments = [dict(row) for row in await self.conn.fetch(
            """SELECT ct.*, 'Core Banking' AS source FROM core_banking_transactions ct JOIN loan_applications la ON la.id=ct.loan_id
               WHERE la.customer_id=$1 AND ct.org_id=$2 AND ct.transaction_type='repayment' ORDER BY ct.transaction_at DESC""", customer_id, org_id,
        )]
        timeline = [dict(row) for row in await self.conn.fetch(
            """SELECT ca.event_type,ca.summary,ca.source,ca.actor_id,u.full_name AS actor_name,ca.application_id,ca.created_at
               FROM customer_activity ca LEFT JOIN users u ON u.id=ca.actor_id AND u.org_id=ca.org_id
               WHERE ca.customer_id=$1 AND ca.org_id=$2 ORDER BY ca.created_at DESC LIMIT 500""", customer_id, org_id,
        )]
        linked_events = [dict(row) for row in await self.conn.fetch(
            """
            SELECT CASE
                     WHEN lower(we.event_type) LIKE '%submit%' THEN 'submitted'
                     WHEN lower(we.event_type) LIKE '%return%' THEN 'returned'
                     WHEN lower(we.event_type) LIKE '%approv%' THEN 'approved'
                     ELSE 'workflow_transition'
                   END AS event_type,
                   coalesce(we.notes, we.event_type) AS summary,
                   'workflow' AS source, we.triggered_by AS actor_id,
                   u.full_name AS actor_name, we.loan_id AS application_id, we.created_at
            FROM workflow_events we
            JOIN loan_applications la ON la.id=we.loan_id AND la.org_id=we.org_id
            LEFT JOIN users u ON u.id=we.triggered_by AND u.org_id=we.org_id
            WHERE la.customer_id=$1 AND we.org_id=$2
            UNION ALL
            SELECT CASE
                     WHEN ae.action='cbs.sync' THEN 'cbs_sync'
                     WHEN ae.entity_type='document' OR lower(ae.action) LIKE '%document%' THEN 'document_uploaded'
                     WHEN lower(ae.action) LIKE '%credit%' THEN 'credit_reviewed'
                     ELSE 'edited'
                   END AS event_type,
                   coalesce(ae.notes,ae.action) AS summary,
                   coalesce(ae.source,'audit') AS source, ae.user_id AS actor_id,
                   u.full_name AS actor_name, la.id AS application_id, ae.created_at
            FROM audit_entries ae
            JOIN loan_applications la ON la.id=ae.entity_id AND ae.entity_type='loan_application' AND la.org_id=ae.org_id
            LEFT JOIN users u ON u.id=ae.user_id AND u.org_id=ae.org_id
            WHERE la.customer_id=$1 AND ae.org_id=$2
            ORDER BY created_at DESC LIMIT 500
            """, customer_id, org_id,
        )]
        timeline.extend(linked_events)
        timeline.sort(key=lambda row: str(row.get("created_at") or ""), reverse=True)
        timeline = timeline[:500]
        snapshots = [dict(row) for row in await self.conn.fetch(
            """SELECT s.* FROM core_banking_loan_snapshots s JOIN loan_applications la ON la.id=s.loan_id
               WHERE la.customer_id=$1 AND s.org_id=$2""", customer_id, org_id,
        )]
        collateral = [dict(row) for row in await self.conn.fetch(
            """SELECT ci.id,ci.application_id,ci.collateral_type,ci.narration,ci.loan_based_price,
                      ci.face_value,ci.force_sale_value,ci.created_at
               FROM collateral_items ci JOIN loan_applications la ON la.id=ci.application_id
               WHERE la.customer_id=$1 AND la.org_id=$2 ORDER BY ci.created_at DESC""", customer_id, org_id,
        )]
        credit_assessment = [dict(row) for row in await self.conn.fetch(
            """SELECT bs.id,bs.loan_application_id,bs.registry_id,bs.status,bs.report_type,bs.submitted_at
               FROM bureau_submissions bs JOIN loan_applications la ON la.id=bs.loan_application_id
               WHERE la.customer_id=$1 AND la.org_id=$2 ORDER BY bs.submitted_at DESC""", customer_id, org_id,
        )]
        communications = [dict(row) for row in await self.conn.fetch(
            """SELECT n.id,n.application_id,n.title,n.message,n.type,n.created_at
               FROM notifications n JOIN loan_applications la ON la.id=n.application_id
               WHERE la.customer_id=$1 AND n.org_id=$2 ORDER BY n.created_at DESC""", customer_id, org_id,
        )]
        for visit in visits:
            if visit.get("status") in {"submitted", "concurred"}:
                timeline.append({
                    "event_type": "visit_completed", "summary": "Field visit completed",
                    "source": "manual_web", "actor_id": visit.get("visiting_officer_id"),
                    "actor_name": None, "application_id": visit.get("loan_id"),
                    "created_at": visit.get("updated_at") or visit.get("created_at"),
                })
        for document in documents:
            timeline.append({
                "event_type": "document_uploaded", "summary": "Document uploaded",
                "source": "manual_web", "actor_id": document.get("uploaded_by"),
                "actor_name": None, "application_id": document.get("loan_id"),
                "created_at": document.get("created_at") or document.get("uploaded_at"),
            })
        for repayment in cbs_repayments + local_repayments:
            timeline.append({
                "event_type": "repayment_detected", "summary": f"Repayment detected from {repayment['source']}",
                "source": "cbs" if repayment["source"] == "Core Banking" else "manual_web",
                "actor_id": repayment.get("recorded_by"), "actor_name": None,
                "application_id": repayment.get("loan_id"),
                "created_at": repayment.get("transaction_at") or repayment.get("created_at"),
            })
        for assessment in credit_assessment:
            timeline.append({
                "event_type": "credit_reviewed", "summary": "Credit bureau evidence recorded",
                "source": "credit_bureau", "actor_id": None, "actor_name": None,
                "application_id": assessment.get("loan_application_id"), "created_at": assessment.get("submitted_at"),
            })
        timeline.sort(key=lambda row: str(row.get("created_at") or ""), reverse=True)
        timeline = timeline[:500]
        return {
            "applications": applications, "visits": visits, "documents": documents,
            "guarantors": guarantors, "repayments": cbs_repayments + local_repayments,
            "timeline": timeline, "cbs_loans": snapshots,
            "collateral": collateral, "credit_assessment": credit_assessment,
            "communications": communications, "collection_activity": [],
        }
