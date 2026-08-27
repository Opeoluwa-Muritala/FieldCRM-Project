(function () {
  "use strict";

  const form = document.getElementById("cam-feasibility-form");
  if (!form) return;
  const editable = form.dataset.editable === "true";
  const money = new Intl.NumberFormat("en-NG", {minimumFractionDigits: 2, maximumFractionDigits: 2});
  const number = (name) => {
    const value = Number(form.elements[name]?.value || 0);
    return Number.isFinite(value) ? value : 0;
  };
  const setText = (id, value, suffix) => {
    const node = document.getElementById(id);
    if (node) node.textContent = value === null ? "Not available" : `${suffix === "₦" ? "₦" : ""}${money.format(value)}${suffix && suffix !== "₦" ? suffix : ""}`;
  };
  const externalTotals = () => {
    let rental = 0;
    let outstanding = 0;
    document.querySelectorAll("[data-history-kind='external']").forEach((row) => {
      rental += Number(row.querySelector("[name='external_rental[]']")?.value || 0);
      outstanding += Number(row.querySelector("[name='external_outstanding[]']")?.value || 0);
    });
    return {rental, outstanding};
  };
  const collateralTotal = () => {
    let total = 0;
    document.querySelectorAll("[data-collateral-row]").forEach((row) => {
      total += Number(row.querySelector("[data-collateral-fsv]")?.value || 0);
    });
    return total;
  };
  const update = () => {
    const totalAssets = number("cash_at_bank") + number("stock") + number("prepayment") + number("fixed_assets");
    const grossProfit = (number("margin") / 100) * number("monthly_turnover");
    const netProfit = grossProfit - number("monthly_expenses");
    const amount = number("recommended_amount");
    const rate = number("interest_rate");
    const tenor = number("proposed_tenor");
    const installment = tenor > 0 ? (((rate / 100) * amount * tenor) + amount) / tenor : 0;
    const external = externalTotals();
    const totalRental = installment + external.rental;
    const dti = netProfit > 0 ? (totalRental / netProfit) * 100 : null;
    const gearing = totalAssets > 0 ? (external.outstanding + amount) / totalAssets : null;
    const collateral = collateralTotal();
    const coverage = amount > 0 ? (collateral / amount) * 100 : null;
    setText("cam-total-assets", totalAssets, "₦");
    setText("cam-gross-profit", grossProfit, "₦");
    setText("cam-net-profit", netProfit, "₦");
    setText("cam-installment", installment, "₦");
    setText("cam-external-rental", external.rental, "₦");
    setText("cam-total-rental", totalRental, "₦");
    setText("cam-dti", dti, "%");
    setText("cam-outstanding", external.outstanding, "₦");
    setText("cam-gearing", gearing === null ? null : gearing * 100, "%");
    setText("cam-collateral-total", collateral, "₦");
    setText("security-total-fsv", collateral, "₦");
    setText("cam-collateral-coverage", coverage, "%");
    updateFlag("cam-dti-flag", dti, 35, "Within 35% ceiling", "Above 35% ceiling");
    updateFlag("cam-gearing-flag", gearing === null ? null : gearing * 100, 50, "Within 50% ceiling", "Above 50% ceiling");
    updateTurnoverTotals();
  };
  const updateFlag = (id, value, ceiling, pass, fail) => {
    const node = document.getElementById(id);
    if (!node) return;
    node.className = "cam-status " + (value === null ? "pending" : value <= ceiling ? "pass" : "fail");
    node.textContent = value === null ? "Cannot calculate" : value <= ceiling ? pass : fail;
  };
  const updateTurnoverTotals = () => {
    let inflow = 0;
    let count = 0;
    const months = new Set();
    const matrix = new Map();
    const banks = new Set();
    document.querySelectorAll("[data-turnover-row]").forEach((row) => {
      const month = row.querySelector("[name='turnover_month[]']")?.value || "";
      const bank = row.querySelector("[name='turnover_bank[]']")?.value.trim() || "";
      const rowInflow = Number(row.querySelector("[name='turnover_amount[]']")?.value || 0);
      const rowCount = Number(row.querySelector("[name='turnover_transaction_count[]']")?.value || 0);
      inflow += rowInflow;
      count += rowCount;
      if (month) months.add(month);
      if (bank) banks.add(bank);
      if (month && bank) {
        const key = `${month}\u0000${bank}`;
        const value = matrix.get(key) || {inflow: 0, count: 0};
        value.inflow += rowInflow;
        value.count += rowCount;
        matrix.set(key, value);
      }
    });
    setText("turnover-total-inflow", inflow, "₦");
    setText("turnover-total-count", count, "");
    setText("turnover-average-inflow", months.size ? inflow / months.size : 0, "₦");
    setText("turnover-average-count", months.size ? count / months.size : 0, "");
    renderTurnoverPivot([...months].sort(), [...banks].sort(), matrix);
  };
  const renderTurnoverPivot = (months, banks, matrix) => {
    const table = document.getElementById("turnover-consolidated-table");
    if (!table) return;
    const head = document.createElement("thead");
    const header = document.createElement("tr");
    ["Month", ...banks.flatMap((bank) => [`${bank} Inflow`, `${bank} Count`]), "Consolidated Turnover", "Consolidated Count"].forEach((label) => {
      const cell = document.createElement("th"); cell.textContent = label; header.appendChild(cell);
    });
    head.appendChild(header);
    const body = document.createElement("tbody");
    const totals = new Map(banks.map((bank) => [bank, {inflow: 0, count: 0}]));
    months.forEach((month) => {
      const row = document.createElement("tr");
      const monthCell = document.createElement("td"); monthCell.textContent = month; row.appendChild(monthCell);
      let consolidatedInflow = 0; let consolidatedCount = 0;
      banks.forEach((bank) => {
        const value = matrix.get(`${month}\u0000${bank}`) || {inflow: 0, count: 0};
        totals.get(bank).inflow += value.inflow; totals.get(bank).count += value.count;
        consolidatedInflow += value.inflow; consolidatedCount += value.count;
        const inflowCell = document.createElement("td"); inflowCell.textContent = `₦${money.format(value.inflow)}`; row.appendChild(inflowCell);
        const countCell = document.createElement("td"); countCell.textContent = money.format(value.count); row.appendChild(countCell);
      });
      const consolidatedCell = document.createElement("td"); consolidatedCell.textContent = `₦${money.format(consolidatedInflow)}`; row.appendChild(consolidatedCell);
      const consolidatedCountCell = document.createElement("td"); consolidatedCountCell.textContent = money.format(consolidatedCount); row.appendChild(consolidatedCountCell);
      body.appendChild(row);
    });
    if (!months.length) {
      const row = document.createElement("tr"); const cell = document.createElement("td"); cell.colSpan = 3; cell.className = "cam-empty"; cell.textContent = "No bank turnover recorded."; row.appendChild(cell); body.appendChild(row);
    }
    table.replaceChildren(head, body);
  };
  const addRow = (templateId, bodyId) => {
    const template = document.getElementById(templateId);
    const body = document.getElementById(bodyId);
    if (!template || !body || body.children.length >= 50) return;
    body.appendChild(template.content.cloneNode(true));
    update();
  };
  document.querySelectorAll("[data-add-row]").forEach((button) => {
    button.addEventListener("click", () => addRow(button.dataset.template, button.dataset.target));
  });
  document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-remove-row]");
    if (button) {
      button.closest("tr")?.remove();
      update();
    }
    const upload = event.target.closest("[data-collateral-upload]");
    if (upload) {
      const dialog = document.getElementById("cam-upload-dialog");
      const uploadForm = document.getElementById("cam-upload-form");
      if (dialog && uploadForm) {
        uploadForm.action = `/applications/${form.dataset.applicationId}/collateral/${upload.dataset.collateralUpload}/documents`;
        dialog.showModal();
      }
    }
    if (event.target.closest("[data-close-upload]")) {
      document.getElementById("cam-upload-dialog")?.close();
    }
  });
  document.addEventListener("input", (event) => {
    if (event.target.getAttribute("form") === "cam-feasibility-form") update();
  });
  document.addEventListener("change", (event) => {
    if (event.target.getAttribute("form") === "cam-feasibility-form") update();
  });
  update();
}());
