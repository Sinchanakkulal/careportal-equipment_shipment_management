/* =========================================================
   CarePortal – equipment-form.js
   Add + Edit equipment in a popup on equipment.html

   - Edit opens from the row data already on the page (no GET)
   - Save closes the popup, shows a message at the top, reloads the list ONCE
   - The popup closes only from the x / Cancel buttons (or after a save)
   - You can edit the same record again and again without reloading the page
   ========================================================= */

var FORM_FIELDS = [
  { id: "equipmentName",     label: "Equipment name",      required: true,  max: 100 },
  { id: "equipmentType",     label: "Equipment type",      required: true,  max: 50  },
  { id: "manufacturer",      label: "Manufacturer",        required: true,  max: 100 },
  { id: "modelNumber",       label: "Model number",        required: true,  max: 50  },
  { id: "serialNumber",      label: "Serial number",       required: true,  max: 50  },
  { id: "assignedPatientId", label: "Assigned patient ID", required: false, max: 50  },
  { id: "notes",             label: "Notes",               required: false, max: 500 }
];

var editId = null;                  // null = add mode, a number = edit mode
var openedFromHome = false;         // true when the page was opened with ?add=1
var SAVE_LABEL_ADD  = "Save Equipment";
var SAVE_LABEL_EDIT = "Update Equipment";

document.addEventListener("DOMContentLoaded", function () {

  document.getElementById("equipmentForm").addEventListener("submit", handleSubmit);

  // remove the red error as soon as the user starts fixing the field
  FORM_FIELDS.forEach(function (f) {
    var input = document.getElementById(f.id);
    input.addEventListener("input", function () { setFieldError(f.id, ""); });
    input.addEventListener("change", function () { setFieldError(f.id, ""); });
  });

  // open for Add
  document.getElementById("addEquipmentBtn").addEventListener("click", function () {
    openEquipmentForm(null);
  });

  // open for Edit (buttons are created by equipment.js, so listen on the table body)
  document.getElementById("equipmentBody").addEventListener("click", function (event) {
    var btn = event.target.closest(".edit-btn");
    if (btn) openEquipmentForm(btn.dataset.id);
  });

  // close: ONLY the x button and the Cancel button
  document.getElementById("closeFormBtn").addEventListener("click", handleCloseClick);
  document.getElementById("cancelFormBtn").addEventListener("click", handleCloseClick);

  // the Home 
  //  link "+ Add Equipment" goes to equipment.html?add=1
  if (new URLSearchParams(window.location.search).get("add") === "1") {
    openedFromHome = true;
    history.replaceState(null, "", "equipment.html");     // a refresh will not reopen the popup
    openEquipmentForm(null);
  }
});

/* ---------- open / close ---------- */

/* id = null -> Add.  id = number -> Edit (data comes from the table row) */
function openEquipmentForm(id) {
  resetForm();

  if (id === null) {
    editId = null;
    document.getElementById("formTitle").textContent = "Add Equipment";
    document.getElementById("submitBtn").textContent = SAVE_LABEL_ADD;
    showFormModal();
    return;
  }

  // never trust the value: the id must be a whole number
  if (!/^\d+$/.test(String(id))) {
    showMessage("error", "Invalid equipment id.");
    return;
  }

  var item = equipmentById[String(id)];          // filled by renderEquipmentTable (equipment.js)
  if (!item) {
    showMessage("error", "This record is not in the list any more. Please refresh the page.");
    return;
  }

  editId = String(id);
  document.getElementById("formTitle").textContent = "Edit Equipment";
  document.getElementById("submitBtn").textContent = SAVE_LABEL_EDIT;
  showFormModal();
  fillForm(item);
}

function showFormModal() {
  document.getElementById("equipmentFormModal").classList.remove("hidden");
  document.getElementById("equipmentName").focus();
}

function closeEquipmentForm() {
  document.getElementById("equipmentFormModal").classList.add("hidden");
  editId = null;
}

/* x or Cancel: close without saving */
function handleCloseClick() {
  closeEquipmentForm();

  if (openedFromHome) {
    window.location.href = "index.html";        // back to Home
  }
}

/* Puts the popup back into a clean, fully usable state every time it opens */
function resetForm() {
  document.getElementById("equipmentForm").reset();
  FORM_FIELDS.forEach(function (f) { setFieldError(f.id, ""); });
  document.getElementById("statusField").classList.add("hidden");
  hideFormError();

  // make sure NOTHING is left disabled from an earlier save
  var controls = document.querySelectorAll("#equipmentForm input, #equipmentForm select, #equipmentForm textarea");
  for (var i = 0; i < controls.length; i++) {
    controls[i].disabled = false;
  }

  var submitBtn = document.getElementById("submitBtn");
  submitBtn.disabled = false;
  submitBtn.textContent = SAVE_LABEL_ADD;

  document.getElementById("cancelFormBtn").disabled = false;
  document.getElementById("closeFormBtn").disabled = false;
}

/* Puts the record's values into the inputs */
function fillForm(item) {
  FORM_FIELDS.forEach(function (f) {
    var value = item[f.id];
    document.getElementById(f.id).value = (value === null || value === undefined) ? "" : value;
  });

  var badge = document.getElementById("statusBadge");
  badge.textContent = item.status;
  badge.className = "badge " + statusClass(item.status);
  document.getElementById("statusField").classList.remove("hidden");
}

/* ---------- error shown INSIDE the popup ---------- */
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

/* ---------- validation ---------- */

function setFieldError(id, message) {
  var input = document.getElementById(id);
  var errorSpan = document.getElementById(id + "Error");
  var fieldBox = input.closest(".field");

  errorSpan.textContent = message;
  fieldBox.classList.toggle("invalid", message !== "");
}

function validateForm() {
  var allValid = true;
  var firstInvalid = null;

  FORM_FIELDS.forEach(function (f) {
    var value = document.getElementById(f.id).value.trim();
    var message = "";

    if (f.required && value === "") {
      message = f.label + " is required.";
    } else if (value.length > f.max) {
      message = f.label + " must be " + f.max + " characters or less.";
    }

    setFieldError(f.id, message);

    if (message !== "") {
      allValid = false;
      if (!firstInvalid) firstInvalid = f.id;
    }
  });

  if (firstInvalid) document.getElementById(firstInvalid).focus();
  return allValid;
}

function getFormData() {
  function val(id) { return document.getElementById(id).value.trim(); }

  return {
    equipmentName:     val("equipmentName"),
    equipmentType:     val("equipmentType"),
    manufacturer:      val("manufacturer"),
    modelNumber:       val("modelNumber"),
    serialNumber:      val("serialNumber"),
    assignedPatientId: val("assignedPatientId") || null,
    notes:             val("notes") || null
  };
}

/* ---------- submit: create (POST) or update (PUT) ---------- */
function handleSubmit(event) {
  event.preventDefault();
  hideFormError();

  if (!validateForm()) return;

  var submitBtn = document.getElementById("submitBtn");
  var closeBtn  = document.getElementById("closeFormBtn");
  var cancelBtn = document.getElementById("cancelFormBtn");
  var isEdit = (editId !== null);
  var editedId = editId;                   // closeEquipmentForm() clears editId

  submitBtn.disabled = true;               // prevent double submit
  submitBtn.textContent = "Saving...";
  closeBtn.disabled = true;                // do not close while the request is running
  cancelBtn.disabled = true;

  var request = isEdit
    ? updateEquipment(editId, getFormData())
    : createEquipment(getFormData());

  request
    .then(function (updated) {
      // keep the stored row current, so editing the same record again shows the new values
      if (isEdit && updated) {
        equipmentById[editedId] = updated;
      }

      var message = isEdit
        ? "Equipment updated successfully."
        : "Equipment saved successfully.";

      closeEquipmentForm();                  // close the popup right away

      if (openedFromHome) {
        // go back to Home and show the message there
        try {
          sessionStorage.setItem("flashMessage", JSON.stringify({ type: "success", text: message }));
        } catch (e) { /* storage blocked: the page still works, just no message */ }
        window.location.href = "index.html";
        return;
      }

      showMessage("success", message);       // green banner at the top of the page

      if(!isEdit) {
        resetFilterInputs();
        currentPage = 1;
      }
      try {
        loadEquipment();                     // the ONE list request (function in equipment.js)
      } catch (e) {
        console.error("Could not refresh the list:", e);
      }
    })
    .catch(function (error) {
      showFormError(error.message);          // e.g. "Serial number already exists"
      submitBtn.disabled = false;
      submitBtn.textContent = isEdit ? SAVE_LABEL_EDIT : SAVE_LABEL_ADD;
      cancelBtn.disabled = false;
      closeBtn.disabled = false;
    });
}