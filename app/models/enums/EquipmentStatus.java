package models.enums;

public enum EquipmentStatus {
    REQUESTED("Requested"),
    APPROVED("Approved"),
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    RETURNED("Returned");
    
    private final String label;

    EquipmentStatus(String label){
        this.label = label;
    }
    public String getLabel(){
        return label;
    }

    public static EquipmentStatus fromLabel(String label){
        for(EquipmentStatus status: values()){
            if(status.label.equals(label)){
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown equipment status: " + label);
    }
}
