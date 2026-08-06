# FieldCRM Audit Report

Generated: 2026-08-05T12:28:49+01:00
Repository: Opeoluwa-Muritala/FieldCRM-Project

## Executive Summary (Top combined risks)

1. Fatal syntax error in mobile user-creation endpoint (blocks server start).
2. MCC finalize endpoint allows crm/head_crm to set final loan amount (authorization change).
3. Uploaded documents may be stored under public static path when cloud fallback is used (PII exposure).
4. Local SQLite DB file present in working tree (risk of accidental commit/exposure).
5. Duplicate GET /queues/legal route (ambiguous behavior).
6. Idempotency race on mobile application creation (unique-constraint not handled).

---

## Code Review — Human findings (short)

- F001: Fatal syntax error in backend/app/api/v1/mobile.py (placeholder `******`) — Critical. Prevents import/start.
- F002: Duplicate GET /queues/legal defined twice — High.
- F003: Idempotency/race risk for create_mobile_application (unique-constraint not handled) — Medium.
- F004: Inconsistent API shape in get_mobile_application (returns raw Pydantic model) — Medium.
- F005: Silent exception swallowing around notifications (no logging) — Low→Medium.

### Code Review — Machine-friendly JSON findings

```json
[
  {
    "id": "F001",
    "title": "Fatal syntax error in mobile user-creation endpoint (placeholder '******')",
    "severity": "Critical",
    "confidence": "High",
    "files": [
      {
        "path": "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py",
        "lines": [3077, 3109],
        "snippet": "@router.post(\"/users\", status_code=status.HTTP_201_CREATED)\\nasync def create_mobile_user(...):\\n    ...\\n    user = await svc.register_user(\\n        current_admin=current_user,\\n        user_in=UserCreate(\\n            org_id=str(current_user.org_id),\\n            full_name=payload.full_name,\\n            email=payload.email,\\n            role=payload.role.lower().replace(\" \", \"_\"),\\n            ******\\n        ),\\n    )"
      }
    ],
    "description": "The literal '******' is present in the UserCreate call, which is invalid Python and causes a SyntaxError when importing the module. Because mobile.py is imported at application startup, this prevents the backend from starting and breaks tests.",
    "suggested_fix": "Replace the placeholder with the password field from the request. For example:\n\nuser = await svc.register_user(\n    current_admin=current_user,\n    user_in=UserCreate(\n        org_id=str(current_user.org_id),\n        full_name=payload.full_name,\n        email=payload.email,\n        role=payload.role.lower().replace(\" \", \"_\"),\n        ******    ),\n)\n",
    "suggested_tests": [
      "Unit/import test that imports app.api.v1.mobile to detect syntax/parse errors.",
      "Integration test: POST /api/v1/mobile/users with valid payload; assert HTTP 201 and returned id/role."
    ],
    "classification": "bug"
  },
  {
    "id": "F002",
    "title": "Duplicate GET /queues/legal route defined twice",
    "severity": "High",
    "confidence": "High",
    "files": [
      {
        "path": "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py",
        "lines": [532, 557],
        "snippet": "@router.get(\"/queues/legal\")\\nasync def get_mobile_legal_queue_exact(...):\\n    SELECT id, ref_no, applicant_name, amount, stage, updated_at, COUNT(*) OVER() AS total_count\\n    FROM loan_applications ..."
      },
      {
        "path": "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py",
        "lines": [2676, 2704],
        "snippet": "@router.get(\"/queues/legal\")\\nasync def get_mobile_legal_queue(...):\\n    SELECT la.id, la.ref_no, la.applicant_name, la.amount, la.stage, la.updated_at,\\n           u.full_name AS officer_name, bm.full_name AS branch_manager_name,\\n           COUNT(*) OVER() AS total_count\\n    FROM loan_applications la ..."
      }
    ],
    "description": "Two separate functions are registered for the same HTTP method and path. This can lead to ambiguous routing and inconsistent responses; maintainability and client contract are affected.",
    "suggested_fix": "Consolidate into one handler that returns a single stable shape (include officer_name and branch_manager_name if needed) and remove the duplicate function. Example consolidated handler included in the human-readable section.",
    "suggested_tests": [
      "Integration test GET /api/v1/mobile/queues/legal asserting response shape and stable content keys (page/size/total/items)."
    ],
    "classification": "bug"
  },
  {
    "id": "F003",
    "title": "Idempotency race / unique-constraint not handled on mobile application creation",
    "severity": "Medium",
    "confidence": "Medium-High",
    "files": [
      {
        "path": "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py",
        "lines": [808, 891],
        "snippet": "if payload.client_request_id:\\n    existing = await conn.fetchrow(...)\\n    if existing: return ...\\n# later: app = await _loan_service(conn).create_loan(...)\\n# no handling of unique-constraint on simultaneous inserts"
      },
      {
        "path": "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\migrations\\025_mobile_creation_idempotency.sql",
        "lines": [1, 8],
        "snippet": "ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS client_request_id UUID;\\nCREATE UNIQUE INDEX IF NOT EXISTS uq_loan_applications_org_client_request ON loan_applications (org_id, client_request_id) WHERE client_request_id IS NOT NULL AND deleted_at IS NULL;"
      }
    ],
    "description": "A unique index exists for client_request_id, but the create flow does not catch unique-violation errors when two identical requests race. This can produce a 500 from the DB rather than an idempotent response.",
    "suggested_fix": "Wrap the create_loan call in try/catch for UniqueViolation (asyncpg.exceptions.UniqueViolationError) and then SELECT the existing loan by client_request_id and return it. Example code provided in human-readable section.",
    "suggested_tests": [
      "Concurrency test that issues two simultaneous create requests with same client_request_id and asserts both responses resolve to the same loan id and no 500 occurs."
    ],
    "classification": "bug"
  },
  {
    "id": "F004",
    "title": "Inconsistent application response shape in GET /applications/{id}",
    "severity": "Medium",
    "confidence": "High",
    "files": [
      {
        "path": "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py",
        "lines": [894, 915],
        "snippet": "return { \"application\": app, \"intake\": await loan_service.get_wizard_data(application_id), ... }"
      }
    ],
    "description": "This endpoint returns the raw Pydantic LoanRow for 'application', while other endpoints return a mobile-optimized dict (_mobile_application). This inconsistency risks client breakage.",
    "suggested_fix": "Return _mobile_application(app, current_user) instead of the raw app. Example shown in the human-readable section.",
    "suggested_tests": [
      "Integration test that GET /api/v1/mobile/applications/{id} returns application fields: id, org_id, borrower_id, current_stage, amount, tenure, product_type, status."
    ],
    "classification": "API contract bug"
  },
  {
    "id": "F005",
    "title": "Silent exception swallowing in notification blocks (no logging)",
    "severity": "Low-Medium",
    "confidence": "High",
    "files": [
      {
        "path": "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py",
        "lines": [1198, 1211],
        "snippet": "try:\\n    ...\\n    await _notification_service(conn).create(...)\\nexcept Exception:\\n    pass"
      }
    ],
    "description": "Notification creation failures are caught and silently ignored (no logging). This reduces observability of failed notifications and makes operational debugging difficult.",
    "suggested_fix": "Log exceptions at warning/error level instead of silent pass. Example:\n\nexcept Exception as exc:\n    logging.getLogger(__name__).exception(\"Notification creation failed for application %s: %s\", application_id, exc)\n",
    "suggested_tests": [
      "Unit test that simulates the notification service throwing and asserts the main response still succeeds and that a log entry was emitted."
    ],
    "classification": "maintainability"
  }
]
```

---

## Security Review — Human findings (short)

- mcc_finalize_role_change: MCC finalize endpoint allows crm/head_crm to set final amount — HIGH.
- uploads_public_static_fallback: Local fallback stores uploads under frontend/static → public exposure — HIGH.
- local_sqlite_db_present: fieldcrm.db exists in working tree — HIGH.
- dependencies_not_pinned: requirements.txt uses open ranges (>=) — MEDIUM.
- role_normalization_inconsistent: ad-hoc role checks vs RoleChecker — MEDIUM.

### Security Review — Machine-friendly JSON findings

```json
[
  {
    "id": "mcc_finalize_role_change",
    "title": "MCC finalize endpoint allows crm/head_crm to set final amount (authorization change)",
    "severity": "HIGH",
    "confidence": "9/10",
    "files": ["C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py"],
    "lines": "2826-2838",
    "snippet": "@router.post(\"/applications/{application_id}/mcc-finalize\")\\nasync def finalize_mobile_mcc(...):\\n    _ensure_roles(current_user, {\"crm\", \"head_crm\"})\\n    ...\\n    row = await conn.fetchrow(... UPDATE loan_applications ...)",
    "description": "The mobile API now authorizes crm/head_crm for finalizing MCC and writing the final loan amount to the database. This appears to be a privilege-escalating change from prior executive-only behavior (ed/md).",
    "evidence": "Code lines 2826-2838 show _ensure_roles(current_user, {\"crm\",\"head_crm\"}) followed by UPDATE loan_applications that sets amount and mcc_finalized_by.",
    "remediation": "Restrict finalize to the correct executive roles (e.g., {\"ed\",\"md\"}) or implement stronger business checks (committee quorum / two-person approval). Use centralized RoleChecker for consistent policy. Add auditing and alerting for finalize actions.",
    "tests": ["Unit test: crm user cannot finalize (403)", "Unit test: ed user can finalize (200) and DB row updated", "Integration test: verify audit log entry created when finalize succeeds"],
    "references": ["OWASP A01:2021 Broken Access Control", "Internal policy: least privilege"]
  },
  {
    "id": "uploads_public_static_fallback",
    "title": "Local upload fallback writes user documents to public static directory (/static/uploads) and exposes PII",
    "severity": "HIGH",
    "confidence": "9/10",
    "files": [
      "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\domains\\documents\\service.py",
      "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\main.py"
    ],
    "lines": "service.py:145-156, main.py:142-153",
    "snippet": "service.py: (target_dir / stored_name).write_bytes(content)\\nstored_path = \"/static/uploads/\" + (relative_dir / stored_name).as_posix()\\nmain.py: app.mount(\"/static\", StaticFiles(directory=static_dir), name=\"static\")",
    "description": "If Cloudinary is not used, uploaded documents are written under frontend/static/uploads and become directly accessible via /static/uploads/<...>. Uploaded documents may contain sensitive PII and should not be publicly addressable.",
    "evidence": "DocumentService writes files to settings.DOCUMENT_UPLOAD_DIR and returns a /static/uploads URL; app mounts the frontend/static dir on /static.",
    "remediation": "Store uploads outside static, use private object storage in production. Serve files only through authenticated endpoints that authorize org/user access. Enforce cloud storage in production and fail fast if not configured.",
    "tests": ["Integration test: verify file uploaded in fallback is NOT reachable via /static/uploads/ (403/404)", "Auth test: only authorized org user can download document via /api/v1/documents/{id}/download"],
    "references": ["OWASP A03:2021 Sensitive Data Exposure"]
  },
  {
    "id": "local_sqlite_db_present",
    "title": "Local SQLite DB file present in repository working tree",
    "severity": "HIGH",
    "confidence": "9/10",
    "files": ["C:\\Users\\LENOVO\\Desktop\\FieldCRM\\fieldcrm.db"],
    "lines": null,
    "snippet": "File exists in repository root: fieldcrm.db",
    "description": "A local SQLite database file exists in the repository workspace. If this file is or gets committed, it may include PII/sensitive data. Even if not committed yet, having it in the project root increases risk of accidental commit and exposure.",
    "evidence": "Workspace file present at the project root (observed in working directory snapshot).",
    "remediation": "Remove it from the repo (git rm --cached fieldcrm.db if tracked), add to .gitignore, and store local DBs outside the repo. If it has been committed in the past, scrub history and rotate any secrets/credentials found.",
    "tests": ["Pre-commit hook to block *.db and large binary files from being staged", "CI repository scan for committed DBs and secrets"],
    "references": ["Git security best practices"]
  },
  {
    "id": "dependencies_not_pinned",
    "title": "Dependencies use open ranges (>=) instead of pinned immutable versions / lockfile",
    "severity": "MEDIUM",
    "confidence": "9/10",
    "files": ["C:\\Users\\LENOVO\\Desktop\\FieldCRM\\requirements.txt"],
    "lines": "1-25",
    "snippet": "fastapi>=0.110.0\\nuvicorn>=0.28.0\\nsqlalchemy>=2.0.0\\n...\\ncloudinary>=1.40.0",
    "description": "Requirements use broad >= ranges rather than pinned versions or a lockfile. This increases supply-chain risk (dependency change can pull in malicious or vulnerable versions).",
    "evidence": "requirements.txt contains only 'package>=version' entries; no lockfile.",
    "remediation": "Adopt a lockfile (pip-tools / pip-compile, poetry.lock) with exact versions and optionally hashes. Add automated dependency scanning to CI.",
    "tests": ["CI job to run pip-audit and fail on critical CVEs", "Dependabot/automated PRs enabled"],
    "references": ["OWASP Software Supply Chain Guidelines"]
  },
  {
    "id": "role_normalization_inconsistent",
    "title": "Inconsistent role normalization between ad-hoc checks and RoleChecker",
    "severity": "MEDIUM",
    "confidence": "8/10",
    "files": [
      "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\api\\v1\\mobile.py",
      "C:\\Users\\LENOVO\\Desktop\\FieldCRM\\backend\\app\\core\\dependencies.py"
    ],
    "lines": "mobile.py:435-438, dependencies.py:71-86",
    "snippet": "mobile._ensure_roles uses _role(current_user) not alias-aware; RoleChecker maps 'loan_officer'->'account_officer'.",
    "evidence": "RoleChecker normalizes allowed_roles using role_aliases while _ensure_roles directly checks _role(current_user) in allowed_roles.",
    "remediation": "Consolidate role checks: prefer RoleChecker usage everywhere or make _ensure_roles normalize aliases identically.",
    "tests": ["Unit test: ensure 'account_officer' and 'loan_officer' are treated equivalently in all authorization checks"]
  }
]
```

---

## Next steps (recommended)

1. Fix F001 (syntax error) immediately so the app and tests run.
2. Patch MCC finalize authorization and add unit tests (high security priority).
3. Move uploads outside static and secure document access.
4. Remove local DB from working tree and add .gitignore + pre-commit hook.
5. Consolidate duplicate route and add idempotency handling.

If you'd like, I can prepare a PR patch for any single item above and run local tests. Which one should be prepared first?

---

End of report.
