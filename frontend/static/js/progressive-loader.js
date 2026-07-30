(() => {
    // Memory and LocalStorage cache for Stale-While-Revalidate
    const cacheGet = (key) => {
        try { return localStorage.getItem('fieldcrm:swr:' + key); } catch (_) { return null; }
    };
    const cacheSet = (key, val) => {
        try { localStorage.setItem('fieldcrm:swr:' + key, val); } catch (_) { }
    };

    // Performance measurements repository
    window.performanceMetrics = {
        fcp: 0,
        lcp: 0,
        sectionRequests: {},
        layoutShifts: 0,
        jsRequests: 0,
        cssRequests: 0
    };

    // Listen to PerformanceObserver for FCP, LCP and CLS
    if (window.PerformanceObserver) {
        // Paint observer (FCP)
        const paintObserver = new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
                if (entry.name === 'first-contentful-paint') {
                    window.performanceMetrics.fcp = entry.startTime;
                }
            }
        });
        try { paintObserver.observe({ type: 'paint', buffered: true }); } catch (_) {}

        // LCP observer
        const lcpObserver = new PerformanceObserver((list) => {
            const entries = list.getEntries();
            if (entries.length > 0) {
                window.performanceMetrics.lcp = entries[entries.length - 1].startTime;
            }
        });
        try { lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true }); } catch (_) {}

        // Layout Shift observer
        const clsObserver = new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
                if (!entry.hadRecentInput) {
                    window.performanceMetrics.layoutShifts += entry.value;
                }
            }
        });
        try { clsObserver.observe({ type: 'layout-shift', buffered: true }); } catch (_) {}
    }

    // Measure resource requests
    if (window.performance) {
        const resources = performance.getEntriesByType('resource');
        let jsCount = 0, cssCount = 0;
        resources.forEach(r => {
            if (r.initiatorType === 'script' || r.name.endsWith('.js')) jsCount++;
            if (r.initiatorType === 'link' || r.name.endsWith('.css')) cssCount++;
        });
        window.performanceMetrics.jsRequests = jsCount;
        window.performanceMetrics.cssRequests = cssCount;
    }

    // Active abort controllers for filters
    const controllers = new Map();

    async function fetchSection(element, retryCount = 0) {
        const src = element.getAttribute('data-section-src');
        if (!src) return;

        const sectionId = element.id || src;

        // Abort previous request for this section if any
        if (controllers.has(sectionId)) {
            controllers.get(sectionId).abort();
        }
        const controller = new AbortController();
        controllers.set(sectionId, controller);

        // Track request start timing
        const startTime = performance.now();
        window.performanceMetrics.sectionRequests[sectionId] = { start: startTime, duration: 0, status: 'pending' };

        // Set aria-busy
        element.setAttribute('aria-busy', 'true');

        // Setup 600ms timeout for loading text (disabled as per request)
        const textTimer = null;

        try {
            const response = await fetch(src, {
                headers: { 'X-Progressive-Load': 'true' },
                signal: controller.signal
            });

            if (!response.ok) throw new Error(`HTTP status ${response.status}`);

            const html = await response.text();
            
            // Extract matching element block from response html if it's a full page
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const newElement = doc.getElementById(element.id) || doc.querySelector(`[data-section-src="${src}"]`);
            
            const contentHTML = newElement ? newElement.innerHTML : html;

            // Only update DOM if changed to prevent layout shifts
            if (element.innerHTML !== contentHTML) {
                // Keep references to pagination/loading indicators
                const sentinel = element.querySelector('.pagination-sentinel');
                const fallback = element.querySelector('.btn-load-more');
                
                element.innerHTML = contentHTML;
                
                if (sentinel) element.appendChild(sentinel);
                if (fallback) element.appendChild(fallback);
            }

            // Cache successfully loaded HTML
            cacheSet(sectionId, contentHTML);
            element.removeAttribute('aria-busy');

            // Success state status clear
            const errStatus = element.querySelector('.section-refresh-failed');
            if (errStatus) errStatus.remove();

            const infoEl = element.querySelector('.section-loading-info');
            if (infoEl) infoEl.remove();

            window.performanceMetrics.sectionRequests[sectionId].duration = performance.now() - startTime;
            window.performanceMetrics.sectionRequests[sectionId].status = 'success';

            // Post-load callbacks
            const event = new CustomEvent('sectionLoaded', { detail: { id: sectionId } });
            element.dispatchEvent(event);

        } catch (error) {
            if (error.name === 'AbortError') return;

            console.warn(`Section load failed: ${src}`, error);

            // Retry quietly up to 3 times
            if (retryCount < 3) {
                setTimeout(() => fetchSection(element, retryCount + 1), 1000 * (retryCount + 1));
                return;
            }

            window.performanceMetrics.sectionRequests[sectionId].status = 'failed';
            element.removeAttribute('aria-busy');
        }
    }

    // Initialize all progressive sections
    function initProgressiveLoading() {
        document.querySelectorAll('[data-section-src]').forEach(element => {
            const sectionId = element.id || element.getAttribute('data-section-src');
            
            // Stale-While-Revalidate: load cache instantly if present
            const cached = cacheGet(sectionId);
            if (cached) {
                element.innerHTML = cached;
            }
            
            fetchSection(element);
        });
    }

    // Automatic Pagination using IntersectionObserver
    function initPagination() {
        const paginatedLists = document.querySelectorAll('[data-paginate-url]');
        paginatedLists.forEach(list => {
            let currentPage = 1;
            let isLoading = false;
            let hasMore = true;

            const container = list.querySelector('.paginated-container') || list;
            
            // Create target load-more sentinel
            const sentinel = document.createElement('div');
            sentinel.className = 'pagination-sentinel';
            sentinel.style.height = '20px';
            list.appendChild(sentinel);

            // Create accessible fallback button
            const fallbackButton = document.createElement('button');
            fallbackButton.type = 'button';
            fallbackButton.className = 'btn-load-more';
            fallbackButton.textContent = 'Load More';
            fallbackButton.style.cssText = 'display: none; width: 100%; margin-top: 12px; padding: 10px; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 6px; cursor: pointer;';
            list.appendChild(fallbackButton);

            const loadNextPage = async () => {
                if (isLoading || !hasMore) return;
                isLoading = true;
                fallbackButton.textContent = 'Loading...';
                fallbackButton.disabled = true;

                const url = new URL(list.getAttribute('data-paginate-url'), window.location.origin);
                url.searchParams.set('page', String(currentPage + 1));
                
                try {
                    const response = await fetch(url.href, {
                        headers: { 'X-Progressive-Load': 'true' }
                    });
                    if (!response.ok) throw new Error('Pagination fetch failed');
                    
                    const html = await response.text();
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    
                    const items = doc.querySelectorAll('[data-paginate-item]');
                    if (items.length === 0) {
                        hasMore = false;
                        sentinel.style.display = 'none';
                        fallbackButton.style.display = 'none';
                        return;
                    }

                    items.forEach(item => {
                        if (!container.querySelector(`[data-id="${item.dataset.id}"]`)) {
                            container.appendChild(item);
                        }
                    });

                    currentPage += 1;
                } catch (err) {
                    console.error('Pagination error', err);
                    fallbackButton.style.display = 'block';
                } finally {
                    isLoading = false;
                    fallbackButton.textContent = 'Load More';
                    fallbackButton.disabled = false;
                }
            };

            fallbackButton.addEventListener('click', loadNextPage);

            if (window.IntersectionObserver) {
                const observer = new IntersectionObserver(entries => {
                    if (entries[0].isIntersecting) {
                        loadNextPage();
                    }
                }, { rootMargin: '200px' });
                observer.observe(sentinel);
            } else {
                fallbackButton.style.display = 'block';
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initProgressiveLoading();
            initPagination();
        });
    } else {
        initProgressiveLoading();
        initPagination();
    }
})();
