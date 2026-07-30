(function () {
    "use strict";

    function requestJson(url, body) {
        return fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: {"Content-Type": "application/json", "Accept": "application/json"},
            body: JSON.stringify(body)
        }).then(async function (response) {
            const data = await response.json().catch(function () { return {}; });
            if (!response.ok) {
                const error = new Error(data.detail || ("HTTP " + response.status));
                error.status = response.status;
                throw error;
            }
            return data;
        });
    }

    function proxyFallback(form) {
        return new Promise(function (resolve, reject) {
            const xhr = new XMLHttpRequest();
            xhr.open("POST", form.action);
            xhr.onload = function () {
                if (xhr.status >= 200 && xhr.status < 400) {
                    let data = {};
                    try { data = JSON.parse(xhr.responseText); } catch (_) {}
                    resolve(data);
                } else {
                    reject(new Error("Fallback upload failed (HTTP " + xhr.status + ")."));
                }
            };
            xhr.onerror = function () { reject(new Error("Network error during fallback upload.")); };
            xhr.send(new FormData(form));
        });
    }

    function cloudinaryUpload(authorization, file, onProgress) {
        return new Promise(function (resolve, reject) {
            const body = new FormData();
            Object.keys(authorization.fields).forEach(function (key) {
                body.append(key, authorization.fields[key]);
            });
            body.append("file", file);
            const xhr = new XMLHttpRequest();
            xhr.open("POST", authorization.upload_url);
            xhr.upload.onprogress = function (event) {
                if (event.lengthComputable) onProgress(Math.round(event.loaded * 100 / event.total));
            };
            xhr.onload = function () {
                let data = {};
                try { data = JSON.parse(xhr.responseText); } catch (_) {}
                if (xhr.status >= 200 && xhr.status < 300) resolve(data);
                else reject(new Error((data.error && data.error.message) || "Cloud upload failed."));
            };
            xhr.onerror = function () { reject(new Error("Network error during cloud upload.")); };
            xhr.send(body);
        });
    }

    window.attachDirectDocumentUpload = function (form) {
        if (!form || form.dataset.directUploadAttached === "true") return;
        form.dataset.directUploadAttached = "true";
        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            const fileInput = form.querySelector('input[type="file"]');
            const file = fileInput && fileInput.files && fileInput.files[0];
            if (!file) return;
            const button = document.getElementById("saveDocBtn");
            const progress = document.getElementById("uploadProgress");
            const bar = document.getElementById("progressBar");
            const label = document.getElementById("progressLabel");
            button.disabled = true;
            progress.style.display = "block";
            bar.style.background = "";

            const payload = {
                filename: file.name,
                mime_type: file.type,
                size_bytes: file.size,
                doc_type: (form.querySelector('[name="category"]') || {}).value || form.dataset.docType || "other"
            };
            try {
                let auth;
                try {
                    const result = await requestJson(form.dataset.authorizationUrl, payload);
                    auth = result.authorization;
                } catch (error) {
                    if (error.status !== 503) throw error;
                    label.textContent = "Secure direct upload unavailable; using server fallback…";
                    const fallback = await proxyFallback(form);
                    window.location.href = fallback.redirect || form.dataset.redirectUrl;
                    return;
                }
                const cloud = await cloudinaryUpload(auth, file, function (pct) {
                    bar.style.width = pct + "%";
                    label.textContent = "Uploading securely… " + pct + "%";
                });
                label.textContent = "Verifying upload…";
                const finalized = await requestJson(form.dataset.finalizeUrl, {
                    intent_id: auth.intent_id,
                    public_id: cloud.public_id,
                    version: cloud.version,
                    signature: cloud.signature
                });
                bar.style.width = "100%";
                label.textContent = "Upload complete — redirecting…";
                window.location.href = finalized.redirect || form.dataset.redirectUrl;
            } catch (error) {
                label.textContent = error.message || "Upload failed. Please retry.";
                bar.style.background = "var(--color-danger, #e53e3e)";
                button.disabled = false;
            }
        });
    };
}());
