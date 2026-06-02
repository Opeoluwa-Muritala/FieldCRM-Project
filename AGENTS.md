# FieldCRM Design System Rules

These rules apply to Figma-driven implementation and UI changes in this project.

## Project Shape

- This is a FastAPI + Jinja2 application with static CSS and JavaScript. Server-rendered pages live in `frontend/templates/`; shared browser assets live in `frontend/static/css/` and `frontend/static/js/`.
- Backend API, auth, RBAC, schemas, and workflow logic live under `backend/app/`.
- Mobile/KMP code exists under `android/` and `shared/`, but web Figma implementations should target the FastAPI/Jinja frontend unless the task explicitly asks for Android.
- Reuse existing backend models and workflow concepts before adding new storage: `LoanApplication`, `StageData`, `WorkflowEvent`, `Document`, and role checks in `backend/app/api/deps.py`.

## Figma MCP Integration Rules

- Run `get_design_context` for the exact Figma node before implementing a Figma-driven screen or component.
- Run `get_screenshot` for visual reference before coding.
- If Figma output is React or Tailwind, treat it as a design description only. Translate it into Jinja templates, static CSS, and vanilla JavaScript that match this project.
- Reuse existing classes, CSS variables, layout patterns, and component vocabulary from `frontend/static/css/dashboard.css` and `frontend/static/css/pipeline.css`.
- Use localhost asset URLs returned by the Figma MCP server directly when provided. Do not invent placeholder artwork when Figma supplies assets.
- Validate final UI against the Figma screenshot for layout, role states, text fit, responsive behavior, and interaction states.

## Styling Rules

- Use the existing dark operational dashboard language: `Outfit` font, dense work surfaces, clear grids, restrained cards, and CSS variables such as `--bg-primary`, `--bg-secondary`, `--accent-color`, `--accent-success`, `--accent-warning`, `--accent-danger`, `--text-main`, `--text-muted`, and `--border-color`.
- Prefer shared CSS files in `frontend/static/css/` over inline styles. New page-level styles should be in a matching CSS file and linked from the template.
- Use 8px to 16px radii consistently with existing cards and controls. Avoid decorative blobs, marketing heroes, oversized empty sections, and one-purpose landing pages.
- Use responsive grids with explicit breakpoints like the existing dashboard. Text must not overlap or overflow on mobile or desktop.
- Use semantic HTML labels, fieldsets, buttons, tables, and status elements for forms and review screens.

## Single Role Dashboard Rule

- IMPORTANT: Each authenticated user should land on one dashboard tailored to their role, not separate disconnected dashboards.
- The dashboard must render role-specific modules based on `current_user.role`:
  - Loan Officer: borrower intake, Loan Application Form, upload/OCR queue, missing documents, returned applications, visitation tasks.
  - Branch Manager: review queue, visitation signoff, document exceptions, approval conditions, final branch controls.
  - Credit Officer: credit risk review, bureau/DTI fields, guarantor strength, collateral and pledge review, low-confidence OCR exceptions.
  - Auditor: compliance checklist, document verification, consent/GSI/signature verification, immutable audit trail review.
  - System Admin/MCR: final approval readiness, committee vote record, disbursement readiness, role/user oversight.
- Navigation should reveal only useful role actions while preserving access to shared authenticated routes where permitted by RBAC.

## Loan Form Digital Entry, Upload, OCR, And Verification

- IMPORTANT: Every loan-related form must support two primary actions: `Fill Form in App` and `Upload Completed Form`.
- Supported forms are:
  - Loan Application Form, required for every borrower.
  - Guarantors Form, required for each guarantor attached to a loan.
  - Pledge and Trust Receipt, required when assets, stock, proceeds, property, goods, vehicles, or other collateral are pledged.
- Manual entry screens must use structured fields, required markers, checkboxes, date fields, amount fields, signature capture, and document upload prompts.
- Upload screens must accept PDF, JPG, JPEG, and PNG, then show image quality status, detected form type, OCR/handwriting extraction status, extracted values, confidence scores, and correction fields.
- OCR values are never final truth. Clear values may prefill fields, but every extracted value must be reviewable and editable.
- Always highlight low-confidence or missing critical fields: applicant name, guarantor name, BVN, loan amount, loan tenor, maximum guarantee amount, collateral description, bank account number, cheque number, GSI mandate, signatures, witness details, and dates.
- Store form data in structured payloads compatible with `StageData.data_json`; include source metadata for each value: `manual`, `ocr`, `corrected`, or `approved`.
- Use `WorkflowEvent` for audit trail events such as upload, OCR extraction, manual correction, document verification, signature capture, officer approval, return, and final approval.

## Required Form Content

- Loan Application Form screens must cover personal details, ID/BVN/contact details, marital status, spouse consent, guarantor details, employment/business details, existing facilities, education, loan purpose, loan amount, loan tenor, collateral/security, repayment method, disbursement account, credit bureau consent, cheque authorization, GSI mandate, applicant signature, and date.
- Guarantors Form screens must support applicant details, guarantor identity, relationship, ID details, phone, BVN, DOB, origin/LGA, education, email, home address, existing loans/guarantees, marital/dependant/spouse details, employment/business details, declaration, maximum guarantee limit, bank account, cheque number, pledged items, guarantor signature, witness signature, and witness date.
- Pledge and Trust Receipt screens must support date, borrower/association name, facility amount in figures and words, shop/house address, obligor name, pledged goods/stock/proceeds/assets, schedule rows, quantity, description, estimated value, borrower signature, witness signature, witness name, witness address, and witness occupation.
- Internal bank officer verification screens must include visitation report fields: visit date, person met, premises description, building type, number of storeys, colour, landmark, direction from branch, visiting officer, account officer, officer signatures, and branch manager signoff.

## Supporting Document Rules

- Document prompts must be dynamic and based on borrower, guarantor, employment type, business type, and pledged asset type.
- Every applicant: passport photograph, valid ID, BVN confirmation where required, proof of address where required, and recent bank statement.
- Employed applicant or guarantor: payslip, employer confirmation, staff ID where available, and salary account statement.
- Self-employed or business applicant/guarantor: business account statement, registration documents where applicable, business address evidence, stock evidence, sales records, invoices, trade/association ID where applicable, and turnover proof.
- Guarantor asset pledge: proof of ownership, item photographs, valuation evidence, and relevant asset documents.
- Goods or stock: inventory list, photographs, purchase receipts, invoices, or valuation.
- Business equipment: ownership proof, serial numbers, photographs, and estimated value.
- Property: title documents, survey plan, deed, allocation letter, valuation report, or bank-required ownership evidence.
- Vehicle: vehicle licence, proof of ownership, photographs, and valuation where applicable.

## Final Review And Approval Rules

- Final review screens must show typed fields, uploaded documents, OCR results, missing information, low-confidence values, required supporting documents, signatures, officer verification, branch manager signoff, and approval status.
- Users must be able to correct extracted text, attach missing documents, sign digitally, save drafts, and resubmit.
- IMPORTANT: Do not allow final approval or disbursement until all required forms are completed, mandatory supporting documents are attached, critical fields are confirmed, required signatures are captured, declarations and consents are accepted, bank officer verification is complete, and branch manager signoff is complete.
- Use explicit readiness states such as `Missing`, `Needs Review`, `Low Confidence`, `Verified`, `Signed`, `Approved`, and `Returned`.

## Backend And Data Rules

- Add or update Pydantic schemas in `backend/app/schemas/` for new structured form, OCR, document, and review payloads.
- Add API routes under `backend/app/api/v1/` and protect every action with `deps.get_current_user` or `deps.RoleChecker`.
- Organization scoping is mandatory. Never expose data across `org_id` boundaries.
- Preserve role workflow ownership using `current_owner_id`, `current_stage`, and explicit return reasons.
- For file uploads, use server-side validation for content type, size, form type, image quality, and required document category.
- Treat OCR confidence and source metadata as first-class data, not UI-only labels.

## Accessibility And Quality

- Every input must have a visible label and server-compatible `name`.
- Every upload, OCR, verification, and approval action must have loading, success, failure, and correction states.
- Use `textContent` for dynamic JavaScript text unless rendering trusted server templates.
- Keep JavaScript in `frontend/static/js/` and avoid global behavior that breaks role-specific pages.
- Add focused tests or import checks when backend schemas, auth, workflow gates, or disbursement readiness rules change.
