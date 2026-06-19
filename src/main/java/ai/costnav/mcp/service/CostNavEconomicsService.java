package ai.costnav.mcp.service;

import ai.costnav.mcp.config.CostParameters;
import ai.costnav.mcp.model.EconomicViability;
import ai.costnav.mcp.model.NavigationPolicyProfile;
import ai.costnav.mcp.model.OpexBreakdown;
import ai.costnav.mcp.model.PolicyRecommendation;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Implements the CostNav economic model (§3.1). Given a policy's
 * simulation-measured signals and the real-world cost parameters, it recomputes
 * each OPEX component, revenue, contribution margin and break-even point —
 * reproducing the paper's Table 3 figures from first principles.
 */
@Service
public class CostNavEconomicsService {

    private final CostParameters params;

    public CostNavEconomicsService(CostParameters params) {
        this.params = params;
    }

    /** Electricity cost — CostNav Eq. (6). */
    public double electricityCost(NavigationPolicyProfile p) {
        return params.electricityRateUsdPerKwh
                * (p.avgPowerKw() / params.electroMechanicalEfficiency)
                * p.avgRuntimeHr();
    }

    /** Repair cost from physical-assistance events — CostNav Eq. (7). */
    public double repairCost(NavigationPolicyProfile p) {
        return (params.robotCostUsd / params.robotLifeRuns)
                * params.repairRate
                * (p.physicalAssistanceRate() / params.physicalAssistanceBaselineRate);
    }

    /** Service compensation for spoiled / failed deliveries — CostNav Eq. (8). */
    public double serviceCompensationCost(NavigationPolicyProfile p) {
        return p.spoiledRate() * params.foodPriceUsd
                + (p.timeoutRate() + p.physicalAssistanceRate()) * params.deliveryFeeUsd;
    }

    /** Total per-run OPEX broken into the five CostNav categories. */
    public OpexBreakdown opexBreakdown(NavigationPolicyProfile p) {
        double electricity = electricityCost(p);
        double repair = repairCost(p);
        double service = serviceCompensationCost(p);
        double pedestrian = p.pedestrianSafetyCostUsd();   // AIS-derived (Eq. 9), measured in sim
        double property = p.propertyDamageCostUsd();        // Eq. (10), measured in sim
        double total = electricity + repair + service + pedestrian + property;

        String largest = largestComponent(electricity, repair, service, pedestrian, property);

        return new OpexBreakdown(
                p.id(),
                round(electricity, 4),
                round(repair, 4),
                round(service, 4),
                round(pedestrian, 4),
                round(property, 4),
                round(total, 4),
                largest);
    }

    /** Revenue per delivery = base fee × SLA compliance (zero on timeout). */
    public double revenue(NavigationPolicyProfile p) {
        return params.deliveryFeeUsd * p.slaCompliance();
    }

    /** Total CAPEX = hardware (per sensor configuration) + data-collection cost. */
    public double capex(NavigationPolicyProfile p) {
        return params.hardwareCostUsd(p.usesLidar(), p.usesGps()) + p.dataCollectionCostUsd();
    }

    /** Full economic-viability verdict — CostNav Eq. (1)–(3). */
    public EconomicViability viability(NavigationPolicyProfile p) {
        double opex = opexBreakdown(p).totalOpexUsd();
        double rev = revenue(p);
        double margin = rev - opex;
        boolean viable = margin > 0;
        Double bep = viable ? round(capex(p) / margin, 1) : null;

        String verdict = viable
                ? String.format("Economically viable: each delivery yields +$%.2f; recovers CAPEX in %.0f runs.",
                        margin, bep)
                : String.format("NOT economically viable: each delivery loses $%.2f. "
                        + "Break-even is undefined because the contribution margin is negative.",
                        -margin);

        return new EconomicViability(
                p.id(),
                round(capex(p), 2),
                round(opex, 4),
                round(rev, 4),
                round(margin, 2),
                viable,
                bep,
                verdict);
    }

    /**
     * Recommend the least-loss policy available and the behavioural change that
     * would most reduce OPEX. Grounded in CostNav's finding (§4.2) that
     * pedestrian-safety cost — driven by collision delta-v — is the dominant OPEX
     * component for most baselines.
     */
    public PolicyRecommendation recommend(NavigationPolicyProfile current,
                                          List<NavigationPolicyProfile> all) {
        NavigationPolicyProfile best = all.stream()
                .max(Comparator.comparingDouble(p -> viability(p).contributionMarginUsd()))
                .orElse(current);

        double currentMargin = viability(current).contributionMarginUsd();
        double bestMargin = viability(best).contributionMarginUsd();
        OpexBreakdown opex = opexBreakdown(current);

        StringBuilder reasoning = new StringBuilder();
        reasoning.append(String.format(
                "Current policy '%s' has a contribution margin of $%.2f/run (loss). ",
                current.displayName(), currentMargin));
        reasoning.append(String.format("Its largest OPEX driver is %s. ", opex.largestComponent()));
        if ("pedestrianSafety".equals(opex.largestComponent())) {
            reasoning.append(String.format(
                    "Pedestrian-safety liability scales with collision delta-v (currently %.2f m/s); "
                    + "reducing impact speed near pedestrians directly lowers AIS-injury cost. ",
                    current.avgCollisionDeltaVMps()));
        }
        if (!best.id().equals(current.id())) {
            reasoning.append(String.format(
                    "Among available baselines, '%s' has the least-negative margin ($%.2f/run).",
                    best.displayName(), bestMargin));
        } else {
            reasoning.append("This policy already has the least-negative margin among available baselines.");
        }

        List<String> actions = List.of(
                String.format("Reduce collision delta-v below the current %.2f m/s to cut pedestrian-safety cost (largest OPEX term for most policies).",
                        current.avgCollisionDeltaVMps()),
                "Smooth acceleration/jerk to reduce cargo spoilage and the service-compensation refunds it triggers.",
                String.format("Lower the physical-assistance rate (currently %.0f%%) to cut repair cost, which scales linearly with it.",
                        current.physicalAssistanceRate() * 100),
                "Note: per CostNav, NO evaluated baseline reaches a positive margin — treat this as loss-minimization, not profitability.");

        return new PolicyRecommendation(
                current.id(),
                round(currentMargin, 2),
                best.id(),
                round(bestMargin, 2),
                current.avgCollisionDeltaVMps(),
                reasoning.toString(),
                actions);
    }

    private String largestComponent(double electricity, double repair, double service,
                                     double pedestrian, double property) {
        double max = electricity;
        String name = "electricity";
        if (repair > max) { max = repair; name = "repair"; }
        if (service > max) { max = service; name = "serviceCompensation"; }
        if (pedestrian > max) { max = pedestrian; name = "pedestrianSafety"; }
        if (property > max) { name = "propertyDamage"; }
        return name;
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
