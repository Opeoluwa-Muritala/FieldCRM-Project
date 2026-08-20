from __future__ import annotations

import re
import unicodedata
from difflib import SequenceMatcher
from uuid import UUID, uuid4

from app.core.field_encryption import blind_index, decrypt_sensitive, encrypt_sensitive, mask_sensitive
from app.domains.customers.repository import CustomerRepository
from app.domains.customers.schemas import CustomerCreate, CustomerInput, DuplicateMatch


EVENT_TAXONOMY = frozenset({
    "created", "edited", "submitted", "returned", "document_uploaded", "visit_completed",
    "credit_reviewed", "approved", "cbs_sync", "repayment_detected", "collection_action",
    "application_linked", "configuration_applied", "workflow_transition",
})


class DuplicateOverrideRequired(RuntimeError):
    def __init__(self, matches: list[DuplicateMatch]):
        super().__init__("Probable duplicate customer requires an override reason")
        self.matches = matches


def normalize_text(value: str | None) -> str | None:
    if not value:
        return None
    folded = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return " ".join(re.sub(r"[^a-zA-Z0-9]+", " ", folded).lower().split()) or None


def name_signature(value: str) -> str:
    return " ".join(sorted((normalize_text(value) or "").split()))


class CustomerService:
    def __init__(self, repo: CustomerRepository):
        self.repo = repo

    @staticmethod
    def _prepared(payload: CustomerInput) -> dict:
        values = payload.model_dump(exclude={"duplicate_override_reason"})
        values.update({
            "normalized_name": normalize_text(payload.legal_name),
            "name_signature": name_signature(payload.legal_name),
            "normalized_address": normalize_text(payload.residential_address),
            "phone_hash": blind_index(payload.phone, context="customer:phone"),
            "email_hash": blind_index(str(payload.email) if payload.email else None, context="customer:email"),
            "bvn_hash": blind_index(payload.bvn, context="customer:bvn"),
            "nin_hash": blind_index(payload.nin, context="customer:nin"),
            "account_hash": blind_index(payload.bank_account, context="customer:account"),
            "cbs_provider": normalize_text(payload.cbs_provider),
        })
        return values

    async def duplicates(self, org_id: UUID, payload: CustomerInput) -> list[DuplicateMatch]:
        values = self._prepared(payload)
        candidates = await self.repo.duplicate_candidates(org_id, values)
        matches: list[DuplicateMatch] = []
        for candidate in candidates:
            rules = []
            for rule, key in (
                ("same_bvn", "bvn_hash"), ("same_nin", "nin_hash"),
                ("same_phone", "phone_hash"), ("same_email", "email_hash"),
            ):
                if values.get(key) and candidate.get(key.replace("_hash", "_lookup_hash")) == values[key]:
                    rules.append(rule)
            if values.get("account_hash") and candidate.get("account_match"):
                rules.append("same_bank_account")
            if values.get("external_customer_id") and candidate.get("external_customer_id") == values["external_customer_id"] and candidate.get("cbs_provider") == values.get("cbs_provider"):
                rules.append("existing_cbs_customer")
            if values.get("normalized_address") and candidate.get("normalized_address") == values["normalized_address"]:
                rules.append("same_address")
            if payload.date_of_birth and candidate.get("date_of_birth") == payload.date_of_birth:
                similarity = SequenceMatcher(None, values["name_signature"], candidate.get("name_signature") or "").ratio()
                if similarity >= 0.82:
                    rules.append("similar_name_and_dob")
            if rules:
                matches.append(DuplicateMatch(
                    customer_id=str(candidate["id"]), customer_number=candidate["customer_number"],
                    legal_name=candidate["legal_name"], matched_rules=sorted(set(rules)),
                ))
        return matches

    async def create(self, *, org_id: UUID, actor_id: UUID, branch_id: UUID | None, payload: CustomerCreate, source: str = "manual_web") -> dict:
        matches = await self.duplicates(org_id, payload)
        reason = (payload.duplicate_override_reason or "").strip()
        if matches and len(reason) < 10:
            raise DuplicateOverrideRequired(matches)
        values = self._prepared(payload)
        values.update({
            "org_id": org_id, "created_by": actor_id, "branch_id": branch_id,
            "customer_number": f"CUST-{uuid4().hex[:12].upper()}",
            "phone_encrypted": encrypt_sensitive(payload.phone, context="customer:phone"),
            "email_encrypted": encrypt_sensitive(str(payload.email) if payload.email else None, context="customer:email"),
            "bvn_encrypted": encrypt_sensitive(payload.bvn, context="customer:bvn"),
            "nin_encrypted": encrypt_sensitive(payload.nin, context="customer:nin"),
        })
        customer = await self.repo.create(values)
        if payload.bank_account:
            await self.repo.add_account(
                customer_id=customer["id"], org_id=org_id,
                encrypted=encrypt_sensitive(payload.bank_account, context="customer:account"),
                lookup_hash=values["account_hash"], bank_name=payload.bank_name, source=source,
            )
        for match in matches:
            await self.repo.add_override(
                org_id=org_id, customer_id=customer["id"], duplicate_id=UUID(match.customer_id),
                rules=match.matched_rules, reason=reason, actor_id=actor_id,
            )
        await self.repo.add_activity(
            org_id=org_id, customer_id=customer["id"], application_id=None,
            event_type="created", actor_id=actor_id, source=source,
            summary="Customer profile created" + (" with duplicate override" if matches else ""),
        )
        return self.public_profile(customer, [])

    @staticmethod
    def public_profile(customer: dict, accounts: list[dict]) -> dict:
        profile = dict(customer)
        profile["phone"] = decrypt_sensitive(profile.pop("phone_encrypted", None), context="customer:phone")
        profile["email"] = decrypt_sensitive(profile.pop("email_encrypted", None), context="customer:email")
        profile["bvn"] = decrypt_sensitive(profile.pop("bvn_encrypted", None), context="customer:bvn")
        profile["nin"] = decrypt_sensitive(profile.pop("nin_encrypted", None), context="customer:nin")
        for key in ("phone_lookup_hash", "email_lookup_hash", "bvn_lookup_hash", "nin_lookup_hash"):
            profile.pop(key, None)
        profile["accounts"] = [
            {
                "id": row["id"], "account_number": decrypt_sensitive(row["account_number_encrypted"], context="customer:account"),
                "bank_name": row.get("bank_name"), "is_primary": row.get("is_primary"), "source": row.get("source"),
            }
            for row in accounts
        ]
        return profile

    async def get_profile(self, customer_id: UUID, org_id: UUID) -> dict | None:
        customer = await self.repo.get(customer_id, org_id)
        if not customer:
            return None
        return self.public_profile(customer, await self.repo.accounts(customer_id, org_id))

    async def search(self, *, org_id: UUID, query: str, role: str, user_id: UUID, branch_id: UUID | None, limit: int = 50) -> list[dict]:
        query = query.strip()
        if len(query) < 2:
            return []
        digits = "".join(filter(str.isdigit, query))
        hashes = {
            "phone_hash": blind_index(query, context="customer:phone") if 7 <= len(digits) <= 15 else None,
            "bvn_hash": blind_index(digits, context="customer:bvn") if len(digits) == 11 else None,
            "nin_hash": blind_index(digits, context="customer:nin") if len(digits) == 11 else None,
            "account_hash": blind_index(digits, context="customer:account") if len(digits) == 10 else None,
        }
        rows = await self.repo.search(
            org_id=org_id, query=query, hashes=hashes, role=role, user_id=user_id,
            branch_id=branch_id, limit=min(max(limit, 1), 50),
        )
        return [{
            "id": str(row["id"]), "legal_name": row["legal_name"],
            "customer_reference": row["customer_number"], "business_name": row.get("business_name"),
            "external_customer_id": row.get("external_customer_id"),
            "masked_phone": mask_sensitive(decrypt_sensitive(row.get("phone_encrypted"), context="customer:phone"), visible=3),
            "masked_bvn": mask_sensitive(decrypt_sensitive(row.get("bvn_encrypted"), context="customer:bvn")),
            "masked_nin": mask_sensitive(decrypt_sensitive(row.get("nin_encrypted"), context="customer:nin")),
        } for row in rows]


def can_view_customer(user, customer: dict) -> bool:
    if str(user.org_id) != str(customer.get("org_id")) or not getattr(user, "is_active", True):
        return False
    role = str(user.role).lower().replace(" ", "_")
    role = {"loan_officer": "account_officer", "relationship_officer": "account_officer", "team_lead": "branch_manager"}.get(role, role)
    if role == "account_officer":
        return str(customer.get("relationship_officer_id") or customer.get("created_by")) == str(user.id)
    if role == "branch_manager":
        return customer.get("branch_id") is not None and str(customer["branch_id"]) == str(getattr(user, "branch_id", None))
    return role in {"branch_supervisor", "credit_analyst", "crm", "head_crm", "auditor", "ed", "md", "legal"}
