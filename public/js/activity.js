var currentPage = 1;          // page the user is viewing (1-based)
var allActivity = []; 
var filteredActivity = [];
var PAGE_SIZE = 10; 

/* Display names and badge colours (the backend sends ADD, EDIT, STATUS_CHANGE) */
var ACTION_LABELS  = { ADD: "Add", EDIT: "Edit", STATUS_CHANGE: "Status Change" };
var ACTION_CLASSES = { ADD: "badge-add", EDIT: "badge-edit", STATUS_CHANGE: "badge-status-change" };

var ENTITY_LABELS = { EQUIPMENT: "Equipment", SHIPMENT: "Shipment" };
var entityNames = { EQUIPMENT: {}, SHIPMENT: {} };

document.addEventListener("DOMContentLoaded", function () {
  document.getElementById("entityTypeFilter").addEventListener("change", applyFilters);
  document.getElementById("searchBox").addEventListener("input", debounce(applyFilters, 300));
  document.getElementById("clearBtn").addEventListener("click", clearFilters);
  document.getElementById("retryBtn").addEventListener("click", fetchAllActivity);

  document.getElementById("prevBtn").addEventListener("click", function () {
    if (currentPage > 1) { currentPage--; renderPage(); }
  });
  document.getElementById("nextBtn").addEventListener("click", function () {
    var totalPages = Math.max(1, Math.ceil(filteredActivity.length / PAGE_SIZE));
    if (currentPage < totalPages) { currentPage++; renderPage(); }
  });

  loadEntityNames().then(fetchAllActivity);
});


function fetchAllActivity() {
  showState("loading");

  getActivityList()
    .then(function (result) {
      allActivity = Array.isArray(result) ? result : [];
      applyFilters();
    })
    .catch(function (error) {
      document.getElementById("errorText").textContent = error.message;
      showState("error");
    });
}


function loadEntityNames() {
  function toList(result) {
    return Array.isArray(result) ? result : (result && result.items) || [];
  }

  var equipmentPromise = getEquipmentList()
    .then(function (result) {
      toList(result).forEach(function (e) {
        entityNames.EQUIPMENT[e.equipmentId] = { name: e.equipmentName };
      });
    })
    .catch(function () { console.warn("Could not load equipment names for the log."); });

  var shipmentPromise = getShipmentList()
    .then(function (result) {
      toList(result).forEach(function (s) {
        entityNames.SHIPMENT[s.shipmentId] = {
          name: s.trackingNumber,
          equipment: s.equipmentName
        };
        // shipments also tell us equipment names that the equipment call may have missed
        if (s.equipmentId && !entityNames.EQUIPMENT[s.equipmentId] && s.equipmentName) {
          entityNames.EQUIPMENT[s.equipmentId] = { name: s.equipmentName };
        }
      });
    })
    .catch(function () { console.warn("Could not load shipment names for the log."); });

  return Promise.all([equipmentPromise, shipmentPromise]);
}

/* Returns the looked-up record for an activity row, or null if unknown */
function findEntity(item) {
  var group = entityNames[item.entityType];
  return (group && group[item.entityId]) || null;
}

/* ---------- Filters ---------- */

function getFilters() {
  return {
    entityType: document.getElementById("entityTypeFilter").value,
    query: document.getElementById("searchBox").value.trim().toLowerCase()
  };
}

/* ---------- Search helpers ---------- */

/* Everything the user can see in a row, as one lowercase string (built once per row) */
function prepareSearchText(item) {
  if (item._search) return;

  var entity = findEntity(item) || {};
  var text = [
    buildDescription(item),
    ACTION_LABELS[item.actionType] || item.actionType,
    item.actionType,
    item.userId,
    item.oldValue,
    item.newValue,
    entity.name,
    entity.equipment,
    (ENTITY_LABELS[item.entityType] || item.entityType),
    item.entityId,
    formatDateTime(item.activityTimestamp)
  ].filter(function (v) { return v !== null && v !== undefined; })
   .join(" ").toLowerCase();

  item._search = text;
  item._words = text.split(/[^a-z0-9]+/).filter(Boolean);
}

/* Number of single-letter edits (add / remove / change) between two words */
function editDistance(a, b) {
  var prev = [], curr = [], i, j;
  for (j = 0; j <= b.length; j++) prev[j] = j;
  for (i = 1; i <= a.length; i++) {
    curr[0] = i;
    for (j = 1; j <= b.length; j++) {
      var cost = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
      curr[j] = Math.min(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost);
    }
    var tmp = prev; prev = curr; curr = tmp;
  }
  return prev[b.length];
}

/* Score one search word against one row: 3 = whole word, 2 = partial, 1 = near match, 0 = no match */
function scoreToken(item, token) {
  if (item._words.indexOf(token) !== -1) return 3;
  if (item._search.indexOf(token) !== -1) return 2;

  // near match (typos): only for words of 4+ letters, not for numbers
  if (token.length >= 4 && !/^\d+$/.test(token)) {
    var maxDist = token.length >= 8 ? 2 : 1;
    for (var i = 0; i < item._words.length; i++) {
      var w = item._words[i];
      if (Math.abs(w.length - token.length) > maxDist && w.length > token.length) {
        w = w.slice(0, token.length);      // compare against the start of long words
      }
      if (editDistance(w, token) <= maxDist) return 1;
    }
  }
  return 0;
}

/* Total score for a row, or 0 if any search word has no match */
function scoreItem(item, tokens) {
  prepareSearchText(item);
  var total = 0;
  for (var i = 0; i < tokens.length; i++) {
    var s = scoreToken(item, tokens[i]);
    if (s === 0) return 0;
    total += s;
  }
  return total;
}


function applyFilters() {
  var f = getFilters();
  var tokens = f.query ? f.query.split(/\s+/) : [];

  // 1) type filter
  var list = allActivity.filter(function (item) {
    return !f.entityType || item.entityType === f.entityType;
  });

  // 2) general search, ranked by how well each row matches
  if (tokens.length) {
    list = list
      .map(function (item, index) {
        return { item: item, score: scoreItem(item, tokens), index: index };
      })
      .filter(function (r) { return r.score > 0; })
      .sort(function (a, b) { return (b.score - a.score) || (a.index - b.index); })
      .map(function (r) { return r.item; });
  }

  filteredActivity = list;
  currentPage = 1;
  renderPage();
}

function clearFilters() {
  document.getElementById("entityTypeFilter").value = "";
  document.getElementById("searchBox").value = "";
  applyFilters();
}

function renderPage() {
  var total = filteredActivity.length;

  if (total === 0) {
    var f = getFilters();
  
   document.getElementById("emptyHint").textContent = (f.entityType || f.query)
  ? "Try a different search or clear the filters."
  : "No activity has been recorded yet.";
    showState("empty");
    return;
  }

  var totalPages = Math.ceil(total / PAGE_SIZE);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  var start = (currentPage - 1) * PAGE_SIZE;
  var pageItems = filteredActivity.slice(start, start + PAGE_SIZE);

  renderActivityTable(pageItems);
  updatePagination(totalPages, total, start, pageItems.length);
  showState("table");
}

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

/* Updates "Showing 1–10 of 12", "Page 1 of 2" and the Previous/Next buttons */
function updatePagination(totalPages, totalItems, startIndex, shownCount) {
  var first = startIndex + 1;
  var last = startIndex + shownCount;

  document.getElementById("pageInfo").textContent =
    "Showing " + first + "–" + last + " of " + totalItems;
  document.getElementById("pageLabel").textContent =
    "Page " + currentPage + " of " + totalPages;

  document.getElementById("prevBtn").disabled = (currentPage <= 1);
  document.getElementById("nextBtn").disabled = (currentPage >= totalPages);
}


/* ---------- Rendering ---------- */

/* Builds one <tr> per activity record */
function renderActivityTable(list) {
  var tbody = document.getElementById("activityBody");
  var html = "";

  list.forEach(function (item) {
    var action = item.actionType;
    var actionLabel = ACTION_LABELS[action] || action;
    var actionClass = ACTION_CLASSES[action] || "";

    html += "<tr>" +
      "<td class='nowrap'>" + escapeHtml(formatDateTime(item.activityTimestamp)) + "</td>" +
      "<td>" + buildEntityCell(item) + "</td>" +
      "<td><span class='badge " + actionClass + "'>" + escapeHtml(actionLabel) + "</span></td>" +
      "<td>" + escapeHtml(buildDescription(item)) + "</td>" +
      "<td class='changes-cell'>" + buildChangesHtml(item) + "</td>" +
      "<td class='nowrap'>" + escapeHtml(item.userId) + "</td>" +
      "</tr>";
  });

  tbody.innerHTML = html;
}

/* "Equipment #2", linking to that record's edit page */
/* Name on the first line, "Equipment #2" on the second */
function buildEntityCell(item) {
  var typeLabel = (ENTITY_LABELS[item.entityType] || item.entityType) + " #" + item.entityId;
  var entity = findEntity(item);

  if (!entity) return escapeHtml(typeLabel);          // name unknown: show the old text

  var sub = typeLabel;
  if (item.entityType === "SHIPMENT" && entity.equipment) {
    sub += " · " + entity.equipment;                  // e.g. "Shipment #2 · Oxygen Concentrator"
  }

  return "<div class='entity-name'>" + escapeHtml(entity.name) + "</div>" +
         "<div class='entity-sub'>" + escapeHtml(sub) + "</div>";
}

/* Shows "Old" and/or "New" blocks. An ADD has no old value, so only "New" is shown. */
function buildChangesHtml(item) {
  var hasOld = item.oldValue !== null && item.oldValue !== undefined && item.oldValue !== "";
  var hasNew = item.newValue !== null && item.newValue !== undefined && item.newValue !== "";

  if (!hasOld && !hasNew) return "—";

  var html = "";
  if (hasOld) {
    html += "<div class='change-block'><span class='change-label'>Old</span><br>" +
            formatChangeValue(item.oldValue) + "</div>";
  }
  if (hasNew) {
    html += "<div class='change-block'><span class='change-label'>New</span><br>" +
            formatChangeValue(item.newValue) + "</div>";
  }
  return html;
}

/* A value is either plain text ("Requested") or JSON of changed fields.
   JSON is shown as "model number: MOD21" lines. Everything is escaped. */
function formatChangeValue(raw) {
  var parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (e) {
    return escapeHtml(raw);                         // plain text
  }

  if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
    return Object.keys(parsed).map(function (key) {
      var value = (parsed[key] === null) ? "—" : parsed[key];
      return "<span class='change-key'>" + escapeHtml(humanizeKey(key)) + ":</span> " +
             escapeHtml(value);
    }).join("<br>");
  }

  return escapeHtml(raw);                           // a number or other plain value
}

/* "modelNumber" -> "model number" */
function humanizeKey(key) {
  return String(key).replace(/([A-Z])/g, " $1").toLowerCase();
}


/* Builds the description shown in the log, with the equipment or shipment name.
   The stored description is not changed. This only affects what is displayed. */
function buildDescription(item) {
  var text = item.description || "";
  var entity = findEntity(item);
  if (!entity || !entity.name) return text;

  var OPEN = "\u201C", CLOSE = "\u201D";               // curly quotes, as in the design doc

  // "...was created for equipment 5"  ->  "...was created for equipment “Wheelchair”"
  text = text.replace(/for equipment (\d+)/i, function (whole, id) {
    var equipment = entityNames.EQUIPMENT[id];
    return equipment ? "for equipment " + OPEN + equipment.name + CLOSE : whole;
  });

  // the backend description already names the record (add equipment, create shipment): done
  if (text.indexOf(entity.name) !== -1) return text;

  // otherwise put the name in front:
  // "Equipment status changed from ..."  ->  "Equipment “Name”: status changed from ..."
  var typeWord = (ENTITY_LABELS[item.entityType] || "Record");
  var rest = text.replace(/^(Equipment|Shipment)\s+/i, "");

  var label = typeWord + " " + OPEN + entity.name + CLOSE;
  if (item.entityType === "SHIPMENT" && entity.equipment) {
    label += " (" + entity.equipment + ")";
  }
  return label + ": " + rest;
}
