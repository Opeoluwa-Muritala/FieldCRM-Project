(function () {
    const autosaveKeyPrefix = "fieldcrm_draft_";
    let autosaveTimer = 0;

    document.addEventListener("DOMContentLoaded", () => {
        setActiveTab();
        bindOfflineBanner();
        bindPullToRefresh();
        bindAutosave();
        bindFab();
        bindBottomSheets();
        bindSwipeCards();
        bindVisitSurvey();
    });

    function setActiveTab() {
        const path = window.location.pathname;
        document.querySelectorAll(".tab-item").forEach((item) => item.classList.remove("active"));
        const label = path.includes("/applications/new")
            ? "New Application"
            : path.includes("/applications")
                ? "My Queue"
                : "Home";
        const target = Array.from(document.querySelectorAll(".tab-item")).find((item) => {
            return item.getAttribute("aria-label") === label;
        });
        if (target) target.classList.add("active");
    }

    function bindOfflineBanner() {
        const banner = document.getElementById("offlineBanner");
        if (!banner) return;
        const sync = () => {
            banner.style.display = navigator.onLine ? "none" : "block";
        };
        window.addEventListener("online", sync);
        window.addEventListener("offline", sync);
        sync();
    }

    function bindPullToRefresh() {
        const content = document.getElementById("mobileContent");
        const sentinel = document.getElementById("ptrSentinel");
        if (!content || !sentinel || !("IntersectionObserver" in window)) return;
        let armed = false;
        content.addEventListener("scroll", () => {
            armed = content.scrollTop <= -48;
        }, { passive: true });
        const observer = new IntersectionObserver((entries) => {
            if (armed && entries.some((entry) => entry.isIntersecting)) {
                window.location.reload();
            }
        }, { root: content, threshold: 1 });
        observer.observe(sentinel);
    }

    function bindAutosave() {
        document.querySelectorAll("form[data-autosave], .mobile-content form").forEach((form) => {
            const key = autosaveKeyPrefix + (form.id || window.location.pathname);
            restoreForm(form, key);
            form.addEventListener("input", () => {
                window.clearTimeout(autosaveTimer);
                autosaveTimer = window.setTimeout(() => saveForm(form, key), 300);
            });
            form.addEventListener("submit", () => localStorage.removeItem(key));
        });
    }

    function saveForm(form, key) {
        const values = {};
        new FormData(form).forEach((value, name) => {
            values[name] = value;
        });
        localStorage.setItem(key, JSON.stringify(values));
    }

    function restoreForm(form, key) {
        const raw = localStorage.getItem(key);
        if (!raw) return;
        try {
            const values = JSON.parse(raw);
            Object.keys(values).forEach((name) => {
                const field = form.elements.namedItem(name);
                if (!field || field.type === "file") return;
                if (field.type === "checkbox") field.checked = values[name] === "on";
                else field.value = values[name];
            });
        } catch {
            localStorage.removeItem(key);
        }
    }

    function bindFab() {
        document.querySelectorAll(".fab").forEach((fab) => {
            fab.addEventListener("click", () => fab.classList.add("active"), { once: true });
        });
    }

    function bindBottomSheets() {
        document.querySelectorAll("[data-sheet-target]").forEach((trigger) => {
            trigger.addEventListener("click", () => {
                document.querySelector(trigger.dataset.sheetTarget)?.classList.add("open");
            });
        });
        document.querySelectorAll("[data-sheet-close]").forEach((trigger) => {
            trigger.addEventListener("click", () => {
                trigger.closest(".bottom-sheet")?.classList.remove("open");
            });
        });
    }

    function bindSwipeCards() {
        document.querySelectorAll("[data-swipe='true']").forEach((card) => {
            const inner = card.querySelector(".lo-task-card-inner");
            if (!inner) return;
            let startX = 0;
            let currentX = 0;
            card.addEventListener("touchstart", (event) => {
                startX = event.touches[0].clientX;
                currentX = startX;
            }, { passive: true });
            card.addEventListener("touchmove", (event) => {
                currentX = event.touches[0].clientX;
                const delta = Math.max(-72, Math.min(72, currentX - startX));
                inner.style.transform = `translateX(${delta}px)`;
            }, { passive: true });
            card.addEventListener("touchend", () => {
                const delta = currentX - startX;
                inner.style.transform = "";
                if (Math.abs(delta) > 56) {
                    const loanId = card.dataset.loanId;
                    if (loanId) window.location.href = `/applications/${loanId}`;
                }
            });
        });
    }

    function bindVisitSurvey() {
        document.querySelectorAll(".visit-survey").forEach((survey) => {
            const steps = Array.from(survey.querySelectorAll(".survey-step"));
            let index = steps.findIndex((step) => step.classList.contains("active"));
            if (index < 0) index = 0;
            showStep(steps, index);
            survey.querySelectorAll("[data-survey-next]").forEach((button) => {
                button.addEventListener("click", () => {
                    index = Math.min(index + 1, steps.length - 1);
                    showStep(steps, index);
                });
            });
            survey.querySelectorAll("[data-survey-prev]").forEach((button) => {
                button.addEventListener("click", () => {
                    index = Math.max(index - 1, 0);
                    showStep(steps, index);
                });
            });
        });
    }

    function showStep(steps, activeIndex) {
        steps.forEach((step, index) => step.classList.toggle("active", index === activeIndex));
        const progress = document.querySelector(".mobile-wizard-progress > span");
        if (progress && steps.length) {
            progress.style.setProperty("--progress", `${((activeIndex + 1) / steps.length) * 100}%`);
        }
    }
})();
