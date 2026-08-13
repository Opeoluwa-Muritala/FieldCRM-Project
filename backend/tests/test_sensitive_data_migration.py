from migrations.encrypt_existing_sensitive_fields import _encrypt_json
from app.core.field_encryption import PREFIX


def test_stage_migration_encrypts_nested_restricted_fields(monkeypatch):
    monkeypatch.setattr(
        "migrations.encrypt_existing_sensitive_fields.encrypt_sensitive",
        lambda value, *, context: f"{PREFIX}{context}:{value}",
    )
    payload = {
        "name": "Ada",
        "bvn": "12345678901",
        "bank": {"account_number": "0011223344"},
        "items": [{"nin": "98765432109"}],
    }

    result, changed = _encrypt_json(payload, scope="intake")

    assert changed == 3
    assert result["name"] == "Ada"
    assert result["bvn"].startswith(f"{PREFIX}intake:bvn:")
    assert result["bank"]["account_number"].startswith(f"{PREFIX}intake:account_number:")
    assert result["items"][0]["nin"].startswith(f"{PREFIX}intake:nin:")


def test_stage_migration_is_idempotent(monkeypatch):
    def encrypt(value, *, context):
        return value if value.startswith(PREFIX) else f"{PREFIX}{context}:{value}"

    monkeypatch.setattr("migrations.encrypt_existing_sensitive_fields.encrypt_sensitive", encrypt)
    encrypted = f"{PREFIX}intake:bvn:ciphertext"
    result, changed = _encrypt_json({"bvn": encrypted}, scope="intake")

    assert result["bvn"] == encrypted
    assert changed == 0
