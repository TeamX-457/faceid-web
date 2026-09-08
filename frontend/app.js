// Shared config, auth, and API helpers for the Guidian AI dashboard.
// Plain fetch()-based client for the Spring Boot backend - no build step needed.

const API_BASE_URL = "http://localhost:8082";

const SessionKeys = { TOKEN: "gd_token", ROLE: "gd_role", USERNAME: "gd_username", FULL_NAME: "gd_full_name" };

function getSession() {
  return {
    token: localStorage.getItem(SessionKeys.TOKEN),
    role: localStorage.getItem(SessionKeys.ROLE),
    username: localStorage.getItem(SessionKeys.USERNAME),
    fullName: localStorage.getItem(SessionKeys.FULL_NAME),
  };
}

function setSession({ token, role, username, fullName }) {
  localStorage.setItem(SessionKeys.TOKEN, token);
  localStorage.setItem(SessionKeys.ROLE, role);
  localStorage.setItem(SessionKeys.USERNAME, username);
  localStorage.setItem(SessionKeys.FULL_NAME, fullName || username);
}

function clearSession() {
  Object.values(SessionKeys).forEach((k) => localStorage.removeItem(k));
}

function logout() {
  clearSession();
  window.location.href = "index.html";
}

// Redirects unauthenticated users to login, and users with the wrong role to their own home page.
function requireRole(requiredRole) {
  const { token, role } = getSession();
  if (!token) {
    window.location.href = "index.html";
    return null;
  }
  if (role !== requiredRole) {
    window.location.href = role === "ADMIN" ? "dashboard.html" : "upload.html";
    return null;
  }
  return getSession();
}

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function apiFetch(path, { method = "GET", body, formData } = {}) {
  const headers = {};
  const { token } = getSession();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let requestBody;
  if (formData) {
    requestBody = formData;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    requestBody = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, { method, headers, body: requestBody });
  } catch {
    throw new ApiError("Network error - is the backend running on " + API_BASE_URL + "?", 0);
  }

  if (res.status === 401) {
    clearSession();
    window.location.href = "index.html";
    throw new ApiError("Session expired. Please sign in again.", 401);
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const data = await res.json();
      message = data.message || data.error || message;
    } catch {
      /* non-JSON error body, keep default message */
    }
    throw new ApiError(message, res.status);
  }

  if (res.status === 204) return undefined;
  const text = await res.text();
  return text ? JSON.parse(text) : undefined;
}

function mediaUrl(path) {
  if (!path) return "";
  return path.startsWith("http") ? path : `${API_BASE_URL}${path}`;
}

function formatDate(iso) {
  if (!iso) return "-";
  return new Date(iso).toLocaleString();
}

function statusBadgeClasses(status) {
  switch (status) {
    case "CONFIRMED":
      return "bg-green-100 text-green-800 border-green-300";
    case "REJECTED":
      return "bg-red-100 text-red-800 border-red-300";
    default:
      return "bg-yellow-100 text-yellow-800 border-yellow-300";
  }
}

function confidencePercent(score) {
  const pct = score <= 1 ? score * 100 : score;
  return Math.max(0, Math.min(100, Math.round(pct)));
}

// Small transient toast in the bottom-right corner.
function showToast(message, isError = false) {
  const el = document.createElement("div");
  el.textContent = message;
  el.className = "hud-toast " + (isError ? "error" : "ok");
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
