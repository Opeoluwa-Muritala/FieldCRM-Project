(() => {
    "use strict";

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const targets = document.querySelectorAll([
        ".signal-strip",
        ".workflow",
        ".operating-model",
        ".mobile-section",
        ".faq",
        ".subpage-hero > *",
        ".platform-section",
        ".control-catalogue",
        ".legal-hero > *",
        ".signal-strip > div",
        ".workflow-rail > li",
        ".model-copy",
        ".role-ledger > div",
        ".mobile-section > *",
        ".faq-list > details",
        ".platform-section > *",
        ".control-catalogue > article",
        ".legal-content > section",
        ".assurance-note > *",
        ".continuity-section > *",
    ].join(","));

    if (reducedMotion.matches || !("IntersectionObserver" in window)) {
        targets.forEach((target) => target.classList.add("is-visible"));
        return;
    }

    document.documentElement.classList.add("motion-ready");
    targets.forEach((target, index) => {
        target.classList.add("reveal-on-scroll");
        target.style.setProperty("--reveal-order", String(index % 6));
    });

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
        });
    }, {rootMargin: "0px 0px -8%", threshold: 0.12});

    targets.forEach((target) => observer.observe(target));
})();
