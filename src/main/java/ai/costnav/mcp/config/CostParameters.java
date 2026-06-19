package ai.costnav.mcp.config;

import org.springframework.stereotype.Component;

/**
 * Real-world referenced cost parameters from the CostNav benchmark
 * (Seong et al., "CostNav: A Navigation Benchmark for Real-World Economic-Cost
 * Evaluation of Physical AI Agents", arXiv:2511.20216). All values are taken
 * directly from Table 5 (Appendix A), which sources each parameter from SEC
 * filings, AIS injury reports and commercial delivery pricing rather than
 * simulator-defined values.
 *
 * <p>These parameters let the economics service recompute every OPEX component
 * from first principles and reproduce the paper's Table 3 figures exactly.
 */
@Component
public class CostParameters {

    // ----- CAPEX (Table 5) -----
    /** P_Robot — retail price of a commercial sidewalk delivery robot (Segway E1) [$/robot]. */
    public final double robotCostUsd = 8000.0;
    /** P_LiDAR — optional LiDAR module [$/robot]. */
    public final double lidarCostUsd = 3000.0;
    /** P_GPS — optional GPS/GNSS module [$/robot]. */
    public final double gpsCostUsd = 2000.0;
    /** N_RobotLifeRun — expected lifetime deliveries per robot [run/robot]. */
    public final double robotLifeRuns = 18250.0;

    // ----- OPEX: revenue & service (Table 5) -----
    /** P_MktRobotDeli — consumer delivery fee for campus robot services [$/run]. */
    public final double deliveryFeeUsd = 3.49;
    /** P_MktFood — average food value refunded on spoilage [$/run]. */
    public final double foodPriceUsd = 31.93;

    // ----- OPEX: electricity (Table 5) -----
    /** P_Elec — U.S. average retail electricity price [$/kWh]. */
    public final double electricityRateUsdPerKwh = 0.2704;
    /** C_ElectroMechanicalEff — grid-to-wheel efficiency factor (derived from Table 3/4). */
    public final double electroMechanicalEfficiency = 0.776;

    // ----- OPEX: repair (Table 5) -----
    /** C_Repair — annual maintenance cost as a fraction of robot price [unitless]. */
    public final double repairRate = 0.15;
    /** C_PhysicalAssistance — real-world baseline rate of on-site physical interventions [/run]. */
    public final double physicalAssistanceBaselineRate = 0.002;

    /** Convenience: total CAPEX recovered per run is amortised over the robot lifetime. */
    public double hardwareCostUsd(boolean usesLidar, boolean usesGps) {
        return robotCostUsd
                + (usesLidar ? lidarCostUsd : 0.0)
                + (usesGps ? gpsCostUsd : 0.0);
    }
}
