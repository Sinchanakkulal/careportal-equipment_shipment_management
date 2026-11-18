package validators;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import exceptions.InvalidStatusTransitionException;
import models.enums.EquipmentStatus;
import models.enums.ShipmentStatus;

/** The status workflow rules: equipment E1 to E8 and shipment S1 to S7 from the design doc. */
public final class StatusTransitionRules {

    private StatusTransitionRules() { }

    private static final Map<EquipmentStatus, Set<EquipmentStatus>> EQUIPMENT =
            new EnumMap<>(EquipmentStatus.class);

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> SHIPMENT =
            new EnumMap<>(ShipmentStatus.class);

    static {
        EQUIPMENT.put(EquipmentStatus.REQUESTED, EnumSet.of(EquipmentStatus.APPROVED));       // E2
        EQUIPMENT.put(EquipmentStatus.APPROVED,  EnumSet.of(EquipmentStatus.ACTIVE));         // E3
        EQUIPMENT.put(EquipmentStatus.ACTIVE,    EnumSet.of(EquipmentStatus.INACTIVE,         // E4, E5
                                                            EquipmentStatus.RETURNED));
        EQUIPMENT.put(EquipmentStatus.INACTIVE,  EnumSet.of(EquipmentStatus.ACTIVE,           // E4, E5
                                                            EquipmentStatus.RETURNED));
        EQUIPMENT.put(EquipmentStatus.RETURNED,  EnumSet.noneOf(EquipmentStatus.class));      // E6: final

        SHIPMENT.put(ShipmentStatus.CREATED,    EnumSet.of(ShipmentStatus.DISPATCHED,         // S2

                                                           ShipmentStatus.CANCELLED));        // S3
        SHIPMENT.put(ShipmentStatus.DISPATCHED, EnumSet.of(ShipmentStatus.IN_TRANSIT,         // S2
                                                           ShipmentStatus.CANCELLED));        // S3
        SHIPMENT.put(ShipmentStatus.IN_TRANSIT, EnumSet.of(ShipmentStatus.DELIVERED,          // S2
                                                           ShipmentStatus.CANCELLED));        // S3
        SHIPMENT.put(ShipmentStatus.DELIVERED,  EnumSet.noneOf(ShipmentStatus.class));        // S4: final
        SHIPMENT.put(ShipmentStatus.CANCELLED,  EnumSet.noneOf(ShipmentStatus.class));        // S5: final
    }

    /** Throws InvalidStatusTransitionException (HTTP 409) if the change is not allowed. */
    public static void checkEquipment(EquipmentStatus current, EquipmentStatus target) {

        if (current == target) {                                                              // E8
            throw new InvalidStatusTransitionException(
                    "Equipment is already " + current.getLabel() + ".");
        }

        Set<EquipmentStatus> allowed = EQUIPMENT.get(current);

        if (allowed.isEmpty()) {                                                              // E6
            throw new InvalidStatusTransitionException(
                    current.getLabel() + " is a final status and cannot be changed.");
        }

        if (!allowed.contains(target)) {                                                      // E7
            throw new InvalidStatusTransitionException(
                    "Cannot change status from " + current.getLabel() + " to " + target.getLabel() + ".");
        }
    }

    /** Same idea for shipments (S4 to S7). */
    public static void checkShipment(ShipmentStatus current, ShipmentStatus target) {


        if (current == target) {                                                              // S7
            throw new InvalidStatusTransitionException(
                    "Shipment is already " + current.getLabel() + ".");
        }

        Set<ShipmentStatus> allowed = SHIPMENT.get(current);

        if (allowed.isEmpty()) {                                                              // S4, S5
            throw new InvalidStatusTransitionException(
                    current.getLabel() + " is a final status and cannot be changed.");
        }

        if (!allowed.contains(target)) {                                                      // S6
            throw new InvalidStatusTransitionException(
                    "Cannot change status from " + current.getLabel() + " to " + target.getLabel() + ".");
        }
    }
}