/* =========================================================
   CarePortal – equipment.js (Equipment list page)

   How the data flows:
     1. loadEquipment()  asks the server for EVERY record (in chunks of 100)
                         -> stored in  allRecords        (the master copy)
     2. refreshView()    applies search + status + type IN THE BROWSER
                         -> stored in  filteredRecords   (computed from allRecords)
     3. renderPage()     cuts out the current page (10 rows) and draws it

   Typing in the search box or changing a filter only runs steps 2 and 3.
   Nothing is sent to the server. The server is called again only after
   something is saved (add / edit / status change), because then the
   master copy is out of date.
   ========================================================= */

var EQUIPMENT_TRANSITIONS = {
  "Requested": ["Approved"],
  "Approved":  ["Active"],
  "Active":    ["Inactive", "Returned"],
  "Inactive":  ["Active", "Returned"],
  "Returned":  []
};

var PAGE_SIZE = 10;            // rows per page (fixed)
var allRecords = [];           // MASTER COPY: every record from the server
var filteredRecords = [];      // records that match the current search + filters
var currentPage = 1;           // page being shown (1-based)
var dataReady = false;         // true only while allRecords holds a finished load
var loadToken = 0;             // identifies the newest load

var equipmentById = {};        // rows currently on screen (used by the Edit popup)
var statusTarget = null;       // the row being changed: { id, name, status }

document.addEventListener("DOMContentLoaded", function () {

  // --- search and filters: these never call the server ---
  document.getElementById("searchInput").addEventListener("input", applyFilters);
  document.getElementById("statusFilter").addEventListener("change", applyFilters);
  document.getElementById("typeFilter").addEventListener("change", applyFilters);
  document.getElementById("clearBtn").addEventListener("click", clearFilters);
  document.getElementById("retryBtn").addEventListener("click", loadEquipment);

  // --- pagination controls (also no server call) ---
  document.getElementById("prevBtn").addEventListener("click", function () {
    currentPage--;
    renderPage();
  });
  document.getElementById("nextBtn").addEventListener("click", function () {
    currentPage++;
    renderPage();
  });

  // --- status popup ---
  document.getElementById("equipmentBody").addEventListener("click", function (event) {
    var btn = event.target.closest(".status-btn");
    if (!btn) return;
    openStatusModal({
      id: btn.dataset.id,
      name: btn.dataset.name,
      status: btn.dataset.status
    });
  });

  document.getElementById("confirmStatusBtn").addEventListener("click", confirmStatusChange);
  document.getElementById("cancelStatusBtn").addEventListener("click", closeStatusModal);

  document.getElementById("statusModal").addEventListener("click", function (event) {
    if (event.target === this) closeStatusModal();
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") closeStatusModal();
  });

  document.getElementById("newStatusSelect").addEventListener("change", function () {
    setModalError("");
  });

  // opened from Home with ?add=1: the popup shows first and the list is not needed
  var addOnly = new URLSearchParams(window.location.search).get("add") === "1";
  if (!addOnly) loadEquipment();
});

/* =========================================================
   Search and filters (all in the browser)
   ========================================================= */

function getFilters() {
  return {
    search: document.getElementById("searchInput").value.trim(),
    status: document.getElementById("statusFilter").value,
    type:   document.getElementById("typeFilter").value
  };
}

/* Does one record match the search text, the status and the type? */
function matchesFilters(item, filters) {
  var search = filters.search.toLowerCase();

  var matchesSearch =
    search === "" ||
    String(item.equipmentName || "").toLowerCase().includes(search) ||
    String(item.serialNumber  || "").toLowerCase().includes(search);

  var matchesStatus = filters.status === "" || item.status === filters.status;
  var matchesType   = filters.type === ""   || item.equipmentType === filters.type;

  return matchesSearch && matchesStatus && matchesType;
}

/* A changed filter means a new result set, so go back to page 1 */
function applyFilters() {
  if (!dataReady) return;          // nothing loaded yet: loadEquipment() will apply the filters when it finishes
  currentPage = 1;
  refreshView();
}

/* Empties the three filter inputs (does not redraw anything) */
function resetFilterInputs() {
  document.getElementById("searchInput").value = "";
  document.getElementById("statusFilter").value = "";
  document.getElementById("typeFilter").value = "";
}

function clearFilters() {
  resetFilterInputs();
  applyFilters();
}

/* Recomputes filteredRecords from allRecords and redraws. NO server call. */
function refreshView() {
  var filters = getFilters();

  filteredRecords = allRecords.filter(function (item) {
    return matchesFilters(item, filters);
  });

  if (filteredRecords.length === 0) {
    var filtering = filters.search || filters.status || filters.type;
    document.getElementById("emptyHint").textContent = filtering
      ? "Try a different search or clear the filters."
      : "Add your first equipment record to get started.";
    showState("empty");
    return;
  }

  renderPage();
  showState("table");
}

/* =========================================================
   Loading from the server (the only place that calls the GET API)
   ========================================================= */

/* Loads ALL records. The backend allows at most 100 per request,
   so ask in chunks until nothing is left. */
function fetchAllEquipment() {
  var all = [];

  function fetchChunk(page) {
    return getEquipmentList({ page: page, size: 100 }).then(function (result) {
      var items = Array.isArray(result) ? result : (result && result.items) || [];
      all = all.concat(items);

      var totalPages = (result && result.totalPages) || 1;
      if (page < totalPages && page < 1000) {      // 1000 = safety stop, never loops forever
        return fetchChunk(page + 1);
      }
      return all;
    });
  }

  return fetchChunk(1);
}

function loadEquipment() {
  var myToken = ++loadToken;
  dataReady = false;                // filters are ignored until this load has finished

  showState("loading");

  fetchAllEquipment()
    .then(function (list) {
      if (myToken !== loadToken) return;          // a newer load started: ignore this answer

      allRecords = Array.isArray(list) ? list : []; ;                          // replace the master copy
      dataReady = true;
      refreshView();                              // re-applies whatever is typed in the filters
    })
    .catch(function (error) {
      if (myToken !== loadToken) return;
      document.getElementById("errorText").textContent = error.message;
      showState("error");
    });
}

/* =========================================================
   Showing things on the page
   ========================================================= */

/* Shows ONE of: "loading" | "empty" | "error" | "table" and hides the others */
function showState(state) {
  var boxes = {
    loading: document.getElementById("loadingBox"),
    empty:   document.getElementById("emptyBox"),
    error:   document.getElementById("errorBox"),
    table:   document.getElementById("tableWrap")
  };

  Object.keys(boxes).forEach(function (name) {
    boxes[name].classList.toggle("hidden", name !== state);
  });

  // the pagination bar is only visible together with the table
  document.getElementById("pagination").classList.toggle("hidden", state !== "table");
}

/* Cuts the current page out of filteredRecords and draws it */
function renderPage() {
  var totalItems = filteredRecords.length;
  var totalPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE));

  if (currentPage > totalPages) currentPage = totalPages;     // e.g. the last row was filtered out
  if (currentPage < 1) currentPage = 1;

  var start = (currentPage - 1) * PAGE_SIZE;
  var pageItems = filteredRecords.slice(start, start + PAGE_SIZE);

  renderEquipmentTable(pageItems);

  document.getElementById("pageInfo").textContent =
    "Showing " + (start + 1) + "–" + (start + pageItems.length) + " of " + totalItems;
  document.getElementById("pageLabel").textContent =
    "Page " + currentPage + " of " + totalPages;

  document.getElementById("prevBtn").disabled = (currentPage <= 1);
  document.getElementById("nextBtn").disabled = (currentPage >= totalPages);
}

/* Builds one <tr> per equipment record */
function renderEquipmentTable(list) {
  var tbody = document.getElementById("equipmentBody");
  var html = "";

  equipmentById = {};
  list.forEach(function (item) {
    equipmentById[item.equipmentId] = item;
  });

  list.forEach(function (item) {
    var nextStatuses = EQUIPMENT_TRANSITIONS[item.status] || [];

    var statusAction = nextStatuses.length
      ? "<button type='button' class='btn btn-primary btn-sm status-btn'" +
          " data-id='" + escapeHtml(item.equipmentId) + "'" +
          " data-name='" + escapeHtml(item.equipmentName) + "'" +
          " data-status='" + escapeHtml(item.status) + "'>Change Status</button>"
      : "<span class='muted'>Final</span>";

    html += "<tr>" +
      "<td>" + escapeHtml(item.equipmentId) + "</td>" +
      "<td>" + escapeHtml(item.equipmentName) + "</td>" +
      "<td>" + escapeHtml(item.equipmentType) + "</td>" +
      "<td>" + (escapeHtml(item.manufacturer) || "—") + "</td>" +
      "<td>" + (escapeHtml(item.modelNumber) || "—") + "</td>" +
      "<td class='nowrap'>" + escapeHtml(item.serialNumber) + "</td>" +
      "<td>" + (escapeHtml(item.assignedPatientId) || "—") + "</td>" +
      "<td><span class='badge " + statusClass(item.status) + "'>" +
            escapeHtml(item.status) + "</span></td>" +
      "<td><span class='cell-clip'>" + (escapeHtml(item.notes) || "—") + "</span></td>" +
      "<td class='nowrap'>" + formatDateTime(item.createdDate) + "</td>" +
      "<td class='nowrap'>" + formatDateTime(item.updatedDate) + "</td>" +
      "<td><div class='row-actions'>" +
            "<button type='button' class='btn btn-secondary btn-sm edit-btn'" +
            " data-id='" + escapeHtml(item.equipmentId) + "'>Edit</button>" +
            statusAction +
      "</div></td>" +
      "</tr>";
  });

  tbody.innerHTML = html;
}

/* =========================================================
   Status popup
   ========================================================= */

function openStatusModal(target) {
  statusTarget = target;

  document.getElementById("statusModalName").textContent = target.name;

  var badge = document.getElementById("statusModalCurrent");
  badge.textContent = target.status;
  badge.className = "badge " + statusClass(target.status);

  var select = document.getElementById("newStatusSelect");
  select.innerHTML = "<option value=''>Select new status</option>";
  (EQUIPMENT_TRANSITIONS[target.status] || []).forEach(function (status) {
    var option = document.createElement("option");
    option.value = status;
    option.textContent = status;
    select.appendChild(option);
  });

  setModalError("");
  resetConfirmButton();
  document.getElementById("statusModal").classList.remove("hidden");
  select.focus();
}

function closeStatusModal() {
  document.getElementById("statusModal").classList.add("hidden");
  statusTarget = null;
  resetConfirmButton();
}

function resetConfirmButton() {
  var btn = document.getElementById("confirmStatusBtn");
  btn.disabled = false;
  btn.textContent = "Confirm";
}

function setModalError(message) {
  var select = document.getElementById("newStatusSelect");
  document.getElementById("newStatusError").textContent = message;
  select.closest(".field").classList.toggle("invalid", message !== "");
}

function confirmStatusChange() {
  if (!statusTarget) return;

  var newStatus = document.getElementById("newStatusSelect").value;
  if (newStatus === "") {
    setModalError("Please select a new status.");
    return;
  }

  var target = statusTarget;
  var confirmBtn = document.getElementById("confirmStatusBtn");
  confirmBtn.disabled = true;
  confirmBtn.textContent = "Saving...";

  changeEquipmentStatus(target.id, newStatus)
    .then(function () {
      closeStatusModal();
      showMessage("success",
        "Status of \"" + target.name + "\" changed from " + target.status + " to " + newStatus + ".");
      loadEquipment();                  // the saved data changed: refresh the master copy ONCE
    })
    .catch(function (error) {
      setModalError(error.message);
      resetConfirmButton();
    });
}