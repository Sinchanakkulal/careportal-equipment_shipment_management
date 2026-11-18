/* =========================================================
   CarePortal – common.js (shared by every page)
   ========================================================= */

/* ---------- FEATURE 1: Header & Navigation ---------- */
document.addEventListener("DOMContentLoaded", function () {
  initNavToggle();
});

/* Opens/closes the nav dropdown on small screens */
function initNavToggle() {
  var toggleBtn = document.getElementById("navToggle");
  var nav = document.getElementById("mainNav");

  if (!toggleBtn || !nav) return;

  toggleBtn.addEventListener("click", function () {
    var isOpen = nav.classList.toggle("open");
    toggleBtn.setAttribute("aria-expanded", isOpen);
  });
}

/* ---------- FEATURE 4: Shared helpers ---------- */

/* "In Transit" -> "badge-in-transit"  (matches the CSS badges) */
function statusClass(status) {
  return "badge-" + String(status).toLowerCase().replace(/\s+/g, "-");
}

/* Makes text safe before putting it inside HTML (stops script injection) */
function escapeHtml(text) {
  if (text === null || text === undefined) return "";
  return String(text)
    .replace(/&/g, "&amp;")      // must be first
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")     // needed for attributes written with "..."
    .replace(/'/g, "&#39;");     // needed for attributes written with '...'
}

/* ---------- FEATURE 6: debounce helper ---------- */

/* Runs fn only after the user has stopped for `delay` milliseconds */
function debounce(fn, delay) {
  var timer;
  return function () {
    clearTimeout(timer);
    timer = setTimeout(fn, delay);
  };
}

var messageTimer = null;
function showMessage(type,text){
  var area = document.getElementById("message-area");
  if(!area) return;

  clearTimeout(messageTimer);
  area.innerHTML = "";

  var alertBox = document.createElement("div");
  alertBox.className = "alert alert-" + type;

  var span = document.createElement("span");
  span.textContent = text;

  var closeBtn = document.createElement("button");
  closeBtn.type = "button";
  closeBtn.className = "alert-close";
  closeBtn.setAttribute("aria-label", "Close message");
  closeBtn.innerHTML = "&times;";
  closeBtn.addEventListener("click", clearMessages);
  
  alertBox.appendChild(span);
  alertBox.appendChild(closeBtn);
  area.appendChild(alertBox);

  window.scrollTo({ top: 0, behavior: "smooth" });

  if(type=== "success"){
    messageTimer = setTimeout(clearMessages,2000);
  }
}

function clearMessages() {
  clearTimeout(messageTimer);
  var area = document.getElementById("message-area");
  if (area) area.innerHTML = "";
}


/* =========================================================
   FEATURE 11: Date formatting helper
   ========================================================= */

/* "2026-11-12" -> "12 Nov 2026". Empty values show "—". */
function formatDate(value) {
  if (!value) return "—";

  var parts = String(value).substring(0, 10).split("-");     // ["2026","11","12"]
  if (parts.length !== 3) return escapeHtml(value);

  var months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  var monthName = months[parseInt(parts[1], 10) - 1];
  if (!monthName) return escapeHtml(value);

  return parseInt(parts[2], 10) + " " + monthName + " " + parts[0];
}

/* True only for a real calendar date written as YYYY-MM-DD (e.g. rejects 2026-02-30) */
function isValidIsoDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;

  var parts = value.split("-");
  var year = parseInt(parts[0], 10);
  var month = parseInt(parts[1], 10);
  var day = parseInt(parts[2], 10);

  var date = new Date(year, month - 1, day);
  return date.getFullYear() === year &&
         date.getMonth() === month - 1 &&
         date.getDate() === day;
}

/* "2026-11-02T11:20:00" or "2026-11-02 11:20:00" -> "2 Nov 2026, 11:20" */
function formatDateTime(value) {
  if (!value) return "—";

  var text = String(value).replace("T", " ");
  var datePart = formatDate(text.substring(0, 10));      // reuses the Feature 11 helper
  var timePart = text.substring(11, 16);                 // "HH:MM"

  return /^\d{2}:\d{2}$/.test(timePart) ? datePart + ", " + timePart : datePart;
}
