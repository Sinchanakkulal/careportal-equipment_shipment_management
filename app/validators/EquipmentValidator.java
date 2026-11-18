package validators;

import dto.EquipmentRequest;
import exceptions.ValidationException;

public class EquipmentValidator {
    
    private EquipmentValidator(){}

    /** Normalizes the request (trim) and throws ValidationException on the first problem. */
    public static void validate(EquipmentRequest request) {
        request.normalize();

        required(request.getEquipmentName(),     "Equipment name",     100);
        required(request.getEquipmentType(),     "Equipment type",     50);
        required(request.getManufacturer(),      "Manufacturer",       100);
        required(request.getModelNumber(),       "Model number",       50);
        required(request.getSerialNumber(),      "Serial number",      50);
        optional(request.getAssignedPatientId(), "Assigned patient ID", 50);
        optional(request.getNotes(),             "Notes",              500);
    }

    private static void required(String value, String label, int max) {
        if (value == null) {
            throw new ValidationException(label + " is required.");
        }
        checkLength(value, label, max);
    }

    private static void optional(String value, String label, int max) {
        if (value != null) {
            checkLength(value, label, max);
        }
    }

     private static void checkLength(String value, String label, int max) {
        if (value.length() > max) {
            throw new ValidationException(label + " must be " + max + " characters or less.");
        }
    }

}
