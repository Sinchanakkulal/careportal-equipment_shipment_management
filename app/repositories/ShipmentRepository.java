package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Singleton;

import exceptions.DuplicateResourceException;
import exceptions.ResourceNotFoundException;
import models.Equipment;
import models.Shipment;
import models.enums.ShipmentStatus;

/** The only class that runs SQL for the shipment table. No business rules, no HTTP. */
@Singleton
public class ShipmentRepository {

    private static final String COLUMNS =
            "s.shipment_id, s.equipment_id, s.tracking_number, s.shipment_date, " +
            "s.expected_delivery_date, s.actual_delivery_date, s.shipment_status, s.notes, " +
            "s.created_date, s.updated_date";

    private static final String JOIN_EQUIPMENT =
            " FROM shipment s JOIN equipment e ON e.equipment_id = s.equipment_id";

    /* ---------- duplicate check ---------- */

    public boolean existsByTrackingNumber(Connection connection, String trackingNumber) throws SQLException {
        String sql = "SELECT 1 FROM shipment WHERE tracking_number = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trackingNumber);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /* ---------- create ---------- */

    /** Inserts a row and returns the generated shipment_id. */
    public long insert(Connection connection, Shipment shipment) throws SQLException {
        String sql = "INSERT INTO shipment (equipment_id, tracking_number, shipment_date, " +
                     "expected_delivery_date, shipment_status, notes) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, shipment.getEquipmentId());
            statement.setString(2, shipment.getTrackingNumber());
            setDate(statement, 3, shipment.getShipmentDate());
            setDate(statement, 4, shipment.getExpectedDeliveryDate());
            statement.setString(5, shipment.getStatus().getLabel());
            statement.setString(6, shipment.getNotes());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("No generated key returned for shipment insert");

        } catch (SQLException e) {
            // 1062 = duplicate entry (tracking_number): two simultaneous requests that both passed the check
            if (e.getErrorCode() == 1062) {
                throw new DuplicateResourceException("Tracking number already exists");
            }
            // 1452 = foreign key failed: the equipment row disappeared or never existed
            if (e.getErrorCode() == 1452) {
                throw new ResourceNotFoundException("The selected equipment does not exist.");
            }
            throw e;
        }
    }

    /* ---------- update ---------- */

    /** Updates the editable fields. Equipment, tracking number and status are NOT touched here. */
    public void update(Connection connection, Shipment shipment) throws SQLException {
        String sql = "UPDATE shipment SET shipment_date = ?, expected_delivery_date = ?, notes = ? " +
                     "WHERE shipment_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, shipment.getShipmentDate());
            setDate(statement, 2, shipment.getExpectedDeliveryDate());
            statement.setString(3, shipment.getNotes());
            statement.setLong(4, shipment.getShipmentId());
            statement.executeUpdate();
        }
    }

    /** Changes the status and the actual delivery date (null unless delivered). */
    public void updateStatus(Connection connection, long id, ShipmentStatus status,
                             LocalDate actualDeliveryDate) throws SQLException {
        String sql = "UPDATE shipment SET shipment_status = ?, actual_delivery_date = ? WHERE shipment_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.getLabel());
            setDate(statement, 2, actualDeliveryDate);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    /* ---------- read one ---------- */

    /** With the equipment name (for responses). */
    public Optional<Shipment> findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + ", e.equipment_name" + JOIN_EQUIPMENT +
                     " WHERE s.shipment_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs,true)) : Optional.empty();
            }
        }
    }

    /** Locks ONLY the shipment row until the transaction ends. No equipment name. */
    public Optional<Shipment> findByIdForUpdate(Connection connection, long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM shipment s WHERE s.shipment_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs,true)) : Optional.empty();
            }
        }
    }

    /** Every equipment row, newest first. */

    public List<Shipment> findAll(Connection connect) throws SQLException{
        String sql = "SELECT " + COLUMNS + ", e.equipment_name" + JOIN_EQUIPMENT +
                 " ORDER BY s.shipment_id DESC";
        try(PreparedStatement statement = connect.prepareStatement(sql);
            ResultSet rs =statement.executeQuery()){

                List<Shipment> result = new ArrayList<>();
                while(rs.next()){
                    result.add(map(rs,true));
                }
                return result;
            }
    }

    /* ---------- helpers ---------- */

    /** Writes a LocalDate (or NULL) without any time-zone conversion. */
    private static void setDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setObject(index, date);
        }
    }

    private Shipment map(ResultSet rs,boolean withEquipmentName ) throws SQLException {
        Shipment shipment = new Shipment();
        shipment.setShipmentId(rs.getLong("shipment_id"));
        shipment.setEquipmentId(rs.getLong("equipment_id"));
        shipment.setTrackingNumber(rs.getString("tracking_number"));
        shipment.setShipmentDate(rs.getObject("shipment_date", LocalDate.class));
        shipment.setExpectedDeliveryDate(rs.getObject("expected_delivery_date", LocalDate.class));
        shipment.setActualDeliveryDate(rs.getObject("actual_delivery_date", LocalDate.class));
        shipment.setStatus(ShipmentStatus.fromLabel(rs.getString("shipment_status")));
        shipment.setNotes(rs.getString("notes"));
        shipment.setCreatedDate(rs.getObject("created_date", LocalDateTime.class));
        shipment.setUpdatedDate(rs.getObject("updated_date", LocalDateTime.class));
        if (withEquipmentName) {
            shipment.setEquipmentName(rs.getString("equipment_name"));
        }
        return shipment;
    }
}