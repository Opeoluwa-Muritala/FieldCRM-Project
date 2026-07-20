document.addEventListener("DOMContentLoaded", () => {
    initializeRoleShell();
    bindFormChoices();
    bindFormCards();
    bindTourStart();
    startGuideOnce();
    startBadgePolling();
    initDrawerBackdrop();
    initScrollReveals();
    initSidebarIcons();
    initRoleStatIcons();
    initSubmitFeedback();
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

function getFieldCrmIcon(value) {
    const normalized = (value || '').toLowerCase();
    if (normalized.includes('dashboard') || normalized.includes('control desk')) return 'icon-home-o';
    if (normalized.includes('disbursement') || normalized.includes('disbursed')) return 'icon-cash-o';
    if (normalized.includes('borrower') || normalized.includes('loan') || normalized.includes('portfolio') || normalized.includes('/reports/par')) return 'icon-loans-o';
    if (normalized.includes('pipeline')) return 'icon-pipeline-o';
    if (normalized.includes('awaiting')) return 'icon-awaiting-o';
    if (normalized.includes('signoff')) return 'icon-signoffs-o';
    if (normalized.includes('visit')) return 'icon-visits-o';
    if (normalized.includes('upload') || normalized.includes('document')) return 'icon-doc-upload-o';
    if (normalized.includes('ocr')) return 'icon-ocr-o';
    if (normalized.includes('flag')) return 'icon-flags-o';
    if (normalized.includes('policy') || normalized.includes('risk') || normalized.includes('alert') || normalized.includes('escalation')) return 'icon-flags-o';
    if (normalized.includes('audit')) return 'icon-audit-o';
    if (normalized.includes('workflow')) return 'icon-audit-o';
    if (normalized.includes('user')) return 'icon-users-o';
    if (normalized.includes('agent') || normalized.includes('ticket')) return 'icon-users-o';
    if (normalized.includes('activity')) return 'icon-activity-o';
    if (normalized.includes('turnaround') || normalized.includes('target')) return 'icon-activity-o';
    if (normalized.includes('new')) return 'icon-new-o';
    if (normalized.includes('approved') || normalized.includes('decision')) return 'icon-signoffs-o';
    if (normalized.includes('review')) return 'icon-reviews-o';
    if (normalized.includes('queue') || normalized.includes('application') || normalized.includes('draft') || normalized.includes('returned')) return 'icon-queue-o';
    return null;
}

function createFieldCrmIcon(iconId, className = 'app-icon') {
    const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    icon.classList.add(className);
    icon.setAttribute('viewBox', '0 0 24 24');
    icon.setAttribute('width', '17');
    icon.setAttribute('height', '17');
    icon.setAttribute('aria-hidden', 'true');
    icon.setAttribute('focusable', 'false');
    const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
    use.setAttribute('href', `/static/img/icons.svg#${iconId}`);
    icon.appendChild(use);
    return icon;
}

function initSidebarIcons() {
    document.querySelectorAll('.sidebar-nav-link[href]').forEach((link) => {
        if (link.querySelector('.app-icon')) return;
        const iconId = getFieldCrmIcon(link.getAttribute('href'));
        if (!iconId) return;
        link.insertBefore(createFieldCrmIcon(iconId), link.firstChild);
    });
}

function initRoleStatIcons() {
    document.querySelectorAll('.role-stat-grid .role-stat').forEach((stat) => {
        if (stat.querySelector('.role-stat-icon')) return;
        const label = stat.querySelector('span')?.textContent || '';
        const iconId = getFieldCrmIcon(label);
        if (!iconId) return;
        stat.appendChild(createFieldCrmIcon(iconId, 'role-stat-icon'));
    });
}

function initSubmitFeedback() {
    document.querySelectorAll('form[data-submit-feedback]').forEach((form) => {
        form.addEventListener('submit', () => {
            if (!form.checkValidity()) return;
            const button = form.querySelector('button[type="submit"]');
            if (!button) return;
            form.classList.add('is-submitting');
            button.classList.add('is-loading');
            button.setAttribute('aria-busy', 'true');
            button.disabled = true;
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
    if (shell && shell.dataset.userRole) return shell.dataset.userRole;

    const role = document.body?.dataset.role || "loan_officer";
    return role
        .split("_")
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ");
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
    const status = document.getElementById("dashboardInlineStatus") || document.createElement("div");
    status.id = "dashboardInlineStatus";
    status.className = "inline-empty-state";
    status.textContent = `${definition.title} draft saved locally for ${activeFormMode === "upload" ? "OCR review" : "manual entry"}.`;
    document.getElementById("inlineFormWorkspace")?.appendChild(status);
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
    return;
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

function initializeRoleShell() {
    const role = document.body?.dataset.role;
    if (!role) return;
    document.documentElement.dataset.role = role;
}

/* === PHASE 5: RIPPLE, FORM INTERACTIONS, DRAWER BACKDROP === */

function initRipples() {
    document.querySelectorAll('.btn-primary-filled').forEach(btn => {
        if (btn.dataset.rippleInit) return;
        btn.dataset.rippleInit = '1';
        btn.addEventListener('pointerdown', function(e) {
            const rect = btn.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height) * 2;
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top  - size / 2;
            const circle = document.createElement('span');
            circle.className = 'ripple-circle';
            circle.style.cssText = `width:${size}px;height:${size}px;left:${x}px;top:${y}px`;
            btn.appendChild(circle);
            circle.addEventListener('animationend', () => circle.remove());
        });
    });
}

function setButtonLoading(btn, isLoading) {
    btn.classList.toggle('is-loading', isLoading);
    btn.disabled = isLoading;
}
window.setButtonLoading = setButtonLoading;

function markFieldError(inputEl) {
    inputEl.classList.remove('field-error');
    void inputEl.offsetWidth;
    inputEl.classList.add('field-error');
    inputEl.addEventListener('input', () => inputEl.classList.remove('field-error'), { once: true });
}
window.markFieldError = markFieldError;

function initDrawerBackdrop() {
    const drawers = document.querySelectorAll('.detail-drawer');
    if (!drawers.length) return;

    let backdrop = document.querySelector('.detail-drawer-backdrop');
    if (!backdrop) {
        backdrop = document.createElement('div');
        backdrop.className = 'detail-drawer-backdrop';
        document.body.appendChild(backdrop);
    }

    drawers.forEach(drawer => {
        const observer = new MutationObserver(() => {
            const isOpen = drawer.classList.contains('open');
            backdrop.classList.toggle('visible', isOpen);
        });
        observer.observe(drawer, { attributes: true, attributeFilter: ['class'] });
    });

    backdrop.addEventListener('click', () => {
        document.querySelectorAll('.detail-drawer.open').forEach(d => d.classList.remove('open'));
    });
}

/* === PHASE 6: SCROLL REVEALS === */

function initScrollReveals() {
    if (!('IntersectionObserver' in window)) return;

    const revealTargets = document.querySelectorAll(
        '.role-list-card, .role-stage-card, .pipeline-card, .queue-card, ' +
        '.lo-task-card, .lo-stat-chip, .lo-activity-item, ' +
        '.flat-card, .role-pipeline-row'
    );
    if (!revealTargets.length) return;

    revealTargets.forEach(el => el.classList.add('reveal-pending'));

    /* Stagger siblings within list containers */
    document.querySelectorAll(
        '.role-card-list, .role-pipeline-list, .pipeline-list, ' +
        '.lo-task-list, .queue-cards, .lo-activity-list'
    ).forEach(container => {
        container.querySelectorAll('.reveal-pending').forEach((child, index) => {
            child.style.transitionDelay = `${Math.min(index, 7) * 35}ms`;
        });
    });

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.remove('reveal-pending');
                entry.target.classList.add('reveal-visible');
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.12, rootMargin: '0px 0px -32px 0px' });

    revealTargets.forEach(el => observer.observe(el));
}

/* === PHASE 7: SKELETON HELPERS === */

function createSkeletonCard() {
    const card = document.createElement('div');
    card.className = 'metric-card';
    card.innerHTML = `
        <div class="skeleton skeleton-text" style="width:55%"></div>
        <div class="skeleton skeleton-title"></div>
    `;
    return card;
}

function createSkeletonRows(count = 5) {
    const fragment = document.createDocumentFragment();
    for (let i = 0; i < count; i++) {
        const row = document.createElement('div');
        row.className = 'skeleton skeleton-row';
        fragment.appendChild(row);
    }
    return fragment;
}
window.createSkeletonCard = createSkeletonCard;
window.createSkeletonRows = createSkeletonRows;

function startBadgePolling() {
    const badgeTargets = document.querySelectorAll("[data-badge-url]");
    if (!badgeTargets.length) return;

    const poll = async () => {
        await Promise.all(Array.from(badgeTargets).map(async (target) => {
            try {
                const response = await fetch(target.dataset.badgeUrl, {
                    headers: { "Accept": "application/json" },
                    credentials: "same-origin"
                });
                if (!response.ok) return;
                const payload = await response.json();
                const key = target.dataset.badgeKey || "count";
                const value = payload[key] ?? 0;
                target.textContent = value;
                target.hidden = Number(value) <= 0;
            } catch {
                target.hidden = target.hidden;
            }
        }));
    };

    poll();
    window.setInterval(poll, 60000);
}
