import asyncio
import json
import os

import asyncpg
from dotenv import load_dotenv


load_dotenv("backend/.env")

TABLES = [
    "loan_applications",
    "documents",
    "ocr_results",
    "ocr_fields",
    "guarantors",
    "pledged_items",
    "stage_data",
    "workflow_events",
    "visitation_reports",
    "notifications",
    "audit_entries",
    "repayment_schedule",
    "repayment_records",
    "business_locations",
    "business_pnl",
    "collateral_items",
    "loan_recommendations",
    "committee_votes",
]


async def main():
    conn = await asyncpg.connect(os.getenv("DATABASE_URL"))
    try:
        users_by_role = await conn.fetch(
            """
            SELECT role, count(*) AS count
            FROM users
            WHERE active = TRUE
            GROUP BY role
            ORDER BY role
            """
        )
        users = await conn.fetch(
            """
            SELECT full_name, email, role
            FROM users
            WHERE active = TRUE
            ORDER BY role, full_name
            """
        )
        counts = {}
        for table in TABLES:
            exists = await conn.fetchval(
                "SELECT to_regclass($1) IS NOT NULL",
                f"public.{table}",
            )
            counts[table] = await conn.fetchval(f"SELECT count(*) FROM {table}") if exists else None

        loan_dependencies = await conn.fetch(
            """SELECT tc.table_name, kcu.column_name, rc.delete_rule
               FROM information_schema.table_constraints tc
               JOIN information_schema.key_column_usage kcu
                 ON tc.constraint_name=kcu.constraint_name AND tc.constraint_schema=kcu.constraint_schema
               JOIN information_schema.referential_constraints rc
                 ON tc.constraint_name=rc.constraint_name AND tc.constraint_schema=rc.constraint_schema
               JOIN information_schema.constraint_column_usage ccu
                 ON ccu.constraint_name=tc.constraint_name AND ccu.constraint_schema=tc.constraint_schema
               WHERE tc.constraint_type='FOREIGN KEY' AND ccu.table_name='loan_applications'
               ORDER BY tc.table_name"""
        )
        matrix = await conn.fetch(
            """SELECT u.full_name AS account_officer, la.loan_type, la.stage, count(*) AS count
               FROM loan_applications la
               JOIN users u ON u.id=la.created_by
               GROUP BY u.full_name, la.loan_type, la.stage
               ORDER BY u.full_name, la.loan_type, la.stage"""
        )
        matrix_exceptions = [dict(row) for row in matrix if row["count"] != 2]
        mcc_finalized = await conn.fetchval(
            "SELECT count(*) FROM loan_applications WHERE mcc_finalized_by IS NOT NULL"
        )

        print(
            json.dumps(
                {
                    "users_by_role": [dict(row) for row in users_by_role],
                    "table_counts": counts,
                    "users": [dict(row) for row in users],
                    "matrix_group_count": len(matrix),
                    "matrix_exceptions": matrix_exceptions,
                    "mcc_finalized_applications": mcc_finalized,
                    "loan_dependencies": [dict(row) for row in loan_dependencies],
                },
                indent=2,
                default=str,
            )
        )
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
