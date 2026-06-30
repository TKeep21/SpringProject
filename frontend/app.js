const AUTH_SERVICE = "http://localhost:8081";
const FINANCE_SERVICE = "http://localhost:8082";
const REPORT_SERVICE = "http://localhost:8083";
const TOKEN_KEY = "financeTrackerJwt";
const USER_KEY = "financeTrackerUser";

const state = {
  token: localStorage.getItem(TOKEN_KEY) || "",
  user: readStoredUser(),
  groups: [],
  categories: [],
  operations: [],
  selectedGroupId: "",
  selectedCategoryId: "",
  authMode: "login"
};

const elements = {
  pageTitle: document.querySelector("#pageTitle"),
  toast: document.querySelector("#toast"),
  responseLog: document.querySelector("#responseLog"),
  sessionUser: document.querySelector("#sessionUser"),
  sessionToken: document.querySelector("#sessionToken"),
  logoutButton: document.querySelector("#logoutButton"),
  refreshButton: document.querySelector("#refreshButton"),
  clearResponseButton: document.querySelector("#clearResponseButton"),
  authPanel: document.querySelector(".auth-panel"),
  authForm: document.querySelector("#authForm"),
  authSubmitButton: document.querySelector("#authSubmitButton"),
  loginModeButton: document.querySelector("#loginModeButton"),
  registerModeButton: document.querySelector("#registerModeButton"),
  loadOverviewButton: document.querySelector("#loadOverviewButton"),
  overviewIncome: document.querySelector("#overviewIncome"),
  overviewExpense: document.querySelector("#overviewExpense"),
  overviewBalance: document.querySelector("#overviewBalance"),
  groupForm: document.querySelector("#groupForm"),
  memberForm: document.querySelector("#memberForm"),
  groupsList: document.querySelector("#groupsList"),
  reloadGroupsButton: document.querySelector("#reloadGroupsButton"),
  categoryForm: document.querySelector("#categoryForm"),
  categoryFilterForm: document.querySelector("#categoryFilterForm"),
  categoriesList: document.querySelector("#categoriesList"),
  operationForm: document.querySelector("#operationForm"),
  operationFilterForm: document.querySelector("#operationFilterForm"),
  operationTypeSelect: document.querySelector("#operationTypeSelect"),
  operationGroupSelect: document.querySelector("#operationGroupSelect"),
  operationCategorySelect: document.querySelector("#operationCategorySelect"),
  operationsTable: document.querySelector("#operationsTable"),
  reportForm: document.querySelector("#reportForm"),
  reportOutput: document.querySelector("#reportOutput"),
  summaryReportButton: document.querySelector("#summaryReportButton"),
  categoryReportButton: document.querySelector("#categoryReportButton"),
  memberReportButton: document.querySelector("#memberReportButton"),
  exportCsvButton: document.querySelector("#exportCsvButton")
};

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || "null");
  } catch {
    return null;
  }
}

function setSession(token, user) {
  state.token = token || "";
  state.user = user || null;

  if (state.token) {
    localStorage.setItem(TOKEN_KEY, state.token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }

  if (state.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(state.user));
  } else {
    localStorage.removeItem(USER_KEY);
  }

  renderSession();
}

function clearSession() {
  setSession("", null);
  state.groups = [];
  state.categories = [];
  state.operations = [];
  state.selectedGroupId = "";
  state.selectedCategoryId = "";
  renderAll();
  showToast("Сессия очищена");
}

function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function compactObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, fieldValue]) => fieldValue !== "" && fieldValue != null)
  );
}

function buildQuery(fields) {
  const params = new URLSearchParams();
  Object.entries(compactObject(fields)).forEach(([key, value]) => {
    if (key === "userIds") {
      value.split(",").map((item) => item.trim()).filter(Boolean).forEach((id) => params.append("userIds", id));
      return;
    }
    params.append(key, value);
  });
  const query = params.toString();
  return query ? `?${query}` : "";
}

async function api(baseUrl, path, options = {}) {
  const includeJson = options.includeJson !== false;
  const headers = { ...(options.headers || {}) };

  if (includeJson) {
    headers["Content-Type"] = "application/json";
  }
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers
  });
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json") ? await response.json() : await response.text();

  if (!response.ok) {
    const error = new Error(readErrorMessage(body, response));
    error.status = response.status;
    error.details = body;
    logResponse({ status: response.status, error: body });
    throw error;
  }

  logResponse({ status: response.status, body });
  return body;
}

function readErrorMessage(body, response) {
  if (body && typeof body === "object") {
    return body.message || body.error || `HTTP ${response.status}`;
  }
  return body || `HTTP ${response.status}`;
}

function logResponse(value) {
  elements.responseLog.textContent = JSON.stringify(value, null, 2);
}

function showToast(message, isError = false) {
  elements.toast.textContent = message;
  elements.toast.classList.toggle("is-error", isError);
  elements.toast.hidden = false;
  window.clearTimeout(showToast.timeoutId);
  showToast.timeoutId = window.setTimeout(() => {
    elements.toast.hidden = true;
  }, 4200);
}

function formatDateInput(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function todayIso() {
  return formatDateInput(new Date());
}

function firstDayOfMonthIso() {
  const now = new Date();
  return formatDateInput(new Date(now.getFullYear(), now.getMonth(), 1));
}

function money(value, currency = "USD") {
  const number = Number(value || 0);
  return `${number.toFixed(2)} ${currency}`;
}

function operationTypeLabel(type) {
  if (type === "EXPENSE") {
    return "Расход";
  }
  if (type === "INCOME") {
    return "Доход";
  }
  return type || "";
}

function groupRoleLabel(role) {
  const labels = {
    OWNER: "Владелец",
    ADMIN: "Администратор",
    MEMBER: "Участник"
  };
  return labels[role] || role || "";
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function groupName(groupId) {
  if (!groupId) {
    return "Личная";
  }
  return state.groups.find((group) => group.id === groupId)?.name || "Группа";
}

function categoryName(categoryId) {
  return state.categories.find((category) => category.id === categoryId)?.name || "Неизвестно";
}

function setAuthMode(mode) {
  state.authMode = mode;
  elements.authPanel.dataset.mode = mode;
  elements.loginModeButton.classList.toggle("is-active", mode === "login");
  elements.registerModeButton.classList.toggle("is-active", mode === "register");
  elements.authSubmitButton.textContent = mode === "login" ? "Войти" : "Зарегистрироваться";
}

function setDefaultDates() {
  document.querySelectorAll('input[type="date"]').forEach((input) => {
    if (input.name === "from" || input.name === "fromDate") {
      input.value = firstDayOfMonthIso();
      return;
    }
    input.value = todayIso();
  });
}

function renderSession() {
  elements.sessionUser.textContent = state.user
    ? `${state.user.firstName || ""} ${state.user.lastName || ""}`.trim() || state.user.email
    : "Не авторизован";
  elements.sessionToken.textContent = state.token ? `${state.token.slice(0, 28)}...` : "Нет токена";
}

function renderGroupOptions() {
  const selects = [
    ["#memberGroupSelect", "Выберите группу", false],
    ["#categoryGroupSelect", "Личная", true],
    ["#categoryFilterGroupSelect", "Личные/стандартные", true],
    ["#operationGroupSelect", "Личная", true],
    ["#operationFilterGroupSelect", "Все доступные", true],
    ["#reportGroupSelect", "Личные + доступные группы", true]
  ];

  selects.forEach(([selector, emptyLabel, includeEmpty]) => {
    const select = document.querySelector(selector);
    if (!select) {
      return;
    }
    const currentValue = select.value;
    const options = includeEmpty ? [`<option value="">${emptyLabel}</option>`] : [];
    options.push(...state.groups.map((group) => `<option value="${group.id}">${escapeHtml(group.name)}</option>`));
    select.innerHTML = options.join("");
    if (state.groups.some((group) => group.id === currentValue) || (includeEmpty && currentValue === "")) {
      select.value = currentValue;
    }
  });
}

function renderGroups() {
  renderGroupOptions();
  if (!state.groups.length) {
    elements.groupsList.className = "list-grid empty-state";
    elements.groupsList.textContent = "Групп пока нет";
    return;
  }

  elements.groupsList.className = "list-grid";
  elements.groupsList.innerHTML = state.groups.map((group) => {
    const members = group.members || [];
    const selected = group.id === state.selectedGroupId ? " is-selected" : "";
    return `
      <article class="item-card${selected}">
        <div class="item-title">
          <span>${escapeHtml(group.name)}</span>
          <button class="secondary-button select-group-button" type="button" data-id="${group.id}">Выбрать</button>
        </div>
        <div class="item-meta">
          <span>${escapeHtml(group.description || "Без описания")}</span>
          <span>Владелец: ${group.ownerUserId}</span>
          <span>Участников: ${members.length}</span>
        </div>
        <div class="pill-row">
          ${members.map((member) => `<span class="pill">${groupRoleLabel(member.role)}: ${member.userId.slice(0, 8)}</span>`).join("")}
        </div>
      </article>
    `;
  }).join("");

  document.querySelectorAll(".select-group-button").forEach((button) => {
    button.addEventListener("click", () => {
      state.selectedGroupId = button.dataset.id;
      renderGroups();
      renderGroupOptions();
      showToast(`Выбрана группа: ${groupName(state.selectedGroupId)}`);
    });
  });
}

function renderCategories() {
  renderOperationCategoryOptions();
  if (!state.categories.length) {
    elements.categoriesList.className = "list-grid empty-state";
    elements.categoriesList.textContent = "Категории не загружены";
    return;
  }

  elements.categoriesList.className = "list-grid";
  elements.categoriesList.innerHTML = state.categories.map((category) => {
    const selected = category.id === state.selectedCategoryId ? " is-selected" : "";
    const scope = category.isDefault ? "Стандартная" : category.groupId ? groupName(category.groupId) : "Личная";
    return `
      <article class="item-card${selected}">
        <div class="item-title">
          <span>${escapeHtml(category.name)}</span>
          <button class="secondary-button select-category-button" type="button" data-id="${category.id}">Выбрать</button>
        </div>
        <div class="pill-row">
          <span class="pill ${category.type === "EXPENSE" ? "expense" : "income"}">${operationTypeLabel(category.type)}</span>
          <span class="pill">${escapeHtml(scope)}</span>
        </div>
        <div class="item-meta">
          <span>ID: ${category.id}</span>
        </div>
      </article>
    `;
  }).join("");

  document.querySelectorAll(".select-category-button").forEach((button) => {
    button.addEventListener("click", () => {
      state.selectedCategoryId = button.dataset.id;
      renderCategories();
      renderOperationCategoryOptions();
      showToast(`Выбрана категория: ${categoryName(state.selectedCategoryId)}`);
    });
  });
}

function renderOperationCategoryOptions() {
  const type = elements.operationTypeSelect.value;
  const groupId = elements.operationGroupSelect.value;
  const usableCategories = state.categories.filter((category) => {
    if (category.type !== type) {
      return false;
    }
    if (!groupId) {
      return category.isDefault || (!category.groupId && category.ownerUserId === state.user?.id);
    }
    return category.isDefault || category.groupId === groupId;
  });

  elements.operationCategorySelect.innerHTML = usableCategories.length
    ? usableCategories.map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`).join("")
    : `<option value="">Сначала загрузите или создайте категорию</option>`;

  if (usableCategories.some((category) => category.id === state.selectedCategoryId)) {
    elements.operationCategorySelect.value = state.selectedCategoryId;
  }
}

function renderOperations() {
  if (!state.operations.length) {
    elements.operationsTable.innerHTML = `<tr><td colspan="6" class="empty-table">Операции не загружены</td></tr>`;
    return;
  }

  elements.operationsTable.innerHTML = state.operations.map((operation) => `
    <tr>
      <td>${operation.operationDate}</td>
      <td><span class="pill ${operation.type === "EXPENSE" ? "expense" : "income"}">${operationTypeLabel(operation.type)}</span></td>
      <td>${money(operation.amount, operation.currency)}</td>
      <td>${escapeHtml(groupName(operation.groupId))}</td>
      <td>${escapeHtml(categoryName(operation.categoryId))}</td>
      <td>${escapeHtml(operation.description || "")}</td>
    </tr>
  `).join("");
}

function renderAll() {
  renderSession();
  renderGroups();
  renderCategories();
  renderOperations();
}

async function loadGroups() {
  if (!state.token) {
    return;
  }
  state.groups = await api(FINANCE_SERVICE, "/api/v1/groups", { includeJson: false });
  if (!state.selectedGroupId && state.groups.length) {
    state.selectedGroupId = state.groups[0].id;
  }
  renderGroups();
}

async function loadCategories(fields = {}) {
  if (!state.token) {
    return;
  }
  state.categories = await api(FINANCE_SERVICE, `/api/v1/categories${buildQuery(fields)}`, { includeJson: false });
  if (!state.selectedCategoryId && state.categories.length) {
    const expense = state.categories.find((category) => category.type === "EXPENSE");
    state.selectedCategoryId = (expense || state.categories[0]).id;
  }
  renderCategories();
}

async function loadOperations(fields = {}) {
  if (!state.token) {
    return;
  }
  const result = await api(FINANCE_SERVICE, `/api/v1/operations${buildQuery({ ...fields, size: 50 })}`, {
    includeJson: false
  });
  state.operations = result.content || [];
  renderOperations();
}

async function refreshData() {
  try {
    await loadGroups();
    await loadCategories();
    await loadOperations();
    showToast("Данные обновлены");
  } catch (error) {
    showToast(error.message, true);
  }
}

async function loadOverview() {
  try {
    const fields = {
      from: firstDayOfMonthIso(),
      to: todayIso(),
      currency: "USD"
    };
    const summary = await api(REPORT_SERVICE, `/api/v1/reports/summary${buildQuery(fields)}`, { includeJson: false });
    elements.overviewIncome.textContent = money(summary.totalIncome, summary.currency);
    elements.overviewExpense.textContent = money(summary.totalExpense, summary.currency);
    elements.overviewBalance.textContent = money(summary.balance, summary.currency);
    showToast("Итоги загружены");
  } catch (error) {
    showToast(error.message, true);
  }
}

async function submitAuth(event) {
  event.preventDefault();
  const values = formValues(elements.authForm);
  const path = state.authMode === "login" ? "/api/v1/auth/login" : "/api/v1/auth/register";
  const payload = state.authMode === "login"
    ? { email: values.email, password: values.password }
    : values;

  try {
    const result = await api(AUTH_SERVICE, path, {
      method: "POST",
      body: JSON.stringify(payload)
    });
    setSession(result.accessToken, result.user);
    await refreshData();
    showToast(state.authMode === "login" ? "Вход выполнен" : "Регистрация выполнена, вход выполнен");
  } catch (error) {
    showToast(error.message, true);
  }
}

async function submitGroup(event) {
  event.preventDefault();
  try {
    const group = await api(FINANCE_SERVICE, "/api/v1/groups", {
      method: "POST",
      body: JSON.stringify(compactObject(formValues(elements.groupForm)))
    });
    state.selectedGroupId = group.id;
    await loadGroups();
    showToast("Группа создана");
  } catch (error) {
    showToast(error.message, true);
  }
}

async function submitMember(event) {
  event.preventDefault();
  const values = formValues(elements.memberForm);
  const groupId = values.groupId;
  delete values.groupId;
  try {
    await api(FINANCE_SERVICE, `/api/v1/groups/${groupId}/members`, {
      method: "POST",
      body: JSON.stringify(values)
    });
    await loadGroups();
    showToast("Участник добавлен");
  } catch (error) {
    showToast(error.message, true);
  }
}

async function submitCategory(event) {
  event.preventDefault();
  try {
    const category = await api(FINANCE_SERVICE, "/api/v1/categories", {
      method: "POST",
      body: JSON.stringify(compactObject(formValues(elements.categoryForm)))
    });
    state.selectedCategoryId = category.id;
    await loadCategories(compactObject(formValues(elements.categoryFilterForm)));
    showToast("Категория создана");
  } catch (error) {
    showToast(error.message, true);
  }
}

async function submitOperation(event) {
  event.preventDefault();
  try {
    await api(FINANCE_SERVICE, "/api/v1/operations", {
      method: "POST",
      body: JSON.stringify(compactObject(formValues(elements.operationForm)))
    });
    await loadOperations(compactObject(formValues(elements.operationFilterForm)));
    await loadOverview();
    showToast("Операция добавлена");
  } catch (error) {
    showToast(error.message, true);
  }
}

function renderSummaryReport(summary) {
  elements.reportOutput.className = "report-output";
  elements.reportOutput.innerHTML = `
    <div class="metric-grid">
      <div class="metric-card income"><span>Доходы</span><strong>${money(summary.totalIncome, summary.currency)}</strong></div>
      <div class="metric-card expense"><span>Расходы</span><strong>${money(summary.totalExpense, summary.currency)}</strong></div>
      <div class="metric-card balance"><span>Баланс</span><strong>${money(summary.balance, summary.currency)}</strong></div>
    </div>
  `;
}

function renderCategoryReport(items) {
  elements.reportOutput.className = "report-output";
  elements.reportOutput.innerHTML = `
    <div class="report-list">
      ${items.length ? items.map((item) => `
        <div class="report-row">
          <strong>${escapeHtml(item.categoryName)}</strong>
          <span class="pill ${item.type === "EXPENSE" ? "expense" : "income"}">${operationTypeLabel(item.type)}</span>
          <strong>${money(item.totalAmount)}</strong>
        </div>
      `).join("") : `<div class="empty-state">Нет данных по категориям</div>`}
    </div>
  `;
}

function renderMemberReport(items) {
  elements.reportOutput.className = "report-output";
  elements.reportOutput.innerHTML = `
    <div class="report-list">
      ${items.length ? items.map((item) => `
        <div class="report-row">
          <strong>${item.userId}</strong>
          <span>Операций: ${item.operationCount}</span>
          <strong>${money(item.balance)}</strong>
        </div>
      `).join("") : `<div class="empty-state">Нет данных по участникам</div>`}
    </div>
  `;
}

async function runReport(kind) {
  const fields = compactObject(formValues(elements.reportForm));
  const endpoints = {
    summary: "/api/v1/reports/summary",
    category: "/api/v1/reports/by-category",
    member: "/api/v1/reports/by-member",
    csv: "/api/v1/reports/export"
  };

  try {
    if (kind === "csv") {
      const csv = await api(REPORT_SERVICE, `${endpoints.csv}${buildQuery({ ...fields, format: "csv" })}`, {
        includeJson: false
      });
      downloadCsv(csv);
      elements.reportOutput.className = "report-output";
      elements.reportOutput.innerHTML = `<div class="empty-state">CSV выгружен: operations-report.csv</div>`;
      showToast("CSV выгружен");
      return;
    }

    const result = await api(REPORT_SERVICE, `${endpoints[kind]}${buildQuery(fields)}`, { includeJson: false });
    if (kind === "summary") {
      renderSummaryReport(result);
    } else if (kind === "category") {
      renderCategoryReport(result);
    } else {
      renderMemberReport(result);
    }
    showToast("Отчет загружен");
  } catch (error) {
    showToast(error.message, true);
  }
}

function downloadCsv(csv) {
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "operations-report.csv";
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function bindEvents() {
  document.querySelectorAll(".nav-tab").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll(".nav-tab").forEach((tab) => tab.classList.remove("is-active"));
      document.querySelectorAll(".tab-panel").forEach((panel) => panel.classList.remove("is-active"));
      button.classList.add("is-active");
      document.querySelector(`#${button.dataset.tab}`).classList.add("is-active");
      elements.pageTitle.textContent = button.textContent;
    });
  });

  elements.loginModeButton.addEventListener("click", () => setAuthMode("login"));
  elements.registerModeButton.addEventListener("click", () => setAuthMode("register"));
  elements.authForm.addEventListener("submit", submitAuth);
  elements.logoutButton.addEventListener("click", clearSession);
  elements.refreshButton.addEventListener("click", refreshData);
  elements.loadOverviewButton.addEventListener("click", loadOverview);
  elements.groupForm.addEventListener("submit", submitGroup);
  elements.memberForm.addEventListener("submit", submitMember);
  elements.reloadGroupsButton.addEventListener("click", loadGroups);
  elements.categoryForm.addEventListener("submit", submitCategory);
  elements.categoryFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    loadCategories(compactObject(formValues(elements.categoryFilterForm))).catch((error) => showToast(error.message, true));
  });
  elements.operationForm.addEventListener("submit", submitOperation);
  elements.operationFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    loadOperations(compactObject(formValues(elements.operationFilterForm))).catch((error) => showToast(error.message, true));
  });
  elements.operationTypeSelect.addEventListener("change", renderOperationCategoryOptions);
  elements.operationGroupSelect.addEventListener("change", renderOperationCategoryOptions);
  elements.summaryReportButton.addEventListener("click", () => runReport("summary"));
  elements.categoryReportButton.addEventListener("click", () => runReport("category"));
  elements.memberReportButton.addEventListener("click", () => runReport("member"));
  elements.exportCsvButton.addEventListener("click", () => runReport("csv"));
  elements.clearResponseButton.addEventListener("click", () => {
    elements.responseLog.textContent = "Запросов пока не было";
  });
}

setAuthMode("login");
setDefaultDates();
bindEvents();
renderAll();

if (state.token) {
  refreshData().catch((error) => showToast(error.message, true));
}
