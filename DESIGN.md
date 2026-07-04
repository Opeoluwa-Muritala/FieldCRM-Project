---
name: Institutional Modernist
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#4c4451'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#7d7483'
  outline-variant: '#cec3d3'
  surface-tint: '#7b41b3'
  primary: '#2e0052'
  on-primary: '#ffffff'
  primary-container: '#4b0082'
  on-primary-container: '#ba7ef4'
  inverse-primary: '#ddb7ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#301600'
  on-tertiary: '#ffffff'
  tertiary-container: '#4f2700'
  on-tertiary-container: '#c98c5c'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#f0dbff'
  primary-fixed-dim: '#ddb7ff'
  on-primary-fixed: '#2c0050'
  on-primary-fixed-variant: '#622599'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#ffdcc3'
  tertiary-fixed-dim: '#fbb884'
  on-tertiary-fixed: '#2f1500'
  on-tertiary-fixed-variant: '#693c12'
  background: '#f7f9fb'
  on-background: '#1E293B'
  surface-variant: '#e0e3e5'
  success-green: '#10B981'
  warning-amber: '#F59E0B'
  danger-red: '#EF4444'
  surface-off-white: '#F2F2F2'
typography:
  display-lg:
    fontFamily: Playfair Display
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
  headline-lg:
    fontFamily: Playfair Display
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-md:
    fontFamily: Playfair Display
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Playfair Display
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: DM Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: DM Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: DM Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-tracked:
    fontFamily: DM Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.1em
  label-md:
    fontFamily: DM Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  headline-lg-mobile:
    fontFamily: Playfair Display
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  button-height: 52px
  input-height: 48px
  brand-mark: 64px
---

## Brand & Style

The design system is engineered for **FieldCRM**, a specialized tool for Mainstreet Microfinance Bank. The brand identity balances the gravitas of a traditional financial institution with the agility of a modern digital product. It targets field officers who require high legibility and efficiency in various lighting conditions.

The visual style is **Corporate / Modern** with a focus on **Tonal Layering**. It utilizes high-contrast typography and a structured layout to convey reliability. The emotional response should be one of "confidence through clarity"—minimizing cognitive load for data-heavy tasks while maintaining a premium, institutional feel through the juxtaposition of elegant serif headlines and functional sans-serif UI elements.

## Colors

The palette is anchored by **Shield Purple**, a deep, authoritative hue that serves as the primary brand touchpoint. 

- **Primary (Shield Purple):** Used for primary actions, active states, and brand-critical components like the 64dp brand mark.
- **Secondary (Slate):** Employed for helper text, secondary icons, and non-critical UI elements to create visual hierarchy.
- **Backgrounds:** The default state uses an Off-White (#F2F2F2) for the canvas to reduce glare in field environments, with pure white used exclusively for card surfaces to create "lift."
- **Semantic Colors:** Success Green, Warning Amber, and Danger Red are reserved strictly for status communication and inline validation, ensuring they retain their communicative power.

## Typography

This design system uses a dual-font strategy to differentiate between **Editorial Content** and **Functional UI**.

- **Playfair Display:** Reserved for high-level headings (20pt/px and above). It provides the institutional "bank" feel. On mobile devices, use the `headline-lg-mobile` variant to ensure long titles do not break the layout.
- **DM Sans:** Used for all functional text. It is chosen for its high x-height and geometric clarity, making it ideal for data entry and reading on small screens.
- **Tracked Labels:** Secondary labels (like category headers or overlines) use DM Sans Bold with 10% letter-spacing and uppercase styling to provide distinct visual separation from body text.

## Layout & Spacing

The design system adheres to a **4dp/4px hard grid**. All dimensions, padding, and margins must be multiples of 4.

- **Mobile (Default):** A fluid 4-column layout with 16px margins and 16px gutters. Mobile-first design is critical for field operations.
- **Tablet/Desktop:** The layout transitions to a **Fixed Grid** centered-card model. Content is constrained to a maximum width of 768px on larger screens to prevent line lengths from becoming unreadable and to maintain a "form-centric" focus.
- **Rhythm:** Vertical spacing should follow a 1:2:4 ratio (8px, 16px, 32px) to group related data points effectively.

## Elevation & Depth

To maintain a professional and clean aesthetic, depth is communicated through **Tonal Layers** and **Soft Ambient Shadows**.

- **Surface Strategy:** Backgrounds are slightly tinted (Off-White) while active surfaces (cards, modals) are pure White (#FFFFFF). This creates a "natural" elevation without heavy shadows.
- **Shadows:** Use a single, highly diffused shadow for "Floating" elements like Primary Action Buttons or Modals. 
  - *Specs:* `0px 4px 20px rgba(75, 0, 130, 0.08)` (A subtle Shield Purple tint in the shadow to maintain color harmony).
- **Interactions:** Elements should not use skeuomorphic "press" effects; instead, use tonal shifts (darkening the background color by 10% on hover/active).

## Shapes

The shape language is **Soft (0.25rem)**. This subtle rounding provides a modern touch while remaining conservative enough for a financial institution.

- **Standard Elements:** Buttons, Input fields, and Checkboxes use a 4px (0.25rem) corner radius.
- **Containers:** Large cards and Modals may scale up to `rounded-lg` (8px) to soften the overall appearance of the dashboard.
- **Icons:** Should follow a "Filled" or "Thick Stroke" (2px) style with slightly rounded terminals to match the UI components.

## Components

- **Primary Buttons:** 52dp height. Solid **Shield Purple** fill with White text. Use 16px horizontal padding.
- **Ghost Buttons:** Transparent background with Shield Purple border (1px) and text. Used for secondary actions like "Cancel" or "Back."
- **Input Fields:** 48dp height. 1px Slate border. On focus, the border thickens to 2px Shield Purple. 
- **Inline Validation:** Error states must include both a Red border and a 12px DM Sans error message below the field. Success states use a Green checkmark icon within the trailing edge of the input.
- **Brand Mark:** The 64dp Shield + M logo must always be placed with at least 24px of clear space. It is the primary anchor for the top-left of the navigation bar.
- **Cards:** Used as the primary container for data groups. Cards have no border but use the ambient shadow defined in the Elevation section.
- **Chips:** For status display (e.g., "Pending," "Approved"). Use high-contrast text on a 10% opacity background of the semantic color (e.g., Success Green text on a light green tint).
- **SectionCard:** Titled white card wrapper used in detail and review screens. 16dp padding, 8dp radius, 1dp elevation.
- **LabelValue:** Two-column row (muted label left, value right) for displaying read-only loan fields inside a SectionCard.
- **StatusChip (string overload):** Accepts `label`, `isPositive`, `isDanger`, `small`. Renders coloured pill: green for positive, red for danger, amber for neutral/warning.

## Mobile Screens

### Roles and Navigation
| Role | Server value | Mobile enum | Home screen |
|---|---|---|---|
| Loan Officer | `loan_officer` | `LOAN_OFFICER` | Dashboard → MyQueue |
| Credit Officer | `credit_officer` | `CREDIT_OFFICER` | Dashboard → CreditReviewQueue |
| Branch Manager | `branch_manager` | `BRANCH_MANAGER` | Dashboard → PendingSignoffs |
| Auditor | `auditor` | `AUDITOR` | Dashboard → ComplianceFlags |
| CRM Officer | `crm` | `CRM` | Dashboard → CrmReview queue |
| Executive (MD/ED) | `md` / `ed` | `EXECUTIVE` | Dashboard → ExecutiveApproval queue |
| System Admin | `system_admin` | `SYSTEM_ADMIN` | Dashboard → SystemActivity |

### New Screens (v2)

#### `CrmReviewScreen`
- **Route:** `Screen.CrmReview`
- **Access:** `CRM`, `SYSTEM_ADMIN`
- **Purpose:** Pre-disbursement credit file completeness review. CRM checks 4 CBN §1.6 gates (bureau 1, bureau 2, CRMS search, NCR reg) via checkboxes before advancing to the Executive.
- **Actions:** Advance to Executive Approval (all 4 checked) | Return to Branch Manager
- **Layout:** SectionCard (loan summary) → SectionCard (uploaded docs with quality chips) → SectionCard (checklist) → SectionCard (notes textarea) → PrimaryButton + SecondaryButton

#### `ExecutiveApprovalScreen`
- **Route:** `Screen.ExecutiveApproval`
- **Access:** `EXECUTIVE`, `SYSTEM_ADMIN`
- **Purpose:** MD/ED issues or declines disbursement instruction. Action is irreversible and logged.
- **Actions:** Issue Disbursement Instruction (confirm dialog) | Cancel
- **Layout:** SectionCard (loan summary) → SectionCard (CRM notes, shown if present) → SectionCard (document summary) → Green authorisation card → PrimaryButton + SecondaryButton

#### `RepaymentScheduleScreen`
- **Route:** `Screen.RepaymentSchedule`
- **Access:** All roles
- **Purpose:** Shows full amortisation table with per-row Paid/Partial/Due status computed from cumulative payments, plus payment history list.
- **Actions:** Record Payment button (CRM / SYSTEM_ADMIN only)
- **Layout:** SectionCard (totals + classification chip + days-overdue) → PrimaryButton (if CRM) → SectionCard (schedule table) → SectionCard (payment history)

#### `ParDashboardScreen`
- **Route:** `Screen.ParDashboard`
- **Access:** `CRM`, `EXECUTIVE`, `AUDITOR`, `SYSTEM_ADMIN`
- **Purpose:** PAR-1/30/90 metric cards, CBN classification breakdown (Current/OLEM/Substandard/Doubtful/Lost with provision rates), full active loan portfolio table.
- **Layout:** 2×2 metric card grid → SectionCard (classification table) → SectionCard (portfolio table with DPD column)

### Document Scanner (DOCUMENT_SCAN mode)
- **Trigger:** `CameraOcrScanner(mode = "DOCUMENT_SCAN", onPdfReady = { pdfBytes -> ... })`
- **Flow:** A4-ratio viewfinder overlay → Capture Page (crops to A4, adds to page list) → thumbnail strip in top-right → Done button assembles `PdfDocument` and calls `onPdfReady`
- **PDF spec:** Each page rendered at 1240×1754px (A4 @ 150 DPI). Multi-page PDFs assembled on-device using Android `PdfDocument` API, then uploaded via `uploadDocumentPdf()`.
- **Server OCR:** After upload, backend fires async OCR extraction (pdfplumber → pytesseract fallback) and auto-fills `stage_data` with ≥80% confidence fields.

## Document Storage

Documents can be stored locally (default) or on **Cloudinary** (production).

Set these three environment variables to enable Cloudinary:
```
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

When enabled, `stored_path` in the `documents` table is the Cloudinary `secure_url`. The `cloud_public_id` and `cloud_preview_url` columns store the Cloudinary identifiers for transforms (e.g. page-1 thumbnail at `.jpg`).

When Cloudinary is not configured, files are saved to `DOCUMENT_UPLOAD_DIR` (local disk) and `stored_path` is a `/static/uploads/…` path.
