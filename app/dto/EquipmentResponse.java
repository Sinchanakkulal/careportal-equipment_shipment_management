package dto;

import common.DateTimeUtil;
import models.Equipment;

public class EquipmentResponse {
 
    private final Long equipmentId;
    private final String equipmentName;
    private final String equipmentType;
    private final String manufacturer;
    private final String modelNumber;
    private final String serialNumber;
    private final String assignedPatientId;
    private final String status;
    private final String notes;
    private final String createdDate;
    private final String updatedDate;

    private EquipmentResponse(Equipment e) {
        this.equipmentId       = e.getEquipmentId();
        this.equipmentName     = e.getEquipmentName();
        this.equipmentType     = e.getEquipmentType();
        this.manufacturer      = e.getManufacturer();
        this.modelNumber       = e.getModelNumber();
        this.serialNumber      = e.getSerialNumber();
        this.assignedPatientId = e.getAssignedPatientId();
        this.status            = e.getStatus().getLabel();           // "Requested", not REQUESTED
        this.notes             = e.getNotes();
        this.createdDate       = DateTimeUtil.format(e.getCreatedDate());
        this.updatedDate       = DateTimeUtil.format(e.getUpdatedDate());
    }

    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(equipment);
    }

    public Long getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getEquipmentType() { return equipmentType; }
    public String getManufacturer() { return manufacturer; }
    public String getModelNumber() { return modelNumber; }
    public String getSerialNumber() { return serialNumber; }
    public String getAssignedPatientId() { return assignedPatientId; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getCreatedDate() { return createdDate; }
    public String getUpdatedDate() { return updatedDate; }


}
