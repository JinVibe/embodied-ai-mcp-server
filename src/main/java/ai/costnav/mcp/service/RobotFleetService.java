package ai.costnav.mcp.service;

import ai.costnav.mcp.model.CollisionEvent;
import ai.costnav.mcp.model.NavigationPolicyProfile;
import ai.costnav.mcp.model.Position;
import ai.costnav.mcp.model.RobotState;
import tools.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the simulated delivery-robot fleet: the catalogue of navigation policies
 * loaded from {@code robot-profiles.json} (CostNav Tables 2–4) and the currently
 * active policy. Synthesises live {@link RobotState} telemetry on demand so the
 * LLM has a "physical-world view" to reason over.
 */
@Service
public class RobotFleetService {

    private static final String ROBOT_MODEL = "Segway E1";
    /** The CostNav map is a 200 m × 200 m urban-sidewalk network (§3.2). */
    private static final double MAP_SIZE_M = 200.0;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Value("classpath:robot-profiles.json")
    private Resource profilesResource;

    private final Map<String, NavigationPolicyProfile> profiles = new LinkedHashMap<>();
    private String activePolicyId = "canvas";

    @PostConstruct
    void load() throws IOException {
        try (InputStream in = profilesResource.getInputStream()) {
            NavigationPolicyProfile[] loaded =
                    jsonMapper.readValue(in, NavigationPolicyProfile[].class);
            for (NavigationPolicyProfile p : loaded) {
                profiles.put(p.id(), p);
            }
        }
    }

    public List<NavigationPolicyProfile> allProfiles() {
        return new ArrayList<>(profiles.values());
    }

    public NavigationPolicyProfile profile(String id) {
        NavigationPolicyProfile p = profiles.get(id);
        if (p == null) {
            throw new IllegalArgumentException(
                    "Unknown policy id: " + id + ". Available: " + profiles.keySet());
        }
        return p;
    }

    public NavigationPolicyProfile activeProfile() {
        return profile(activePolicyId);
    }

    public String activePolicyId() {
        return activePolicyId;
    }

    /** Switch the active navigation policy; returns the newly active profile. */
    public NavigationPolicyProfile setActivePolicy(String id) {
        NavigationPolicyProfile p = profile(id); // validates
        this.activePolicyId = id;
        return p;
    }

    /**
     * Build a snapshot of live telemetry for the active policy, mid-delivery.
     * Economic-relevant signals come straight from the policy profile; battery
     * and position are an operational layer this harness adds (battery depletion
     * is out-of-scope in CostNav §6, surfaced here as an operational extension).
     */
    public RobotState currentState() {
        NavigationPolicyProfile p = activeProfile();

        // ~60% of the way through the current run.
        double progress = 0.6;
        double distance = round(p.avgDistanceKm() * progress, 4);
        double runtime = round(p.avgRuntimeHr() * progress, 4);

        // Operational-layer battery model: rougher driving (more physical
        // assistance) correlates with lower remaining charge. Deterministic.
        double battery = round(80.0 - p.physicalAssistanceRate() * 40.0, 1);

        // Deterministic position derived from progress along a diagonal route.
        Position position = new Position(
                round(MAP_SIZE_M * progress, 1),
                round(MAP_SIZE_M * progress * 0.5, 1));

        List<CollisionEvent> collisions = synthesizeCollisions(p);

        String note = String.format(
                "Running '%s' on %s. %d collision event(s) logged this run; "
                + "largest impact delta-v %.2f m/s.",
                p.displayName(), ROBOT_MODEL, collisions.size(), p.avgCollisionDeltaVMps());

        return new RobotState(
                ROBOT_MODEL,
                p.id(),
                p.displayName(),
                battery,
                position,
                distance,
                runtime,
                p.avgVelocityMps(),
                collisions.size(),
                collisions,
                note);
    }

    /**
     * Build a small, deterministic collision log consistent with the policy's
     * measured liability (pedestrian and property contacts).
     */
    private List<CollisionEvent> synthesizeCollisions(NavigationPolicyProfile p) {
        List<CollisionEvent> events = new ArrayList<>();
        if (p.pedestrianSafetyCostUsd() > 0.0) {
            events.add(new CollisionEvent(
                    "pedestrian",
                    round(p.avgCollisionImpulseNs(), 2),
                    p.avgCollisionDeltaVMps(),
                    round(p.pedestrianSafetyCostUsd(), 2)));
        }
        if (p.propertyDamageCostUsd() > 0.0) {
            events.add(new CollisionEvent(
                    "property_infrastructure",
                    round(p.avgCollisionImpulseNs() * 0.5, 2),
                    round(p.avgCollisionDeltaVMps() * 0.5, 2),
                    round(p.propertyDamageCostUsd(), 2)));
        }
        return Collections.unmodifiableList(events);
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
