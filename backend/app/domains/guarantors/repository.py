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
        from app.core.field_encryption import blind_index, decrypt_sensitive, encrypt_sensitive
        encrypted_bvn = encrypt_sensitive(bvn, context="guarantor:bvn")
        encrypted_account = encrypt_sensitive(account_number, context="guarantor:account_number")
        encrypted_cheque = encrypt_sensitive(cheque_number, context="guarantor:cheque_number")
        row = await self.conn.fetchrow(
            self.sql("upsert_submitted"),
            loan_id,
            org_id,
            slot,
            full_name,
            relationship_to_client,
            encrypted_bvn,
            phone,
            home_address,
            employment_type,
            monthly_salary,
            max_guarantee_amount,
            bank_name,
            encrypted_account,
            encrypted_cheque,
            signature_detected,
            witness_signature_detected,
            blind_index(bvn, context="guarantor:bvn"),
            blind_index(account_number, context="guarantor:account_number"),
        )
        result = dict(row)
        result["bvn"] = decrypt_sensitive(result.get("bvn"), context="guarantor:bvn")
        result["account_number"] = decrypt_sensitive(result.get("account_number"), context="guarantor:account_number")
        result["cheque_number"] = decrypt_sensitive(result.get("cheque_number"), context="guarantor:cheque_number")
        return result
