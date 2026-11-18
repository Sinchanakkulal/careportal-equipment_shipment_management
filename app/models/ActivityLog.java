package models;

import java.time.LocalDateTime;
import models.enums.ActionType;
import models.enums.EntityType;

public class ActivityLog {

    private Long activityId;
    private EntityType entityType;
    private Long entityId;
    private ActionType actionType;
    private String description;
    private String oldValue;
    private String newValue;
    private String userId;
    private LocalDateTime activityTimestamp;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getActivityTimestamp() { return activityTimestamp; }
    public void setActivityTimestamp(LocalDateTime activityTimestamp) { this.activityTimestamp = activityTimestamp; }

}
