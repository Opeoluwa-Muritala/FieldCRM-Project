import pytest

from app.main import safe_relative_redirect


@pytest.mark.parametrize(
    "value",
    [
        "https://attacker.example",
        "//attacker.example",
        "/\\attacker.example",
        "/%5cattacker.example",
        "/%252f%252fattacker.example",
        "/dashboard\nLocation: https://attacker.example",
    ],
)
def test_unsafe_redirect_targets_are_rejected(value):
    assert safe_relative_redirect(value) is None


def test_same_site_redirect_target_is_accepted():
    assert safe_relative_redirect("/applications?stage=review") == "/applications?stage=review"
