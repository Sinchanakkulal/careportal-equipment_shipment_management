package services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

import dto.EquipmentRequest;
import dto.EquipmentResponse;
import exceptions.DuplicateResourceException;
import exceptions.ResourceNotFoundException;
import exceptions.ValidationException;
import models.Equipment;
import models.FieldChange;
import models.enums.EquipmentStatus;
import repositories.EquipmentRepository;
import validators.EquipmentValidator;
import dto.StatusChangeRequest;
import validators.StatusTransitionRules;
import services.Common;

/** Business rules for equipment. No HTTP and no SQL in this class. */
@Singleton
public class EquipmentService {

    private final Common common;
    private final EquipmentRepository equipmentRepository;
    private final ActivityService activityService;

    @Inject
    public EquipmentService(Common common,

                            EquipmentRepository equipmentRepository,
                            ActivityService activityService) {
        this.common = common;
        this.equipmentRepository = equipmentRepository;
        this.activityService = activityService;
    }

    /* =====================================================
       STEP 3: create
       ===================================================== */

    /**
     * Adds equipment: validate, then in ONE transaction reject a duplicate serial number,
     * save (status is always Requested) and write ONE activity record.
     */
    public EquipmentResponse create(EquipmentRequest request, String userId) {

        EquipmentValidator.validate(request);              // before touching the database

        Connection connect = null;
        try{
            connect = common.openTransaction();

            if (equipmentRepository.existsBySerialNumber(connect,request.getSerialNumber())) {
                throw new DuplicateResourceException("Serial number already exists");
            }

            Equipment equipment = new Equipment();
            equipment.setEquipmentName(request.getEquipmentName());
            equipment.setEquipmentType(request.getEquipmentType());
            equipment.setManufacturer(request.getManufacturer());
            equipment.setModelNumber(request.getModelNumber());
            equipment.setSerialNumber(request.getSerialNumber());
            equipment.setAssignedPatientId(request.getAssignedPatientId());
            equipment.setNotes(request.getNotes());
            equipment.setStatus(EquipmentStatus.REQUESTED);          // rule E1

            long newId = equipmentRepository.insert(connect,equipment);

            Equipment saved = equipmentRepository.findById(connect,newId)
                    .orElseThrow(() -> new IllegalStateException("Equipment " + newId + " missing after insert"));

            activityService.logEquipmentAdded(connect,saved, userId);

            EquipmentResponse response = EquipmentResponse.from(saved);
            common.commit(connect);
            return response;
        } catch (SQLException e){
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        } finally{
            common.closeConnection(connect);
        }
    }

    /* =====================================================
       STEP 4: list (search, filter, paginate) and get one
       ===================================================== */

    public List<EquipmentResponse> list() {

    Connection connect = null;
    try {
        connect = common.openConnection();

        List<EquipmentResponse> items = new ArrayList<>();
        for (Equipment equipment : equipmentRepository.findAll(connect)) {
            items.add(EquipmentResponse.from(equipment));
        }
        return items;
    } catch (SQLException e) {
        throw new RuntimeException("Database error: " + e.getMessage(), e);
    } finally {
        common.closeConnection(connect);
    }
}
        

    public EquipmentResponse getById(long id) {
        Connection connect = null;
        try{
            connect = common.openConnection();
            Equipment equipment = equipmentRepository.findById(connect,id)
                .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_RECORD_NOT_FOUND));
            return EquipmentResponse.from(equipment);
        }catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        } finally {
            common.closeConnection(connect);
        }
    }

    /* =====================================================
       STEP 5: update
       ===================================================== */

    /**
     * Updates equipment details (not the status).
     * 1. validate   2. start a transaction   3. lock and read the current row (404 if missing)
     * 4. reject a serial number used by ANOTHER record   5. compare old and new
     * 6. nothing changed -> stop (no update, no activity)
     * 7. save   8. write ONE activity record with only the changed fields   9. commit
     */
    public EquipmentResponse update(long id, EquipmentRequest request, String userId) {

        EquipmentValidator.validate(request);
        Connection connect = null;
        try{
            connect = common.openConnection();
            Equipment current = equipmentRepository.findByIdForUpdate(connect, id)
                    .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_RECORD_NOT_FOUND));

            if (equipmentRepository.existsBySerialNumberOfOtherEquipment(
                    connect, request.getSerialNumber(), id)) {
                throw new DuplicateResourceException("Serial number already exists");
            }


            // compare BEFORE changing anything: these are the old values for the log
            List<FieldChange> changes = new ArrayList<>();
            addIfChanged(changes, "equipmentName",     "name",                current.getEquipmentName(),     request.getEquipmentName());
            addIfChanged(changes, "equipmentType",     "type",                current.getEquipmentType(),     request.getEquipmentType());
            addIfChanged(changes, "manufacturer",      "manufacturer",        current.getManufacturer(),      request.getManufacturer());
            addIfChanged(changes, "modelNumber",       "model",               current.getModelNumber(),       request.getModelNumber());
            addIfChanged(changes, "serialNumber",      "serial number",       current.getSerialNumber(),      request.getSerialNumber());
            addIfChanged(changes, "assignedPatientId", "assigned patient ID", current.getAssignedPatientId(), request.getAssignedPatientId());
            addIfChanged(changes, "notes",             "notes",               current.getNotes(),             request.getNotes());

            if (changes.isEmpty()) {
                return EquipmentResponse.from(current);       // nothing to save, nothing to log
            }

            current.setEquipmentName(request.getEquipmentName());
            current.setEquipmentType(request.getEquipmentType());
            current.setManufacturer(request.getManufacturer());
            current.setModelNumber(request.getModelNumber());
            current.setSerialNumber(request.getSerialNumber());
            current.setAssignedPatientId(request.getAssignedPatientId());
            current.setNotes(request.getNotes());
            // status is deliberately not copied: it only changes through the status endpoint

            equipmentRepository.update(connect, current);

            Equipment saved = equipmentRepository.findById(connect, id)
                    .orElseThrow(() -> new IllegalStateException("Equipment " + id + " missing after update"));

            activityService.logEquipmentEdited(connect, id, changes, userId);

            return EquipmentResponse.from(saved);

        }catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        } finally {
            common.closeConnection(connect);
        }
    }

    /* =====================================================
       STEP 6: change status
       ===================================================== */

    /**
     * 1. read the requested status (400 if missing or unknown)
     * 2. start a transaction   3. lock and read the current row (404 if missing)
     * 4. check the transition rules (409 if not allowed)
     * 5. save the new status   6. write ONE activity record   7. commit
     */
    public EquipmentResponse changeStatus(long id, StatusChangeRequest request, String userId) {

        final EquipmentStatus target = parseStatus(request.getStatus());

        Connection connect = null;
        try{
            connect = common.openConnection();
            Equipment current = equipmentRepository.findByIdForUpdate(connect, id)
                    .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_RECORD_NOT_FOUND));

            EquipmentStatus oldStatus = current.getStatus();

            StatusTransitionRules.checkEquipment(oldStatus, target);     // throws -> nothing is saved

            equipmentRepository.updateStatus(connect, id, target);

            Equipment saved = equipmentRepository.findById(connect, id)
                    .orElseThrow(() -> new IllegalStateException("Equipment " + id + " missing after status change"));

            activityService.logEquipmentStatusChanged(connect, id, oldStatus, target, userId);

            return EquipmentResponse.from(saved);
        }catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        } finally {
            common.closeConnection(connect);
        }
    }

    /** "Approved" -> APPROVED. Missing or unknown text -> 400. */
    private static EquipmentStatus parseStatus(String text) {
        String value = blankToNull(text);
        if (value == null) {
            throw new ValidationException("Status is required.");
        }
        try {
            return EquipmentStatus.fromLabel(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid status. Allowed values: Requested, Approved, Active, Inactive, Returned.");
        }
    }
    
    /* ---------- helpers ---------- */

    private static void addIfChanged(List<FieldChange> changes, String key, String label,
                                     String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(key, label, oldValue, newValue));
        }
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}