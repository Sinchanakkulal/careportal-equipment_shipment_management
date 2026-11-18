package dto;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EquipmentRequest {
    private String equipmentName;
    private String equipmentType;
    private String manufacturer;
    private String modelNumber;
    private String serialNumber;
    private String assignedPatientId;
    private String notes;
    
    /** Trims every value. Blank text becomes null. */
    public void normalize() {
        equipmentName     = clean(equipmentName);
        equipmentType     = clean(equipmentType);
        manufacturer      = clean(manufacturer);
        modelNumber       = clean(modelNumber);
        serialNumber      = clean(serialNumber);
        assignedPatientId = clean(assignedPatientId);
        notes             = clean(notes);
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

     public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

}
