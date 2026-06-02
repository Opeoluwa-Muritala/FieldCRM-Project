document.addEventListener("DOMContentLoaded", () => {
    bindFormChoices();
    bindFormCards();
    bindTourStart();
    startGuideOnce();
});

let currentTourStep = 0;
let activeTourTarget = null;
let activeTourSteps = [];
let activeFormType = "loan_application";
let activeFormMode = "manual";

const formDefinitions = {
    loan_application: {
        title: "Loan Application Form",
        sections: [
            {
                title: "Applicant and Identity",
                fields: [
                    ["full_name", "text", "Full Name"],
                    ["phone", "tel", "Phone Number"],
                    ["bvn", "text", "BVN"],
                    ["means_of_id", "text", "Means of ID"],
                    ["id_number", "text", "ID Number"],
                    ["marital_status", "select", "Marital Status", ["Single", "Married", "Separated", "Divorced"]]
                ]
            },
            {
                title: "Loan and Security",
                fields: [
                    ["loan_purpose", "textarea", "Loan Purpose"],
                    ["loan_amount", "number", "Loan Amount"],
                    ["loan_tenor", "text", "Loan Tenor"],
                    ["collateral_description", "textarea", "Collateral / Security"],
                    ["repayment_method", "select", "Repayment Method", ["Cheque", "Standing Order", "Direct Debit", "Cash Deposit"]],
                    ["disbursement_account", "text", "Disbursement Account Number"]
                ]
            },
            {
                title: "Consents and Signature",
                fields: [
                    ["credit_bureau_consent", "checkbox", "Credit bureau consent accepted"],
                    ["cheque_authorization", "checkbox", "Cheque authorization accepted"],
                    ["gsi_mandate", "checkbox", "GSI mandate accepted"],
                    ["applicant_signature", "text", "Applicant Signature"],
                    ["signature_date", "date", "Date"]
                ]
            }
        ]
    },
    guarantor: {
        title: "Guarantors Form",
        sections: [
            {
                title: "Guarantor Identity",
                fields: [
                    ["guarantor_name", "text", "Guarantor Full Name"],
                    ["relationship", "text", "Relationship to Client"],
                    ["phone", "tel", "Phone Number"],
                    ["bvn", "text", "BVN"],
                    ["date_of_birth", "date", "Date of Birth"],
                    ["home_address", "textarea", "Home Address"]
                ]
            },
            {
                title: "Employment and Guarantee",
                fields: [
                    ["employment_status", "select", "Employment Status", ["Employed", "Self Employed", "Public Servant", "Other"]],
                    ["employer_or_business", "text", "Employer / Business Name"],
                    ["maximum_guarantee", "number", "Maximum Guarantee Amount"],
                    ["bank_account", "text", "Bank Account Number"],
                    ["cheque_number", "text", "Cheque Number"],
                    ["pledged_items", "textarea", "Pledged Items"]
                ]
            },
            {
                title: "Declaration and Witness",
                fields: [
                    ["declaration_accepted", "checkbox", "Declaration accepted"],
                    ["guarantor_signature", "text", "Guarantor Signature"],
                    ["witness_signature", "text", "Witness Signature"],
                    ["witness_date", "date", "Witness Date"]
                ]
            }
        ]
    },
    pledge_trust_receipt: {
        title: "Pledge and Trust Receipt",
        sections: [
            {
                title: "Facility and Pledger",
                fields: [
                    ["receipt_date", "date", "Date"],
                    ["borrower_or_association", "text", "Borrower / Association Name"],
                    ["facility_amount_figures", "number", "Facility Amount in Figures"],
                    ["facility_amount_words", "text", "Facility Amount in Words"],
                    ["shop_or_house_address", "textarea", "Shop / House Address"],
                    ["obligor_name", "text", "Obligor Name"]
                ]
            },
            {
                title: "Pledged Asset Schedule",
                fields: [
                    ["pledged_goods", "textarea", "Pledged Goods / Stock / Assets"],
                    ["quantity", "text", "Quantity"],
                    ["description", "textarea", "Description"],
                    ["estimated_value", "number", "Estimated Value"]
                ]
            },
            {
                title: "Execution and Witness",
                fields: [
                    ["borrower_signature", "text", "Borrower Signature"],
                    ["witness_signature", "text", "Witness Signature"],
                    ["witness_name", "text", "Witness Name"],
                    ["witness_address", "textarea", "Witness Address"],
                    ["witness_occupation", "text", "Witness Occupation"]
                ]
            }
        ]
    }
};

const roleGuideSteps = {
    "Loan Officer": [
        {
            targetId: "formsTitle",
            title: "Start with loan forms",
            desc: "Capture the Loan Application, Guarantors Form, and Pledge and Trust Receipt by filling in-app or uploading completed forms."
        },
        {
            targetId: "loanApplicationCard",
            title: "Choose capture method",
            desc: "Use Fill Form in App for structured entry or Upload Completed Form when OCR should extract handwritten or scanned values."
        },
        {
            targetId: "ocrTitle",
            title: "Correct OCR before submission",
            desc: "Low-confidence values and missing signatures must be corrected before sending the file forward."
        },
        {
            targetId: "verificationTitle",
            title: "Complete visitation details",
            desc: "Record visit facts, premises details, landmark, and officer signature before the application leaves your queue."
        }
    ],
    "Branch Manager": [
        {
            targetId: "roleQueueTitle",
            title: "Review assigned approvals",
            desc: "Your queue shows only applications waiting for your approval or return decision."
        },
        {
            targetId: "docsTitle",
            title: "Check document exceptions",
            desc: "Review missing or inconsistent supporting documents before signoff."
        },
        {
            targetId: "verificationTitle",
            title: "Sign off verification",
            desc: "Confirm visitation details and final signoff before moving the loan to the next step."
        },
        {
            targetId: "readinessTitle",
            title: "Watch approval gates",
            desc: "Locked readiness items show what must be completed before final approval or release."
        }
    ],
    "Credit Officer": [
        {
            targetId: "roleQueueTitle",
            title: "Review credit queue",
            desc: "Your queue focuses on credit risk, guarantor strength, loan terms, and collateral review."
        },
        {
            targetId: "ocrTitle",
            title: "Inspect critical values",
            desc: "Validate extracted loan amount, tenor, BVN, maximum guarantee amount, collateral, cheque, and account values."
        },
        {
            targetId: "docsTitle",
            title: "Validate evidence",
            desc: "Confirm business, employment, collateral, stock, property, vehicle, or equipment evidence based on the application."
        },
        {
            targetId: "readinessTitle",
            title: "Clear risk blockers",
            desc: "Use readiness states to identify missing documents, low-confidence values, and incomplete confirmations."
        }
    ],
    "Auditor": [
        {
            targetId: "roleQueueTitle",
            title: "Review compliance queue",
            desc: "Your queue shows files assigned to you for compliance review and return decisions."
        },
        {
            targetId: "ocrTitle",
            title: "Confirm extracted evidence",
            desc: "Check OCR corrections and critical fields before marking compliance items as verified."
        },
        {
            targetId: "docsTitle",
            title: "Verify required documents",
            desc: "Confirm IDs, statements, proof of address, ownership evidence, valuation, and mandate documents."
        },
        {
            targetId: "readinessTitle",
            title: "Check approval lock",
            desc: "The file should remain blocked until signatures, declarations, consents, and verification are complete."
        }
    ],
    "System Admin": [
        {
            targetId: "roleQueueTitle",
            title: "Monitor final queues",
            desc: "Your workspace focuses on final approval readiness, committee decisions, disbursement readiness, and escalations."
        },
        {
            targetId: "readinessTitle",
            title: "Control final approval",
            desc: "Final approval should stay locked until every required gate is complete."
        },
        {
            targetId: "docsTitle",
            title: "Review document gaps",
            desc: "Use document groups to identify unresolved evidence gaps before release."
        },
        {
            targetId: "navAudit",
            title: "Open system audit",
            desc: "The audit view is reserved for the system admin and contains immutable workflow events."
        }
    ]
};

function bindFormChoices() {
    if (getCurrentRole() !== "Loan Officer") return;

    const buttons = document.querySelectorAll("[data-form-choice]");
    buttons.forEach((button) => {
        button.addEventListener("click", () => {
            const group = button.closest(".choice-row");
            if (!group) return;

            group.querySelectorAll(".choice-btn").forEach((item) => {
                item.classList.remove("active");
            });
            button.classList.add("active");

            const card = button.closest(".form-card");
            if (!card) return;

            const mode = button.dataset.formChoice;
            const status = card.querySelector(".status-pill");
            if (!status) return;

            status.className = "status-pill";
            if (mode === "upload") {
                status.classList.add("low");
                status.textContent = "OCR Review";
            } else {
                status.classList.add("review");
                status.textContent = "Needs Review";
            }

            openFormWorkspace(card.dataset.formType || "loan_application", mode);
        });
    });
}

function bindFormCards() {
    if (getCurrentRole() !== "Loan Officer") return;

    document.querySelectorAll(".form-card[data-form-type]").forEach((card) => {
        card.addEventListener("click", (event) => {
            if (event.target.closest("button")) return;
            const activeChoice = card.querySelector(".choice-btn.active");
            const mode = activeChoice ? activeChoice.dataset.formChoice : "manual";
            openFormWorkspace(card.dataset.formType || "loan_application", mode);
        });
    });
}

function bindTourStart() {
    window.startTrainingTour = startTrainingTour;
    window.openFirstForm = openFirstForm;
    window.saveFormDraft = saveFormDraft;
}

function startGuideOnce() {
    const storageKey = getUsageGuideStorageKey();
    if (!localStorage.getItem(storageKey)) {
        setTimeout(() => {
            startTrainingTour();
        }, 600);
    }
}

function getCurrentRole() {
    const shell = document.querySelector(".app-shell");
    return shell ? shell.dataset.userRole || "Loan Officer" : "Loan Officer";
}

function getUsageGuideStorageKey() {
    return `fieldcrm_usage_guide_seen_${getCurrentRole().replace(/\s+/g, "_").toLowerCase()}`;
}

function openFirstForm() {
    if (getCurrentRole() !== "Loan Officer") return;

    const target = document.getElementById("loanApplicationCard");
    if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "center" });
        target.classList.add("focus-pulse");
        setTimeout(() => target.classList.remove("focus-pulse"), 700);
        openFormWorkspace("loan_application", "manual");
    }
}

function openFormWorkspace(formType, mode) {
    if (getCurrentRole() !== "Loan Officer") return;

    activeFormType = formType;
    activeFormMode = mode;
    const definition = formDefinitions[formType] || formDefinitions.loan_application;
    const workspace = document.getElementById("inlineFormWorkspace");
    if (!workspace) return;

    workspace.replaceChildren();
    workspace.classList.add("active");

    const header = document.createElement("div");
    header.className = "inline-form-header";
    const headingGroup = document.createElement("div");
    const modeLabel = document.createElement("span");
    modeLabel.className = "section-kicker";
    modeLabel.textContent = mode === "upload" ? "Upload completed form" : "Fill form in app";
    const title = document.createElement("h2");
    title.textContent = definition.title;
    headingGroup.appendChild(modeLabel);
    headingGroup.appendChild(title);
    const actions = document.createElement("div");
    actions.className = "inline-form-actions";
    const closeBtn = document.createElement("button");
    closeBtn.className = "btn btn-secondary";
    closeBtn.type = "button";
    closeBtn.textContent = "Close";
    closeBtn.addEventListener("click", closeFormWorkspace);
    const saveBtn = document.createElement("button");
    saveBtn.className = "btn btn-primary";
    saveBtn.type = "button";
    saveBtn.textContent = "Save Draft";
    saveBtn.addEventListener("click", saveFormDraft);
    actions.appendChild(closeBtn);
    actions.appendChild(saveBtn);
    header.appendChild(headingGroup);
    header.appendChild(actions);
    workspace.appendChild(header);

    const body = document.createElement("div");
    workspace.appendChild(body);

    if (mode === "upload") {
        renderUploadMode(body, definition);
    } else {
        renderManualMode(body, definition);
    }

    workspace.scrollIntoView({ behavior: "smooth", block: "start" });
}

function closeFormWorkspace() {
    const workspace = document.getElementById("inlineFormWorkspace");
    if (!workspace) return;
    workspace.classList.remove("active");
    workspace.replaceChildren();
    const empty = document.createElement("div");
    empty.className = "inline-empty-state";
    empty.textContent = "Select a form action to open the digital entry or upload workspace here.";
    workspace.appendChild(empty);
}

function saveFormDraft() {
    if (getCurrentRole() !== "Loan Officer") return;

    const definition = formDefinitions[activeFormType] || formDefinitions.loan_application;
    alert(`${definition.title} draft saved locally for ${activeFormMode === "upload" ? "OCR review" : "manual entry"}.`);
    closeFormWorkspace();
}

function renderManualMode(container, definition) {
    const form = document.createElement("form");
    form.className = "generated-form";
    definition.sections.forEach((section) => {
        const sectionEl = document.createElement("section");
        sectionEl.className = "generated-section";

        const heading = document.createElement("h3");
        heading.textContent = section.title;
        sectionEl.appendChild(heading);

        const grid = document.createElement("div");
        grid.className = "generated-grid";
        section.fields.forEach((field) => grid.appendChild(createGeneratedField(field)));
        sectionEl.appendChild(grid);
        form.appendChild(sectionEl);
    });
    container.appendChild(form);
}

function createGeneratedField(field) {
    const [name, type, label, options] = field;
    const group = document.createElement("div");
    group.className = type === "textarea" ? "generated-field wide" : "generated-field";

    if (type === "checkbox") {
        const wrapper = document.createElement("label");
        const input = document.createElement("input");
        input.type = "checkbox";
        input.name = name;
        wrapper.appendChild(input);
        wrapper.appendChild(document.createTextNode(` ${label}`));
        group.appendChild(wrapper);
        return group;
    }

    const labelEl = document.createElement("label");
    labelEl.textContent = label;
    labelEl.setAttribute("for", `field_${name}`);
    group.appendChild(labelEl);

    let input;
    if (type === "select") {
        input = document.createElement("select");
        const empty = document.createElement("option");
        empty.textContent = "Select";
        empty.value = "";
        input.appendChild(empty);
        options.forEach((optionText) => {
            const option = document.createElement("option");
            option.value = optionText;
            option.textContent = optionText;
            input.appendChild(option);
        });
    } else if (type === "textarea") {
        input = document.createElement("textarea");
        input.rows = 3;
    } else {
        input = document.createElement("input");
        input.type = type;
    }
    input.id = `field_${name}`;
    input.name = name;
    group.appendChild(input);
    return group;
}

function renderUploadMode(container, definition) {
    const panel = document.createElement("div");
    panel.className = "upload-panel";

    const dropzone = document.createElement("div");
    dropzone.className = "upload-dropzone";
    const title = document.createElement("strong");
    title.textContent = `Upload completed ${definition.title}`;
    const hint = document.createElement("span");
    hint.textContent = "Accepted: PDF, JPG, JPEG, PNG. OCR values must be reviewed before submission.";
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".pdf,.jpg,.jpeg,.png";
    dropzone.appendChild(title);
    dropzone.appendChild(hint);
    dropzone.appendChild(input);
    panel.appendChild(dropzone);

    const heading = document.createElement("h3");
    heading.textContent = "OCR Review Preview";
    panel.appendChild(heading);

    const list = document.createElement("div");
    list.className = "ocr-review-list";
    [
        ["Applicant / Guarantor Name", "Grace Omowunmi", "96%"],
        ["BVN", "222***4455", "88%"],
        ["Loan / Guarantee Amount", "NGN 500,000?", "61%"],
        ["Signature", "Unreadable", "22%"]
    ].forEach(([label, value, confidence]) => {
        const row = document.createElement("div");
        row.className = "ocr-review-item";
        const labelEl = document.createElement("strong");
        labelEl.textContent = label;
        const valueInput = document.createElement("input");
        valueInput.value = value;
        const confidenceEl = document.createElement("span");
        confidenceEl.className = Number(confidence.replace("%", "")) < 70 ? "status-pill low" : "status-pill verified";
        confidenceEl.textContent = confidence;
        row.appendChild(labelEl);
        row.appendChild(valueInput);
        row.appendChild(confidenceEl);
        list.appendChild(row);
    });
    panel.appendChild(list);
    container.appendChild(panel);
}

function startTrainingTour() {
    currentTourStep = 0;
    activeTourSteps = getAvailableTourSteps();
    if (activeTourSteps.length === 0) return;

    let overlay = document.getElementById("tourOverlay");
    if (!overlay) {
        overlay = document.createElement("div");
        overlay.id = "tourOverlay";
        overlay.className = "tour-overlay";
        overlay.addEventListener("click", handleOverlayGuideClick);

        const tooltip = document.createElement("div");
        tooltip.id = "tourTooltip";
        tooltip.className = "tour-tooltip";
        tooltip.addEventListener("click", (event) => {
            event.stopPropagation();
        });

        const indicator = document.createElement("div");
        indicator.id = "tourIndicator";
        indicator.className = "tour-step-indicator";

        const title = document.createElement("div");
        title.id = "tourTitle";
        title.className = "tour-step-title";

        const desc = document.createElement("div");
        desc.id = "tourDesc";
        desc.className = "tour-step-desc";

        const footer = document.createElement("div");
        footer.className = "btn-group";

        const prevBtn = document.createElement("button");
        prevBtn.id = "tourPrevBtn";
        prevBtn.className = "btn btn-secondary";
        prevBtn.type = "button";
        prevBtn.textContent = "Back";
        prevBtn.addEventListener("click", previousTourStep);

        const nextBtn = document.createElement("button");
        nextBtn.id = "tourNextBtn";
        nextBtn.className = "btn btn-primary";
        nextBtn.type = "button";
        nextBtn.textContent = "Next";
        nextBtn.addEventListener("click", nextTourStep);

        footer.appendChild(prevBtn);
        footer.appendChild(nextBtn);
        tooltip.appendChild(indicator);
        tooltip.appendChild(title);
        tooltip.appendChild(desc);
        tooltip.appendChild(footer);
        overlay.appendChild(tooltip);
        document.body.appendChild(overlay);
    }

    overlay.style.display = "block";
    showTourStep(0);
}

function showTourStep(stepIndex) {
    const step = activeTourSteps[stepIndex];
    const targetElement = document.getElementById(step.targetId);
    const tooltip = document.getElementById("tourTooltip");

    if (!targetElement || !tooltip) {
        nextTourStep();
        return;
    }

    document.getElementById("tourIndicator").textContent = `Step ${stepIndex + 1} of ${activeTourSteps.length}`;
    document.getElementById("tourTitle").textContent = step.title;
    document.getElementById("tourDesc").textContent = step.desc;

    if (activeTourTarget) {
        activeTourTarget.classList.remove("tour-highlight");
    }
    activeTourTarget = targetElement;
    activeTourTarget.classList.add("tour-highlight");

    const prevBtn = document.getElementById("tourPrevBtn");
    const nextBtn = document.getElementById("tourNextBtn");
    prevBtn.style.visibility = stepIndex === 0 ? "hidden" : "visible";
    nextBtn.textContent = stepIndex === activeTourSteps.length - 1 ? "Finish" : "Next";

    const rect = targetElement.getBoundingClientRect();
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;

    let top = rect.bottom + scrollTop + 12;
    let left = rect.left + scrollLeft;

    if (left + 340 > window.innerWidth) {
        left = window.innerWidth - 356;
    }
    if (left < 16) left = 16;

    tooltip.style.top = `${top}px`;
    tooltip.style.left = `${left}px`;
    targetElement.scrollIntoView({ behavior: "smooth", block: "center" });
}

function nextTourStep() {
    if (currentTourStep < activeTourSteps.length - 1) {
        currentTourStep += 1;
        showTourStep(currentTourStep);
        return;
    }
    completeTour();
}

function previousTourStep() {
    if (currentTourStep > 0) {
        currentTourStep -= 1;
        showTourStep(currentTourStep);
    }
}

function completeTour() {
    const overlay = document.getElementById("tourOverlay");
    if (overlay) {
        overlay.style.display = "none";
    }
    if (activeTourTarget) {
        activeTourTarget.classList.remove("tour-highlight");
        activeTourTarget = null;
    }
    localStorage.setItem(getUsageGuideStorageKey(), "true");
}

function getAvailableTourSteps() {
    const role = getCurrentRole();
    const steps = roleGuideSteps[role] || roleGuideSteps["Loan Officer"];
    return steps.filter((step) => document.getElementById(step.targetId));
}

function handleOverlayGuideClick() {
    nextTourStep();
}
