import base64
import os

from app.config import settings
from app.core.field_encryption import blind_index, decrypt_sensitive, encrypt_sensitive, mask_sensitive


def test_sensitive_field_round_trip_and_random_nonce(monkeypatch):
    key = base64.urlsafe_b64encode(os.urandom(32)).decode("ascii")
    lookup = base64.urlsafe_b64encode(os.urandom(32)).decode("ascii")
    monkeypatch.setattr(settings, "FIELD_ENCRYPTION_KEY", key)
    monkeypatch.setattr(settings, "FIELD_LOOKUP_KEY", lookup)
    first = encrypt_sensitive("22216142222", context="loan_application:bvn")
    second = encrypt_sensitive("22216142222", context="loan_application:bvn")
    assert first.startswith("enc:v1:")
    assert first != second
    assert decrypt_sensitive(first, context="loan_application:bvn") == "22216142222"
    assert blind_index("222 1614 2222", context="loan_application:bvn") == blind_index(
        "22216142222", context="loan_application:bvn"
    )
    assert mask_sensitive("22216142222") == "*******2222"
