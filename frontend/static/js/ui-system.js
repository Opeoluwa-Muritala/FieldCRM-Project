(function () {
  'use strict';
  window.showFieldMessage = function (message, tone) {
    tone = tone || 'error';
    var region = document.getElementById('field-message-region');
    if (!region) { region = document.createElement('div'); region.id = 'field-message-region'; region.className = 'field-message-region'; region.setAttribute('aria-live', 'polite'); document.body.appendChild(region); }
    var item = document.createElement('div'); item.className = 'field-toast ' + (tone === 'error' ? 'is-error' : 'is-success'); item.setAttribute('role', 'status'); item.textContent = message;
    var dismiss = document.createElement('button'); dismiss.type = 'button'; dismiss.className = 'field-toast-dismiss'; dismiss.textContent = '×'; dismiss.setAttribute('aria-label', 'Dismiss message');
    var remove = function () { item.classList.add('motion-dismiss'); window.setTimeout(function () { item.remove(); }, 160); };
    dismiss.addEventListener('click', remove); item.appendChild(dismiss); region.appendChild(item); window.setTimeout(remove, 6000);
  };
  document.addEventListener('click', function (event) {
    var control = event.target.closest('[data-confirm]');
    if (!control || control.dataset.confirmed === 'true') return;
    event.preventDefault();
    var old = document.getElementById('field-confirmation'); if (old) old.remove();
    var backdrop = document.createElement('div'); backdrop.id = 'field-confirmation-backdrop';
    var panel = document.createElement('div'); panel.id = 'field-confirmation'; panel.setAttribute('role', 'alertdialog'); panel.setAttribute('aria-modal', 'true'); panel.setAttribute('aria-label', 'Confirm action');
    var message = document.createElement('p'); message.textContent = control.dataset.confirm;
    var cancel = document.createElement('button'); cancel.type = 'button'; cancel.textContent = 'Cancel'; cancel.className = 'btn btn-secondary';
    var proceed = document.createElement('button'); proceed.type = 'button'; proceed.textContent = 'Confirm'; proceed.className = 'btn btn-primary';
    var close = function () { panel.remove(); backdrop.remove(); document.removeEventListener('keydown', onKey); control.focus(); };
    var onKey = function (e) { if (e.key === 'Escape') close(); if (e.key === 'Tab') { e.preventDefault(); (document.activeElement === proceed ? cancel : proceed).focus(); } };
    cancel.addEventListener('click', close); proceed.addEventListener('click', function () { panel.remove(); backdrop.remove(); document.removeEventListener('keydown', onKey); control.dataset.confirmed = 'true'; control.click(); delete control.dataset.confirmed; });
    backdrop.addEventListener('click', function (e) { if (e.target === backdrop) close(); });
    panel.append(message, cancel, proceed); document.body.append(backdrop, panel); document.addEventListener('keydown', onKey); proceed.focus();
  });
  (function () { var timer; var reset = function () { clearTimeout(timer); timer = setTimeout(async function () { var body = new URLSearchParams({ next: window.location.pathname + window.location.search }); try { await fetch('/logout', { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body }); } finally { window.location.href = '/login'; } }, 15 * 60 * 1000); }; ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'].forEach(function (name) { document.addEventListener(name, reset, { passive: true }); }); reset(); }());
}());
