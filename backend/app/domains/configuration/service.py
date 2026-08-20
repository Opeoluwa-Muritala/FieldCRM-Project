import copy
import re
from urllib.parse import urlparse

from app.core.exceptions import DomainException
from app.domains.configuration.catalog import FEATURE_DEFAULTS, PRESETS, default_payload

HIGH_RISK_PREFIXES = ("features.", "workflow.", "approval_matrix.", "security.", "integrations.")


class ConfigurationService:
    def __init__(self, repo):
        self.repo = repo

    @staticmethod
    def _payload(row) -> dict:
        return dict(row["payload"]) if row else {}

    async def effective(self, org_id):
        row = await self.repo.current(org_id)
        if row:
            return self._payload(row), row
        name = await self.repo.organisation_name(org_id) or "FieldCRM"
        return default_payload(name), None

    async def create_draft(self, org_id, actor_id, draft):
        payload, _ = await self.effective(org_id)
        return await self.repo.create(org_id=org_id, payload=payload, reason=draft.reason,
                                      effective_at=draft.effective_at, actor_id=actor_id)

    @staticmethod
    def _validate_value(path: str, value):
        root, key = path.split(".", 1)
        if root == "features":
            if key not in FEATURE_DEFAULTS or not isinstance(value, bool):
                raise DomainException("Select a supported feature and boolean state.", 400)
        elif path == "organisation.preset":
            if value not in PRESETS:
                raise DomainException("Select a supported organisation preset.", 400)
        elif path.endswith("_email") and value:
            if not re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", str(value)):
                raise DomainException("Enter a valid support email.", 400)
        elif path.endswith("_url") and value:
            parsed = urlparse(str(value))
            if parsed.scheme != "https" or not parsed.hostname:
                raise DomainException("Brand asset URLs must use HTTPS.", 400)
        elif path == "branding.brand_accent":
            if not re.fullmatch(r"#[0-9A-Fa-f]{6}", str(value)):
                raise DomainException("Brand accent must be a six-digit hex colour.", 400)
        elif root not in {"organisation", "branding", "sla", "security", "workflow", "approval_matrix",
                          "documents", "integrations", "field_operations"}:
            raise DomainException("Unknown configuration section.", 400)
        if isinstance(value, str) and len(value) > 500:
            raise DomainException("Configuration value is too long.", 400)

    async def patch(self, version_id, org_id, actor_id, change):
        async with self.repo.conn.transaction():
            row = await self.repo.get(version_id, org_id, lock=True)
            if not row or row["status"] != "draft":
                raise DomainException("Only a draft configuration can be changed.", 409)
            self._validate_value(change.setting_path, change.value)
            payload = copy.deepcopy(self._payload(row))
            root, key = change.setting_path.split(".", 1)
            payload.setdefault(root, {})
            old = payload[root].get(key)
            payload[root][key] = change.value
            high_risk = bool(row["high_risk"]) or change.setting_path.startswith(HIGH_RISK_PREFIXES)
            updated = await self.repo.patch(version_id, org_id, payload, high_risk)
            await self.repo.log_change(org_id=org_id, version_id=version_id, path=change.setting_path,
                                       old=old, new=change.value, actor_id=actor_id, reason=change.reason)
            return updated

    async def validation_errors(self, row, org_id):
        features = {**FEATURE_DEFAULTS, **self._payload(row).get("features", {})}
        errors = []
        for product in await self.repo.product_dependencies(org_id):
            code = product["code"]
            if product["guarantor_required"] and not features["guarantors"]:
                errors.append(f"Product {code} requires guarantors.")
            if product["collateral_required"] and not features["collateral"]:
                errors.append(f"Product {code} requires collateral.")
            if product.get("cbs_enabled") and not features["cbs_integration"]:
                errors.append(f"Product {code} requires CBS integration.")
        if features["credit_bureau_integration"] and features["manual_credit_bureau_evidence"]:
            errors.append("Choose direct credit bureau integration or manual evidence, not both.")
        return errors

    async def validate(self, version_id, org_id, actor_id):
        row = await self.repo.get(version_id, org_id)
        if not row or row["status"] != "draft":
            raise DomainException("Only a draft can be validated.", 409)
        errors = await self.validation_errors(row, org_id)
        if errors:
            raise DomainException("Configuration validation failed: " + " ".join(errors), 409)
        payload = self._payload(row)
        needs_approval = bool(row["high_risk"] and payload.get("security", {}).get("configuration_second_approver", True))
        return await self.repo.validate(version_id, org_id, actor_id, needs_approval)

    async def approve(self, version_id, org_id, actor_id):
        row = await self.repo.approve(version_id, org_id, actor_id)
        if not row:
            raise DomainException("A different Configuration Admin must approve this version.", 409)
        return row

    async def publish(self, version_id, org_id, actor_id):
        async with self.repo.conn.transaction():
            row = await self.repo.get(version_id, org_id, lock=True)
            if not row or row["status"] != "validated":
                raise DomainException("Validate and approve this version before publishing.", 409)
            if row["requires_second_approval"] and not row["approved_by"]:
                raise DomainException("Second approval is required before publishing.", 409)
            return await self.repo.publish(version_id, org_id, actor_id)

    async def feature_enabled(self, org_id, name: str) -> bool:
        if name not in FEATURE_DEFAULTS:
            return False
        payload, _ = await self.effective(org_id)
        return bool({**FEATURE_DEFAULTS, **payload.get("features", {})}[name])
