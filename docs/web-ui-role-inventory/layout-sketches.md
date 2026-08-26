# FieldCRM web layout and arrangement sketches

Last verified: 26 August 2026

These sketches are structural, not pixel-perfect. Every HTML template is mapped to one of these patterns in `source-catalog.md`. Shared shells, components, and partials use the fragment sketches near the end.

## W01 — Authenticated dashboard

Used by role dashboards and the configuration hub landing page.

```text
DESKTOP
┌──────────────┬──────────────────────────────────────────────────────┐
│ Logo         │ Top bar: page title        search  bell  profile   │
│              ├──────────────────────────────────────────────────────┤
│ Navigation   │ Hero / date / main supporting action               │
│ groups       ├──────────┬──────────┬──────────┬──────────┐          │
│              │ Metric 1 │ Metric 2 │ Metric 3 │ Metric 4 │          │
│              ├──────────┴──────────┼──────────┴──────────┤          │
│              │ Priority work/list  │ Secondary summary   │          │
│ Logout       │                     │                     │          │
└──────────────┴─────────────────────┴─────────────────────┴──────────┘

MOBILE
┌──────────────────────────┐
│ ☰  Page title       Bell │
├──────────────────────────┤
│ Hero / main action       │
├────────────┬─────────────┤
│ Metric 1   │ Metric 2    │
├────────────┼─────────────┤
│ Metric 3   │ Metric 4    │
├──────────────────────────┤
│ Priority work/list       │
├──────────────────────────┤
│ Secondary summary        │
└──────────────────────────┘
```

## W02 — Queue, directory, and pipeline

Used by Awaiting Me, review queues, applications, users, borrowers, work queues, and pipeline pages.

```text
┌──────────────┬──────────────────────────────────────────────────────┐
│ Sidebar      │ Top bar                                             │
│              ├──────────────────────────────────────────────────────┤
│              │ Title / description                    Main action  │
│              ├──────────────────────────────────────────────────────┤
│              │ Filters: search | state | date | Apply | Clear      │
│              ├──────────────────────────────────────────────────────┤
│              │ Optional metrics / pipeline stage summaries         │
│              ├──────────────────────────────────────────────────────┤
│              │ Table or card list                                  │
│              │ identity | reference | amount | state | row action │
│              ├──────────────────────────────────────────────────────┤
│              │ Pagination / load more / empty state                │
└──────────────┴──────────────────────────────────────────────────────┘
```

At mobile width, filters and cards stack. Tables remain inside a horizontal scroll container; semantic order is unchanged.

## W03 — Application dossier/detail

Used by role-specific application detail workstations.

```text
┌──────────────┬──────────────────────────────────────────────────────┐
│ Sidebar      │ Top bar                                             │
│              ├───────────────────┬──────────────────────────────────┤
│              │ STICKY EVIDENCE   │ WORK AREA                        │
│              │ Identity/status   │ Summary/readiness                │
│              │ Timeline          │ Screening / recommendation       │
│              │ Documents         │ Checklist / exceptions           │
│              │ Integrations      │ Audit / role-specific controls   │
│              │                   │                                  │
│              │                   ├──────────────────────────────────┤
│              │                   │ Decision/action footer           │
└──────────────┴───────────────────┴──────────────────────────────────┘
```

Tablet/mobile turns both columns into one flow: identity → evidence → work cards → decision actions.

## W04 — Approval readiness and concurrence

Used by `shared/approve.html` for Team Lead/Supervisor approval and related readiness screens.

```text
┌──────────────┬───────────────────┬──────────────────────────────────┐
│ App sidebar  │ REVIEW SIDEBAR    │ DECISION DESK                    │
│              │ Borrower/ref      │ Intro                            │
│              │ Requested amount  │ 01 Forms completed               │
│              │ Readiness 81%     │ 02 Supporting documents          │
│              │ Critical items    │ 03 Data quality                   │
│              │ Review dots       │ 04 Declarations                   │
│              │                   │ 05 Field verification             │
│              │                   │ 06 Approval chain                 │
│              │                   │ Recommendations                  │
│              │                   ├──────────────────────────────────┤
│              │                   │ Attestations + amount             │
│              │                   │ PRIMARY → secondary → cancel      │
└──────────────┴───────────────────┴──────────────────────────────────┘
```

On narrow screens: application card → readiness → review status → evidence sections → recommendations → attestations → primary forward → return/correction → cancel.

## W05 — Credit/CRM/executive decision desk

Used by credit review, CRM/Head CRM review, ED, MD, executive instruction, MCC summary, and disbursement decisions.

```text
┌──────────────┬──────────────────────────────────────────────────────┐
│ Sidebar      │ Identity / amount / stage summary                   │
│              ├──────────────────────────────────────────────────────┤
│              │ Compact tabs or supporting analysis action          │
│              ├──────────────────────┬───────────────────────────────┤
│              │ Evidence/documents   │ Analysis/recommendations      │
│              │ Screening/history    │ Notes/amount/conditions       │
│              ├──────────────────────┴───────────────────────────────┤
│              │ DECISION PANEL                                      │
│              │ Deep-purple forward | plum supporting | red deny   │
│              │ cancel/back                                          │
└──────────────┴──────────────────────────────────────────────────────┘
```

Only the forward/move-up control is deep purple. Upload, analysis, request input, and previews remain plum.

## W06 — Wizard / stepped data entry

Used by application and guarantor wizards.

```text
┌──────────────┬──────────────────────────────────────────────────────┐
│ Sidebar      │ Step 1 ─ Step 2 ─ Step 3 ─ … ─ Review              │
│              ├──────────────────────────────────────────────────────┤
│              │ Section title and help                              │
│              │ ┌──────────────────┬──────────────────┐              │
│              │ │ Label + field    │ Label + field    │              │
│              │ ├──────────────────┼──────────────────┤              │
│              │ │ Conditional/repeating groups        │              │
│              │ └──────────────────────────────────────┘              │
│              ├──────────────────────────────────────────────────────┤
│              │ Back | Save draft              Continue / Submit    │
└──────────────┴──────────────────────────────────────────────────────┘
```

Mobile uses one field per row and full-width footer actions. Reviewer mode locks fields but retains navigation.

## W07 — Standard single-form page

Used by return, upload, payment, valuation, settings, customer creation, and repayment feasibility pages.

```text
┌──────────────┬──────────────────────────────────────────────────────┐
│ Sidebar      │ Context/title                                       │
│              ├──────────────────────────────────────────────────────┤
│              │ Optional summary/callout                            │
│              ├──────────────────────────────────────────────────────┤
│              │ Form card                                            │
│              │ labels + inputs + validation/help                    │
│              │ repeating items / upload zone where relevant         │
│              ├──────────────────────────────────────────────────────┤
│              │ Primary/secondary/danger/cancel actions              │
└──────────────┴──────────────────────────────────────────────────────┘
```

## W08 — OCR/document split workstation

Used by OCR review and protected document correction.

```text
┌──────────────┬──────────────────────────┬───────────────────────────┐
│ Sidebar      │ DOCUMENT VIEWER          │ EXTRACTED FIELDS          │
│              │ Zoom− | Zoom+ | Fit      │ field / OCR / confidence  │
│              │                          │ correction control        │
│              │ protected page image(s) │ critical/review badge     │
│              │ Prev / Next              │ Save corrections          │
│              │                          │ Mark verified             │
└──────────────┴──────────────────────────┴───────────────────────────┘
```

Mobile stacks viewer before extracted fields; compact controls remain plum and verification remains the primary action.

## W09 — Portfolio/report/table

Used by audit, PAR, repayment schedule, system activity, search results, and reporting screens.

```text
┌──────────────┬──────────────────────────────────────────────────────┐
│ Sidebar      │ Report title / date / source                        │
│              ├────────────┬────────────┬────────────┐               │
│              │ Metric     │ Metric     │ Metric     │               │
│              ├────────────┴────────────┴────────────┴───────────────┤
│              │ Filters / classification / search                   │
│              ├──────────────────────────────────────────────────────┤
│              │ Read-only table / timeline                          │
│              ├──────────────────────────────────────────────────────┤
│              │ Pagination / export or permitted supporting action  │
└──────────────┴──────────────────────────────────────────────────────┘
```

## W10 — Application Overview / Customer 360

Used by read-only dossier and customer aggregation pages.

```text
┌──────────────┬──────────────────────────────────────────────────────┐
│ Sidebar      │ PURPLE IDENTITY HERO + persistent status            │
│              ├──────────────────────────────────────────────────────┤
│              │ Section tabs (horizontal scroll on mobile)          │
│              ├──────────────────────────────┬───────────────────────┤
│              │ Main record summaries        │ Readiness/quick links │
│              │ Loans/exposure/documents     │ CBS/source context    │
│              │ Visits/collateral/guarantors │ Recent activity       │
└──────────────┴──────────────────────────────┴───────────────────────┘
```

## W11 — Configuration Admin editor

Used by configuration hub pages.

```text
┌────────────────┬────────────────────────────────────────────────────┐
│ Config sidebar │ Hub/version/product title + draft/effective state │
│                ├────────────────────────────────────────────────────┤
│ Versions       │ Search/filter or version banner                   │
│ Products       ├──────────────────────┬─────────────────────────────┤
│ Forms          │ Definition/list      │ Editor/detail               │
│ Documents      │ cards/table          │ fields, rules, validation   │
│ Features       │                      │                              │
│ Exit/logout    ├──────────────────────┴─────────────────────────────┤
│                │ Save/validate/approve/publish actions              │
└────────────────┴────────────────────────────────────────────────────┘
```

MFA is a centred standalone card: FieldCRM/control-centre identity → TOTP field → verify/cancel.

## W12 — Authentication and simple status

Used by login, forgot/reset password, error, and not-found pages.

```text
DESKTOP LOGIN
┌──────────────────────────────┬──────────────────────────────┐
│ Brand/onboarding hero        │ Centred glass form card      │
│ product statement            │ identity/password inputs     │
│ workspace preview/metrics    │ primary action + help links  │
└──────────────────────────────┴──────────────────────────────┘

SIMPLE STATUS
┌─────────────────────────────────────────────────────────────┐
│                     Brand/logo                              │
│                 Centred status card                         │
│              message / guidance / action                    │
└─────────────────────────────────────────────────────────────┘
```

## W13 — Public landing and terms

```text
┌─────────────────────────────────────────────────────────────┐
│ Sticky public header: logo | nav | sign-in/action          │
├──────────────────────────────┬──────────────────────────────┤
│ Hero statement + actions     │ Product preview             │
├──────────────────────────────┴──────────────────────────────┤
│ Capability band                                             │
├─────────────────────────────────────────────────────────────┤
│ Workflow / field operations / FAQ                          │
├─────────────────────────────────────────────────────────────┤
│ Footer                                                      │
└─────────────────────────────────────────────────────────────┘
```

Terms uses the same public header with a narrow, single-column legal article.

## W14 — Printable/document output

Used by offer-letter templates and server-rendered report/document surfaces.

```text
┌─────────────────────────────────────────────────────────────┐
│ Institution logo / report header / reference / date        │
├─────────────────────────────────────────────────────────────┤
│ Recipient and facility summary                              │
├─────────────────────────────────────────────────────────────┤
│ Terms, conditions, schedules, signatures                    │
├─────────────────────────────────────────────────────────────┤
│ Footer/support/audit metadata                               │
└─────────────────────────────────────────────────────────────┘
```

Print output uses neutral ink and the configured institution/report branding; interactive controls are omitted.

## F01 — Sidebar and tab-bar fragments

```text
Sidebar fragment: GROUP LABEL → icon/label link → optional count badge
Mobile tab bar:     icon+label | icon+label | icon+label | More
```

Navigation visibility is server-authorized; hidden UI never replaces route authorization.

## F02 — Dossier evidence fragment

```text
Card title
├─ status/source/last-updated metadata
├─ evidence or readiness rows
└─ preview/compact link where authorized
```

Used by application readiness, flags, document checklist, CBS summary, and recommendation components.

## F03 — Progressive partial fragment

```text
Metric partial: [label + value + optional delta]
Row partial:    [identity | amount | state | owner | link]
Fallback:       [skeleton] → [loaded content] or [error + Retry Loading]
```

## Responsive breakpoint summary

| Width | Behavior |
|---|---|
| Above 1180px | Persistent full sidebar and wide workspace |
| 1024–1179px | Drawer/reduced rail depending shell; grids start collapsing |
| 768–1023px | Tablet rail, wrapped actions, one-column two-pane workstations |
| 641–767px | Mobile header/drawer, mostly single-column content |
| 421–640px | Full-width action stacks and compact padding |
| 420px and below | 46px touch targets, smallest safe page padding |

