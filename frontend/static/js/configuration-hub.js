(() => {
    "use strict";

    const search = document.querySelector("[data-feature-search]");
    if (!search) return;

    const cards = Array.from(document.querySelectorAll("[data-feature-card]"));
    const groups = Array.from(document.querySelectorAll("[data-feature-group]"));
    const empty = document.querySelector("[data-empty-search]");

    search.addEventListener("input", () => {
        const query = search.value.trim().toLocaleLowerCase();
        let visibleCount = 0;

        cards.forEach((card) => {
            const visible = !query || (card.dataset.searchText || "").includes(query);
            card.hidden = !visible;
            if (visible) visibleCount += 1;
        });

        groups.forEach((group) => {
            group.hidden = !group.querySelector("[data-feature-card]:not([hidden])");
        });

        if (empty) empty.hidden = visibleCount !== 0;
    });
})();
