package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Singleton;

import controllers.Inject;
import controllers.Result;
import exceptions.DuplicateResourceException;
import models.Equipment;
import models.enums.EquipmentStatus;

/** The only class that runs SQL for the equipment table. No business rules, no HTTP. */
@Singleton
public class EquipmentRepository {

    private static final String COLUMNS =
            "equipment_id, equipment_name, equipment_type, manufacturer, model_number, " +
            "serial_number, assigned_patient_id, status, notes, created_date, updated_date";

    /* ---------- duplicate checks ---------- */

    /** True if an equipment row already has this serial number. */
    public boolean existsBySerialNumber(Connection connect ,String serialNumber) throws SQLException {
        String sql = "SELECT 1 FROM equipment WHERE serial_number = ? LIMIT 1";
        try (PreparedStatement statement = connect.prepareStatement(sql)) {
            statement.setString(1, serialNumber);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** STEP 5: True if a DIFFERENT equipment row already has this serial number. */
    public boolean existsBySerialNumberOfOtherEquipment(Connection connect, String serialNumber,
                                                        long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM equipment WHERE serial_number = ? AND equipment_id <> ? LIMIT 1";
        try (PreparedStatement statement = connect.prepareStatement(sql)) {
            statement.setString(1, serialNumber);
            statement.setLong(2, excludeId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /* ---------- create ---------- */

    /** Inserts a row and returns the generated equipment_id. */
    public long insert(Connection connect, Equipment equipment) throws SQLException {
    
        String sql = "INSERT INTO equipment (equipment_name, equipment_type, manufacturer, " +
                     "model_number, serial_number, assigned_patient_id, status, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try(PreparedStatement statement = connect.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, equipment.getEquipmentName());
            statement.setString(2, equipment.getEquipmentType());
            statement.setString(3, equipment.getManufacturer());
            statement.setString(4, equipment.getModelNumber());
            statement.setString(5, equipment.getSerialNumber());
            statement.setString(6, equipment.getAssignedPatientId());   // null is stored as NULL
            statement.setString(7, equipment.getStatus().getLabel());
            statement.setString(8, equipment.getNotes());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("No generated key returned for equipment insert");

        } catch (SQLException e) {
            // 1062 = duplicate entry: catches two simultaneous requests that both passed the check
            if (e.getErrorCode() == 1062) {
                throw new DuplicateResourceException("Serial number already exists");
            }
            throw e;
        }
    }

    /* ---------- STEP 5: update ---------- */

    /** Updates the editable fields. Status is NOT touched here. updated_date refreshes by itself. */
    public void update(Connection connect, Equipment equipment) throws SQLException {
        String sql = "UPDATE equipment SET equipment_name = ?, equipment_type = ?, manufacturer = ?, " +
                     "model_number = ?, serial_number = ?, assigned_patient_id = ?, notes = ? " +
                     "WHERE equipment_id = ?";

        try (PreparedStatement statement = connect.prepareStatement(sql)) {
            statement.setString(1, equipment.getEquipmentName());
            statement.setString(2, equipment.getEquipmentType());
            statement.setString(3, equipment.getManufacturer());
            statement.setString(4, equipment.getModelNumber());
            statement.setString(5, equipment.getSerialNumber());
            statement.setString(6, equipment.getAssignedPatientId());
            statement.setString(7, equipment.getNotes());
            statement.setLong(8, equipment.getEquipmentId());
            statement.executeUpdate();

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DuplicateResourceException("Serial number already exists");
            }
            throw e;
        }
    }

        public void updateStatus(Connection connect, long id, EquipmentStatus status) throws SQLException {
        String sql = "UPDATE equipment SET status = ? WHERE equipment_id = ?";
        try (PreparedStatement statement = connect.prepareStatement(sql)) {
            statement.setString(1, status.getLabel());
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    /* ---------- read one ---------- */

    public Optional<Equipment> findById(Connection connect,long id) throws SQLException {
        return findOne(connect,id, true);
    }

    /** STEP 5: same as findById, but locks the row until the transaction ends. */
    public Optional<Equipment> findByIdForUpdate(Connection connect, long id) throws SQLException {
        return findOne(connect,id, true);
    }

    private Optional<Equipment> findOne(Connection connect, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM equipment WHERE equipment_id = ?" +
                     (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connect.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

/** Every equipment row, newest first. */

    public List<Equipment> findAll(Connection connect) throws SQLException{
        String sql = "SELECT " + COLUMNS + " FROM equipment ORDER BY equipment_id DESC";
        try(PreparedStatement statement = connect.prepareStatement(sql);
            ResultSet rs =statement.executeQuery()){

                List<Equipment> result = new ArrayList<>();
                while(rs.next()){
                    result.add(map(rs));
                }
                return result;
            }
    }



    /* ---------- row mapping ---------- */

    private Equipment map(ResultSet rs) throws SQLException {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(rs.getLong("equipment_id"));
        equipment.setEquipmentName(rs.getString("equipment_name"));
        equipment.setEquipmentType(rs.getString("equipment_type"));
        equipment.setManufacturer(rs.getString("manufacturer"));
        equipment.setModelNumber(rs.getString("model_number"));
        equipment.setSerialNumber(rs.getString("serial_number"));
        equipment.setAssignedPatientId(rs.getString("assigned_patient_id"));
        equipment.setStatus(EquipmentStatus.fromLabel(rs.getString("status")));
        equipment.setNotes(rs.getString("notes"));
        equipment.setCreatedDate(rs.getObject("created_date", LocalDateTime.class));
        equipment.setUpdatedDate(rs.getObject("updated_date", LocalDateTime.class));
        return equipment;
    }
}