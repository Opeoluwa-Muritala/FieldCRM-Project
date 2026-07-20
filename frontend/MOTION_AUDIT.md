# FieldCRM Motion Audit

Motion follows `DESIGN.md`'s Institutional Modernist direction: it is short,
state-led, and disabled for reduced-motion users.  The pre-existing dashboard
CSS still contains legacy purple/dark-theme tokens; this audit applies no
colour or spacing changes and treats `DESIGN.md` as the visual source of truth.

| Screen group | Before | Now / purpose |
| --- | --- | --- |
| Shared/auth (login, reset, notifications, settings, search, audit, pipeline, repayment, return/approve, loan view, applications, borrowers) | Static content and instant feedback. | Main content enters once; tables/lists sequence briefly; status changes, empty states, form validation and toasts communicate state changes. |
| Account officer (dashboard, queue, detail, wizard, uploads, OCR, visits) | Static cards, queues, forms and upload progress. | KPI/queue cards stagger on first paint; wizard content uses the shared entry transition; real upload bar widths ease with actual XHR progress; OCR indicators pulse quietly. |
| Branch manager/supervisor and credit analyst/officer | Static review queues and detail status. | Queue rows stagger only on initial render; changed status badges cross-fade/pulse once; action forms retain width while submitting. |
| CRM, committee, executive, auditor and legal | Static dashboard/report/review content. | Main report/review panel enters once; metric grids/table rows sequence; evidentiary timeline entries remain static unless inserted dynamically. |
| System administration | Static users/activity tables and feedback. | User/activity rows sequence; success/error feedback uses an accessible fade-in toast; destructive actions retain quiet press feedback. |
| External client intake (start, application/guarantor forms, upload, success/error) | Static forms and abrupt upload feedback. | Form content enters once, validation fades in, real upload progress eases, and empty/success/error panels fade rather than bounce. |

## Shared element rules

- Shell, sidebar and topbar remain static. Only the main content area enters.
- Cards and list/table rows stagger for at most eight items (35 ms increments).
- Queue removal can call `removeQueueItem(element)` to collapse only the item
  the user just acted on.
- `motion.js` observes future status-chip mutations, so initial statuses do
  not animate; only a changed badge receives the one-time pulse.
- `motion.css` provides skeleton, progress, submit-loading, toast, empty-state
  and reduced-motion behavior for every screen group without dependencies.
