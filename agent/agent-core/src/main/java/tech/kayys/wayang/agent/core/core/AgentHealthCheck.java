package tech.kayys.wayang.agent.core.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.Map;

/**
 * SmallRye Health readiness probe for the Gollek Agent subsystem.
 *
 * <p>
 * Exposed at {@code /q/health/ready} (Quarkus default) and also at the
 * custom {@code /api/v1/agents/health} endpoint in {@code AgentResource}.
 * Kubernetes liveness / readiness probes should target {@code /q/health/ready}.
 * </p>
 *
 * <h2>Health criteria</h2>
 * <ul>
 * <li>At least one skill must be registered.</li>
 * <li>No more than {@code MAX_UNHEALTHY_RATIO} of all registered skills may
 * report unhealthy simultaneously — this prevents a single flaky
 * optional skill from marking the entire system DOWN.</li>
 * </ul>
 */
@Readiness
@ApplicationScoped
public class AgentHealthCheck implements HealthCheck {

    /**
     * Maximum fraction of unhealthy skills before the agent system is marked DOWN.
     */
    private static final double MAX_UNHEALTHY_RATIO = 0.5;

    @Inject
    SkillRegistry skillRegistry;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("gollek-agent");

        int total = skillRegistry.size();
        builder.withData("total_skills", total);

        if (total == 0) {
            return builder.down().withData("reason", "No skills registered").build();
        }

        Map<String, tech.kayys.wayang.agent.spi.skills.SkillHealth> skillHealth = skillRegistry.checkHealth();
        long healthy = skillHealth.values().stream().filter(tech.kayys.wayang.agent.spi.skills.SkillHealth::healthy).count();
        long unhealthy = total - healthy;
        double ratio = (double) unhealthy / total;

        builder.withData("healthy_skills", (int) healthy)
                .withData("unhealthy_skills", (int) unhealthy);

        // List the unhealthy skill IDs for diagnostics
        skillHealth.entrySet().stream()
                .filter(e -> !e.getValue().healthy())
                .forEach(e -> builder.withData("unhealthy:" + e.getKey(), e.getValue().message()));

        if (ratio > MAX_UNHEALTHY_RATIO) {
            return builder.down()
                    .withData("reason", "Too many unhealthy skills: " + unhealthy + "/" + total)
                    .build();
        }

        return builder.up().build();
    }
}
