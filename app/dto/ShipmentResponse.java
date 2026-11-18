package dto;

import common.DateTimeUtil;
import models.Shipment;

/** What the API returns for one shipment. Dates are YYYY-MM-DD, timestamps are ISO date-times. */
public class ShipmentResponse {

    private final Long shipmentId;
    private final Long equipmentId;
    private final String equipmentName;
    private final String trackingNumber;
    private final String shipmentDate;
    private final String expectedDeliveryDate;
    private final String actualDeliveryDate;
    private final String shipmentStatus;
    private final String notes;
    private final String createdDate;
    private final String updatedDate;

    private ShipmentResponse(Shipment s) {
        this.shipmentId           = s.getShipmentId();
        this.equipmentId          = s.getEquipmentId();
        this.equipmentName        = s.getEquipmentName();
        this.trackingNumber       = s.getTrackingNumber();
        this.shipmentDate         = s.getShipmentDate() == null ? null : s.getShipmentDate().toString();
        this.expectedDeliveryDate = s.getExpectedDeliveryDate() == null ? null : s.getExpectedDeliveryDate().toString();
        this.actualDeliveryDate   = s.getActualDeliveryDate() == null ? null : s.getActualDeliveryDate().toString();
        this.shipmentStatus       = s.getStatus().getLabel();            // "In Transit", not IN_TRANSIT
        this.notes                = s.getNotes();
        this.createdDate          = DateTimeUtil.format(s.getCreatedDate());
        this.updatedDate          = DateTimeUtil.format(s.getUpdatedDate());

    }

    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(shipment);
    }

    public Long getShipmentId() { return shipmentId; }
    public Long getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getShipmentDate() { return shipmentDate; }
    public String getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public String getActualDeliveryDate() { return actualDeliveryDate; }
    public String getShipmentStatus() { return shipmentStatus; }
    public String getNotes() { return notes; }
    public String getCreatedDate() { return createdDate; }
    public String getUpdatedDate() { return updatedDate; }
}