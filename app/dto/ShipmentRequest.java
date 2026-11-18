package dto;

/** Body of POST /api/shipments and PUT /api/shipments/:id. Unknown properties (status, ids...) are dropped. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShipmentRequest {

    private Long equipmentId;
    private String trackingNumber;
    private String shipmentDate;
    private String expectedDeliveryDate;
    private String notes;

    /** Trims every text value. Blank text becomes null. */
    public void normalize() {
        trackingNumber       = clean(trackingNumber);
        shipmentDate         = clean(shipmentDate);
        expectedDeliveryDate = clean(expectedDeliveryDate);
        notes                = clean(notes);
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public String getTrackingNumber() { return trackingNumber; }

    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getShipmentDate() { return shipmentDate; }
    public void setShipmentDate(String shipmentDate) { this.shipmentDate = shipmentDate; }

    public String getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(String expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}