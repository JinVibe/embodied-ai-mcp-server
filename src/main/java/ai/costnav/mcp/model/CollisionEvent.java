package ai.costnav.mcp.model;

/**
 * A single collision logged during the current run. Impulse and delta-v are the
 * physics quantities CostNav maps to cost (pedestrian injury / property damage).
 *
 * @param obstacleType       e.g. "pedestrian", "bollard", "mailbox", "building_glass"
 * @param impulseNs          collision impulse [N·s]
 * @param deltaVMps          collision delta-v [m/s]
 * @param estimatedCostUsd   estimated liability for this event [$]
 */
public record CollisionEvent(
        String obstacleType,
        double impulseNs,
        double deltaVMps,
        double estimatedCostUsd
) {}
