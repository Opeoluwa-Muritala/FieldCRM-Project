import re

from app.core.exceptions import DomainException


class ProductService:
    def __init__(self, repo): self.repo = repo

    async def create(self, org_id, version_id, definition):
        if not await self.repo.draft_version(version_id, org_id):
            raise DomainException("Products can only be added to a draft configuration version.", 409)
        if len({field.field_key for field in definition.fields}) != len(definition.fields):
            raise DomainException("Form field keys must be unique within a product.", 422)
        sections = {section.section_key for section in definition.sections if section.requirement == "hidden"}
        if any(field.section_key in sections and field.requirement == "required" for field in definition.fields):
            raise DomainException("A hidden section cannot contain a required field.", 422)
        async with self.repo.conn.transaction():
            return await self.repo.create(org_id, version_id, definition)

    @staticmethod
    def visible(field, values):
        condition = dict(field.get("visibility_condition") or {})
        if not condition: return True
        return values.get(condition.get("field")) == condition.get("equals")

    def validate_values(self, fields, values):
        errors = []
        for field in fields:
            if field["requirement"] == "hidden" or not self.visible(field, values): continue
            value = values.get(field["field_key"])
            if field["requirement"] == "required" and value in (None, "", []):
                errors.append(f"{field['label']} is required.")
                continue
            rules = dict(field.get("validation_rules") or {})
            if value not in (None, "") and rules.get("pattern") and not re.fullmatch(rules["pattern"], str(value)):
                errors.append(f"{field['label']} has an invalid format.")
        return errors
