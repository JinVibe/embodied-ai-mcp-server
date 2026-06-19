package ai.costnav.mcp.model;

/**
 * Compact comparison row for one policy, used by the {@code list_navigation_policies}
 * tool so the LLM can compare all baselines at a glance.
 *
 * @param id                     policy id
 * @param displayName            human-readable name
 * @param category               "rule-based" or "learning-based"
 * @param slaCompliance          SLA compliance [fraction]
 * @param totalOpexUsd           total OPEX [$/run]
 * @param contributionMarginUsd  contribution margin [$/run]
 * @param economicallyViable     whether margin is positive
 */
public record PolicySummary(
        String id,
        String displayName,
        String category,
        double slaCompliance,
        double totalOpexUsd,
        double contributionMarginUsd,
        boolean economicallyViable
) {}
