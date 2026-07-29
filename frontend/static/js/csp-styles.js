(() => {
    const applyComputedTemplateStyles = () => {
        document.querySelectorAll('[data-csp-style]').forEach((element) => {
            element.style.cssText = element.dataset.cspStyle;
            element.removeAttribute('data-csp-style');
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', applyComputedTemplateStyles, { once: true });
    } else {
        applyComputedTemplateStyles();
    }
})();
