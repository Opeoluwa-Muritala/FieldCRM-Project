(() => {
    "use strict";

    const unsafeMethods = new Set(["POST", "PUT", "PATCH", "DELETE"]);

    function token() {
        const prefix = "csrf_token=";
        const entry = document.cookie.split(";").map((part) => part.trim()).find((part) => part.startsWith(prefix));
        return entry ? decodeURIComponent(entry.slice(prefix.length)) : "";
    }

    function protectForm(form) {
        const method = (form.method || "GET").toUpperCase();
        if (!unsafeMethods.has(method)) return;
        let input = form.querySelector('input[name="csrf_token"]');
        if (!input) {
            input = document.createElement("input");
            input.type = "hidden";
            input.name = "csrf_token";
            form.appendChild(input);
        }
        input.value = token();
    }

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll("form").forEach(protectForm);
    });
    document.addEventListener("submit", (event) => protectForm(event.target), true);

    const originalFetch = window.fetch.bind(window);
    window.fetch = (input, init = {}) => {
        const requestUrl = new URL(input instanceof Request ? input.url : String(input), window.location.href);
        const method = String(init.method || (input instanceof Request ? input.method : "GET")).toUpperCase();
        if (requestUrl.origin === window.location.origin && unsafeMethods.has(method)) {
            const headers = new Headers(input instanceof Request ? input.headers : undefined);
            new Headers(init.headers || {}).forEach((value, key) => headers.set(key, value));
            const csrfToken = token();
            if (csrfToken) headers.set("X-CSRF-Token", csrfToken);
            init = {...init, headers};
        }
        return originalFetch(input, init);
    };
})();
