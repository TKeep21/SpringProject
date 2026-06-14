const AUTH_SERVICE = "http://localhost:8081";
const FINANCE_SERVICE = "http://localhost:8082";
const REPORT_SERVICE = "http://localhost:8083";
const TOKEN_KEY = "financeTrackerJwt";

const tokenStatus = document.querySelector("#tokenStatus");
const logoutButton = document.querySelector("#logoutButton");

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  }
  updateTokenStatus();
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  updateTokenStatus();
}

function updateTokenStatus() {
  const token = getToken();
  tokenStatus.textContent = token ? `${token.slice(0, 24)}...` : "No token";
}

function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function compactObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, fieldValue]) => fieldValue !== "")
  );
}

function buildQuery(fields) {
  const params = new URLSearchParams();

  Object.entries(fields).forEach(([key, value]) => {
    if (!value) {
      return;
    }

    if (key === "userIds") {
      value
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean)
        .forEach((userId) => params.append("userIds", userId));
      return;
    }

    params.append(key, value);
  });

  const query = params.toString();
  return query ? `?${query}` : "";
}

function endpointUrl(baseUrl, path, queryFields = null) {
  return `${baseUrl}${path}${queryFields ? buildQuery(queryFields) : ""}`;
}

function authHeaders(includeJson = true) {
  const headers = {};
  const token = getToken();

  if (includeJson) {
    headers["Content-Type"] = "application/json";
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const error = new Error(`HTTP ${response.status} ${response.statusText}`);
    error.details = body;
    throw error;
  }

  return body;
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  return parseResponse(response);
}

function showResult(card, value) {
  card.querySelector(".result").textContent = JSON.stringify(value, null, 2);
  card.querySelector(".error").textContent = "";
}

function showError(card, error) {
  card.querySelector(".result").textContent = "";
  card.querySelector(".error").textContent = JSON.stringify(
    {
      message: error.message,
      details: error.details || null
    },
    null,
    2
  );
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

function setDefaultDates() {
  document.querySelectorAll('input[name="operationDate"]').forEach((input) => {
    input.value = todayIso();
  });

  document.querySelectorAll('.report-form input[name="from"]').forEach((input) => {
    input.value = firstDayOfMonthIso();
  });

  document.querySelectorAll('.report-form input[name="to"]').forEach((input) => {
    input.value = todayIso();
  });
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

const actions = {
  async register(form) {
    const data = formValues(form);
    const result = await requestJson(endpointUrl(AUTH_SERVICE, "/api/v1/auth/register"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    setToken(result.accessToken);
    return result;
  },

  async login(form) {
    const data = formValues(form);
    const result = await requestJson(endpointUrl(AUTH_SERVICE, "/api/v1/auth/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    setToken(result.accessToken);
    return result;
  },

  async me() {
    return requestJson(endpointUrl(AUTH_SERVICE, "/api/v1/auth/me"), {
      headers: authHeaders(false)
    });
  },

  async createGroup(form) {
    return requestJson(endpointUrl(FINANCE_SERVICE, "/api/v1/groups"), {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(compactObject(formValues(form)))
    });
  },

  async addMember(form) {
    const data = formValues(form);
    const groupId = data.groupId;
    delete data.groupId;

    return requestJson(endpointUrl(FINANCE_SERVICE, `/api/v1/groups/${groupId}/members`), {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(data)
    });
  },

  async createCategory(form) {
    return requestJson(endpointUrl(FINANCE_SERVICE, "/api/v1/categories"), {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(compactObject(formValues(form)))
    });
  },

  async createOperation(form) {
    return requestJson(endpointUrl(FINANCE_SERVICE, "/api/v1/operations"), {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(compactObject(formValues(form)))
    });
  },

  async getOperations(form) {
    return requestJson(endpointUrl(FINANCE_SERVICE, "/api/v1/operations", compactObject(formValues(form))), {
      headers: authHeaders(false)
    });
  },

  async summaryReport(form) {
    return requestJson(endpointUrl(REPORT_SERVICE, "/api/v1/reports/summary", compactObject(formValues(form))), {
      headers: authHeaders(false)
    });
  },

  async categoryReport(form) {
    return requestJson(endpointUrl(REPORT_SERVICE, "/api/v1/reports/by-category", compactObject(formValues(form))), {
      headers: authHeaders(false)
    });
  },

  async memberReport(form) {
    return requestJson(endpointUrl(REPORT_SERVICE, "/api/v1/reports/by-member", compactObject(formValues(form))), {
      headers: authHeaders(false)
    });
  },

  async exportCsv(form) {
    const fields = compactObject({ ...formValues(form), format: "csv" });
    const csv = await requestJson(endpointUrl(REPORT_SERVICE, "/api/v1/reports/export", fields), {
      headers: authHeaders(false)
    });
    downloadCsv(csv);
    return {
      downloaded: "operations-report.csv",
      bytes: new Blob([csv]).size,
      preview: csv
    };
  }
};

document.querySelectorAll(".action-card").forEach((card) => {
  const actionName = card.dataset.action;
  const form = card.querySelector("form");

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    try {
      const result = await actions[actionName](form);
      showResult(card, result);
    } catch (error) {
      showError(card, error);
    }
  });
});

logoutButton.addEventListener("click", clearToken);

setDefaultDates();
updateTokenStatus();
