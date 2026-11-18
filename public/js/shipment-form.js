/* =========================================================
   CarePortal – shipment-form.js
   Add + Edit shipment in a popup on shipments.html

   - Edit opens from the row data already on the page (NO server call)
   - Add loads the equipment list ONCE (the first time the popup opens) and keeps it
   - Save closes the popup, shows a message at the top, reloads the list ONCE
   - The popup closes only from the x / Cancel buttons (or after a save)
   - Status is NOT changed here: that is the "Change Status" popup in shipments.js
   ========================================================= */

/* One entry per input: its id, label, whether required, and max length (0 = no limit) */
var FORM_FIELDS = [
  { id: "equipmentId",          label: "Equipment",              required: true,  max: 0   },
  { id: "trackingNumber",       label: "Tracking number",        required: true,  max: 50  },
  { id: "shipmentDate",         label: "Shipment date",          required: true,  max: 0   },
  { id: "expectedDeliveryDate", label: "Expected delivery date", required: true,  max: 0   },
  { id: "notes",                label: "Notes",                  required: false, max: 500 }
];

var DATE_FIELDS = ["shipmentDate", "expectedDeliveryDate"];

var editId = null;                 // null = add mode, a number = edit mode
var openedFromHome = false;        // true when the page was opened with ?add=1
var SAVE_LABEL_ADD  = "Save Shipment";
var SAVE_LABEL_EDIT = "Update Shipment";

var equipmentOptions = null;       // null = not loaded yet; an array once loaded (kept for this page visit)
var equipmentRequest = null;       // the request that is running right now (so two opens share ONE request)

document.addEventListener("DOMContentLoaded", function () {

  document.getElementById("shipmentForm").addEventListener("submit", handleSubmit);

  // remove the red error as soon as the user starts fixing the field
  FORM_FIELDS.forEach(function (f) {
    var input = document.getElementById(f.id);
    input.addEventListener("input", function () { setFieldError(f.id, ""); });
    input.addEventListener("change", function () { setFieldError(f.id, ""); });
  });

  // changing either date also clears the date-order error on the other one
  DATE_FIELDS.forEach(function (id) {
    document.getElementById(id).addEventListener("change", function () {
      DATE_FIELDS.forEach(function (other) { setFieldError(other, ""); });
    });
  });

  // open for Add
  document.getElementById("addShipmentBtn").addEventListener("click", function () {
    openShipmentForm(null);
  });

  // open for Edit (buttons are created by shipments.js, so listen on the table body)
  document.getElementById("shipmentBody").addEventListener("click", function (event) {
    var btn = event.target.closest(".edit-btn");
    if (btn) openShipmentForm(btn.dataset.id);
  });

  // close: ONLY the x button and the Cancel button
  document.getElementById("closeFormBtn").addEventListener("click", handleCloseClick);
  document.getElementById("cancelFormBtn").addEventListener("click", handleCloseClick);

  // the Home page link "+ Create Shipment" goes to shipments.html?add=1
  if (new URLSearchParams(window.location.search).get("add") === "1") {
    openedFromHome = true;
    history.replaceState(null, "", "shipments.html");     // a refresh will not reopen the popup
    openShipmentForm(null);
  }
});

/* ---------- open / close ---------- */

/* id = null -> Add.  id = number -> Edit (data comes from the table row) */
function openShipmentForm(id) {
  resetForm();

  if (id === null) {
    editId = null;
    document.getElementById("formTitle").textContent = "Add Shipment";
    document.getElementById("submitBtn").textContent = SAVE_LABEL_ADD;
    showFormModal("trackingNumber");
    fillEquipmentSelectForAdd();
    return;
  }

  // never trust the value: the id must be a whole number
  if (!/^\d+$/.test(String(id))) {
    showMessage("error", "Invalid shipment id.");
    return;
  }

  var item = shipmentById[String(id)];           // filled by renderShipmentTable (shipments.js)
  if (!item) {
    showMessage("error", "This shipment is not in the list any more. Please refresh the page.");
    return;
  }

  editId = String(id);
  document.getElementById("formTitle").textContent = "Edit Shipment";
  document.getElementById("submitBtn").textContent = SAVE_LABEL_EDIT;
  showFormModal("shipmentDate");

  fillEquipmentSelectForEdit(item);
  fillForm(item);
  lockFields();
}

function showFormModal(focusId) {
  document.getElementById("shipmentFormModal").classList.remove("hidden");
  var target = document.getElementById(focusId);
  if (target && !target.disabled) target.focus();
}

function isFormOpen() {
  return !document.getElementById("shipmentFormModal").classList.contains("hidden");
}

function closeShipmentForm() {
  document.getElementById("shipmentFormModal").classList.add("hidden");
  editId = null;
}

/* x or Cancel: close without saving */
function handleCloseClick() {
  closeShipmentForm();

  if (openedFromHome) {
    window.location.href = "index.html";        // back to Home
  }
}

/* Puts the popup back into a clean, fully usable state every time it opens */
function resetForm() {
  document.getElementById("shipmentForm").reset();
  FORM_FIELDS.forEach(function (f) { setFieldError(f.id, ""); });
  document.getElementById("statusField").classList.add("hidden");
  document.getElementById("lockHint").classList.add("hidden");
  hideFormError();

  // make sure NOTHING is left disabled from an earlier edit
  var controls = document.querySelectorAll("#shipmentForm input, #shipmentForm select, #shipmentForm textarea");
  for (var i = 0; i < controls.length; i++) {
    controls[i].disabled = false;
  }

  var submitBtn = document.getElementById("submitBtn");
  submitBtn.disabled = false;
  submitBtn.textContent = SAVE_LABEL_ADD;

  document.getElementById("cancelFormBtn").disabled = false;
  document.getElementById("closeFormBtn").disabled = false;
}

/* Puts the shipment's values into the inputs (equipment is handled separately) */
function fillForm(item) {
  FORM_FIELDS.forEach(function (f) {
    if (f.id === "equipmentId") return;
    var value = item[f.id];
    if (value === null || value === undefined) value = "";
    if (DATE_FIELDS.indexOf(f.id) !== -1) value = String(value).substring(0, 10);
    document.getElementById(f.id).value = value;
  });

  var badge = document.getElementById("statusBadge");
  badge.textContent = item.shipmentStatus;
  badge.className = "badge " + statusClass(item.shipmentStatus);
  document.getElementById("statusField").classList.remove("hidden");
}

/* Equipment and tracking number cannot change after creation */
function lockFields() {
  document.getElementById("equipmentId").disabled = true;
  document.getElementById("trackingNumber").disabled = true;
  document.getElementById("lockHint").classList.remove("hidden");
}

/* ---------- messages shown INSIDE the popup ---------- */
function showFormError(message) {
  var box = document.getElementById("formError");
  box.textContent = message;
  box.classList.remove("hidden");
}

function hideFormError() {
  var box = document.getElementById("formError");
  box.textContent = "";
  box.classList.add("hidden");
}

/* ---------- Equipment dropdown ---------- */

/* EDIT: the equipment cannot change, so the dropdown needs only this ONE option. No server call. */
function fillEquipmentSelectForEdit(item) {
  var select = document.getElementById("equipmentId");
  select.innerHTML = "";

  var option = document.createElement("option");
  option.value = item.equipmentId;
  option.textContent = item.equipmentName || ("Equipment " + item.equipmentId);
  select.appendChild(option);
  select.value = String(item.equipmentId);
}

/* ADD: the dropdown needs EVERY equipment (not only the ones that already have shipments) */
function fillEquipmentSelectForAdd() {
  var select = document.getElementById("equipmentId");
  var submitBtn = document.getElementById("submitBtn");

  if (equipmentOptions !== null) {               // loaded before: no server call
    drawEquipmentOptions(equipmentOptions);
    return;
  }

  select.innerHTML = "<option value=''>Loading equipment...</option>";
  select.disabled = true;
  submitBtn.disabled = true;                     // cannot save until the equipment is there

  loadEquipmentOptions()
    .then(function (list) {
      // the popup may have been closed or switched to Edit while we waited
      if (isFormOpen() && editId === null) drawEquipmentOptions(list);
    })
    .catch(function (error) {
      if (isFormOpen() && editId === null) {
        select.innerHTML = "<option value=''>Could not load equipment</option>";
        showFormError(error.message + " Close this popup and open it again to retry.");
      }
    });
}

/* Returns a Promise of the equipment list. Asks the server only the first time. */
function loadEquipmentOptions() {
  if (equipmentOptions !== null) return Promise.resolve(equipmentOptions);
  if (equipmentRequest) return equipmentRequest;

  equipmentRequest = fetchAllEquipmentList()
    .then(function (list) {
      equipmentOptions = Array.isArray(list) ? list : [];
      equipmentRequest = null;
      return equipmentOptions;
    })
    .catch(function (error) {
      equipmentRequest = null;                   // allow a retry the next time the popup opens
      throw error;
    });

  return equipmentRequest;
}

/* The backend allows at most 100 per request, so ask in chunks until nothing is left */
function fetchAllEquipmentList() {
  var all = [];

  function fetchChunk(page) {
    return getEquipmentList({ page: page, size: 100 }).then(function (result) {
      var items = Array.isArray(result) ? result : (result && result.items) || [];
      all = all.concat(items);

      var totalPages = (result && result.totalPages) || 1;
      if (page < totalPages && page < 1000) {
        return fetchChunk(page + 1);
      }
      return all;
    });
  }

  return fetchChunk(1);
}

function drawEquipmentOptions(list) {
  var select = document.getElementById("equipmentId");
  var sorted = list.slice().sort(function (a, b) {
    return String(a.equipmentName).localeCompare(String(b.equipmentName));
  });

  select.innerHTML = "";

  var placeholder = document.createElement("option");
  placeholder.value = "";
  placeholder.textContent = sorted.length ? "Select equipment" : "No equipment available";
  select.appendChild(placeholder);

  sorted.forEach(function (item) {
    var option = document.createElement("option");
    option.value = item.equipmentId;
    option.textContent = item.equipmentName + " (" + item.serialNumber + ")";   // textContent = safe
    select.appendChild(option);
  });

  select.disabled = false;
  document.getElementById("submitBtn").disabled = (sorted.length === 0);

  if (sorted.length === 0) {
    showFormError("Add equipment first, then create a shipment for it.");
  }
}

/* ---------- validation ---------- */

function setFieldError(id, message) {
  var input = document.getElementById(id);
  var errorSpan = document.getElementById(id + "Error");
  var fieldBox = input.closest(".field");

  errorSpan.textContent = message;
  fieldBox.classList.toggle("invalid", message !== "");
}

function validateForm() {
  var firstInvalid = null;

  function fail(id, message) {
    setFieldError(id, message);
    if (!firstInvalid) firstInvalid = id;
  }

  // 1) required fields and length limits
  FORM_FIELDS.forEach(function (f) {
    var value = document.getElementById(f.id).value.trim();
    var message = "";

    if (f.required && value === "") {
      message = f.label + " is required.";
    } else if (f.max > 0 && value.length > f.max) {
      message = f.label + " must be " + f.max + " characters or less.";
    }

    setFieldError(f.id, message);
    if (message !== "" && !firstInvalid) firstInvalid = f.id;
  });

  // 2) dates must be real dates
  var shipmentDate = document.getElementById("shipmentDate").value;
  var expectedDate = document.getElementById("expectedDeliveryDate").value;

  if (shipmentDate !== "" && !isValidIsoDate(shipmentDate)) {
    fail("shipmentDate", "Enter a valid shipment date.");
  }
  if (expectedDate !== "" && !isValidIsoDate(expectedDate)) {
    fail("expectedDeliveryDate", "Enter a valid expected delivery date.");
  }

  // 3) expected delivery date must not be before the shipment date
  if (isValidIsoDate(shipmentDate) && isValidIsoDate(expectedDate) && expectedDate < shipmentDate) {
    fail("expectedDeliveryDate", "Expected delivery date cannot be before the shipment date.");
  }

  if (firstInvalid) {
    var target = document.getElementById(firstInvalid);
    if (!target.disabled) target.focus();
  }
  return firstInvalid === null;
}

/* Reads the form into the JSON object we send to the backend */
function getFormData() {
  function val(id) { return document.getElementById(id).value.trim(); }

  return {
    equipmentId:          parseInt(val("equipmentId"), 10),   // the backend expects a number
    trackingNumber:       val("trackingNumber"),
    shipmentDate:         val("shipmentDate"),
    expectedDeliveryDate: val("expectedDeliveryDate"),
    notes:                val("notes") || null
  };
}

/* ---------- after a successful save: close, message at the top, refresh the list ONCE ---------- */
function finishAndRefresh(message, isNew) {
  closeShipmentForm();

  if (openedFromHome) {
    // go back to Home and show the message there
    try {
      sessionStorage.setItem("flashMessage", JSON.stringify({ type: "success", text: message }));
    } catch (e) { /* storage blocked: the page still works, just no message */ }
    window.location.href = "index.html";
    return;
  }

  showMessage("success", message);               // green banner at the top of the page

  if (isNew) {
    resetFilterInputs();                         // so an active filter cannot hide the new shipment
    currentPage = 1;                             // newest first, so it is on page 1
  }

  try {
    loadShipments();                             // the ONE list request (function in shipments.js)
  } catch (e) {
    console.error("Could not refresh the list:", e);
  }
}

/* ---------- submit: create (POST) or update (PUT) ---------- */
function handleSubmit(event) {
  event.preventDefault();

  var submitBtn = document.getElementById("submitBtn");
  if (submitBtn.disabled) return;          // a save is already running (or the equipment list is still loading)

  hideFormError();

  if (!validateForm()) return;

  var closeBtn  = document.getElementById("closeFormBtn");
  var cancelBtn = document.getElementById("cancelFormBtn");
  var isEdit = (editId !== null);

  submitBtn.disabled = true;               // prevent double submit
  submitBtn.textContent = "Saving...";
  closeBtn.disabled = true;                // do not close while the request is running
  cancelBtn.disabled = true;

  var request = isEdit
    ? updateShipment(editId, getFormData())
    : createShipment(getFormData());

  request
    .then(function () {
      finishAndRefresh(isEdit ? "Shipment updated successfully."
                              : "Shipment created successfully.", !isEdit);
    })
    .catch(function (error) {
      showFormError(error.message);        // e.g. "Tracking number already exists"
      submitBtn.disabled = false;
      submitBtn.textContent = isEdit ? SAVE_LABEL_EDIT : SAVE_LABEL_ADD;
      cancelBtn.disabled = false;
      closeBtn.disabled = false;
    });
}