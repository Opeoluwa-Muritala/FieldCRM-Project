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
        modal.className = 'preview-modal-overlay';
        modal.innerHTML = `
            <section class="preview-modal-container">
                <header class="preview-modal-header">
                    <h2 id="document-preview-title" class="preview-modal-title"></h2>
                    <button type="button" data-preview-close aria-label="Close document preview" class="preview-modal-close">&times;</button>
                </header>
                <div data-preview-content class="preview-modal-body"></div>
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
        // Local documents may be stored as authorised PDFs rather than
        // Cloudinary page images. Detect that response and embed it in the same
        // protected modal; cloud documents continue through page images.
        const firstResponse = await fetch(pageUrl(url, 1), { credentials: 'same-origin' });
        if (!firstResponse.ok) throw new Error('Preview request failed');
        const firstType = (firstResponse.headers.get('content-type') || '').split(';', 1)[0].toLowerCase();
        if (firstType === 'application/pdf') {
            const objectUrl = URL.createObjectURL(await firstResponse.blob());
            const frame = document.createElement('iframe');
            frame.className = 'preview-modal-pdf';
            frame.title = title.textContent || 'Document preview';
            frame.src = objectUrl;
            frame.addEventListener('load', () => URL.revokeObjectURL(objectUrl), { once: true });
            if (requestId === previewRequestId && !modal.hidden) preview.replaceChildren(frame);
            return;
        }

        function startLoadingPage(pageNum) {
            const image = document.createElement('img');
            image.alt = `Document page ${pageNum}`;
            image.className = 'preview-modal-image';
            image.src = pageUrl(url, pageNum);
            const loadPromise = streamPreviewImage(image).then(() => image);
            return { image, loadPromise };
        }

        const firstImage = document.createElement('img');
        firstImage.alt = 'Document page 1';
        firstImage.className = 'preview-modal-image';
        firstImage.src = URL.createObjectURL(await firstResponse.blob());
        const firstLoad = streamPreviewImage(firstImage).then(() => {
            URL.revokeObjectURL(firstImage.src);
            return firstImage;
        });
        let current = { image: firstImage, loadPromise: firstLoad };

        for (let page = 1; page <= MAX_PREVIEW_PAGES; page += 1) {
            if (modal.hidden || requestId !== previewRequestId) return;
            
            let next = null;
            if (page < MAX_PREVIEW_PAGES) {
                next = startLoadingPage(page + 1);
            }

            try {
                const loadedImage = await current.loadPromise;
                if (modal.hidden || requestId !== previewRequestId) return;
                if (page === 1) preview.replaceChildren(loadedImage);
                else preview.appendChild(loadedImage);
                current = next;
            } catch (_) {
                if (page === 1) throw new Error('Preview image request failed');
                return;
            }
        }
    }
})();
