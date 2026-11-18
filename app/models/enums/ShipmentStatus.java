package models.enums;

/** The five shipment statuses. The label is what is stored in MySQL and shown in the UI. */
public enum ShipmentStatus {
    CREATED("Created"),
    DISPATCHED("Dispatched"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    ShipmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** "In Transit" -> IN_TRANSIT. Throws if the text is not a known status. */
    public static ShipmentStatus fromLabel(String label) {
        for (ShipmentStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown shipment status: " + label);
    }
}