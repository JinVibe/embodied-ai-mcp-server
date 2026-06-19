package ai.costnav.mcp.model;

/**
 * Economic-viability verdict for a policy, following CostNav Eq. (1)–(3):
 * profit per delivery = revenue − OPEX, and break-even point = CAPEX / margin.
 * When the contribution margin is negative the BEP is undefined and the system
 * is not economically viable.
 *
 * @param policyId               policy evaluated
 * @param capexUsd               total capital expenditure [$]
 * @param opexPerRunUsd          total operational cost [$/run]
 * @param revenuePerRunUsd       revenue per delivery = fee × SLA [$/run]
 * @param contributionMarginUsd  revenue − OPEX [$/run]
 * @param economicallyViable     true iff contribution margin > 0
 * @param breakEvenRuns          deliveries to recover CAPEX, or null if not viable
 * @param verdict                human-readable summary
 */
public record EconomicViability(
        String policyId,
        double capexUsd,
        double opexPerRunUsd,
        double revenuePerRunUsd,
        double contributionMarginUsd,
        boolean economicallyViable,
        Double breakEvenRuns,
        String verdict
) {}
