package validators;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import dto.ShipmentRequest;
import exceptions.ValidationException;

/** Mandatory fields, data types, dates and length limits for shipments. */
public final class ShipmentValidator {

    private ShipmentValidator() { }

    /** Normalizes the request (trim) and throws ValidationException on the first problem. */
    public static void validate(ShipmentRequest request) {
        request.normalize();

        if (request.getEquipmentId() == null || request.getEquipmentId() < 1) {
            throw new ValidationException("Equipment is required.");
        }

        if (request.getTrackingNumber() == null) {
            throw new ValidationException("Tracking number is required.");
        }
        if (request.getTrackingNumber().length() > 50) {
            throw new ValidationException("Tracking number must be 50 characters or less.");
        }

        if (request.getShipmentDate() == null) {
            throw new ValidationException("Shipment date is required.");
        }
        if (request.getExpectedDeliveryDate() == null) {
            throw new ValidationException("Expected delivery date is required.");
        }

        LocalDate shipmentDate = parseDate(request.getShipmentDate(), "Shipment date");
        LocalDate expectedDate = parseDate(request.getExpectedDeliveryDate(), "Expected delivery date");

        if (expectedDate.isBefore(shipmentDate)) {
            throw new ValidationException("Expected delivery date cannot be before the shipment date.");
        }

        if (request.getNotes() != null && request.getNotes().length() > 500) {
            throw new ValidationException("Notes must be 500 characters or less.");
        }
    }

    /** "2026-11-05" -> LocalDate. A date that does not exist (2026-02-30) is rejected. */
    public static LocalDate parseDate(String text, String label) {
        LocalDate date;
        try {
            date = LocalDate.parse(text);                    // strict ISO: yyyy-MM-dd
        } catch (DateTimeParseException e) {
            throw new ValidationException(label + " must be a valid date (YYYY-MM-DD).");
        }
        if (date.getYear() < 1900 || date.getYear() > 2100) {   // keeps values inside what MySQL DATE handles well
            throw new ValidationException(label + " must be a valid date (YYYY-MM-DD).");
        }
        return date;
    }
}