from uuid import UUID

from app.core.base_repository import BaseRepository


class GuarantorRepository(BaseRepository):
    domain = "guarantors"

    async def upsert_submitted(
        self,
        *,
        loan_id: UUID,
        org_id: UUID,
        slot: int,
        full_name: str | None = None,
        relationship_to_client: str | None = None,
        bvn: str | None = None,
        phone: str | None = None,
        home_address: str | None = None,
        employment_type: str | None = None,
        monthly_salary: float | None = None,
        max_guarantee_amount: float | None = None,
        bank_name: str | None = None,
        account_number: str | None = None,
        cheque_number: str | None = None,
        signature_detected: bool = False,
        witness_signature_detected: bool = False,
    ) -> dict:
        row = await self.conn.fetchrow(
            self.sql("upsert_submitted"),
            loan_id,
            org_id,
            slot,
            full_name,
            relationship_to_client,
            bvn,
            phone,
            home_address,
            employment_type,
            monthly_salary,
            max_guarantee_amount,
            bank_name,
            account_number,
            cheque_number,
            signature_detected,
            witness_signature_detected,
        )
        return dict(row)
