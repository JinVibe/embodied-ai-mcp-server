package ai.costnav.mcp.model;

import java.util.List;

/**
 * A cost-aware navigation recommendation: which policy minimises loss per run and
 * what behavioural changes would most reduce OPEX, grounded in CostNav's finding
 * that pedestrian-safety cost (driven by collision delta-v) is the dominant OPEX
 * component for most baselines (§4.2).
 *
 * @param currentPolicyId          policy currently active
 * @param currentMarginUsd         current contribution margin [$/run]
 * @param recommendedPolicyId      least-negative-margin policy available
 * @param recommendedMarginUsd     its contribution margin [$/run]
 * @param currentCollisionDeltaV   current avg collision delta-v [m/s]
 * @param reasoning                explanation of the recommendation
 * @param actionItems              concrete behavioural changes to lower OPEX
 */
public record PolicyRecommendation(
        String currentPolicyId,
        double currentMarginUsd,
        String recommendedPolicyId,
        double recommendedMarginUsd,
        double currentCollisionDeltaV,
        String reasoning,
        List<String> actionItems
) {}
