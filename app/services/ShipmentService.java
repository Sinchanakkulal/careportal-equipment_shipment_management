package services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;

import dto.EquipmentResponse;
import dto.PagedResponse;
import dto.ShipmentRequest;
import dto.ShipmentResponse;
import dto.StatusChangeRequest;
import exceptions.DuplicateResourceException;
import exceptions.ResourceNotFoundException;
import exceptions.ValidationException;
import models.Equipment;
import models.FieldChange;
import models.Shipment;
import models.enums.ShipmentStatus;
import play.db.Database;
import repositories.EquipmentRepository;
import repositories.ShipmentRepository;
import validators.ShipmentValidator;
import validators.StatusTransitionRules;
import services.Common;

/** Business rules for shipments. No HTTP and no SQL in this class. */
@Singleton
public class ShipmentService {

   

    private final Common common;
    private final ShipmentRepository shipmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityService activityService;

    @Inject
    public ShipmentService(Common common,
                           ShipmentRepository shipmentRepository,
                           EquipmentRepository equipmentRepository,
                           ActivityService activityService) {
        this.common = common;
        this.shipmentRepository = shipmentRepository;
        this.equipmentRepository = equipmentRepository;
        this.activityService = activityService;
    }

    /* =====================================================
       create
       ===================================================== */

    /**
     * 1. validate   2. start a transaction   3. the equipment must exist (404)
     * 4. reject a duplicate tracking number (409)   5. save (status is always Created)
     * 6. write ONE activity record   7. commit
     */
    public ShipmentResponse create(ShipmentRequest request, String userId) {

        ShipmentValidator.validate(request);
        final LocalDate shipmentDate = ShipmentValidator.parseDate(request.getShipmentDate(), "Shipment date");
        final LocalDate expectedDate = ShipmentValidator.parseDate(request.getExpectedDeliveryDate(), "Expected delivery date");

        Connection connect = null;
        try{

            connect = common.openConnection();
            // foreign key check: a shipment's equipment must exist
            equipmentRepository.findById(connect, request.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_EQUIPMENT_MISSING));

            if (shipmentRepository.existsByTrackingNumber(connect, request.getTrackingNumber())) {
                throw new DuplicateResourceException("Tracking number already exists");
            }

            Shipment shipment = new Shipment();
            shipment.setEquipmentId(request.getEquipmentId());
            shipment.setTrackingNumber(request.getTrackingNumber());
            shipment.setShipmentDate(shipmentDate);
            shipment.setExpectedDeliveryDate(expectedDate);
            shipment.setNotes(request.getNotes());
            shipment.setStatus(ShipmentStatus.CREATED);                  // rule S1

            long newId = shipmentRepository.insert(connect, shipment);

            Shipment saved = shipmentRepository.findById(connect, newId)
                    .orElseThrow(() -> new IllegalStateException("Shipment " + newId + " missing after insert"));

            activityService.logShipmentCreated(connect, saved, userId);

            return ShipmentResponse.from(saved);
        }catch (SQLException e){
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }finally {
            common.closeConnection(connect);
        }
    }

    /* =====================================================
       list (search, filter, paginate) and get one
       ===================================================== */

    public List<ShipmentResponse> list() {
        Connection connect = null;
    try {
        connect = common.openConnection();

        List<ShipmentResponse> items = new ArrayList<>();
        for (Shipment shipment : shipmentRepository.findAll(connect)) {
            items.add(ShipmentResponse.from(shipment));
        }
        return items;
    } catch (SQLException e) {
        throw new RuntimeException("Database error: " + e.getMessage(), e);
    } finally {
        common.closeConnection(connect);
    }
}

    public ShipmentResponse getById(long id) {
        Connection connect = null;
       try{
            connect = common.openConnection();
            Shipment shipment = shipmentRepository.findById(connect, id)
                    .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_RECORD_NOT_FOUND));
            return ShipmentResponse.from(shipment);
        }catch (SQLException e){
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }finally{
            common.closeConnection(connect);
        }
    }

    /* =====================================================
       update details (not the status)
       ===================================================== */

    /**
     * 1. validate   2. start a transaction   3. lock and read the current row (404 if missing)
     * 4. equipment and tracking number must be unchanged   5. compare old and new
     * 6. nothing changed -> stop (no update, no activity)
     * 7. save   8. write ONE activity record with only the changed fields   9. commit
     */
    public ShipmentResponse update(long id, ShipmentRequest request, String userId) {


        ShipmentValidator.validate(request);
        final LocalDate shipmentDate = ShipmentValidator.parseDate(request.getShipmentDate(), "Shipment date");

        final LocalDate expectedDate = ShipmentValidator.parseDate(request.getExpectedDeliveryDate(), "Expected delivery date");

        Connection connect = null;
        try{
            connect = common.openConnection();
            Shipment current = shipmentRepository.findByIdForUpdate(connect, id)
                    .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_RECORD_NOT_FOUND));

            // a shipment stays tied to its equipment and keeps its tracking number
            if (!Objects.equals(current.getEquipmentId(), request.getEquipmentId())) {
                throw new ValidationException("The equipment of a shipment cannot be changed.");
            }
            if (!Objects.equals(current.getTrackingNumber(), request.getTrackingNumber())) {
                throw new ValidationException("The tracking number of a shipment cannot be changed.");
            }

            // keep the dates consistent once the shipment has been delivered
            if (current.getActualDeliveryDate() != null && shipmentDate.isAfter(current.getActualDeliveryDate())) {
                throw new ValidationException("Shipment date cannot be after the actual delivery date.");
            }

            // compare BEFORE changing anything: these are the old values for the log
            List<FieldChange> changes = new ArrayList<>();
            addIfChanged(changes, "shipmentDate",         "shipment date",
                         current.getShipmentDate().toString(), shipmentDate.toString());
            addIfChanged(changes, "expectedDeliveryDate", "expected delivery date",
                         current.getExpectedDeliveryDate().toString(), expectedDate.toString());
            addIfChanged(changes, "notes",                "notes",
                         current.getNotes(), request.getNotes());

            if (changes.isEmpty()) {
                // nothing to save, nothing to log
                return ShipmentResponse.from(shipmentRepository.findById(connect, id)

                        .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_RECORD_NOT_FOUND)));
            }

            current.setShipmentDate(shipmentDate);
            current.setExpectedDeliveryDate(expectedDate);
            current.setNotes(request.getNotes());
            // status is deliberately not copied: it only changes through the status endpoint

            shipmentRepository.update(connect, current);

            Shipment saved = shipmentRepository.findById(connect, id)
                    .orElseThrow(() -> new IllegalStateException("Shipment " + id + " missing after update"));

            activityService.logShipmentEdited(connect, id, changes, userId);

            return ShipmentResponse.from(saved);
        }catch (SQLException e){
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }finally{
            common.closeConnection(connect);
        }
    }

    /* =====================================================
       change status
       ===================================================== */

    /**
     * 1. read the requested status (400 if missing or unknown)
     * 2. start a transaction   3. lock and read the current row (404 if missing)
     * 4. check the transition rules (409)
     * 5. Delivered needs a valid actual delivery date that is not before the shipment date (400)
     * 6. save   7. write ONE activity record   8. commit
     */
    public ShipmentResponse changeStatus(long id, StatusChangeRequest request, String userId) {


        final ShipmentStatus target = parseStatus(request.getStatus());
        final String deliveryDateText = blankToNull(request.getActualDeliveryDate());

        Connection connect = null;
        try{

            connect = common.openConnection();
            Shipment current = shipmentRepository.findByIdForUpdate(connect, id)
                    .orElseThrow(() -> new ResourceNotFoundException(Common.MSG_RECORD_NOT_FOUND));

            ShipmentStatus oldStatus = current.getStatus();

            StatusTransitionRules.checkShipment(oldStatus, target);      // throws -> nothing is saved

            LocalDate actualDeliveryDate = current.getActualDeliveryDate();

            if (target == ShipmentStatus.DELIVERED) {
                if (deliveryDateText == null) {
                    throw new ValidationException("Actual delivery date is required for a delivered shipment.");
                }
                actualDeliveryDate = ShipmentValidator.parseDate(deliveryDateText, "Actual delivery date");

                if (actualDeliveryDate.isBefore(current.getShipmentDate())) {
                    throw new ValidationException("Actual delivery date cannot be before the shipment date.");
                }
            }
            // for any other status the actual delivery date is ignored

            shipmentRepository.updateStatus(connect, id, target, actualDeliveryDate);

            Shipment saved = shipmentRepository.findById(connect, id)
                    .orElseThrow(() -> new IllegalStateException("Shipment " + id + " missing after status change"));

            activityService.logShipmentStatusChanged(connect, id, oldStatus, target, userId);


            return ShipmentResponse.from(saved);
        }catch (SQLException e){
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }finally{
            common.closeConnection(connect);
        }
    }

    /* ---------- helpers ---------- */

    /** "In Transit" -> IN_TRANSIT. Missing or unknown text -> 400. */
    private static ShipmentStatus parseStatus(String text) {
        String value = blankToNull(text);
        if (value == null) {
            throw new ValidationException("Status is required.");
        }
        try {
            return ShipmentStatus.fromLabel(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid status. Allowed values: Created, Dispatched, In Transit, Delivered, Cancelled.");
        }
    }

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