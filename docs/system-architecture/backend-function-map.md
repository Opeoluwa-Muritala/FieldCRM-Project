# Backend function map

All entries are **Confirmed**.

| Function or class | File | Runtime | Called by | Calls | Inputs | Outputs | Side effects |
|---|---|---|---|---|---|---|---|
| `app`, `lifespan` | `backend/app/main.py` | startup/request/shutdown | Vercel ASGI | engine/cache/rate-limit init; routers | environment/config | ASGI app | opens/disposes resources |
| `get_current_user` | `core/dependencies.py` | request dependency | protected routes | JWT decode, cache, user repo | bearer/cookie | `UserRow` | caches user; 401 |
| `RoleChecker` | `core/dependencies.py` | request dependency | role routes | `get_current_user` | allowed roles | `UserRow` | 403 on denial |
| JWT/password helpers | `core/security.py` | request/service | auth/reset | jose, PBKDF2/bcrypt | credentials/claims | token/boolean/hash | none |
| `AuthService.authenticate_user` | `domains/auth/service.py` | request | API/web login | auth repo, verify password | email/password | user or none | DB read |
| `UserService` invitation/user methods | `domains/users/service.py` | request | users router/admin UI | repository, email, token/hash | user/org data | user/result dict | DB writes/email |
| `LoanService` | `domains/loans/service.py` | request | application routes | `LoanRepository` | org/user/application data | rows/domain results | DB transactions |
| web loan route handlers | `domains/loans/router.py` | request | browser | services/repos/integrations/templates | form/query/path/dependencies | HTML/JSON/303 | workflow, audit, notification writes |
| mobile route handlers | `api/v1/mobile.py` | request | Android/API caller | DB/services/Pydantic | JSON/form/files/Bearer | JSON | CRUD/workflow writes |
| `DocumentService.save_upload` | `domains/documents/service.py` | request + detached task | document routes | validation, Cloudinary, repository, audit, OCR | `UploadFile` + identity | document dict | object/file + DB writes |
| `upload_document` / signed URLs | `services/cloud_storage_service.py` | request | document service/routes | Cloudinary SDK | bytes/public ID | upload result/URL | external object operations |
| `OcrExtractionService.process_document` | `services/ocr_extraction_service.py` | detached async work | document service | pdf/image tools, OCR repo | stored document | OCR result | CPU/file read + DB writes |
| `verify_bvn` | `domains/verification/service.py` | request | wizard/workflow | QoreID, SQL | BVN/application | normalized dict | external GET + verification/audit rows |
| `screen_entity` | `domains/aml/service.py` | request | workflow | Youverify, SQL | name/application | normalized dict | external POST + sanction/audit rows |
| credit providers/service | `domains/credit_bureau/service.py` | request | bureau route/workflow | CreditRegistry/CRC | identity/account | normalized report | external POST + DB writes |
| `EmailService` | `services/email_service.py` | request | invitations/notifications | JSON `urlopen` | recipient/message | boolean | external POST |
| PDF functions | `services/pdf_service.py` | request | offer generation | WeasyPrint/zip | application/clauses | bytes | CPU/memory |
| repository classes | `domains/*/repository.py` | request | services/routes | connection adapter + SQL | IDs/data | rows/scalars | SQL reads/writes |
| `SQLAlchemyConnection` / SQLite wrapper | `core/database.py` | request | repositories/routes | async SQLAlchemy/aiosqlite | SQL + args | rows/results | pooled DB I/O/transactions |
| middleware classes | `core/middleware.py`, `core/cache.py` | request | ASGI app | headers/cache invalidation | scope/messages | HTTP response | request ID/security/cache headers |
| domain/browser exception handlers | `core/exceptions.py`, `main.py` | request error | FastAPI | JSON/template/redirect | exception/request | 4xx/5xx response | none |

Important direct-SQL route logic remains in `domains/loans/router.py` and `api/v1/mobile.py`; the repository layer is not universal. Pydantic request models are strongest in the JSON APIs. Server-rendered routes commonly use FastAPI `Form`, `Query`, `Path`, `UploadFile`, and manual domain validation instead.
