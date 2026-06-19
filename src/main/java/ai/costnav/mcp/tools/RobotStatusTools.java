package ai.costnav.mcp.tools;

import ai.costnav.mcp.model.EconomicViability;
import ai.costnav.mcp.model.NavigationPolicyProfile;
import ai.costnav.mcp.model.OpexBreakdown;
import ai.costnav.mcp.model.PolicyRecommendation;
import ai.costnav.mcp.model.PolicySummary;
import ai.costnav.mcp.model.RobotState;
import ai.costnav.mcp.service.CostNavEconomicsService;
import ai.costnav.mcp.service.RobotFleetService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP tools exposed to LLM clients (e.g. Claude Desktop). Each method becomes a
 * callable tool that lets the model inspect the physical robot's state and reason
 * about its economic viability using the CostNav cost model.
 */
@Component
public class RobotStatusTools {

    private final RobotFleetService fleet;
    private final CostNavEconomicsService economics;

    public RobotStatusTools(RobotFleetService fleet, CostNavEconomicsService economics) {
        this.fleet = fleet;
        this.economics = economics;
    }

    @McpTool(name = "get_robot_status",
            description = "Get the delivery robot's current live telemetry: active navigation "
                    + "policy, battery level, position on the map, distance/runtime so far, and "
                    + "the collision events logged during the current run.")
    public RobotState getRobotStatus() {
        return fleet.currentState();
    }

    @McpTool(name = "list_navigation_policies",
            description = "List all available navigation policies (baselines) with their SLA "
                    + "compliance, total per-run OPEX, contribution margin and whether each is "
                    + "economically viable. Use this to compare policies.")
    public List<PolicySummary> listNavigationPolicies() {
        return fleet.allProfiles().stream()
                .map(p -> {
                    EconomicViability v = economics.viability(p);
                    return new PolicySummary(
                            p.id(),
                            p.displayName(),
                            p.category(),
                            p.slaCompliance(),
                            v.opexPerRunUsd(),
                            v.contributionMarginUsd(),
                            v.economicallyViable());
                })
                .toList();
    }

    @McpTool(name = "set_active_policy",
            description = "Switch the robot's active navigation policy by id (e.g. 'canvas', "
                    + "'nav2-gps', 'navdp'). Returns the robot's new live status under that policy.")
    public RobotState setActivePolicy(
            @McpToolParam(description = "Policy id to activate (see list_navigation_policies)", required = true)
            String policyId) {
        fleet.setActivePolicy(policyId);
        return fleet.currentState();
    }

    @McpTool(name = "get_opex_breakdown",
            description = "Break down the active policy's per-run operating expenditure (OPEX) into "
                    + "the five CostNav categories: electricity, repair, service compensation, "
                    + "pedestrian safety, and property damage. Identifies the dominant cost driver.")
    public OpexBreakdown getOpexBreakdown() {
        return economics.opexBreakdown(fleet.activeProfile());
    }

    @McpTool(name = "get_economic_viability",
            description = "Evaluate whether the active policy is economically viable: returns CAPEX, "
                    + "per-run OPEX, revenue, contribution margin (revenue − OPEX), and the "
                    + "break-even point in number of deliveries (undefined if the margin is negative).")
    public EconomicViability getEconomicViability() {
        return economics.viability(fleet.activeProfile());
    }

    @McpTool(name = "recommend_navigation_policy",
            description = "Recommend how to minimise loss per delivery: the least-negative-margin "
                    + "policy available and concrete behavioural changes (e.g. reduce collision "
                    + "delta-v near pedestrians) that lower the dominant OPEX components.")
    public PolicyRecommendation recommendNavigationPolicy() {
        NavigationPolicyProfile current = fleet.activeProfile();
        return economics.recommend(current, fleet.allProfiles());
    }
}
