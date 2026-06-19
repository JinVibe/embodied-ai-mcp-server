package ai.costnav.mcp.model;

import java.util.List;

/**
 * Live operational telemetry for the delivery robot — the "physical-world view"
 * the LLM queries before reasoning about cost. Economic fields are paper-faithful
 * (CostNav); battery and position form an operational layer this harness adds on
 * top (battery depletion is explicitly listed as out-of-scope in CostNav §6, so
 * we surface it as an operational extension rather than a paper metric).
 *
 * @param robotModel            hardware platform, e.g. "Segway E1"
 * @param activePolicyId        currently running navigation policy id
 * @param activePolicyName      human-readable policy name
 * @param batteryPercent        remaining battery [%] (operational layer)
 * @param position              current position on the 200 m × 200 m map
 * @param distanceTraveledKm    distance covered in the current run [km]
 * @param runtimeElapsedHr      elapsed time in the current run [hr]
 * @param avgVelocityMps        current average velocity [m/s]
 * @param collisionsThisRun     number of collisions logged this run
 * @param recentCollisions      most recent collision events
 * @param operationalNote       short human-readable status note
 */
public record RobotState(
        String robotModel,
        String activePolicyId,
        String activePolicyName,
        double batteryPercent,
        Position position,
        double distanceTraveledKm,
        double runtimeElapsedHr,
        double avgVelocityMps,
        int collisionsThisRun,
        List<CollisionEvent> recentCollisions,
        String operationalNote
) {}
