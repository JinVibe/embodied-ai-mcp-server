package ai.costnav.mcp.model;

/**
 * Per-run operational expenditure broken into the five CostNav categories
 * (§3.1.3). Reproduces the OPEX rows of Table 3.
 *
 * @param policyId               policy this breakdown is for
 * @param electricityUsd         energy cost [$/run] — Eq. (6)
 * @param repairUsd              repair cost from physical-assistance events [$/run] — Eq. (7)
 * @param serviceCompensationUsd refunds for spoiled/timed-out deliveries [$/run] — Eq. (8)
 * @param pedestrianSafetyUsd    AIS-derived pedestrian injury liability [$/run] — Eq. (9)
 * @param propertyDamageUsd      urban infrastructure damage [$/run] — Eq. (10)
 * @param totalOpexUsd           sum of the five components [$/run]
 * @param largestComponent       name of the dominant cost driver
 */
public record OpexBreakdown(
        String policyId,
        double electricityUsd,
        double repairUsd,
        double serviceCompensationUsd,
        double pedestrianSafetyUsd,
        double propertyDamageUsd,
        double totalOpexUsd,
        String largestComponent
) {}
