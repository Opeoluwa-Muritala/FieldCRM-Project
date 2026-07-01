# Android API Reference

The Android app should use bearer-token authentication and JSON endpoints.
The existing web UI routes remain unchanged.

## Authentication

Login:

```http
POST /api/v1/auth/login-bearer
Content-Type: application/x-www-form-urlencoded

username={email}&password={password}
```

Response:

```json
{
  "access_token": "jwt-token",
  "token_type": "bearer"
}
```

Use the token on mobile API requests:

```http
Authorization: Bearer jwt-token
```

## Base Path

All Android workflow routes are under:

```text
/api/v1/mobile
```

## User and Dashboard

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/mobile/me` | Current authenticated user |
| `GET` | `/api/v1/mobile/dashboard` | Role-specific dashboard data |
| `GET` | `/api/v1/mobile/queues/{queue_name}` | Role queue data |
| `GET` | `/api/v1/mobile/notifications` | List current user's notifications, newest first |
| `PATCH` | `/api/v1/mobile/notifications/{id}/read` | Mark one notification as read |
| `DELETE` | `/api/v1/mobile/notifications` | Clear current user's notifications |

Notification response:

```json
[
  {
    "id": "notif_abc123",
    "title": "Application Returned",
    "message": "MMFB-041 was returned by Branch Manager for correction",
    "created_at": "2026-06-30T08:23:00Z",
    "is_read": false,
    "application_id": "app_xyz789",
    "type": "application_returned"
  }
]
```

Mark-read response:

```json
{ "ok": true }
```

Queue names:

- `loan-officer`
- `visits-due`
- `awaiting-concurrence`
- `pending-signoffs`
- `credit-reviews`
- `ocr-exceptions`
- `compliance-flags`
- `system-control`

## Applications

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/mobile/applications` | List applications |
| `POST` | `/api/v1/mobile/applications` | Create loan draft |
| `GET` | `/api/v1/mobile/applications/{application_id}` | Full application detail bundle |
| `GET` | `/api/v1/mobile/applications/{application_id}/intake` | Intake wizard data |
| `PUT` | `/api/v1/mobile/applications/{application_id}/intake/steps/{step}` | Save intake step 1-9 |

Create payload:

```json
{
  "customer_type": "new",
  "loan_type": "enterprise",
  "applicant_name": "Grace Omowunmi"
}
```

Save intake step payload:

```json
{
  "data": {
    "full_name": "Grace Omowunmi",
    "phone": "08012345678",
    "bvn": "22233344455"
  }
}
```

Saving intake step 9 advances the application to `ocr_review`.

## Guarantors

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/mobile/applications/{application_id}/guarantors/{slot}` | Get guarantor slot data |
| `PUT` | `/api/v1/mobile/applications/{application_id}/guarantors/{slot}/steps/{step}` | Save guarantor step 1-8 |

`slot` must be `1` or `2`. Saving guarantor step 8 marks the slot submitted.

Payload:

```json
{
  "data": {
    "name": "Tunde Bakare",
    "relationship": "Brother",
    "phone": "08022223333"
  }
}
```

## Documents

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/mobile/applications/{application_id}/documents` | List documents |
| `POST` | `/api/v1/mobile/applications/{application_id}/documents` | Upload and store a physical document |

Multipart payload:

```http
POST /api/v1/mobile/applications/{application_id}/documents
Content-Type: multipart/form-data

file=@loan-form.pdf
doc_type=loan_application_form
form_code=MMFB/CRM/01
```

Rules:

- `file` is required.
- `doc_type` is required and defaults to `other` only for compatibility.
- `form_code` is optional; known form categories are mapped by the backend when omitted.
- Accepted MIME types are `application/pdf`, `image/jpeg`, and `image/png`.
- Maximum file size is 10 MB.
- The response returns the created document metadata row.

## OCR Review

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/mobile/applications/{application_id}/ocr-review` | Save OCR corrections or verify OCR |

Payload:

```json
{
  "action": "verify",
  "corrections": {
    "amount": "500000",
    "bvn": "22233344455"
  }
}
```

`verify` advances the application to `credit_review`.

## Visitation

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/mobile/applications/{application_id}/visitation` | Get visitation report |
| `PUT` | `/api/v1/mobile/applications/{application_id}/visitation` | Submit officer visitation report |
| `POST` | `/api/v1/mobile/applications/{application_id}/visitation/signoff` | Branch manager signoff |

Officer payload:

```json
{
  "met_with": "Customer",
  "premises_description": "Blue kiosk beside market entrance",
  "direction_from_branch": "From branch, turn left at junction..."
}
```

Manager signoff payload:

```json
{
  "decision": "concurred",
  "notes": "Premises confirmed"
}
```

## Credit Review, Approval, and Return

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/mobile/applications/{application_id}/credit-review` | Submit credit verdict |
| `GET` | `/api/v1/mobile/applications/{application_id}/approval-readiness` | Get readiness checklist |
| `POST` | `/api/v1/mobile/applications/{application_id}/approve` | Approve for disbursement |
| `POST` | `/api/v1/mobile/applications/{application_id}/return` | Return application for correction |

Credit review payload:

```json
{
  "recommendation_decision": "Recommend Approval",
  "recommendation_notes": "Applicant cashflow and guarantors are acceptable."
}
```

Return payload:

```json
{
  "reason_category": "Missing Documents",
  "corrections": ["Bank Statement", "Guarantor Cheque"],
  "notes": "Upload a clear recent bank statement and guarantor cheque."
}
```
