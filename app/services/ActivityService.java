package services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.ActivityLog;
import models.Equipment;
import models.FieldChange;
import models.Shipment;
import models.enums.ActionType;
import models.enums.EntityType;
import models.enums.EquipmentStatus;
import models.enums.ShipmentStatus;
import play.libs.Json;
import repositories.ActivityRepository;
import dto.ActivityResponse;

/** The only writer of activity records. Always uses the caller's Connection (same transaction). */
@Singleton
public class ActivityService {

    // curly quotes, as in the design doc's message format (escapes avoid file-encoding problems)
    private static final String OPEN = "\u201C";
    private static final String CLOSE = "\u201D";
    private static final String ELLIPSIS = "\u2026";

    private static final int MAX_DESCRIPTION = 500;   // activity_log.description is VARCHAR(500)
    private static final int MAX_SHOWN_VALUE = 60;    // longest old/new value shown in a description


    private final Common common;
    private final ActivityRepository repository;

    @Inject
    public ActivityService(Common common, ActivityRepository repository) {
        this.common = common;
        this.repository = repository;
    }

    /* =====================================================
       EQUIPMENT
       ===================================================== */

    /** Equipment "{name}" was added.  old_value = null, new_value = key details (JSON) */
    public void logEquipmentAdded(Connection connection,Equipment equipment, String userId)
            throws SQLException {

        ObjectNode details = Json.newObject();
        details.put("equipmentName", equipment.getEquipmentName());
        details.put("equipmentType", equipment.getEquipmentType());
        details.put("manufacturer", equipment.getManufacturer());
        details.put("modelNumber", equipment.getModelNumber());
        details.put("serialNumber", equipment.getSerialNumber());
        details.put("assignedPatientId", equipment.getAssignedPatientId());
        details.put("status", equipment.getStatus().getLabel());

        save(connection,EntityType.EQUIPMENT, equipment.getEquipmentId(), ActionType.ADD,
             "Equipment " + OPEN + equipment.getEquipmentName() + CLOSE + " was added",
             null, details.toString(), userId);
    }

    /** One field: Equipment model changed from "A" to "B".  Many: Equipment manufacturer, notes changed */
    public void logEquipmentEdited(Connection connection, long equipmentId,

                                   List<FieldChange> changes, String userId) throws SQLException {
        logEdit(connection, EntityType.EQUIPMENT, "Equipment", equipmentId, changes, userId);
    }

    /** Equipment status changed from "Requested" to "Approved".  old_value / new_value = plain text */
    public void logEquipmentStatusChanged(Connection connection, long equipmentId,
                                          EquipmentStatus oldStatus, EquipmentStatus newStatus,
                                          String userId) throws SQLException {

        save(connection, EntityType.EQUIPMENT, equipmentId, ActionType.STATUS_CHANGE,
             "Equipment status changed from " + OPEN + oldStatus.getLabel() + CLOSE +
             " to " + OPEN + newStatus.getLabel() + CLOSE,
             oldStatus.getLabel(), newStatus.getLabel(), userId);
    }

    /* =====================================================
       SHIPMENT (STEP 7)
       ===================================================== */

    /** Shipment "TRK1001" was created for equipment 101.  old_value = null, new_value = key details (JSON) */
    public void logShipmentCreated(Connection connection, Shipment shipment, String userId)
            throws SQLException {

        ObjectNode details = Json.newObject();
        details.put("trackingNumber", shipment.getTrackingNumber());
        details.put("equipmentId", shipment.getEquipmentId());
        details.put("shipmentDate", shipment.getShipmentDate().toString());
        details.put("expectedDeliveryDate", shipment.getExpectedDeliveryDate().toString());
        details.put("status", shipment.getStatus().getLabel());

        save(connection, EntityType.SHIPMENT, shipment.getShipmentId(), ActionType.ADD,
             "Shipment " + OPEN + shipment.getTrackingNumber() + CLOSE +

             " was created for equipment " + shipment.getEquipmentId(),
             null, details.toString(), userId);
    }

    /** One field: Shipment notes changed from "A" to "B".  Many: Shipment expected delivery date, notes changed */
    public void logShipmentEdited(Connection connection, long shipmentId,
                                  List<FieldChange> changes, String userId) throws SQLException {
        logEdit(connection, EntityType.SHIPMENT, "Shipment", shipmentId, changes, userId);
    }

    /** Shipment status changed from "Dispatched" to "In Transit".  old_value / new_value = plain text */
    public void logShipmentStatusChanged(Connection connection, long shipmentId,
                                         ShipmentStatus oldStatus, ShipmentStatus newStatus,
                                         String userId) throws SQLException {

        save(connection, EntityType.SHIPMENT, shipmentId, ActionType.STATUS_CHANGE,
             "Shipment status changed from " + OPEN + oldStatus.getLabel() + CLOSE +
             " to " + OPEN + newStatus.getLabel() + CLOSE,
             oldStatus.getLabel(), newStatus.getLabel(), userId);
    }
        /* =====================================================
       READ HISTORY (STEP 8)
       ===================================================== */

    /** Newest first. entityId may only be used together with entityType. */
    public List<ActivityResponse> list() {

        Connection connection= null;
        try{
            connection = common.openConnection();
            List<ActivityResponse> items = new ArrayList<>();
            for(ActivityLog log: repository.findAll(connection)){
                items.add(ActivityResponse.from(log));
            }
            return items;
        }catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }finally {
        common.closeConnection(connection);
        }
    }

    /* =====================================================
       shared helpers
       ===================================================== */

    /** One edit record listing only the changed fields; old/new values are JSON of those fields. */
    private void logEdit(Connection connection, EntityType entityType, String entityWord,
                         long entityId, List<FieldChange> changes, String userId) throws SQLException {

        ObjectNode oldValues = Json.newObject();
        ObjectNode newValues = Json.newObject();
        List<String> labels = new ArrayList<>();


        for (FieldChange change : changes) {
            oldValues.put(change.getKey(), change.getOldValue());    // null is stored as JSON null
            newValues.put(change.getKey(), change.getNewValue());
            labels.add(change.getLabel());
        }

        String description;
        if (changes.size() == 1) {
            FieldChange only = changes.get(0);
            description = entityWord + " " + only.getLabel() +
                          " changed from " + quote(only.getOldValue()) +
                          " to " + quote(only.getNewValue());
        } else {
            description = entityWord + " " + String.join(", ", labels) + " changed";
        }

        save(connection, entityType, entityId, ActionType.EDIT,
             description, oldValues.toString(), newValues.toString(), userId);
    }

    private void save(Connection connection, EntityType entityType, long entityId, ActionType actionType,
                      String description, String oldValue, String newValue, String userId)
            throws SQLException {

        ActivityLog log = new ActivityLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setActionType(actionType);
        log.setDescription(limit(description));
        log.setOldValue(oldValue);
        log.setNewValue(newValue);

        log.setUserId(userId);

        repository.insert(connection, log);
    }

    /** "value", shortened if long; "(empty)" for null. */
    private static String quote(String value) {
        String shown = (value == null) ? "(empty)" : shorten(value, MAX_SHOWN_VALUE);
        return OPEN + shown + CLOSE;
    }

    private static String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + ELLIPSIS;
    }

    /** Safety net: the column holds 500 characters. */
    private static String limit(String text) {
        return text.length() <= MAX_DESCRIPTION
                ? text
                : text.substring(0, MAX_DESCRIPTION - 1) + ELLIPSIS;
    }
}