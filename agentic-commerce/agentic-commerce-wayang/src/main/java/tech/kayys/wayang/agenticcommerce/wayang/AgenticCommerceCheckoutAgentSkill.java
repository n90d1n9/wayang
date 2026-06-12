package tech.kayys.wayang.agenticcommerce.wayang;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCancelCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCompleteCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCreateCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceUpdateCheckoutSessionRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime {@link AgentSkill} wrapper for one Agentic Commerce checkout operation.
 */
public final class AgenticCommerceCheckoutAgentSkill implements AgentSkill {

    private static final List<String> ROUTING_KEYS = List.of(
            "skill",
            "skillId",
            "operation",
            "payload",
            "request",
            "body",
            "checkout_session_id",
            "checkoutSessionId");

    private final AgenticCommerceCheckoutService service;
    private final SkillDefinition definition;
    private final String operation;

    public AgenticCommerceCheckoutAgentSkill(
            AgenticCommerceCheckoutService service,
            SkillDefinition definition) {
        this.service = Objects.requireNonNull(service, "service");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.operation = operation(definition);
    }

    public static AgenticCommerceCheckoutAgentSkill of(
            AgenticCommerceCheckoutService service,
            String skillId) {
        SkillDefinition definition = new AgenticCommerceCheckoutSkillProjector()
                .skillForId(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Agentic Commerce checkout skill id: " + skillId));
        return new AgenticCommerceCheckoutAgentSkill(service, definition);
    }

    @Override
    public String id() {
        return definition.id();
    }

    @Override
    public String name() {
        return definition.name();
    }

    @Override
    public List<String> aliases() {
        return List.of(operation);
    }

    @Override
    public String description() {
        return definition.description();
    }

    @Override
    public String version() {
        return AgenticCommerceWayangMaps.text(definition.metadata().get(SkillMetadataKeys.KEY_VERSION));
    }

    @Override
    public String category() {
        return SkillCategory.EXECUTION.name();
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        return Uni.createFrom()
                .item(() -> executeNow(context == null ? Map.of() : context))
                .onFailure()
                .recoverWithItem(this::errorResult);
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        String requested = AgenticCommerceWayangMaps.firstText(inputs, "skillId", "skill", "operation");
        return requested.isBlank() || id().equals(requested) || operation.equals(requested);
    }

    private Map<String, Object> executeNow(Map<String, Object> context) {
        AgenticCommerceCheckoutHttpResult result = switch (operation) {
            case AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION -> service.create(
                    AgenticCommerceCreateCheckoutSessionRequest.fromMap(payload(context)));
            case AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION -> service.retrieve(sessionId(context));
            case AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION -> service.update(
                    sessionId(context),
                    AgenticCommerceUpdateCheckoutSessionRequest.fromMap(payload(context)));
            case AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION -> service.complete(
                    sessionId(context),
                    AgenticCommerceCompleteCheckoutSessionRequest.fromMap(payload(context)));
            case AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION -> cancel(context);
            default -> throw new IllegalStateException("Unsupported Agentic Commerce checkout operation: " + operation);
        };
        return resultMap(result);
    }

    private AgenticCommerceCheckoutHttpResult cancel(Map<String, Object> context) {
        Map<String, Object> payload = payload(context);
        if (payload.isEmpty()) {
            return service.cancel(sessionId(context));
        }
        return service.cancel(
                sessionId(context),
                AgenticCommerceCancelCheckoutSessionRequest.fromMap(payload));
    }

    private Map<String, Object> resultMap(AgenticCommerceCheckoutHttpResult result) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("success", result.successful());
        values.put("status", result.successful() ? "SUCCESS" : "FAILURE");
        values.put("observation", observation(result));
        values.put("skill_id", id());
        values.put("operation", result.operation());
        values.put("status_code", result.response().statusCode());
        values.put("valid", result.valid());
        values.put("successful", result.successful());
        values.put("issue_count", result.issueCount());
        values.put("result", result.toMap());
        values.put("body", result.body());
        if (result.hasCheckoutSession()) {
            values.put("checkout_session", result.checkoutSession().toMap());
            values.put("checkout_session_id", result.checkoutSession().id());
            values.put("checkout_status", result.checkoutSession().status());
        }
        if (result.hasError()) {
            values.put("error", result.error().toMap());
        }
        return Map.copyOf(values);
    }

    private String observation(AgenticCommerceCheckoutHttpResult result) {
        if (result.hasError()) {
            return "Agentic Commerce checkout operation "
                    + operation
                    + " failed: "
                    + result.error().code();
        }
        String sessionId = result.hasCheckoutSession() ? result.checkoutSession().id() : "";
        String suffix = sessionId.isBlank() ? "" : " for checkout session " + sessionId;
        return "Agentic Commerce checkout operation "
                + operation
                + " returned HTTP "
                + result.response().statusCode()
                + suffix
                + ".";
    }

    private Map<String, Object> errorResult(Throwable error) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("success", false);
        values.put("status", "ERROR");
        values.put("observation", error == null ? "Agentic Commerce checkout skill failed." : error.getMessage());
        values.put("skill_id", id());
        values.put("operation", operation);
        values.put("error", error == null ? "" : error.getClass().getName());
        return Map.copyOf(values);
    }

    private static Map<String, Object> payload(Map<String, Object> context) {
        Object nested = AgenticCommerceWayangMaps.first(context, "payload", "request", "body");
        if (nested instanceof Map<?, ?> map) {
            return withoutSessionRouting(map);
        }
        return withoutSessionRouting(context);
    }

    private static Map<String, Object> withoutSessionRouting(Map<?, ?> values) {
        Map<String, Object> payload = new LinkedHashMap<>(AgenticCommerceWayangMaps.copy(values));
        ROUTING_KEYS.forEach(payload::remove);
        return Map.copyOf(payload);
    }

    private static String sessionId(Map<String, Object> context) {
        String sessionId = AgenticCommerceWayangMaps.firstText(
                context,
                "checkout_session_id",
                "checkoutSessionId",
                "id");
        if (sessionId.isBlank()) {
            Object nested = AgenticCommerceWayangMaps.first(context, "payload", "request", "body");
            if (nested instanceof Map<?, ?> map) {
                sessionId = AgenticCommerceWayangMaps.firstText(
                        map,
                        "checkout_session_id",
                        "checkoutSessionId",
                        "id");
            }
        }
        return AgenticCommerceWayangMaps.required(sessionId, "checkout_session_id");
    }

    private static String operation(SkillDefinition definition) {
        String operation = AgenticCommerceWayangMaps.text(definition.metadata().get(AgenticCommerceWayang.METADATA_OPERATION));
        if (operation.isBlank()) {
            throw new IllegalArgumentException("Agentic Commerce checkout skill definition is missing operation metadata");
        }
        return operation;
    }
}
