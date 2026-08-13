"""Encrypt legacy restricted identifiers in place.

Run with the schema-owner/maintenance DATABASE_URL after migration 041. The
command is dry-run by default and never prints field values.
"""
from __future__ import annotations

import argparse
import asyncio
import json
from collections.abc import Mapping

from app.core.database import close_pool, get_connection, init_pool
from app.core.field_encryption import blind_index, decrypt_sensitive, encrypt_sensitive, encryption_configured

RESTRICTED_STAGE_FIELDS = {
    "bvn": "bvn",
    "nin": "nin",
    "account_number": "account_number",
    "bank_account_number": "account_number",
    "cheque_number": "cheque_number",
}


def _encrypt_json(value, *, scope: str):
    changed = 0
    if isinstance(value, Mapping):
        output = {}
        for key, child in value.items():
            normalized = str(key).lower()
            if normalized in RESTRICTED_STAGE_FIELDS and isinstance(child, str) and child:
                encrypted = encrypt_sensitive(child, context=f"{scope}:{RESTRICTED_STAGE_FIELDS[normalized]}")
                output[key] = encrypted
                changed += int(encrypted != child)
            else:
                output[key], nested = _encrypt_json(child, scope=scope)
                changed += nested
        return output, changed
    if isinstance(value, list):
        output = []
        for child in value:
            encrypted, nested = _encrypt_json(child, scope=scope)
            output.append(encrypted)
            changed += nested
        return output, changed
    return value, 0


async def migrate(*, apply: bool) -> dict[str, int]:
    if not encryption_configured():
        raise RuntimeError("FIELD_ENCRYPTION_KEY and FIELD_LOOKUP_KEY must both be configured")
    totals = {"loan_rows": 0, "guarantor_rows": 0, "stage_fields": 0}
    async with get_connection() as conn:
        loans = await conn.fetch(
            "SELECT id, bvn, bvn_lookup_hash FROM loan_applications WHERE bvn IS NOT NULL AND bvn <> ''"
        )
        guarantors = await conn.fetch(
            """SELECT id, bvn, account_number, cheque_number, bvn_lookup_hash, account_lookup_hash FROM guarantors
               WHERE NULLIF(bvn, '') IS NOT NULL OR NULLIF(account_number, '') IS NOT NULL
                  OR NULLIF(cheque_number, '') IS NOT NULL"""
        )
        stages = await conn.fetch("SELECT id, stage, data_json FROM stage_data")

        async with conn.transaction():
            for row in loans:
                values = dict(row)
                raw = values["bvn"]
                plain = decrypt_sensitive(raw, context="loan_application:bvn")
                encrypted = encrypt_sensitive(raw, context="loan_application:bvn")
                if encrypted != raw or not values.get("bvn_lookup_hash"):
                    totals["loan_rows"] += 1
                    if apply:
                        await conn.execute(
                            "UPDATE loan_applications SET bvn=$1, bvn_lookup_hash=$2 WHERE id=$3",
                            encrypted, blind_index(plain, context="loan_application:bvn"), row["id"],
                        )

            for row in guarantors:
                values = dict(row)
                plain_bvn = decrypt_sensitive(values.get("bvn"), context="guarantor:bvn")
                plain_account = decrypt_sensitive(values.get("account_number"), context="guarantor:account_number")
                replacements = {
                    "bvn": encrypt_sensitive(values.get("bvn"), context="guarantor:bvn"),
                    "account_number": encrypt_sensitive(values.get("account_number"), context="guarantor:account_number"),
                    "cheque_number": encrypt_sensitive(values.get("cheque_number"), context="guarantor:cheque_number"),
                }
                if (any(replacements[key] != values.get(key) for key in replacements)
                        or (plain_bvn and not values.get("bvn_lookup_hash"))
                        or (plain_account and not values.get("account_lookup_hash"))):
                    totals["guarantor_rows"] += 1
                    if apply:
                        await conn.execute(
                            """UPDATE guarantors SET bvn=$1, account_number=$2, cheque_number=$3,
                               bvn_lookup_hash=$4, account_lookup_hash=$5 WHERE id=$6""",
                            replacements["bvn"], replacements["account_number"], replacements["cheque_number"],
                            blind_index(plain_bvn, context="guarantor:bvn"),
                            blind_index(plain_account, context="guarantor:account_number"), row["id"],
                        )

            for row in stages:
                scope = "guarantor_stage" if str(row["stage"]).startswith("guarantor_") else "intake"
                encrypted_json, changed = _encrypt_json(row["data_json"] or {}, scope=scope)
                totals["stage_fields"] += changed
                if apply and changed:
                    await conn.execute(
                        "UPDATE stage_data SET data_json=CAST($1 AS jsonb) WHERE id=$2",
                        json.dumps(encrypted_json, separators=(",", ":")),
                        row["id"],
                    )
    return totals


async def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="commit encryption updates; default is dry-run")
    args = parser.parse_args()
    await init_pool()
    try:
        totals = await migrate(apply=args.apply)
        mode = "applied" if args.apply else "dry-run"
        print(f"{mode}: {totals['loan_rows']} loan rows, {totals['guarantor_rows']} guarantor rows, "
              f"{totals['stage_fields']} stage fields")
    finally:
        await close_pool()


if __name__ == "__main__":
    asyncio.run(main())
