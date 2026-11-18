package dto;

/** Body of PATCH /api/equipment/:id/status and PATCH /api/shipments/:id/status */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusChangeRequest {

    private String status;
    private String actualDeliveryDate;      // used by shipments only ("2026-11-05"), ignored for equipment

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(String actualDeliveryDate) { this.actualDeliveryDate = actualDeliveryDate; }
}