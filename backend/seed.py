"""
FieldCRM Database Seed Script
Populates the Neon PostgreSQL database with realistic test data covering all roles,
loan stages, workflow events, notifications, and supporting records.

Run: python seed.py
"""

import asyncio
import hashlib
import secrets
import base64
import uuid
from datetime import datetime, timedelta, date

import asyncpg


# ── CONFIG ────────────────────────────────────────────────────────────────────

DATABASE_URL = (
    "postgresql://neondb_owner:npg_KMc4xg6LaXdq"
    "@ep-shiny-lake-aqk367dh-pooler.c-8.us-east-1.aws.neon.tech"
    "/neondb?sslmode=require"
)

PBKDF2_ITERATIONS = 260000
DEFAULT_PASSWORD = "FieldCRM@2025"


# ── HELPERS ───────────────────────────────────────────────────────────────────

def _hash_password(plain: str) -> str:
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", plain.encode(), salt, PBKDF2_ITERATIONS, dklen=32)
    return (
        f"pbkdf2_sha256${PBKDF2_ITERATIONS}$"
        f"{base64.b64encode(salt).decode()}$"
        f"{base64.b64encode(digest).decode()}"
    )


def uid() -> str:
    return str(uuid.uuid4())


def ago(days=0, hours=0, minutes=0) -> datetime:
    return datetime.utcnow() - timedelta(days=days, hours=hours, minutes=minutes)


def ref(n: int) -> str:
    return f"MMFB-{n:04d}"


# ── MAIN ─────────────────────────────────────────────────────────────────────

async def seed():
    print("Connecting to database…")
    conn = await asyncpg.connect(DATABASE_URL)
    print("Connected.\n")

    try:
        # ── 1. APPLY SCHEMA MIGRATION (drops & recreates all tables) ────────
        print("Applying schema migration…")
        import os
        migration_path = os.path.join(os.path.dirname(__file__), "migrations", "001_full_schema.sql")
        with open(migration_path, "r", encoding="utf-8") as f:
            migration_sql = f.read()
        await conn.execute(migration_sql)
        print("Schema ready.\n")

        # ── 2. ORGANISATION ──────────────────────────────────────────────────
        org_id = uid()
        await conn.execute(
            """INSERT INTO organisations (id, name, code, active)
               VALUES ($1, $2, $3, TRUE)""",
            org_id, "Mainstreet Microfinance Bank", "MMFB"
        )
        print(f"Created organisation: Mainstreet MFB ({org_id})")

        # ── 3. USERS (one per role) ───────────────────────────────────────────
        pw = _hash_password(DEFAULT_PASSWORD)

        u_lo1 = uid()   # loan officer 1
        u_lo2 = uid()   # loan officer 2
        u_co  = uid()   # credit officer
        u_bm  = uid()   # branch manager
        u_aud = uid()   # auditor
        u_adm = uid()   # system admin

        users = [
            (u_lo1, "Chidi Okafor",        "chidi@mmfb.com",    "loan_officer",    pw),
            (u_lo2, "Ngozi Eze",            "ngozi@mmfb.com",    "loan_officer",    pw),
            (u_co,  "Fatima Al-Hassan",     "fatima@mmfb.com",   "credit_officer",  pw),
            (u_bm,  "Samuel Adebayo",       "samuel@mmfb.com",   "branch_manager",  pw),
            (u_aud, "Amaka Nwosu",          "amaka@mmfb.com",    "auditor",         pw),
            (u_adm, "Emeka Obi",            "emeka@mmfb.com",    "system_admin",    pw),
        ]
        for uid_, name, email, role, pw_hash in users:
            await conn.execute(
                """INSERT INTO users (id, org_id, full_name, email, password_hash, role, active)
                   VALUES ($1,$2,$3,$4,$5,$6,TRUE)""",
                uid_, org_id, name, email, pw_hash, role
            )
        print(f"Created {len(users)} users (password: {DEFAULT_PASSWORD})")

        # ── 4. LOAN APPLICATIONS ─────────────────────────────────────────────
        # We create 12 applications spread across all stages

        apps = []

        def add_app(ref_no, name, stage, amount, loan_type="enterprise",
                    bvn=None, phone=None, tenor=12, created_by=u_lo1,
                    credit_officer=None, branch_manager=None, days_ago=5):
            app_id = uid()
            apps.append(dict(
                id=app_id, ref_no=ref_no, name=name, stage=stage,
                amount=amount, loan_type=loan_type,
                bvn=bvn or f"22{ref_no[-4:]}3456",
                phone=phone or f"080{ref_no[-4:]}1234",
                tenor=tenor, created_by=created_by,
                credit_officer=credit_officer, branch_manager=branch_manager,
                days_ago=days_ago,
            ))
            return app_id

        id_01 = add_app(ref(1),  "Adaeze Kalu",        "intake",                250_000,  "enterprise", days_ago=1,  created_by=u_lo1)
        id_02 = add_app(ref(2),  "Emeka Nwachukwu",    "intake",                180_000,  "msef",       days_ago=2,  created_by=u_lo2)
        id_03 = add_app(ref(3),  "Chidinma Okeke",     "ocr_review",            320_000,  "enterprise", days_ago=4,  created_by=u_lo1)
        id_04 = add_app(ref(4),  "Bola Afolabi",       "ocr_review",            500_000,  "payee",      days_ago=5,  created_by=u_lo2)
        id_05 = add_app(ref(5),  "Yetunde Bankole",    "credit_review",         750_000,  "enterprise", days_ago=7,  created_by=u_lo1, credit_officer=u_co)
        id_06 = add_app(ref(6),  "Ikechukwu Obi",      "credit_review",         420_000,  "msef",       days_ago=8,  created_by=u_lo2, credit_officer=u_co)
        id_07 = add_app(ref(7),  "Hauwa Musa",         "branch_approval",     1_200_000,  "enterprise", days_ago=10, created_by=u_lo1, credit_officer=u_co, branch_manager=u_bm)
        id_08 = add_app(ref(8),  "Sola Adeyemi",       "branch_approval",       600_000,  "payee",      days_ago=11, created_by=u_lo2, credit_officer=u_co, branch_manager=u_bm)
        id_09 = add_app(ref(9),  "Rasheedat Alli",     "disbursement_ready",    950_000,  "enterprise", days_ago=14, created_by=u_lo1, credit_officer=u_co, branch_manager=u_bm)
        id_10 = add_app(ref(10), "Nnamdi Egwu",        "disbursed",           1_500_000,  "enterprise", days_ago=20, created_by=u_lo1, credit_officer=u_co, branch_manager=u_bm)
        id_11 = add_app(ref(11), "Blessing Nwankwo",   "returned",              300_000,  "msef",       days_ago=12, created_by=u_lo2)
        id_12 = add_app(ref(12), "Taiwo Oladele",      "rejected",              850_000,  "enterprise", days_ago=15, created_by=u_lo1, credit_officer=u_co)

        for a in apps:
            await conn.execute(
                """INSERT INTO loan_applications (
                       id, org_id, ref_no, customer_type, loan_type, stage, applicant_name,
                       bvn, phone, amount, tenor_months, purpose, repayment_mode,
                       created_by, current_owner_id, credit_officer_id, branch_manager_id,
                       created_at, updated_at
                   ) VALUES (
                       $1,$2,$3,'new',$4,$5,$6,$7,$8,$9,$10,
                       'Business expansion and working capital','direct_debit',
                       $11,$11,$12,$13,$14,$14
                   )""",
                a["id"], org_id, a["ref_no"], a["loan_type"], a["stage"], a["name"],
                a["bvn"], a["phone"], a["amount"], a["tenor"],
                a["created_by"], a["credit_officer"], a["branch_manager"],
                ago(days=a["days_ago"])
            )
        print(f"Created {len(apps)} loan applications across all stages")

        # ── 5. GUARANTORS (for mid-to-late stage apps) ───────────────────────
        guarantor_apps = [id_05, id_06, id_07, id_08, id_09, id_10]
        guarantor_data = [
            ("Chukwuemeka Obi",    "Brother",  "0809111222", "22012345678", "Employed",     85_000, "Access Bank",  "0123456789"),
            ("Aminu Garba",        "Business", "0803222333", "22023456789", "Self-employed", 120_000, "GTBank",       "1234567890"),
            ("Ngozi Eze",          "Sister",   "0812333444", "22034567890", "Employed",     75_000, "Zenith Bank",  "2345678901"),
            ("Adebayo Olusegun",   "Colleague","0701444555", "22045678901", "Civil servant", 95_000, "First Bank",   "3456789012"),
            ("Fatimah Bello",      "Neighbor", "0802555666", "22056789012", "Trader",       60_000, "UBA",          "4567890123"),
            ("Emeka Igwe",         "Friend",   "0813666777", "22067890123", "Employed",     110_000,"Polaris Bank", "5678901234"),
        ]
        for i, (app_id, gdata) in enumerate(zip(guarantor_apps, guarantor_data)):
            g_name, relation, g_phone, g_bvn, g_emp, g_sal, g_bank, g_acct = gdata
            g_id = uid()
            await conn.execute(
                """INSERT INTO guarantors (
                       id, loan_id, org_id, slot, full_name, relationship_to_client,
                       bvn, phone, home_address, employment_type, monthly_salary,
                       max_guarantee_amount, bank_name, account_number, form_stage,
                       signature_detected, witness_signature_detected
                   ) VALUES ($1,$2,$3,1,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,'submitted',TRUE,TRUE)""",
                g_id, app_id, org_id, g_name, relation,
                g_bvn, g_phone, f"12 Main Street, Lagos",
                g_emp, g_sal, g_sal * 3, g_bank, g_acct
            )
        print(f"Created {len(guarantor_apps)} guarantor records")

        # ── 6. STAGE DATA (credit review scores) ─────────────────────────────
        credit_apps = [id_05, id_06, id_07, id_08, id_09, id_10]
        scores = [740, 690, 810, 670, 760, 800]
        dtis   = [0.32, 0.38, 0.28, 0.41, 0.30, 0.25]
        for app_id, score, dti in zip(credit_apps, scores, dtis):
            await conn.execute(
                """INSERT INTO stage_data (loan_id, stage, data_json, saved_by)
                   VALUES ($1, 'credit_review', $2::jsonb, $3)""",
                app_id,
                f'{{"credit_score":{score},"dti_ratio":{dti},"income_verified":true,'
                f'"bureau_source":"Bureau Pull — Lagos Node"}}',
                u_co
            )
        print(f"Created credit review stage_data for {len(credit_apps)} applications")

        # ── 7. WORKFLOW EVENTS ────────────────────────────────────────────────
        # Build a realistic event chain per application stage

        async def log_event(loan_id, event_type, from_stage, to_stage, triggered_by, role, notes=None, days=5, hours=0):
            await conn.execute(
                """INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage,
                       triggered_by, triggered_role, notes, created_at)
                   VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)""",
                loan_id, org_id, event_type, from_stage, to_stage,
                triggered_by, role, notes, ago(days=days, hours=hours)
            )

        # intake → ocr_review apps (3, 4)
        for a_id, d in [(id_03, 4), (id_04, 5)]:
            await log_event(a_id, "Application Created", None, "intake", u_lo1, "loan_officer", "New borrower registered and intake form submitted.", days=d, hours=4)
            await log_event(a_id, "Advance to OCR Review", "intake", "ocr_review", u_lo1, "loan_officer", "Documents uploaded and advanced to OCR queue.", days=d, hours=2)

        # credit_review apps (5, 6)
        for a_id, d, officer in [(id_05, 7, u_lo1), (id_06, 8, u_lo2)]:
            await log_event(a_id, "Application Created", None, "intake", officer, "loan_officer", "New borrower intake form submitted.", days=d, hours=6)
            await log_event(a_id, "Advance to OCR Review", "intake", "ocr_review", officer, "loan_officer", "OCR scan completed. Documents verified.", days=d, hours=4)
            await log_event(a_id, "OCR Verified", "ocr_review", "credit_review", u_co, "credit_officer", "NIN/BVN extracted successfully with 94% confidence.", days=d-1, hours=5)

        # branch_approval apps (7, 8)
        for a_id, d, officer in [(id_07, 10, u_lo1), (id_08, 11, u_lo2)]:
            await log_event(a_id, "Application Created", None, "intake", officer, "loan_officer", days=d, hours=8)
            await log_event(a_id, "Advance to OCR Review", "intake", "ocr_review", officer, "loan_officer", days=d, hours=6)
            await log_event(a_id, "OCR Verified", "ocr_review", "credit_review", u_co, "credit_officer", "Documents verified and passed to credit review.", days=d-1, hours=7)
            await log_event(a_id, "Credit Underwriting Verdict", "credit_review", "branch_approval", u_co, "credit_officer", "Recommend Approval — strong credit profile, DTI within limits.", days=d-2, hours=4)

        # disbursement_ready (9)
        await log_event(id_09, "Application Created", None, "intake", u_lo1, "loan_officer", days=14, hours=8)
        await log_event(id_09, "OCR Verified", "ocr_review", "credit_review", u_co, "credit_officer", days=13, hours=6)
        await log_event(id_09, "Credit Underwriting Verdict", "credit_review", "branch_approval", u_co, "credit_officer", "Recommend Approval — excellent bureau score 760.", days=12, hours=4)
        await log_event(id_09, "Branch Final Approval", "branch_approval", "disbursement_ready", u_bm, "branch_manager", "All gates passed. KYC and collateral attested.", days=11, hours=2)

        # disbursed (10)
        await log_event(id_10, "Application Created", None, "intake", u_lo1, "loan_officer", days=20, hours=8)
        await log_event(id_10, "OCR Verified", "ocr_review", "credit_review", u_co, "credit_officer", days=19, hours=6)
        await log_event(id_10, "Credit Underwriting Verdict", "credit_review", "branch_approval", u_co, "credit_officer", "Recommend Approval — highest credit score in batch (800).", days=18, hours=4)
        await log_event(id_10, "Branch Final Approval", "branch_approval", "disbursement_ready", u_bm, "branch_manager", "Approved. Collateral receipts signed.", days=17, hours=2)
        await conn.execute(
            """UPDATE loan_applications SET stage='disbursed', disbursed_at=$1 WHERE id=$2""",
            ago(days=16), id_10
        )

        # returned (11)
        await log_event(id_11, "Application Created", None, "intake", u_lo2, "loan_officer", days=12, hours=8)
        await log_event(id_11, "Return Application", "intake", "returned", u_bm, "branch_manager",
                        "Incomplete guarantor documentation. Re-submit with valid bank statements.", days=12, hours=2)
        await conn.execute(
            """UPDATE loan_applications SET return_reason=$1, returned_at=$2 WHERE id=$3""",
            "Incomplete guarantor documentation", ago(days=12, hours=2), id_11
        )

        # rejected (12)
        await log_event(id_12, "Application Created", None, "intake", u_lo1, "loan_officer", days=15, hours=8)
        await log_event(id_12, "OCR Verified", "ocr_review", "credit_review", u_co, "credit_officer", days=14, hours=6)
        await log_event(id_12, "Credit Underwriting Verdict", "credit_review", "rejected", u_co, "credit_officer",
                        "Recommend Rejection — DTI exceeds 40% limit. BVN flagged in bureau.", days=13, hours=4)

        print("Created workflow events for all applicable applications")

        # ── 8. VISITATION REPORTS ─────────────────────────────────────────────
        visitation_apps = [
            (id_05, "Submitted", False, None),
            (id_06, "Submitted", False, None),
            (id_07, "Concurred", True,  u_bm),
            (id_08, "Concurred", True,  u_bm),
            (id_09, "Concurred", True,  u_bm),
            (id_10, "Concurred", True,  u_bm),
        ]
        premises = [
            "Well-stocked provisions store, approximately 12sqm. Stock estimated at ₦180,000.",
            "Active tailoring shop with 3 sewing machines. Clean premises, 2 apprentices.",
            "Established food vendor with daily turnover of ~₦35,000. Strong customer base.",
            "ICT accessories shop, good inventory. Location near main market entrance.",
            "Hair salon with 4 styling stations. Active clientele observed during visit.",
            "Electronics repair workshop, tools present and in use. Moderate activity.",
        ]
        directions = [
            "200m from Oshodi bus stop, left at First Bank ATM, blue building.",
            "Off Apapa Road junction, opposite Unity School. Yellow gate.",
            "Beside Shoprite on Allen Avenue, 3rd floor, room 301.",
            "Along Lagos-Ibadan expressway, Berger bus stop, right side.",
            "Behind Balogun market, second alley left, sign visible from road.",
            "Agege Motor Road, near Orile junction. Facing the petrol station.",
        ]
        for i, (a_id, status, mgr_concur, mgr_id) in enumerate(visitation_apps):
            officer = u_lo1 if i % 2 == 0 else u_lo2
            mgr_concurred_at = ago(days=10 - i) if mgr_concur else None
            await conn.execute(
                """INSERT INTO visitation_reports (
                       loan_id, org_id, visit_date, met_with, premises_description,
                       direction_from_branch, business_condition, visiting_officer_id,
                       visiting_officer_signature, account_officer_id, manager_concurrence,
                       manager_id, manager_notes, manager_concurred_at, status
                   ) VALUES ($1,$2,$3,$4,$5,$6,'Good',$7,TRUE,$7,$8,$9,$10,$11,$12)""",
                a_id, org_id, date.today() - timedelta(days=8 - i),
                "Business owner — present and cooperative.",
                premises[i], directions[i],
                officer, mgr_concur, mgr_id,
                "Site visit confirmed. Business is active and viable." if mgr_concur else None,
                mgr_concurred_at,
                status.lower()
            )
        print(f"Created {len(visitation_apps)} visitation reports")

        # ── 9. NOTIFICATIONS ─────────────────────────────────────────────────
        notifications = [
            (u_lo1, id_07, "Application Approved", "Your loan application MMFB-0007 (Hauwa Musa — ₦1.2M) has been approved and is ready for disbursement.", "approval", False),
            (u_lo1, id_05, "Credit Review Complete", "Credit officer verdict for MMFB-0005: Recommend Approval. DTI: 32%, Score: 740.", "credit_review", False),
            (u_lo2, id_08, "Application Approved", "Your loan application MMFB-0008 (Sola Adeyemi — ₦600K) has been approved.", "approval", True),
            (u_lo2, id_11, "Application Returned", "Your loan application MMFB-0011 was returned: Incomplete guarantor documentation.", "returned", False),
            (u_lo1, id_09, "Credit Review Complete", "Credit officer verdict for MMFB-0009: Recommend Approval. DTI: 30%, Score: 760.", "credit_review", True),
            (u_bm,  id_07, "Branch Action Required", "MMFB-0007 (Hauwa Musa) is awaiting your final approval. All gates passed.", "approval", False),
            (u_co,  id_05, "Credit Review Assigned", "MMFB-0005 (Yetunde Bankole — ₦750K) has been assigned to your credit review queue.", "credit_review", True),
            (u_adm, id_10, "Disbursement Completed", "MMFB-0010 (Nnamdi Egwu — ₦1.5M) has been fully disbursed.", "disbursement", True),
        ]
        for n_uid, a_id, title, message, n_type, is_read in notifications:
            await conn.execute(
                """INSERT INTO notifications (user_id, org_id, application_id, title, message, type, is_read)
                   VALUES ($1,$2,$3,$4,$5,$6,$7)""",
                n_uid, org_id, a_id, title, message, n_type, is_read
            )
        print(f"Created {len(notifications)} notifications")

        # ── SUMMARY ───────────────────────────────────────────────────────────
        sep = "=" * 60
        print(f"\n{sep}")
        print("  DATABASE SEEDED SUCCESSFULLY")
        print(sep)
        print(f"\n  Organisation:  Mainstreet Microfinance Bank (code: MMFB)")
        print(f"  Password:      {DEFAULT_PASSWORD}  (all users)\n")
        print("  LOGIN CREDENTIALS")
        print("  " + "-" * 50)
        print("  chidi@mmfb.com    -> Loan Officer (LO-1)")
        print("  ngozi@mmfb.com    -> Loan Officer (LO-2)")
        print("  fatima@mmfb.com   -> Credit Officer")
        print("  samuel@mmfb.com   -> Branch Manager")
        print("  amaka@mmfb.com    -> Auditor")
        print("  emeka@mmfb.com    -> System Admin / MCR")
        print("\n  Applications by stage:")
        stages = {}
        for a in apps:
            stages.setdefault(a["stage"], []).append(a["ref_no"])
        for stage, refs in sorted(stages.items()):
            print(f"    {stage:<22} {', '.join(refs)}")
        print()

    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(seed())
