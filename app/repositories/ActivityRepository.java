package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

import dto.ActivityResponse;
import models.ActivityLog;
import models.enums.ActionType;
import models.enums.EntityType;

/**
 * The only class that runs SQL for activity_log.
 * Insert and read only: history is never edited or deleted, so there is no update or delete method.
 */
@Singleton
public class ActivityRepository {

    private static final String COLUMNS =
            "activity_id, entity_type, entity_id, action_type, description, " +
            "old_value, new_value, user_id, activity_timestamp";

    /** activity_timestamp is not set here: MySQL fills it with the current time. */
    public void insert(Connection connection, ActivityLog log) throws SQLException {
        String sql = "INSERT INTO activity_log " +
                     "(entity_type, entity_id, action_type, description, old_value, new_value, user_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, log.getEntityType().name());
            statement.setLong(2, log.getEntityId());
            statement.setString(3, log.getActionType().name());
            statement.setString(4, log.getDescription());
            statement.setString(5, log.getOldValue());
            statement.setString(6, log.getNewValue());
            statement.setString(7, log.getUserId());
            statement.executeUpdate();
        }
    }

    /** Every activity record, newest first. */
    public List<ActivityLog> findAll(Connection connection) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM activity_log" +
                    " ORDER BY activity_timestamp DESC, activity_id DESC";

        try (PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()) {

            List<ActivityLog> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    private ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setActivityId(rs.getLong("activity_id"));
        log.setEntityType(EntityType.valueOf(rs.getString("entity_type")));
        log.setEntityId(rs.getLong("entity_id"));
        log.setActionType(ActionType.valueOf(rs.getString("action_type")));
        log.setDescription(rs.getString("description"));
        log.setOldValue(rs.getString("old_value"));
        log.setNewValue(rs.getString("new_value"));
        log.setUserId(rs.getString("user_id"));
        log.setActivityTimestamp(rs.getObject("activity_timestamp", LocalDateTime.class));
        return log;
    }
}