package ai.costnav.mcp;

import ai.costnav.mcp.config.CostParameters;
import ai.costnav.mcp.model.EconomicViability;
import ai.costnav.mcp.model.NavigationPolicyProfile;
import ai.costnav.mcp.model.OpexBreakdown;
import ai.costnav.mcp.service.CostNavEconomicsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that the economic model reproduces the published CostNav figures
 * (Table 3 / Table 4) from first principles. If these pass, the server's numbers
 * are faithful to the paper.
 */
class CostNavEconomicsServiceTest {

    private final CostNavEconomicsService economics =
            new CostNavEconomicsService(new CostParameters());

    /** CANVAS (RGB + GPS) — the paper's headline policy (Table 3 / Table 4). */
    private static final NavigationPolicyProfile CANVAS = new NavigationPolicyProfile(
            "canvas", "CANVAS (RGB + GPS)", "learning-based", "RGB camera + GPS",
            false, true, 2165.79,
            0.70, 0.10, 0.00, 0.20,
            1.1003, 0.0200, 149.36, 0.41,
            14.38, 6.00, 0.0287, 0.0902);

    /** Nav2 w/ AMCL — a rule-based LiDAR baseline (Table 3). */
    private static final NavigationPolicyProfile NAV2_AMCL = new NavigationPolicyProfile(
            "nav2-amcl", "Nav2 w/ AMCL (LiDAR)", "rule-based", "LiDAR + GPS",
            true, true, 0.0,
            0.43, 0.12, 0.12, 0.33,
            1.1704, 0.0212, 116.35, 0.83,
            29.32, 0.00, 0.0260, 0.0498);

    @Test
    void reproducesCanvasOpexBreakdown() {
        OpexBreakdown o = economics.opexBreakdown(CANVAS);
        // Table 3, CANVAS column.
        assertEquals(0.0002, o.electricityUsd(), 1e-4, "electricity");
        assertEquals(3.8910, o.serviceCompensationUsd(), 1e-3, "service compensation");
        assertEquals(6.5753, o.repairUsd(), 1e-3, "repair");
        assertEquals(14.3800, o.pedestrianSafetyUsd(), 1e-3, "pedestrian safety");
        assertEquals(6.0000, o.propertyDamageUsd(), 1e-3, "property damage");
        assertEquals(30.8466, o.totalOpexUsd(), 1e-2, "total OPEX");
        assertEquals("pedestrianSafety", o.largestComponent());
    }

    @Test
    void reproducesCanvasContributionMargin() {
        EconomicViability v = economics.viability(CANVAS);
        assertEquals(2.4430, v.revenuePerRunUsd(), 1e-3, "revenue = fee x SLA");
        assertEquals(12165.79, v.capexUsd(), 1e-2, "CAPEX");
        assertEquals(-28.40, v.contributionMarginUsd(), 1e-2, "contribution margin");
        assertFalse(v.economicallyViable(), "no baseline is viable per CostNav");
        assertNull(v.breakEvenRuns(), "BEP undefined for negative margin");
    }

    @Test
    void reproducesNav2AmclEconomics() {
        OpexBreakdown o = economics.opexBreakdown(NAV2_AMCL);
        // Table 3, Nav2 w/ AMCL column.
        assertEquals(5.4021, o.serviceCompensationUsd(), 1e-3, "service compensation");
        assertEquals(10.8493, o.repairUsd(), 1e-3, "repair");
        assertEquals(45.5716, o.totalOpexUsd(), 1e-2, "total OPEX");

        EconomicViability v = economics.viability(NAV2_AMCL);
        assertEquals(1.5007, v.revenuePerRunUsd(), 1e-3, "revenue");
        assertEquals(13000.0, v.capexUsd(), 1e-2, "CAPEX (base + LiDAR + GPS)");
        assertEquals(-44.07, v.contributionMarginUsd(), 1e-2, "contribution margin");
    }
}
