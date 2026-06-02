const formOrder = ["loan_application", "guarantor", "pledge_trust_receipt", "field_visitation_report"];

const forms = {
    loan_application: {
        title: "Loan Application Form",
        repeatLabel: "Loan Application",
        steps: [
            {
                title: "Section 1: Applicant Information",
                fields: [
                    ["applicant_full_name", "Applicant Full Name"],
                    ["gender", "Gender"],
                    ["date_of_birth", "Date of Birth"],
                    ["marital_status", "Marital Status"],
                    ["nationality", "Nationality"],
                    ["residential_address", "Residential Address"],
                    ["business_address", "Business Address"],
                    ["phone_numbers", "Phone Number(s)"],
                    ["email_address", "Email Address"],
                    ["bvn", "BVN"],
                    ["means_of_identification", "Means of Identification"],
                    ["id_number", "ID Number"],
                    ["id_issue_date", "ID Issue Date"],
                    ["id_expiry_date", "ID Expiry Date"]
                ],
                uploads: [["passport_photo", "Passport Photograph"], ["valid_id", "Valid ID"], ["signature_image", "Applicant Signature Image"]]
            },
            {
                title: "Section 2: Spouse Information",
                fields: [["spouse_name", "Spouse Name"], ["spouse_phone_number", "Spouse Phone Number"], ["spouse_occupation", "Spouse Occupation"], ["spouse_employer", "Spouse Employer"], ["spouse_address", "Spouse Address"]]
            },
            {
                title: "Section 3: Employment Information",
                fields: [["employer_name", "Employer Name"], ["employer_address", "Employer Address"], ["employment_status", "Employment Status"], ["position", "Position"], ["staff_number", "Staff Number"], ["date_employed", "Date Employed"], ["monthly_salary", "Monthly Salary"], ["other_income", "Other Income"], ["salary_account_details", "Salary Account Details"]],
                uploads: [["payslip", "Payslip"], ["employment_letter", "Employment Letter"], ["employer_confirmation", "Employer Confirmation Letter"], ["salary_account_statement", "Salary Account Statement"]]
            },
            {
                title: "Section 4: Business Information",
                fields: [["business_name", "Business Name"], ["nature_of_business", "Nature of Business"], ["business_registration_number", "Business Registration Number"], ["business_address", "Business Address"], ["years_in_operation", "Years in Operation"], ["monthly_sales", "Monthly Sales"], ["monthly_expenses", "Monthly Expenses"], ["average_monthly_turnover", "Average Monthly Turnover"]],
                uploads: [["business_registration_documents", "Business Registration Documents"], ["stock_records", "Stock Records"], ["invoices", "Invoices"], ["sales_records", "Sales Records"], ["business_account_statement", "Business Account Statement"]]
            },
            {
                title: "Section 5: Existing Facilities",
                fields: [],
                table: { id: "existing_facilities", title: "Existing Loan Facilities Table", columns: ["Institution Name", "Facility Type", "Outstanding Balance", "Monthly Repayment", "Maturity Date", "Status"] }
            },
            {
                title: "Section 6: Loan Request Details",
                fields: [["loan_type", "Loan Type"], ["loan_amount_requested", "Loan Amount Requested"], ["loan_purpose", "Loan Purpose"], ["tenor", "Tenor"], ["repayment_frequency", "Repayment Frequency"], ["repayment_method", "Repayment Method"], ["proposed_disbursement_account", "Proposed Disbursement Account"], ["proposed_disbursement_date", "Proposed Disbursement Date"]]
            },
            {
                title: "Section 7: Collateral Information",
                fields: [],
                table: { id: "collateral_schedule", title: "Collateral Schedule Table", columns: ["Collateral Type", "Description", "Ownership Status", "Estimated Value", "Supporting Document Reference"] },
                uploads: [["proof_of_ownership", "Proof of Ownership"], ["asset_photographs", "Asset Photographs"], ["inventory_lists", "Inventory Lists"], ["valuation_reports", "Valuation Reports"], ["vehicle_documents", "Vehicle Documents"], ["title_documents", "Title Documents"], ["other_collateral_evidence", "Other Collateral Evidence"]]
            },
            {
                title: "Section 8: Consents and Authorisations",
                fields: [["credit_bureau_consent", "Credit Bureau Consent"], ["cheque_authorisation", "Cheque Authorisation"], ["gsi_mandate", "GSI Mandate"], ["terms_acceptance", "Terms and Conditions Acceptance"]],
                uploads: [["signed_consent", "Signed Consent Image"], ["mandate_document", "Mandate Document"]]
            },
            {
                title: "Section 9: Applicant Declaration",
                fields: [["applicant_signature", "Applicant Signature"], ["signature_date", "Signature Date"], ["witness_name", "Witness Name"], ["witness_signature", "Witness Signature"], ["witness_date", "Witness Date"]],
                uploads: [["applicant_signature_image", "Applicant Signature Image"], ["witness_signature_image", "Witness Signature Image"]]
            },
            {
                title: "Applicant Supporting Documents",
                fields: [],
                uploads: [["passport_photograph", "Passport Photograph"], ["national_id_card", "National ID Card"], ["drivers_licence", "Driver's Licence"], ["international_passport", "International Passport"], ["voters_card", "Voter's Card"], ["utility_bill", "Utility Bill"], ["tenancy_agreement", "Tenancy Agreement"], ["other_proof_of_address", "Other Proof of Address"], ["recent_bank_statements", "Recent Bank Statements"], ["salary_account_statements", "Salary Account Statements"], ["business_account_statements", "Business Account Statements"]]
            }
        ]
    },
    guarantor: {
        title: "Guarantors Form",
        repeatLabel: "Guarantor",
        steps: [
            {
                title: "Guarantor Information",
                fields: [["guarantor_full_name", "Guarantor Full Name"], ["relationship_to_applicant", "Relationship to Applicant"], ["gender", "Gender"], ["marital_status", "Marital Status"], ["phone_number", "Phone Number"], ["email_address", "Email Address"], ["bvn", "BVN"], ["residential_address", "Residential Address"]],
                uploads: [["passport_photo", "Passport Photograph"], ["valid_id", "Valid ID"], ["signature_image", "Guarantor Signature Image"]]
            },
            {
                title: "Identification Details",
                fields: [["id_type", "ID Type"], ["id_number", "ID Number"], ["issue_date", "Issue Date"], ["expiry_date", "Expiry Date"]]
            },
            {
                title: "Employment or Business Details",
                fields: [["employer_name", "Employer Name"], ["employer_address", "Employer Address"], ["position", "Position"], ["monthly_income", "Monthly Income"], ["business_name", "Business Name"], ["nature_of_business", "Nature of Business"], ["business_address", "Business Address"]]
            },
            {
                title: "Existing Obligations",
                fields: [],
                table: { id: "existing_loans", title: "Existing Loans Table", columns: ["Institution", "Facility Type", "Outstanding Balance", "Monthly Repayment", "Status"] }
            },
            {
                title: "Existing Guarantees",
                fields: [],
                table: { id: "existing_guarantees", title: "Existing Guarantees Table", columns: ["Borrower Name", "Institution", "Guaranteed Amount", "Outstanding Exposure", "Status"] }
            },
            {
                title: "Guarantee Details",
                fields: [["maximum_guarantee_limit", "Maximum Guarantee Limit"], ["bank_name", "Bank Name"], ["account_number", "Account Number"], ["account_name", "Account Name"], ["cheque_number", "Cheque Number"]],
                uploads: [["cheque_image", "Cheque Image"]]
            },
            {
                title: "Pledged Items",
                fields: [],
                table: { id: "guarantor_pledged_items", title: "Guarantor Pledged Items Table", columns: ["Item Description", "Quantity", "Estimated Value", "Ownership Evidence"] },
                uploads: [["proof_of_ownership", "Proof of Ownership"], ["item_photographs", "Item Photographs"], ["valuation_evidence", "Valuation Evidence"]]
            },
            {
                title: "Guarantor Declaration",
                fields: [["declaration_text", "Declaration Text"], ["guarantor_signature", "Guarantor Signature"], ["signature_date", "Signature Date"], ["witness_name", "Witness Name"], ["witness_signature", "Witness Signature"], ["witness_date", "Witness Date"]],
                uploads: [["guarantor_signature_image", "Guarantor Signature Image"], ["witness_signature_image", "Witness Signature Image"]]
            },
            {
                title: "Guarantor Supporting Documents",
                fields: [],
                uploads: [["passport_photograph", "Passport Photograph"], ["valid_means_of_identification", "Valid Means of Identification"], ["bvn_evidence", "BVN Evidence"], ["proof_of_address", "Proof of Address"], ["bank_statement", "Bank Statement"], ["payslip", "Payslip"], ["employer_confirmation_letter", "Employer Confirmation Letter"], ["business_documents", "Business Documents"]]
            }
        ]
    },
    pledge_trust_receipt: {
        title: "Pledge and Trust Receipt",
        repeatLabel: "Pledge",
        steps: [
            {
                title: "Borrower Information",
                fields: [["borrower_name", "Borrower Name"], ["association_name", "Association Name"], ["facility_amount", "Facility Amount"], ["shop_address", "Shop Address"], ["house_address", "House Address"]],
                uploads: [["facility_document", "Facility Document"]]
            },
            {
                title: "Pledged Assets",
                fields: [],
                table: { id: "asset_schedule", title: "Asset Schedule Table", columns: ["Asset Description", "Quantity", "Unit", "Estimated Value", "Ownership Evidence", "Remarks"] },
                uploads: [["proof_of_ownership", "Proof of Ownership"], ["asset_photos", "Asset Photographs"], ["purchase_receipts", "Purchase Receipts / Invoices"]]
            },
            {
                title: "Pledged Goods and Stock",
                fields: [],
                table: { id: "goods_stock_schedule", title: "Goods and Stock Schedule Table", columns: ["Item Description", "Quantity", "Estimated Value", "Storage Location"] }
            },
            {
                title: "Sales Proceeds Information",
                fields: [["expected_sales_proceeds", "Expected Sales Proceeds"], ["collection_method", "Collection Method"], ["deposit_account", "Deposit Account"]]
            },
            {
                title: "Borrower Declaration",
                fields: [["borrower_signature", "Borrower Signature"], ["signature_date", "Signature Date"], ["witness_name", "Witness Name"], ["witness_signature", "Witness Signature"], ["witness_date", "Witness Date"]],
                uploads: [["borrower_signature_image", "Borrower Signature Image"], ["witness_signature_image", "Witness Signature Image"]]
            },
            {
                title: "Supporting Documents for Pledge and Trust Receipt",
                fields: [],
                uploads: [["proof_of_ownership", "Proof of Ownership"], ["asset_photographs", "Asset Photographs"], ["inventory_lists", "Inventory Lists"], ["valuation_reports", "Valuation Reports"], ["vehicle_documents", "Vehicle Documents"], ["title_documents", "Title Documents"], ["other_collateral_evidence", "Other Collateral Evidence"]]
            }
        ]
    },
    field_visitation_report: {
        title: "Field Visitation Report",
        repeatLabel: "Visit Report",
        steps: [
            {
                title: "Visitation Details",
                fields: [["date_of_visitation", "Date of Visitation"], ["time_of_visitation", "Time of Visitation"], ["person_met", "Person Met"], ["relationship_to_applicant", "Relationship to Applicant"]],
                uploads: [["premises_photo", "Premises Photograph"], ["visiting_officer_signature_image", "Visiting Officer Signature Image"]]
            },
            {
                title: "Premises Description",
                fields: [["premises_description", "Premises Description"], ["building_type", "Building Type"], ["number_of_storeys", "Number of Storeys"], ["building_colour", "Building Colour"], ["landmark", "Landmark"], ["branch_direction_description", "Direction From Branch"]],
                uploads: [["address_evidence", "Address / Location Evidence"]]
            },
            {
                title: "Business Assessment",
                fields: [["nature_of_business", "Nature of Business"], ["business_condition", "Business Condition"], ["customer_traffic", "Customer Traffic"], ["stock_observation", "Stock Observation"], ["general_remarks", "General Remarks"]]
            },
            {
                title: "Officer Confirmation",
                fields: [["visiting_officer_name", "Visiting Officer Name"], ["visiting_officer_signature", "Visiting Officer Signature"], ["date", "Date"]],
                uploads: [["account_officer_signature_image", "Account Officer Signature Image"], ["final_signoff_signature_image", "Final Signoff Signature Image"]]
            }
        ]
    }
};

const repeatTabs = {
    loan_application: ["Loan Application"],
    guarantor: ["Guarantor 1"],
    pledge_trust_receipt: ["Pledge 1"],
    field_visitation_report: ["Visit Report 1"]
};

const guarantorOcrReview = {
    detectedForm: {
        status: "Recognised",
        keywords: ["GUARANTORS FORM", "MMFB/CRM/03", "Customer Details", "Guarantor Details", "Declaration", "Signature of Guarantor"]
    },
    fields: [
        ["customer_title", "Customer Title", "MR", 94, false],
        ["customer_name", "Customer Name", "Grace Omowunmi", 91, true],
        ["customer_address", "Customer Address", "12 Herbert Macaulay Way, Yaba", 76, true],
        ["customer_status_new_or_existing", "Customer Status", "New", 93, false],
        ["guarantor_full_name", "Guarantor Full Name", "Tunde Adewale", 88, true],
        ["relationship_to_client", "Relationship to Client", "Business associate", 82, true],
        ["means_of_id", "Means of ID", "National ID", 79, true],
        ["id_number", "ID Number", "NIN-22334455667", 74, true],
        ["id_date_issued", "ID Date Issued", "2023-01-12", 81, false],
        ["id_expiry_date", "ID Expiry Date", "2028-01-12", 81, true],
        ["telephone_number", "Telephone Number", "08055550101", 92, true],
        ["alternative_number", "Alternative Number", "", 35, false],
        ["bvn", "BVN", "22334455667", 84, true],
        ["date_of_birth", "Date of Birth", "1982-04-18", 78, false],
        ["state_of_origin", "State of Origin", "Ogun", 73, false],
        ["lga", "LGA", "Ijebu Ode", 71, false],
        ["education_level", "Education Level", "Graduate", 89, false],
        ["email_address", "Email Address", "tunde@example.com", 86, false],
        ["home_address", "Home Address", "18 Allen Avenue, Ikeja, Lagos", 69, true],
        ["has_loan_with_bank", "Has Loan With Bank", "No", 95, false],
        ["has_existing_guarantees", "Existing Guarantees", "Yes - one active guarantee", 66, false],
        ["marital_status", "Marital Status", "Married", 88, false],
        ["spouse_name", "Spouse Name", "Bisi Adewale", 77, false],
        ["spouse_telephone_number", "Spouse Telephone", "", 42, false],
        ["employment_type", "Employment Type", "Self Employed", 90, true],
        ["type_of_business", "Type of Business", "Wholesale provisions", 85, false],
        ["average_monthly_sales", "Average Monthly Sales", "NGN 1,800,000", 63, false],
        ["maximum_guarantee_amount_figures", "Maximum Guarantee Amount", "NGN 500,000", 68, true],
        ["maximum_guarantee_amount_words", "Maximum Guarantee Amount Words", "Five Hundred Thousand Naira Only", 72, true],
        ["guarantor_bank_name", "Recovery Bank Name", "GTBank", 87, true],
        ["guarantor_account_number", "Recovery Account Number", "0123000001", 79, true],
        ["guarantor_cheque_number", "Recovery Cheque Number", "000441", 64, true],
        ["guarantor_signature_detected", "Guarantor Signature", "Detected", 71, true],
        ["witness_signature_detected", "Witness Signature", "Unreadable", 38, true]
    ],
    pledgedItems: [
        ["1", "Shop stock", "STK-001", "Provisions and household goods", "NGN 750,000"]
    ],
    supportingDocs: [
        "Passport photograph",
        "Valid means of ID",
        "Proof of address",
        "Bank account statement",
        "BVN confirmation",
        "Business account statement",
        "Business registration document where applicable",
        "Evidence of business address",
        "Stock or sales evidence",
        "Proof of ownership",
        "Item photographs",
        "Valuation evidence"
    ],
    validationFlags: [
        "Maximum guarantee amount is critical and must be reviewed.",
        "Existing guarantees detected; flag for affordability/risk review.",
        "Spouse phone number is missing while marital status is Married.",
        "Average monthly sales confidence is below 70%.",
        "Witness signature is unreadable and must be corrected.",
        "Cheque number confidence is below 70%; verify recovery details."
    ],
    declarationSummary: [
        "The applicant is well known to the guarantor.",
        "The guarantor may be responsible if the applicant defaults.",
        "The guarantee may continue until the facility is fully repaid.",
        "The bank may recover outstanding amounts from the guarantor account.",
        "Cheque and pledged-item details may be used for recovery.",
        "The guarantor confirms supplied information is correct."
    ],
    officialUse: [
        ["date_of_visitation", "Date of Visitation", "Pending"],
        ["met_with", "Met With", "Pending"],
        ["premises_description", "Premises Description", "Pending"],
        ["visiting_officer_signature_detected", "Visiting Officer Signature", "Pending"],
        ["branch_manager_signoff_detected", "Final Signoff", "Pending"]
    ]
};

let activeForm = "loan_application";
let activeMode = "manual";
let activeStep = 0;
let activeRepeatIndex = 0;

document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    activeForm = forms[params.get("form")] ? params.get("form") : "loan_application";
    activeMode = params.get("mode") === "upload" ? "upload" : "manual";
    bindControls();
    render();
});

function bindControls() {
    document.querySelectorAll("[data-form]").forEach((button) => {
        button.addEventListener("click", () => {
            activeForm = button.dataset.form;
            activeStep = 0;
            activeRepeatIndex = 0;
            render();
        });
    });

    document.querySelectorAll("[data-mode]").forEach((button) => {
        button.addEventListener("click", () => {
            activeMode = button.dataset.mode;
            activeStep = 0;
            render();
        });
    });

    document.getElementById("addRepeatTabBtn").addEventListener("click", addRepeatTab);
    document.getElementById("prevStepBtn").addEventListener("click", previousStep);
    document.getElementById("nextStepBtn").addEventListener("click", nextStep);
    
    const selector = document.getElementById("appSelector");
    if (selector) {
        selector.addEventListener("change", () => {
            activeStep = 0;
            activeRepeatIndex = 0;
            render();
        });
    }
}

function addRepeatTab() {
    const label = forms[activeForm].repeatLabel;
    repeatTabs[activeForm].push(`${label} ${repeatTabs[activeForm].length + 1}`);
    activeRepeatIndex = repeatTabs[activeForm].length - 1;
    activeStep = 0;
    render();
}

function previousStep() {
    if (activeStep > 0) {
        activeStep -= 1;
        render();
    }
}

async function nextStep() {
    await saveCurrentFormData();
    
    const lastStep = forms[activeForm].steps.length - 1;
    if (activeStep < lastStep) {
        activeStep += 1;
        render();
        return;
    }

    const formIndex = formOrder.indexOf(activeForm);
    if (formIndex < formOrder.length - 1) {
        activeForm = formOrder[formIndex + 1];
        activeStep = 0;
        activeRepeatIndex = 0;
        render();
        return;
    }

    alert("All form sections have been saved and verified successfully.");
}

async function render() {
    const definition = forms[activeForm];
    const step = definition.steps[activeStep];
    document.getElementById("formPageTitle").textContent = `${definition.title} - ${repeatTabs[activeForm][activeRepeatIndex]}`;
    document.getElementById("formStepCount").textContent = `Step ${activeStep + 1} of ${definition.steps.length}: ${step.title}`;
    document.getElementById("formPageMode").textContent = activeMode === "upload" ? "Upload + OCR" : "Manual Entry";
    document.querySelectorAll("[data-form]").forEach((button) => button.classList.toggle("active", button.dataset.form === activeForm));
    document.querySelectorAll("[data-mode]").forEach((button) => button.classList.toggle("active", button.dataset.mode === activeMode));
    document.getElementById("prevStepBtn").disabled = activeStep === 0;
    document.getElementById("nextStepBtn").textContent = getNextButtonText();
    renderRepeatTabs();
    activeMode === "upload" ? renderUpload(step) : renderManual(step);
    renderAuditTrailInputs();
    
    await loadSavedFormData();
}

function getNextButtonText() {
    const lastStep = forms[activeForm].steps.length - 1;
    if (activeStep < lastStep) return "Next";
    const formIndex = formOrder.indexOf(activeForm);
    if (formIndex < formOrder.length - 1) return `Next Form: ${forms[formOrder[formIndex + 1]].title}`;
    return "Save All Forms";
}


function renderRepeatTabs() {
    let repeatRow = document.getElementById("repeatTabsRow");
    if (!repeatRow) {
        repeatRow = document.createElement("section");
        repeatRow.id = "repeatTabsRow";
        repeatRow.className = "repeat-tabs-row";
        document.querySelector(".form-page-panel").insertBefore(repeatRow, document.getElementById("formPageBody"));
    }
    repeatRow.replaceChildren();
    repeatTabs[activeForm].forEach((label, index) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = index === activeRepeatIndex ? "tab-btn active" : "tab-btn";
        button.textContent = label;
        button.addEventListener("click", () => {
            activeRepeatIndex = index;
            activeStep = 0;
            render();
        });
        repeatRow.appendChild(button);
    });
}

function renderManual(step) {
    const body = document.getElementById("formPageBody");
    body.replaceChildren();
    renderFields(body, step);
    if (step.table) renderScheduleTable(body, step.table);
    renderUploads(body, step.uploads || []);
}

function renderFields(container, step) {
    const grid = document.createElement("div");
    grid.className = "form-page-grid";
    step.fields.forEach(([name, label]) => {
        const field = document.createElement("label");
        field.textContent = label;
        const input = document.createElement(needsTextarea(name) ? "textarea" : "input");
        input.name = scopedName(name);
        field.appendChild(input);
        grid.appendChild(field);
    });
    container.appendChild(grid);
}

function renderScheduleTable(container, tableDefinition) {
    const section = document.createElement("section");
    section.className = "schedule-section";

    const header = document.createElement("div");
    header.className = "schedule-header";
    const title = document.createElement("h3");
    title.textContent = tableDefinition.title;
    const addButton = document.createElement("button");
    addButton.type = "button";
    addButton.className = "btn btn-secondary";
    addButton.textContent = "Add Row";
    addButton.addEventListener("click", () => addScheduleRow(table));
    header.appendChild(title);
    header.appendChild(addButton);
    section.appendChild(header);

    const table = document.createElement("table");
    table.className = "schedule-table";
    const thead = document.createElement("thead");
    const headRow = document.createElement("tr");
    tableDefinition.columns.forEach((column) => {
        const th = document.createElement("th");
        th.textContent = column;
        headRow.appendChild(th);
    });
    const actionHead = document.createElement("th");
    actionHead.textContent = "";
    headRow.appendChild(actionHead);
    thead.appendChild(headRow);
    table.appendChild(thead);

    const tbody = document.createElement("tbody");
    table.appendChild(tbody);
    section.appendChild(table);
    container.appendChild(section);
    addScheduleRow(table);
}

function addScheduleRow(table) {
    const columns = table.querySelectorAll("thead th").length - 1;
    const row = document.createElement("tr");
    for (let index = 0; index < columns; index += 1) {
        const cell = document.createElement("td");
        const input = document.createElement("input");
        input.type = index === columns - 1 ? "number" : "text";
        cell.appendChild(input);
        row.appendChild(cell);
    }
    const action = document.createElement("td");
    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "btn btn-secondary";
    remove.textContent = "Remove";
    remove.addEventListener("click", () => row.remove());
    action.appendChild(remove);
    row.appendChild(action);
    table.querySelector("tbody").appendChild(row);
}

function renderUploads(container, uploads) {
    if (!uploads.length) return;
    const section = document.createElement("section");
    section.className = "proof-upload-section";
    const heading = document.createElement("h3");
    heading.textContent = "Required uploads";
    section.appendChild(heading);
    const grid = document.createElement("div");
    grid.className = "proof-upload-grid";
    uploads.forEach(([name, label]) => {
        const field = document.createElement("label");
        field.textContent = label;
        const input = document.createElement("input");
        input.type = "file";
        input.name = scopedName(name);
        input.accept = ".pdf,.jpg,.jpeg,.png";
        field.appendChild(input);
        grid.appendChild(field);
    });
    section.appendChild(grid);
    container.appendChild(section);
}

function renderAuditTrailInputs() {
    const body = document.getElementById("formPageBody");
    const section = document.createElement("section");
    section.className = "audit-trail-section";
    const heading = document.createElement("h3");
    heading.textContent = "Audit Trail and Workflow Action";
    section.appendChild(heading);

    const grid = document.createElement("div");
    grid.className = "form-page-grid";
    [
        ["uploaded_document", "Uploaded Document Reference"],
        ["ocr_extraction_result", "OCR Extraction Result"],
        ["manual_corrections", "Manual Corrections"],
        ["approval_status", "Approval Status"],
        ["comments", "Comments"],
        ["workflow_action", "Workflow Action"]
    ].forEach(([name, label]) => {
        const field = document.createElement("label");
        field.textContent = label;
        const input = document.createElement(name.includes("comments") || name.includes("corrections") || name.includes("result") ? "textarea" : "input");
        input.name = scopedName(name);
        field.appendChild(input);
        grid.appendChild(field);
    });
    section.appendChild(grid);
    body.appendChild(section);
}

function renderUpload(step) {
    const body = document.getElementById("formPageBody");
    body.replaceChildren();
    const upload = document.createElement("div");
    upload.className = "form-upload-workspace";
    
    upload.innerHTML = `
        <div style="border: 2px dashed #4e4e6e; padding: 30px; border-radius: 8px; text-align: center; background: #1e1e2e; cursor: pointer; transition: border-color 0.2s;" id="dropZone">
            <span style="font-size: 2rem; display: block; margin-bottom: 10px;">📄</span>
            <strong>Drag & Drop or Click to upload completed ${activeForm.replace('_', ' ')} form</strong>
            <p style="color: #8f8f9e; font-size: 0.85rem; margin-top: 5px;">Accepts PDF, JPG, JPEG, PNG</p>
            <input type="file" id="ocrFileInput" accept=".pdf,.jpg,.jpeg,.png" style="display: none;">
            <div id="uploadStatusBlock" style="margin-top: 15px; display: none;">
                <span class="status-pill review" id="uploadStatusPill">Processing OCR...</span>
            </div>
        </div>
    `;
    body.appendChild(upload);

    const dropZone = upload.querySelector("#dropZone");
    const fileInput = upload.querySelector("#ocrFileInput");
    const statusBlock = upload.querySelector("#uploadStatusBlock");
    const statusPill = upload.querySelector("#uploadStatusPill");

    dropZone.addEventListener("click", () => fileInput.click());
    fileInput.addEventListener("change", async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        statusBlock.style.display = "block";
        statusPill.style.display = "inline-block";
        statusPill.textContent = "Processing OCR...";
        statusPill.className = "status-pill review";

        const appId = document.getElementById("appSelector")?.value;
        if (!appId) {
            statusPill.textContent = "Error: Select Target Application first";
            statusPill.className = "status-pill low";
            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        try {
            const response = await fetch(`/api/v1/applications/${appId}/forms/${activeForm}/ocr`, {
                method: "POST",
                body: formData
            });
            if (response.ok) {
                const result = await response.json();
                statusPill.textContent = `OCR Complete: ${result.detected_form} (Quality: ${result.image_quality})`;
                statusPill.className = "status-pill verified";
                
                renderOcrReviewWorkspace(body, result);
            } else {
                const err = await response.json();
                statusPill.textContent = `OCR Failed: ${err.detail || "Invalid file format"}`;
                statusPill.className = "status-pill low";
            }
        } catch (err) {
            statusPill.textContent = "Network Error during OCR simulation";
            statusPill.className = "status-pill low";
        }
    });

    if (activeForm === "guarantor") {
        renderGuarantorOcrReview(body);
        return;
    }
    renderFields(body, step);
    if (step.table) renderScheduleTable(body, step.table);
    renderUploads(body, step.uploads || []);
}


function renderGuarantorOcrReview(container) {
    renderFormDetection(container);
    renderOcrFields(container);
    renderPledgedItems(container);
    renderValidationFlags(container);
    renderSupportingDocs(container);
    renderDeclarationSummary(container);
    renderOfficialUse(container);
}

function renderFormDetection(container) {
    const section = createReviewSection("Guarantor Form Detection");
    const status = document.createElement("p");
    status.textContent = `Status: ${guarantorOcrReview.detectedForm.status}`;
    section.appendChild(status);
    const keywords = document.createElement("div");
    keywords.className = "ocr-chip-row";
    guarantorOcrReview.detectedForm.keywords.forEach((keyword) => {
        const chip = document.createElement("span");
        chip.className = "status-pill verified";
        chip.textContent = keyword;
        keywords.appendChild(chip);
    });
    section.appendChild(keywords);
    container.appendChild(section);
}

function renderOcrFields(container) {
    const section = createReviewSection("Extracted Guarantor Fields");
    const table = document.createElement("table");
    table.className = "schedule-table ocr-extraction-table";
    const thead = document.createElement("thead");
    const head = document.createElement("tr");
    ["Field", "Extracted Value", "Confidence", "Review"].forEach((label) => {
        const th = document.createElement("th");
        th.textContent = label;
        head.appendChild(th);
    });
    thead.appendChild(head);
    table.appendChild(thead);
    const tbody = document.createElement("tbody");
    guarantorOcrReview.fields.forEach(([name, label, value, confidence, critical]) => {
        const row = document.createElement("tr");
        row.className = confidence < 70 || critical ? "needs-review-row" : "";
        [label, value].forEach((text, index) => {
            const td = document.createElement("td");
            if (index === 1) {
                const input = document.createElement("input");
                input.name = scopedName(name);
                input.value = text;
                td.appendChild(input);
            } else {
                td.textContent = text;
            }
            row.appendChild(td);
        });
        const confidenceCell = document.createElement("td");
        confidenceCell.textContent = `${confidence}%`;
        row.appendChild(confidenceCell);
        const reviewCell = document.createElement("td");
        const badge = document.createElement("span");
        badge.className = confidence < 70 ? "status-pill low" : critical ? "status-pill review" : "status-pill verified";
        badge.textContent = confidence < 70 ? "Manual Review" : critical ? "Critical Review" : "Accepted";
        reviewCell.appendChild(badge);
        row.appendChild(reviewCell);
        tbody.appendChild(row);
    });
    table.appendChild(tbody);
    section.appendChild(table);
    container.appendChild(section);
}

function renderPledgedItems(container) {
    const section = createReviewSection("Pledged Items / Collateral Schedule");
    const table = document.createElement("table");
    table.className = "schedule-table";
    const head = document.createElement("tr");
    ["Item No", "Item Name", "Serial Number", "Description", "Estimated Value", ""].forEach((label) => {
        const th = document.createElement("th");
        th.textContent = label;
        head.appendChild(th);
    });
    const thead = document.createElement("thead");
    thead.appendChild(head);
    table.appendChild(thead);
    table.appendChild(document.createElement("tbody"));
    guarantorOcrReview.pledgedItems.forEach((rowValues) => addSpecificScheduleRow(table, rowValues));
    const add = document.createElement("button");
    add.className = "btn btn-secondary";
    add.type = "button";
    add.textContent = "Add Pledged Item Row";
    add.addEventListener("click", () => addSpecificScheduleRow(table, ["", "", "", "", ""]));
    section.appendChild(table);
    section.appendChild(add);
    container.appendChild(section);
}

function addSpecificScheduleRow(table, values) {
    const row = document.createElement("tr");
    values.forEach((value) => {
        const td = document.createElement("td");
        const input = document.createElement("input");
        input.value = value;
        td.appendChild(input);
        row.appendChild(td);
    });
    const action = document.createElement("td");
    const remove = document.createElement("button");
    remove.className = "btn btn-secondary";
    remove.type = "button";
    remove.textContent = "Remove";
    remove.addEventListener("click", () => row.remove());
    action.appendChild(remove);
    row.appendChild(action);
    table.querySelector("tbody").appendChild(row);
}

function renderValidationFlags(container) {
    const section = createReviewSection("Validation Flags");
    const list = document.createElement("ul");
    list.className = "review-list";
    guarantorOcrReview.validationFlags.forEach((flag) => {
        const item = document.createElement("li");
        item.textContent = flag;
        list.appendChild(item);
    });
    section.appendChild(list);
    container.appendChild(section);
}

function renderSupportingDocs(container) {
    const section = createReviewSection("Supporting Document Requests");
    const grid = document.createElement("div");
    grid.className = "proof-upload-grid";
    guarantorOcrReview.supportingDocs.forEach((label) => {
        const field = document.createElement("label");
        field.textContent = label;
        const input = document.createElement("input");
        input.type = "file";
        input.accept = ".pdf,.jpg,.jpeg,.png";
        field.appendChild(input);
        grid.appendChild(field);
    });
    section.appendChild(grid);
    container.appendChild(section);
}

function renderDeclarationSummary(container) {
    const section = createReviewSection("Plain-language Declaration Summary");
    const list = document.createElement("ol");
    list.className = "review-list";
    guarantorOcrReview.declarationSummary.forEach((text) => {
        const item = document.createElement("li");
        item.textContent = text;
        list.appendChild(item);
    });
    section.appendChild(list);
    container.appendChild(section);
}

function renderOfficialUse(container) {
    const section = createReviewSection("Official Use / Visitation Report");
    const grid = document.createElement("div");
    grid.className = "form-page-grid";
    guarantorOcrReview.officialUse.forEach(([name, label, value]) => {
        const field = document.createElement("label");
        field.textContent = label;
        const input = document.createElement("input");
        input.name = scopedName(name);
        input.value = value;
        field.appendChild(input);
        grid.appendChild(field);
    });
    section.appendChild(grid);
    container.appendChild(section);
}

function createReviewSection(title) {
    const section = document.createElement("section");
    section.className = "ocr-review-section";
    const heading = document.createElement("h3");
    heading.textContent = title;
    section.appendChild(heading);
    return section;
}

function scopedName(name) {
    return `${activeForm}_${activeRepeatIndex}_${activeStep}_${name}`;
}

function needsTextarea(name) {
    return ["purpose", "description", "address", "goods", "items", "asset"].some((part) => name.includes(part));
}

async function saveCurrentFormData() {
    const appId = document.getElementById("appSelector")?.value;
    if (!appId) return;

    const data = {};
    const inputs = document.getElementById("formPageBody").querySelectorAll("input, textarea, select");
    
    inputs.forEach(input => {
        if (!input.name || input.type === 'file') return;
        
        let source = input.dataset.source || "manual";
        let confidence = 100;
        
        if (activeMode === "upload") {
            source = "ocr";
            if (input.dataset.originalOcrValue !== undefined && input.value !== input.dataset.originalOcrValue) {
                source = "corrected";
            }
        }
        
        data[input.name] = {
            value: input.value,
            source: source,
            confidence: confidence
        };
    });

    const payload = {
        form_type: activeForm,
        data: data,
        source: activeMode === "upload" ? "ocr" : "manual",
        completed: true
    };

    try {
        await fetch(`/api/v1/applications/${appId}/forms/${activeForm}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });
    } catch (e) {
        console.error("Error saving form data:", e);
    }
}

async function loadSavedFormData() {
    const appId = document.getElementById("appSelector")?.value;
    if (!appId) return;

    try {
        const response = await fetch(`/api/v1/applications/${appId}/forms/${activeForm}`);
        if (response.ok) {
            const result = await response.json();
            const savedData = result.data || {};
            Object.keys(savedData).forEach(fieldName => {
                const input = document.querySelector(`[name="${fieldName}"]`);
                if (input) {
                    input.value = savedData[fieldName].value || "";
                    input.dataset.source = savedData[fieldName].source || "manual";
                }
            });
        }
    } catch (e) {
        console.warn("No existing saved form data found.");
    }
}

function renderOcrReviewWorkspace(container, ocrResult) {
    const detectionSec = createReviewSection("OCR Form Detection & Image Quality Status");
    detectionSec.innerHTML = `
        <div style="background: #1a1a26; padding: 15px; border-radius: 6px; border-left: 4px solid var(--accent-color); margin-bottom: 20px;">
            <p style="margin: 0; font-size: 0.95rem;">Detected Form: <strong>${ocrResult.detected_form}</strong></p>
            <p style="margin: 5px 0 0 0; font-size: 0.95rem;">Image Quality: <strong style="color: var(--accent-success);">${ocrResult.image_quality}</strong></p>
        </div>
    `;
    container.appendChild(detectionSec);

    const fieldsSec = createReviewSection("Extracted Fields & Manual Review/Correction");
    const table = document.createElement("table");
    table.className = "schedule-table ocr-extraction-table";
    table.innerHTML = `
        <thead>
            <tr>
                <th>Field</th>
                <th>Extracted & Prefilled Value</th>
                <th>Confidence</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody></tbody>
    `;
    const tbody = table.querySelector("tbody");
    
    ocrResult.fields.forEach(f => {
        const row = document.createElement("tr");
        const isLow = f.confidence < 70;
        if (isLow) row.className = "needs-review-row";
        
        row.innerHTML = `
            <td><strong>${f.label}</strong> ${f.critical ? '<span style="color: var(--accent-danger);">*</span>' : ''}</td>
            <td>
                <input type="text" name="${scopedName(f.name)}" value="${f.value}" 
                       data-original-ocr-value="${f.value}"
                       style="background: #1a1a26; color: var(--text-main); border: 1px solid ${isLow ? 'var(--accent-danger)' : '#3e3e5e'}; padding: 6px; border-radius: 4px; width: 100%;">
            </td>
            <td><strong style="color: ${isLow ? 'var(--accent-danger)' : 'var(--accent-success)'};">${f.confidence}%</strong></td>
            <td>
                <span class="status-pill ${isLow ? 'low' : 'verified'}">${isLow ? 'Low Confidence (Verify)' : 'Verified'}</span>
            </td>
        `;
        tbody.appendChild(row);
    });
    fieldsSec.appendChild(table);
    container.appendChild(fieldsSec);

    if (ocrResult.validation_flags && ocrResult.validation_flags.length > 0) {
        const flagsSec = createReviewSection("Extracted Verification Alerts");
        const alertBox = document.createElement("div");
        alertBox.style.cssText = "background: #2d1e24; border-left: 4px solid var(--accent-danger); padding: 15px; border-radius: 6px; margin-bottom: 20px;";
        const list = document.createElement("ul");
        list.style.cssText = "margin: 0; padding-left: 20px; color: var(--text-main);";
        ocrResult.validation_flags.forEach(flag => {
            const li = document.createElement("li");
            li.textContent = flag;
            list.appendChild(li);
        });
        alertBox.appendChild(list);
        flagsSec.appendChild(alertBox);
        container.appendChild(flagsSec);
    }
}

