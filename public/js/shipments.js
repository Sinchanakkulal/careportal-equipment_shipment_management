/* =========================================================
   CarePortal – shipments.js (Shipment list page)

   How the data flows:
     1. loadShipments()  asks the server for EVERY shipment (in chunks of 100)
                         -> stored in  allRecords        (the master copy)
     2. refreshView()    applies search + status + equipment IN THE BROWSER
                         -> stored in  filteredRecords   (computed from allRecords)
     3. renderPage()     cuts out the current page (10 rows) and draws it

   Search, filters and paging never call the server. The server is called
   again only after something is saved (add / edit / status change),
   because then the master copy is out of date.

   The Equipment filter lists ONLY the equipment that appears in the shipments.
   ========================================================= */

/* Which statuses may follow each status (what the popup OFFERS).
   The backend is the one that actually enforces these rules. */
var SHIPMENT_TRANSITIONS = {
  "Created":    ["Dispatched", "Cancelled"],
  "Dispatched": ["In Transit", "Cancelled"],
  "In Transit": ["Delivered", "Cancelled"],
  "Delivered":  [],
  "Cancelled":  []
};

var PAGE_SIZE = 10;            // rows per page (fixed)
var allRecords = [];           // MASTER COPY: every shipment from the server
var filteredRecords = [];      // shipments that match the current search + filters
var currentPage = 1;           // page being shown (1-based)
var dataReady = false;         // true only while allRecords holds a finished load
var loadToken = 0;             // identifies the newest load

var shipmentById = {};         // rows currently on screen (used by the Edit popup)
var statusTarget = null;       // the row being changed

document.addEventListener("DOMContentLoaded", function () {

  // --- search and filters: these never call the server ---
  document.getElementById("searchInput").addEventListener("input", applyFilters);
  document.getElementById("statusFilter").addEventListener("change", applyFilters);
  document.getElementById("equipmentFilter").addEventListener("change", applyFilters);
  document.getElementById("clearBtn").addEventListener("click", clearFilters);
  document.getElementById("retryBtn").addEventListener("click", loadShipments);

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
  // one listener on the table body handles every "Change Status" button
  document.getElementById("shipmentBody").addEventListener("click", function (event) {
    var btn = event.target.closest(".status-btn");
    if (!btn) return;
    var item = shipmentById[btn.dataset.id];
    if (item) openStatusModal(item);
  });

  document.getElementById("confirmStatusBtn").addEventListener("click", confirmStatusChange);
  document.getElementById("cancelStatusBtn").addEventListener("click", closeStatusModal);
  document.getElementById("newStatusSelect").addEventListener("change", onNewStatusChange);
  document.getElementById("actualDeliveryDate").addEventListener("input", function () {
    setDateError("");
  });

  // clicking the dark area outside the white box closes the popup
  document.getElementById("statusModal").addEventListener("click", function (event) {
    if (event.target === this) closeStatusModal();
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") closeStatusModal();
  });

  // opened from Home with ?add=1: the popup shows first and the list is not needed
  var addOnly = new URLSearchParams(window.location.search).get("add") === "1";
  if (!addOnly) loadShipments();
});

/* =========================================================
   Search and filters (all in the browser)
   ========================================================= */

function getFilters() {
  return {
    search:      document.getElementById("searchInput").value.trim(),
    status:      document.getElementById("statusFilter").value,
    equipmentId: document.getElementById("equipmentFilter").value
  };
}

/* Does one shipment match the search text, the status and the equipment? */
function matchesFilters(item, filters) {
  var search = filters.search.toLowerCase();

  var matchesSearch =
    search === "" ||
    String(item.trackingNumber || "").toLowerCase().includes(search);

  var matchesStatus    = filters.status === "" || item.shipmentStatus === filters.status;
  var matchesEquipment = filters.equipmentId === "" || String(item.equipmentId) === filters.equipmentId;

  return matchesSearch && matchesStatus && matchesEquipment;
}

/* A changed filter means a new result set, so go back to page 1 */
function applyFilters() {
  if (!dataReady) return;          // nothing loaded yet: loadShipments() applies the filters when it finishes
  currentPage = 1;
  refreshView();
}

/* Empties the three filter inputs (does not redraw anything) */
function resetFilterInputs() {
  document.getElementById("searchInput").value = "";
  document.getElementById("statusFilter").value = "";
  document.getElementById("equipmentFilter").value = "";
}

function clearFilters() {
  resetFilterInputs();
  applyFilters();
}

/* Fills the Equipment filter with ONLY the equipment that appears in the shipments.
   Built from the master copy (allRecords), so the list does not shrink while you filter. */
function buildEquipmentFilter() {
  var select = document.getElementById("equipmentFilter");
  var previous = select.value;               // keep the selection if it is still valid

  var seen = {};
  var equipment = [];
  allRecords.forEach(function (item) {
    var id = String(item.equipmentId);
    if (!seen[id]) {
      seen[id] = true;
      equipment.push({ id: id, name: item.equipmentName || ("Equipment " + id) });
    }
  });

  equipment.sort(function (a, b) { return a.name.localeCompare(b.name); });

  select.innerHTML = "";

  var all = document.createElement("option");
  all.value = "";
  all.textContent = "All equipment";
  select.appendChild(all);

  equipment.forEach(function (e) {
    var option = document.createElement("option");
    option.value = e.id;
    option.textContent = e.name + " (#" + e.id + ")";       // textContent = safe
    select.appendChild(option);
  });

  select.value = seen[previous] ? previous : "";
}

/* Recomputes filteredRecords from allRecords and redraws. NO server call. */
function refreshView() {
  var filters = getFilters();

  filteredRecords = allRecords.filter(function (item) {
    return matchesFilters(item, filters);
  });

  if (filteredRecords.length === 0) {
    var filtering = filters.search || filters.status || filters.equipmentId;
    document.getElementById("emptyHint").textContent = filtering
      ? "Try a different search or clear the filters."
      : "Create your first shipment to get started.";
    showState("empty");
    return;
  }

  renderPage();
  showState("table");
}

/* =========================================================
   Loading from the server (the only place that calls the GET API)
   ========================================================= */

/* Loads ALL shipments. The backend allows at most 100 per request,
   so ask in chunks until nothing is left. */
function fetchAllShipments() {
  var all = [];

  function fetchChunk(page) {
    return getShipmentList({ page: page, size: 10 }).then(function (result) {
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

function loadShipments() {
  var myToken = ++loadToken;
  dataReady = false;                // filters are ignored until this load has finished

  showState("loading");

  fetchAllShipments()
    .then(function (list) {
      if (myToken !== loadToken) return;          // a newer load started: ignore this answer

      allRecords = Array.isArray(list) ? list : [];                       // replace the master copy
      buildEquipmentFilter();                     // equipment that appears in the shipments
      dataReady = true;
      refreshView();                              // re-applies whatever is typed in the filters
    })
    .catch(function (error) {
      if (myToken !== loadToken) return;
      console.error("Could not load shipments:", error);   // shows the file and line in the Console (F12)
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

  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  var start = (currentPage - 1) * PAGE_SIZE;
  var pageItems = filteredRecords.slice(start, start + PAGE_SIZE);

  renderShipmentTable(pageItems);

  document.getElementById("pageInfo").textContent =
    "Showing " + (start + 1) + "–" + (start + pageItems.length) + " of " + totalItems;
  document.getElementById("pageLabel").textContent =
    "Page " + currentPage + " of " + totalPages;

  document.getElementById("prevBtn").disabled = (currentPage <= 1);
  document.getElementById("nextBtn").disabled = (currentPage >= totalPages);
}

/* Builds one <tr> per shipment (all fields) */
function renderShipmentTable(list) {
  var tbody = document.getElementById("shipmentBody");
  var html = "";

  shipmentById = {};
  list.forEach(function (item) {
    shipmentById[item.shipmentId] = item;
  });

  list.forEach(function (item) {
    // decided PER ROW, so it must be inside the loop
    var canChange = (SHIPMENT_TRANSITIONS[item.shipmentStatus] || []).length > 0;
    var statusAction = canChange
      ? "<button type='button' class='btn btn-primary btn-sm status-btn'" +
          " data-id='" + escapeHtml(item.shipmentId) + "'>Change Status</button>"
      : "<span class='muted'>Final</span>";

    html += "<tr>" +
      "<td>" + escapeHtml(item.shipmentId) + "</td>" +
      "<td class='nowrap'>" + escapeHtml(item.trackingNumber) + "</td>" +
      "<td>" + escapeHtml(item.equipmentId) + "</td>" +
      "<td>" + (escapeHtml(item.equipmentName) || "—") + "</td>" +
      "<td class='nowrap'>" + formatDate(item.shipmentDate) + "</td>" +
      "<td class='nowrap'>" + formatDate(item.expectedDeliveryDate) + "</td>" +
      "<td class='nowrap'>" + formatDate(item.actualDeliveryDate) + "</td>" +
      "<td><span class='badge " + statusClass(item.shipmentStatus) + "'>" +
            escapeHtml(item.shipmentStatus) + "</span></td>" +
      "<td><span class='cell-clip'>" + (escapeHtml(item.notes) || "—") + "</span></td>" +
      "<td class='nowrap'>" + formatDateTime(item.createdDate) + "</td>" +
      "<td class='nowrap'>" + formatDateTime(item.updatedDate) + "</td>" +
      "<td><div class='row-actions'>" +
            "<button type='button' class='btn btn-secondary btn-sm edit-btn'" +
            " data-id='" + escapeHtml(item.shipmentId) + "'>Edit</button>" +
            statusAction +
      "</div></td>" +
      "</tr>";
  });

  tbody.innerHTML = html;
}

/* =========================================================
   Change status popup (PATCH /api/shipments/:id/status)
   ========================================================= */

/* Today as YYYY-MM-DD in the user's local time */
function todayIso() {
  var d = new Date();
  function pad(n) { return (n < 10 ? "0" : "") + n; }
  return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate());
}

function openStatusModal(item) {
  statusTarget = {
    id:           item.shipmentId,
    name:         item.trackingNumber,
    status:       item.shipmentStatus,
    shipmentDate: item.shipmentDate
  };

  document.getElementById("statusModalName").textContent =
    item.trackingNumber + (item.equipmentName ? " – " + item.equipmentName : "");

  var badge = document.getElementById("statusModalCurrent");
  badge.textContent = item.shipmentStatus;
  badge.className = "badge " + statusClass(item.shipmentStatus);

  // fill the dropdown with ONLY the logical next statuses
  var select = document.getElementById("newStatusSelect");
  select.innerHTML = "<option value=''>Select new status</option>";
  (SHIPMENT_TRANSITIONS[item.shipmentStatus] || []).forEach(function (status) {
    var option = document.createElement("option");
    option.value = status;
    option.textContent = status;
    select.appendChild(option);
  });

  document.getElementById("deliveryDateField").classList.add("hidden");
  document.getElementById("actualDeliveryDate").value = "";
  setModalError("");
  setDateError("");
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

/* Error under the "New status" dropdown */
function setModalError(message) {
  var select = document.getElementById("newStatusSelect");
  document.getElementById("newStatusError").textContent = message;
  select.closest(".field").classList.toggle("invalid", message !== "");
}

/* Error under the "Actual Delivery Date" input */
function setDateError(message) {
  var input = document.getElementById("actualDeliveryDate");
  document.getElementById("actualDeliveryDateError").textContent = message;
  input.closest(".field").classList.toggle("invalid", message !== "");
}

/* Shows the delivery-date field only when "Delivered" is selected */
function onNewStatusChange() {
  setModalError("");
  setDateError("");

  var isDelivered = (document.getElementById("newStatusSelect").value === "Delivered");
  document.getElementById("deliveryDateField").classList.toggle("hidden", !isDelivered);

  var dateInput = document.getElementById("actualDeliveryDate");
  if (isDelivered && dateInput.value === "") dateInput.value = todayIso();   // sensible default
}

function confirmStatusChange() {
  if (!statusTarget) return;

  var newStatus = document.getElementById("newStatusSelect").value;
  setModalError("");
  setDateError("");

  if (newStatus === "") {
    setModalError("Please select a new status.");
    return;
  }

  var actualDate = null;

  if (newStatus === "Delivered") {
    actualDate = document.getElementById("actualDeliveryDate").value;
    var shipmentDate = String(statusTarget.shipmentDate).substring(0, 10);

    if (actualDate === "") {
      setDateError("Actual delivery date is required.");
      return;
    }
    if (!isValidIsoDate(actualDate)) {
      setDateError("Enter a valid delivery date.");
      return;
    }
    if (isValidIsoDate(shipmentDate) && actualDate < shipmentDate) {
      setDateError("Actual delivery date cannot be before the shipment date.");
      return;
    }
  }

  var target = statusTarget;                      // closeStatusModal() clears statusTarget
  var confirmBtn = document.getElementById("confirmStatusBtn");
  confirmBtn.disabled = true;                     // prevent double submit
  confirmBtn.textContent = "Saving...";

  changeShipmentStatus(target.id, newStatus, actualDate)
    .then(function () {
      closeStatusModal();
      showMessage("success",
        "Status of \"" + target.name + "\" changed from " + target.status + " to " + newStatus + ".");
      loadShipments();                            // the saved data changed: refresh the master copy ONCE
    })
    .catch(function (error) {
      // a message about the delivery date belongs under the date field
      if (/delivery date/i.test(error.message)) {
        setDateError(error.message);
      } else {
        setModalError(error.message);
      }
      resetConfirmButton();
    });
}