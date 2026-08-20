"""Manual authenticated dashboard smoke runner.

This is intentionally not a pytest test. Supply credentials explicitly rather
than weakening password verification or embedding staff identities in source.
"""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

from fastapi.testclient import TestClient

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))

from app.main import app as fastapi_app  # noqa: E402


def _smoke_users() -> list[dict[str, str]]:
    raw = os.getenv("FIELDCRM_SMOKE_USERS", "")
    if not raw:
        raise RuntimeError(
            "FIELDCRM_SMOKE_USERS must be a JSON array of email, role, and path objects"
        )
    users = json.loads(raw)
    if not isinstance(users, list) or not users:
        raise ValueError("FIELDCRM_SMOKE_USERS must contain at least one user")
    for user in users:
        if not isinstance(user, dict) or not all(
            isinstance(user.get(key), str) and user[key].strip()
            for key in ("email", "role", "path")
        ):
            raise ValueError("Each smoke user requires non-empty email, role, and path strings")
        if not user["path"].startswith("/") or user["path"].startswith("//"):
            raise ValueError("Dashboard smoke paths must be local absolute paths")
    return users


def run_dashboard_smoke() -> None:
    password = os.getenv("FIELDCRM_SMOKE_PASSWORD", "")
    if not password:
        raise RuntimeError("FIELDCRM_SMOKE_PASSWORD is required")

    with TestClient(fastapi_app) as client:
        for user in _smoke_users():
            login = client.post(
                "/api/v1/auth/login",
                data={"username": user["email"], "password": password},
            )
            if login.status_code != 200:
                print(f"Login failed for role {user['role']}: HTTP {login.status_code}")
                continue
            token = login.json().get("access_token", "")
            response = client.get(
                user["path"],
                headers={
                    "Authorization": f"Bearer {token}",
                    "X-Progressive-Load": "true",
                },
            )
            print(
                f"Role {user['role']:20} Path {user['path']:20} "
                f"HTTP {response.status_code} Bytes {len(response.content)}"
            )


if __name__ == "__main__":
    run_dashboard_smoke()
