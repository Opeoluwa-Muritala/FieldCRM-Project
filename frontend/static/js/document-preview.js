(() => {
    const MAX_PREVIEW_PAGES = 100;
    let modal;
    let preview;
    let title;
    let previewRequestId = 0;

    function cancelPreview() {
        previewRequestId += 1;
    }

    function closePreview() {
        if (!modal) return;
        cancelPreview();
        preview.replaceChildren();
        modal.hidden = true;
        modal.style.display = 'none';
        document.body.style.overflow = '';
    }

    function ensureModal() {
        if (modal) return;
        modal = document.createElement('div');
        modal.hidden = true;
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-labelledby', 'document-preview-title');
        modal.style.cssText = 'position:fixed;inset:0;z-index:3000;background:rgba(17,24,39,.68);padding:24px;display:grid;place-items:center;';
        modal.innerHTML = `
            <section style="width:min(100%,1000px);height:min(92vh,850px);background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 24px 60px rgba(0,0,0,.35);display:flex;flex-direction:column;">
                <header style="display:flex;align-items:center;justify-content:space-between;gap:16px;padding:12px 16px;border-bottom:1px solid #e5e7eb;">
                    <h2 id="document-preview-title" style="margin:0;font-size:16px;"></h2>
                    <button type="button" data-preview-close aria-label="Close document preview" style="border:0;background:transparent;font-size:28px;line-height:1;cursor:pointer;">&times;</button>
                </header>
                <div data-preview-content style="flex:1;overflow:auto;background:#f3f4f6;padding:20px;display:flex;flex-direction:column;gap:16px;align-items:center;"></div>
            </section>`;
        title = modal.querySelector('#document-preview-title');
        preview = modal.querySelector('[data-preview-content]');
        modal.querySelector('[data-preview-close]').addEventListener('click', closePreview);
        modal.addEventListener('pointerdown', (event) => {
            if (event.target === modal) closePreview();
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !modal.hidden) closePreview();
        });
        document.body.appendChild(modal);
    }

    function pageUrl(url, page) {
        const parsed = new URL(url, window.location.origin);
        parsed.searchParams.set('page', String(page));
        return parsed.href;
    }

    function streamPreviewImage(image) {
        return new Promise((resolve, reject) => {
            image.addEventListener('load', resolve, { once: true });
            image.addEventListener('error', reject, { once: true });
        });
    }

    document.addEventListener('click', (event) => {
        let link = event.target.closest('[data-document-preview], a[href*="/api/v1/documents/"][href*="/preview"]');
        if (!link) {
            const row = event.target.closest('tr');
            const clickedControl = event.target.closest('a, button, input, select, textarea, label, [role="button"]');
            if (!row || clickedControl) return;
            // A document category is a review target: clicking anywhere in
            // its table row opens the same protected preview as its link.
            link = row.querySelector('[data-document-preview], a[href*="/api/v1/documents/"][href*="/preview"]');
        }
        if (!link || !link.href) return;
        event.preventDefault();
        event.stopPropagation();
        ensureModal();
        cancelPreview();
        const requestId = ++previewRequestId;
        title.textContent = link.dataset.documentTitle || link.textContent.trim() || 'Document preview';
        preview.innerHTML = '<div class="document-preview-loading" role="status" aria-live="polite"><span class="document-preview-shimmer"></span><span>Loading document preview…</span></div>';
        modal.hidden = false;
        modal.style.display = 'grid';
        document.body.style.overflow = 'hidden';
        renderPreviewPages(link.href, requestId).catch((error) => {
            if (requestId !== previewRequestId || modal.hidden) return;
            preview.textContent = 'Unable to render this document preview.';
            console.error('Document preview failed', error);
        });
    }, true);

    async function renderPreviewPages(url, requestId) {
        for (let page = 1; page <= MAX_PREVIEW_PAGES; page += 1) {
            if (modal.hidden || requestId !== previewRequestId) return;
            const image = document.createElement('img');
            image.alt = `Document page ${page}`;
            image.style.cssText = 'max-width:100%;height:auto;background:#fff;box-shadow:0 2px 10px rgba(0,0,0,.18);';
            image.src = pageUrl(url, page);
            try {
                // The browser progressively renders each image response. The
                // endpoint returns a Cloudinary-rendered PNG, never PDF bytes.
                await streamPreviewImage(image);
                if (modal.hidden || requestId !== previewRequestId) return;
                if (page === 1) preview.replaceChildren(image);
                else preview.appendChild(image);
            } catch (_) {
                if (page === 1) throw new Error('Preview image request failed');
                return; // No next page (or the document is a single image).
            }
        }
    }
})();
