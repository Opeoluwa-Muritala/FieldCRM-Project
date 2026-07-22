# FieldCRM Electronic Signing and Document Evidence Structure

## Document status

- Assessment type: Current-state technical structure
- Platform: FieldCRM
- Jurisdiction considered: Nigeria
- Primary laws considered: Evidence Act 2011 (as amended in 2023), Nigeria Data Protection Act 2023, and Cybercrimes (Prohibition, Prevention, etc.) Act 2015
- Compliance status: Not production-ready for legally significant electronic signatures
- Current indicative rating: 2/10

This document describes the system as it currently exists. It does not state that the existing process is legally compliant and is not a substitute for advice from a qualified Nigerian lawyer.

## 1. Current user and authentication structure

### Staff users

Staff authenticate using an email/username and password. Successful authentication creates an HTTP-only session cookie. Staff permissions are role-based and include roles such as account officer, branch manager, credit analyst, CRM, Head CRM, auditor, ED, MD, legal and system administrator.

### Applicant users

An account officer creates a time-limited client-intake link containing a signed JWT. The applicant opens the link and receives a client-session cookie tied to:

- the loan application ID;
- the organisation ID;
- the originating officer ID; and
- an expiry time.

The current applicant flow does not require a permanent applicant account, password, signature-time OTP, verified-email challenge, biometric verification or transaction-specific identity confirmation.

Possession of the shared link and client-session cookie is therefore the principal evidence that the person completing the form was authorised to use that session.

## 2. Current application form structure

The client wizard stores application information in stages. The current visible flow accepts steps 1 through 8, including:

1. Applicant details.
2. Spousal consent, where applicable.
3. Guarantor details.
4. Employment or business details.
5. Existing credit facilities.
6. Loan request details.
7. Disbursement account information.
8. Collateral pledge information.

Form responses are merged into JSON stage data and selected fields are also copied into the main loan-application record.

Completing step 8 advances the application to branch-manager review.

## 3. Current signature capture

Signatures are drawn on an HTML canvas. When drawing ends, the browser converts the canvas to a base64-encoded data URL and submits it in a hidden form field.

The current templates contain fields for:

- spouse signature;
- borrower pledge signature;
- witness pledge signature; and
- applicant signature.

The declaration-and-consent page containing the main applicant signature is coded as step 9. The server currently permits only steps 1 through 8, so this signing page is not reachable through the normal client flow.

The CRM review screen separately provides an `Applicant Signature` canvas. That screen is used by an authenticated CRM officer rather than an independently authenticated applicant. The resulting image is stored in CRM review stage data under `applicant_signature`.

### Present evidential meaning

The stored value proves that an image was submitted through a particular HTTP request. By itself, it does not prove:

- who physically drew it;
- that the purported applicant controlled the session;
- that the applicant intended the image to sign a particular document;
- what exact document content was displayed at the time;
- that the signed content remained unchanged; or
- that a CRM employee did not create the image.

## 4. Current consent structure

The code contains consent fields for:

- credit-bureau disclosure;
- credit-check authorisation;
- cheque-recovery authorisation;
- Global Standing Instruction mandate; and
- a general declaration that the supplied information is true.

The applicant-facing consent block is on the unreachable step 9. Similar fields appear on the CRM review screen and are saved by a staff-authenticated request.

The system presently saves Boolean-like values such as `"true"`. It does not preserve an immutable snapshot of the full wording displayed, a consent-text version, a language version or proof that the applicant personally accepted each statement.

## 5. Current data storage structure

### Loan and form records

The principal records include:

- `loan_applications`;
- `stage_data`;
- `guarantors`;
- `pledged_items`;
- `documents`;
- `ocr_results` and `ocr_fields`;
- `workflow_events`; and
- `audit_entries`.

`stage_data` stores JSON responses, the user who saved them and a database timestamp. Application data can be updated during the workflow.

### Documents

Document metadata includes:

- document UUID;
- loan and organisation UUIDs;
- document type and form code;
- original filename;
- MIME type and size;
- quality and verification state;
- uploader and upload time;
- Cloudinary public ID;
- preview location; and
- upload status.

Production documents are stored as authenticated Cloudinary assets. FieldCRM authorises a request before issuing a short-lived download URL or streaming a Cloudinary-rendered preview image.

## 6. Current PDF generation structure

WeasyPrint renders HTML and CSS into PDF bytes. Generated offer letters include:

- organisation name;
- reference number;
- generation date;
- borrower name;
- facility amount;
- interest rate;
- tenor;
- facility type;
- repayment frequency;
- offer clauses; and
- blank physical-signature lines.

The PDF does not currently contain:

- the applicant's captured signature;
- a signature event ID;
- the exact signing time;
- the authentication method;
- the signed form version;
- the consent wording version;
- a signed-payload hash;
- a PDF hash;
- an organisational digital seal;
- a verification URL or QR code; or
- wording explaining that it is a printable representation of an electronic record.

The offer letter is generated after workflow approval. It is not currently a PDF representation of a borrower-signed offer acceptance.

## 7. Current audit structure

General audit records can store:

- organisation ID;
- entity type and ID;
- action;
- acting staff user and role;
- field name;
- old and new values;
- source;
- notes;
- request ID; and
- database creation time.

Workflow events record stage movement, the responsible staff user, role, notes and time.

The current signing flow does not create a dedicated immutable signature event containing:

- signer identity and verification result;
- applicant authentication method;
- OTP transaction ID;
- signing IP address;
- user-agent or proportionate device information;
- displayed consent wording;
- document/template version;
- canonical signed payload;
- signed-payload hash;
- PDF-generation time;
- PDF hash;
- trusted timestamp evidence; or
- subsequent amendment history.

Database comments describe audit records as append-only, but the base migration notes that database-level revocation of update and delete privileges must be performed separately by the database administrator.

## 8. Current integrity and versioning structure

The system currently does not cryptographically bind a signature image to the complete application data.

There is no dedicated signed-document version table or canonical signed payload. Cloudinary uploads are configured with overwrite enabled and deterministic public IDs. Accordingly, an asset may be replaced at the same logical location unless separate versioning controls are applied.

The database stores size and location metadata but does not store a SHA-256 or equivalent checksum for each signed source record and final PDF.

Amendments do not currently create a complete superseded-version chain requiring re-signature.

## 9. Current document access structure

Downloads and previews require an authenticated FieldCRM user. Document lookup checks that the document belongs to the user's organisation.

This provides tenant isolation, but the document endpoint does not itself demonstrate checks for:

- assignment to the particular application;
- branch restrictions;
- document-category restrictions;
- least-privilege role access; or
- an individual business need to view the document.

Document UUIDs are difficult to guess, but UUID secrecy is not a substitute for application-level authorisation.

The client-intake upload routes bind the loan and organisation IDs to the signed client-session token, which reduces straightforward cross-application ID substitution.

## 10. Current privacy and data-protection structure

The system processes substantial personal and financial information, including names, contact information, BVN, identity documents, account information, signatures, guarantor information and loan records.

The reviewed signing interface does not presently provide a complete privacy notice stating:

- the controller's identity and contact details;
- the lawful basis for each purpose;
- the data categories collected;
- purposes of processing;
- recipients and processors;
- cross-border transfers;
- retention periods or criteria;
- data-subject rights;
- complaint and remediation channels; and
- the right to complain to the Nigeria Data Protection Commission.

The current code review did not identify a complete implementation for:

- a formal retention schedule;
- applicant data-subject access requests;
- correction, restriction, objection, portability and deletion workflows;
- litigation-hold handling;
- automated expiry of signature data;
- prevention of signature reuse for another purpose;
- documented cross-border safeguards for Cloudinary;
- a data-processing agreement control register;
- a signing-system DPIA; or
- breach-notification workflow.

HTTPS and authenticated Cloudinary storage provide important transport and storage protections. The reviewed code does not establish application-level encryption of signature data or signed payloads at rest.

## 11. Current Evidence Act position

A generated PDF is a computer-generated document or electronic record. If tendered in Nigerian proceedings, section 84 requirements are likely to apply.

The present system retains some useful evidence, including database timestamps, staff accounts, workflow events, document metadata and cloud identifiers. It does not yet retain a complete evidence package that can reliably demonstrate authorship, signature intent, exact signed content and post-signature integrity.

A future section 84 certificate would ordinarily need to identify the record, explain how it was produced, describe the relevant system and address the statutory conditions concerning regular use, ordinary-course input, proper operation and accurate reproduction. The current records would make that certificate materially harder to support for an applicant signature.

## 12. Current special-document risks

The platform handles or refers to financial declarations, credit checks, GSI mandates, cheque-recovery authority, collateral pledges, guarantor forms and offer letters.

These documents may be subject to additional CBN, NIBSS, banking, stamping, witnessing or registration requirements. The present generic canvas-signature mechanism does not itself establish compliance with those requirements.

The system should not be extended without specific legal approval to wills, affidavits, deeds, land instruments, powers of attorney, notarised documents, court documents or other instruments requiring special execution formalities.

## 13. Current structure summary

```text
Loan officer creates signed share link
              |
              v
Applicant opens link and receives client-session cookie
              |
              v
Applicant completes wizard steps 1-8
              |
              +----> Drawn spouse/pledge/witness images may be stored
              |
              +----> Main applicant consent/signature step 9 is unreachable
              |
              v
Application advances to staff workflow
              |
              v
CRM screen can capture "Applicant Signature" under CRM authentication
              |
              v
Stage JSON + general audit/workflow events stored
              |
              v
Documents stored as authenticated Cloudinary assets
              |
              v
Authorised users preview images or download through short-lived URLs
              |
              v
Offer PDF generated later from selected loan fields and clauses
```

## 14. Current mandatory gaps before production signing

1. The actual applicant must perform the signing action.
2. The applicant consent/signing step must be reachable and server-validated.
3. Staff must not create or submit an applicant signature.
4. The complete signed payload must be frozen, versioned and hashed.
5. The signature must be linked to that payload, signer, intention and time.
6. The generated PDF must be produced only from the frozen signed payload.
7. Signed documents must use immutable versions rather than overwrite semantics.
8. A dedicated signing audit trail must be retained.
9. Appropriate applicant authentication must be added for financial mandates.
10. Privacy notices, lawful bases, retention, data-subject rights and processor/transfer controls must be documented and implemented.
11. Document-level least-privilege access control must be added.
12. Each regulated or specially executed document must receive Nigerian legal approval.

## 15. Recommended status label

Until the mandatory gaps are addressed, generated documents containing signature images should be labelled internally as:

> Unverified printable representation of information captured electronically. The displayed signature image has not been cryptographically bound to this document and must not, by itself, be treated as proof of execution by the named person.

They should not be represented as original handwritten documents, certified copies or conclusively authenticated electronic signatures.

