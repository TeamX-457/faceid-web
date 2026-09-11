// Shared config, auth, and API helpers for the Guidian AI dashboard.
// Plain fetch()-based client for the Spring Boot backend - no build step needed.

// Local dev talks to the backend running on localhost; everywhere else (the deployed static
// site) talks to the deployed backend. Update PROD_API_BASE_URL if the Render service name
// in render.yaml ever changes.
const PROD_API_BASE_URL = "https://faceid-java-backend.onrender.com";
const API_BASE_URL =
  ["localhost", "127.0.0.1"].includes(window.location.hostname) ? "http://localhost:8082" : PROD_API_BASE_URL;

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

// ---------- Theme (light/dark) ----------

const THEME_KEY = "gd_theme";

function applyStoredTheme() {
  const theme = localStorage.getItem(THEME_KEY) || "dark";
  document.documentElement.setAttribute("data-theme", theme);
}

const SUN_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>';
const MOON_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>';

function paintThemeToggleButton(btn, theme) {
  if (btn.hasAttribute("data-icon-toggle")) {
    btn.innerHTML = theme === "light" ? MOON_ICON : SUN_ICON;
    btn.title = theme === "light" ? "Switch to dark mode" : "Switch to light mode";
  } else {
    btn.textContent = theme === "light" ? "Dark mode" : "Light mode";
  }
}

function toggleTheme() {
  const current = document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
  const next = current === "light" ? "dark" : "light";
  localStorage.setItem(THEME_KEY, next);
  document.documentElement.setAttribute("data-theme", next);
  const btn = document.getElementById("theme-toggle-btn");
  if (btn) paintThemeToggleButton(btn, next);
}

// ---------- Shared icon rail (left nav) ----------

const RAIL_ICONS = {
  dashboard: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>',
  students: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="7" r="3"/><path d="M2 21v-2a5 5 0 0 1 5-5h4a5 5 0 0 1 5 5v2"/><circle cx="18" cy="8" r="2.5"/><path d="M16 21v-1.5a4 4 0 0 0-1-2.6"/></svg>',
  scan: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 8a2 2 0 0 1 2-2h2l1.5-2h7L17 6h2a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><circle cx="12" cy="13" r="3.5"/></svg>',
  logout: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/></svg>',
};

// Renders the left icon rail into #rail-mount, scoped to the current user's role.
// activePage is one of "dashboard" | "students" | "scan".
function renderIconRail(activePage) {
  const mount = document.getElementById("rail-mount");
  if (!mount) return;
  const { role, fullName } = getSession();

  const navItems =
    role === "ADMIN"
      ? [
          { key: "dashboard", href: "dashboard.html", icon: RAIL_ICONS.dashboard, title: "Dashboard" },
          { key: "students", href: "students.html", icon: RAIL_ICONS.students, title: "Students" },
        ]
      : [{ key: "scan", href: "upload.html", icon: RAIL_ICONS.scan, title: "Scan Terminal" }];

  const navHtml = navItems
    .map(
      (item) =>
        `<a href="${item.href}" class="rail-btn ${item.key === activePage ? "active" : ""}" title="${item.title}">${item.icon}</a>`
    )
    .join("");

  mount.outerHTML = `
    <aside class="icon-rail" id="rail-mount">
      <div class="rail-brand" title="Guidian AI">G</div>
      <nav class="rail-nav">${navHtml}</nav>
      <div class="rail-bottom">
        <button id="theme-toggle-btn" data-icon-toggle class="rail-btn"></button>
        <button onclick="logout()" class="rail-btn" title="Logout${fullName ? " (" + fullName + ")" : ""}">${RAIL_ICONS.logout}</button>
      </div>
    </aside>`;
}

// Call once per page after the nav is in the DOM.
function initThemeToggle() {
  applyStoredTheme();
  const btn = document.getElementById("theme-toggle-btn");
  if (!btn) return;
  const current = document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
  paintThemeToggleButton(btn, current);
  btn.addEventListener("click", toggleTheme);
}
