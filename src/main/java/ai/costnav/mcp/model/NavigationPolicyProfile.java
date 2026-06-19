package ai.costnav.mcp.model;

/**
 * Simulation-measured quantities for one navigation policy (baseline), taken from
 * CostNav Tables 2, 3 and 4. These are the raw operational signals that the
 * economic model converts into per-run cost. Every value here is reproduced from
 * the paper; the {@code CostNavEconomicsService} recomputes the monetary OPEX
 * components from these signals plus {@code CostParameters}.
 *
 * @param id                       stable identifier, e.g. "canvas"
 * @param displayName              human-readable name, e.g. "CANVAS (RGB + GPS)"
 * @param category                 "rule-based" or "learning-based"
 * @param sensors                  sensor configuration description
 * @param usesLidar                whether the configuration includes a LiDAR module
 * @param usesGps                  whether the configuration includes a GPS module
 * @param dataCollectionCostUsd    teleoperation data-collection CAPEX (learning-based only)
 * @param slaCompliance            fraction of deliveries meeting the SLA (revenue driver)
 * @param spoiledRate              fraction of runs where cargo arrived damaged
 * @param timeoutRate              fraction of runs that timed out
 * @param physicalAssistanceRate   fraction of runs needing physical operator recovery
 * @param avgVelocityMps           average velocity [m/s]
 * @param avgPowerKw               average mechanical power draw [kW]
 * @param avgCollisionImpulseNs    average collision impulse [N·s]
 * @param avgCollisionDeltaVMps    average collision delta-v [m/s] (drives pedestrian injury)
 * @param pedestrianSafetyCostUsd  AIS-derived pedestrian injury liability [$/run]
 * @param propertyDamageCostUsd    urban infrastructure damage [$/run]
 * @param avgRuntimeHr             average run time [hr/run]
 * @param avgDistanceKm            average distance per run [km/run]
 */
public record NavigationPolicyProfile(
        String id,
        String displayName,
        String category,
        String sensors,
        boolean usesLidar,
        boolean usesGps,
        double dataCollectionCostUsd,
        double slaCompliance,
        double spoiledRate,
        double timeoutRate,
        double physicalAssistanceRate,
        double avgVelocityMps,
        double avgPowerKw,
        double avgCollisionImpulseNs,
        double avgCollisionDeltaVMps,
        double pedestrianSafetyCostUsd,
        double propertyDamageCostUsd,
        double avgRuntimeHr,
        double avgDistanceKm
) {}
