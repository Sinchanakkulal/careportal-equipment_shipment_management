package dto;

import common.DateTimeUtil;
import models.ActivityLog;

/** What the API returns for one activity record. */
public class ActivityResponse {

    private final Long activityId;
    private final String entityType;          // EQUIPMENT or SHIPMENT
    private final Long entityId;
    private final String actionType;          // ADD, EDIT or STATUS_CHANGE
    private final String description;
    private final String oldValue;
    private final String newValue;
    private final String userId;
    private final String activityTimestamp;   // 2026-11-03T10:15:30

    private ActivityResponse(ActivityLog log) {
        this.activityId        = log.getActivityId();
        this.entityType        = log.getEntityType().name();
        this.entityId          = log.getEntityId();
        this.actionType        = log.getActionType().name();
        this.description       = log.getDescription();
        this.oldValue          = log.getOldValue();
        this.newValue          = log.getNewValue();
        this.userId            = log.getUserId();
        this.activityTimestamp = DateTimeUtil.format(log.getActivityTimestamp());
    }

    public static ActivityResponse from(ActivityLog log) {
        return new ActivityResponse(log);
    }

    public Long getActivityId() { return activityId; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getActionType() { return actionType; }
    public String getDescription() { return description; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getUserId() { return userId; }
    public String getActivityTimestamp() { return activityTimestamp; }
}