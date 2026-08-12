# Verification checklist

Use test/local credentials and non-production resources. Do not print `.env` values.

## Safe local commands

```powershell
py -3.12 --version
py -3.12 -m pip install -r requirements.txt
Set-Location backend
py -3.12 -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

In a second terminal:

```powershell
Invoke-WebRequest http://127.0.0.1:8000/api/v1/health
Invoke-WebRequest http://127.0.0.1:8000/login
Invoke-WebRequest http://127.0.0.1:8000/static/css/dashboard.css
py -3.12 test_imports.py
py -3.12 backend/test_http.py
py -3.12 backend/test_routes_render.py
py -3.12 backend/test_responsive_smoke.py
py -3.12 -m pytest backend/tests -q
```

No configured mypy, Ruff, Black, or Flake8 command exists (**Confirmed**).

## Manual checks

- [ ] Configure all required variable names from `deployment-map.md`; confirm values in Vercel without copying them into reports.
- [ ] Confirm production `DATABASE_URL` is pooled and reachable; verify transactions and tenant scoping using test data.
- [ ] Confirm Redis TLS/connectivity, distributed login/reset limits, and cache invalidation.
- [ ] Login: cookie is HttpOnly, Secure in HTTPS, SameSite Lax, expires as expected; logout deletes both supported cookie names.
- [ ] Exercise each role and confirm 401/403 behavior and organization ownership isolation.
- [ ] Complete staff and client application wizard steps and verify redirects and validation.
- [ ] Confirm the two documented missing guarantor routes return 404 until fixed.
- [ ] Upload PDF/JPEG/PNG within and beyond limits; reject signature/extension mismatch.
- [ ] Preview every page of a multi-page PDF; verify unauthorized users cannot access another organization's document.
- [ ] Verify Cloudinary authenticated upload/download and ensure no production local-file fallback.
- [ ] Verify OCR completion after the upload response, specifically across serverless instance termination.
- [ ] Verify QoreID, AML, CreditRegistry/CRC, and email using provider sandbox dashboards.
- [ ] Verify CORS against actual production and preview origins; inspect CSP/security/request-ID headers.
- [ ] In Vercel preview, confirm templates and all `/static` assets are bundled, cold start succeeds, function size/duration is acceptable, and no API route is intercepted.
- [ ] Run production smoke checks only with authorized test records; do not run migrations or mutate production customer data.

## Mermaid validation

- [x] One `.mmd` file and one `flowchart LR`.
- [x] Five required top-level subgraphs.
- [x] Unique node IDs and closed subgraphs.
- [x] Frontend HTTP actions connect to backend route groups.
- [x] Responses and errors return to browser handlers.
- [x] Deployment nodes connect to the same FastAPI/static nodes.
- [x] Unverified nodes use dashed styling.
- [x] No secret values included.

No Mermaid CLI or repository Mermaid validator exists; syntax is manually validated.
