/* Shared motion hooks.  They only animate new state, never historical data. */
document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('[data-motion-page], .responsive-content, .content-wrapper');
    if (root) root.setAttribute('data-motion-page', '');

    document.querySelectorAll('.dashboard-grid, .metrics-grid, .kpi-grid, .card-grid, .queue-cards, .queue-list, table tbody').forEach(grid => grid.classList.add('motion-stagger'));
    const observer = 'IntersectionObserver' in window && !matchMedia('(prefers-reduced-motion: reduce)').matches
        ? new IntersectionObserver(entries => entries.forEach(entry => { if (entry.isIntersecting) { entry.target.classList.add('motion-visible'); observer.unobserve(entry.target); } }), { threshold: .08 })
        : null;
    document.querySelectorAll('.motion-reveal').forEach(el => observer ? observer.observe(el) : el.classList.add('motion-visible'));

    document.querySelectorAll('form').forEach(form => form.addEventListener('submit', () => {
        const submit = form.querySelector('button[type="submit"]:not([data-no-motion-loading])');
        if (!submit || !form.checkValidity()) return;
        submit.classList.add('motion-loading'); submit.disabled = true; submit.setAttribute('aria-busy', 'true');
    }));

    document.querySelectorAll('.status-chip, .status-badge, .status-pill').forEach(chip => {
        const observer = new MutationObserver(() => { chip.classList.remove('motion-status-change'); requestAnimationFrame(() => chip.classList.add('motion-status-change')); });
        observer.observe(chip, { childList: true, characterData: true, subtree: true });
    });
});

window.removeQueueItem = function(element) {
    if (!element) return;
    element.classList.add('motion-remove');
    window.setTimeout(() => element.remove(), 210);
};
