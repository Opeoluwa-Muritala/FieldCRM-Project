# Android parity audit — Credit Analyst

| Role | Destination | Web reference | Android reference | Web capability | Android capability | Gap | Evidence | Severity |
|---|---|---|---|---|---|---|---|---|
| Credit Analyst | Dashboard | `credit_analyst/dashboard.html`; `GET /dashboard` | `roles/creditanalyst/dashboard/CreditAnalystDashboard.kt`; `DashboardViewModel` | Awaiting-analysis file cards plus data-exception cards showing field, document and confidence. | Reviews-due, OCR-exception and reviewed-today metrics with queue shortcuts. | Actionable dossier and exception previews are absent. | **Confirmed** | Medium |
| Credit Analyst | Credit Assessment Queue | `credit_analyst/review_queue.html`; `GET /my-reviews` | `CreditAssessmentQueue` → `CreditReviewQueueScreen.kt` | Applicant, reference, product, amount and OCR issue count; opens review directly. | Dashboard review items rendered as credit-review cards, currently opening shared application detail first. | Direct review entry and complete issue-count context are reduced. | **Confirmed** | Medium |
| Credit Analyst | Document Data Exceptions | `credit_analyst/ocr_exceptions.html`; `GET /ocr-exceptions` | `CreditOcrExceptionQueue` → `OcrExceptionsScreen.kt`; navigation callback in `FieldCRMApp.kt` | Field/document/confidence list opens `/applications/{id}/ocr-review` for correction. | Lists exceptions, but selection navigates to `Screen.ApplicationDetail`. | The destination does not open the correction action, so an assigned exception cannot be directly resolved from this queue. | **Confirmed** | High |
| Credit Analyst | Credit Risk Review | `shared/credit_review.html`; credit review, bureau pull and checklist routes | `CreditAssessmentReview.kt` → `CreditOfficerReviewScreen.kt`; `ApplicationViewModel` | Summary, affordability comparison table/variance notes, document verification, OCR overrides, bureau pull/report, checklist and recommendation with amount/notes. | Bureau data load, DTI, OCR rows and recommendation decision/notes submission. | Missing web tabbed evidence breadth, declared-vs-bank affordability table, document metadata/statuses, reviewer override controls, recommended amount and full checklist editing. This can prevent an equivalent underwriting record from being completed. | **Confirmed** | High |
| Credit Analyst | Application Dossier | role-authorized `/applications/{id}` detail | `ApplicationDetailScreen.kt`; detail and audit APIs | Readable identity, intake, guarantors, collateral, documents, workflow and prior decisions. | Shared dossier with the same broad categories. | Credit assessment/readiness is not a dedicated dossier slice and some fields are shown through generic “not available” composition. | **Confirmed** | Medium |
| Credit Analyst | Search | `shared/search_results.html`; `GET /search` | `roles/creditanalyst/search/CreditAnalystSearch.kt`; `SearchViewModel` | Credit-owned dossiers and exception discovery within authorized scope. | Shared application-shaped results with a credit-specific title. | OCR exceptions are not a typed result category and credit filters/sorting are absent. | **Confirmed** | Medium |
| Credit Analyst | Settings | `shared/settings.html`; settings routes | Existing `SettingsScreen.kt`, excluded by workspace policy | Account details and password change. | Not reachable. | **Missing as a reachable destination.** Foundation touch: route policy. | **Confirmed** | High |

## Missing-entirely destinations

- **High — Confirmed:** reachable Settings/password page.

## Summary

- High: **3**
- Medium: **4**
- Low: **0**

The most consequential gap is the reduced Credit Risk Review, which cannot capture the same affordability, document, OCR override, checklist and recommended-amount evidence as the web workflow.
