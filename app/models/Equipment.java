package models;

import java.time.LocalDateTime;
import models.enums.EquipmentStatus;

public class Equipment {
    private Long equipmentId;
    private String equipmentName;
    private String equipmentType;
    private String manufacturer;
    private String modelNumber;
    private String serialNumber;
    private String assignedPatientId;
    private EquipmentStatus status;
    private String notes;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    
    public String getEquipmentName() {
        return equipmentName;
    }
    public void setEquipmentName(String equipmentName){
        this.equipmentName=equipmentName;
    }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getAssignedPatientId() { return assignedPatientId; }
    public void setAssignedPatientId(String assignedPatientId) { this.assignedPatientId = assignedPatientId; }

    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
}
