(() => {
    const PDFJS_URL = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.10.38/pdf.min.mjs';
    const PDFJS_WORKER_URL = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.10.38/pdf.worker.min.mjs';
    let modal;
    let preview;
    let title;
    let pdfjsPromise;

    async function getPdfJs() {
        pdfjsPromise ||= import(PDFJS_URL).then((pdfjs) => {
            pdfjs.GlobalWorkerOptions.workerSrc = PDFJS_WORKER_URL;
            return pdfjs;
        });
        return pdfjsPromise;
    }

    function closePreview() {
        if (!modal) return;
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

    document.addEventListener('click', (event) => {
        const link = event.target.closest('[data-document-preview], a[href*="/api/v1/documents/"]');
        if (!link || !link.href) return;
        event.preventDefault();
        event.stopPropagation();
        ensureModal();
        title.textContent = link.dataset.documentTitle || link.textContent.trim() || 'Document preview';
        preview.innerHTML = '<div class="document-preview-loading" role="status" aria-live="polite"><span class="document-preview-shimmer"></span><span>Loading document preview…</span></div>';
        modal.hidden = false;
        modal.style.display = 'grid';
        document.body.style.overflow = 'hidden';
        renderPdf(link.href).catch((error) => {
            preview.textContent = 'Unable to render this document preview.';
            console.error('PDF preview failed', error);
        });
    }, true);

    async function renderPdf(url) {
        const [pdfjs, response] = await Promise.all([
            getPdfJs(),
            fetch(url, { credentials: 'same-origin' }),
        ]);
        if (!response.ok) throw new Error(`Preview request failed: ${response.status}`);
        const pdf = await pdfjs.getDocument({ data: await response.arrayBuffer() }).promise;
        if (modal.hidden) return;
        preview.replaceChildren();
        for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
            if (modal.hidden) return;
            const page = await pdf.getPage(pageNumber);
            const viewport = page.getViewport({ scale: 1.35 });
            const canvas = document.createElement('canvas');
            const context = canvas.getContext('2d');
            canvas.width = Math.ceil(viewport.width);
            canvas.height = Math.ceil(viewport.height);
            canvas.style.cssText = 'max-width:100%;height:auto;background:#fff;box-shadow:0 2px 10px rgba(0,0,0,.18);';
            preview.appendChild(canvas);
            await page.render({ canvasContext: context, viewport }).promise;
        }
    }
})();
