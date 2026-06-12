package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Projects Agentic Commerce checkout operations into Wayang skill definitions.
 */
public final class AgenticCommerceCheckoutSkillProjector {

    private static final List<CheckoutSkillSpec> CHECKOUT_SKILLS = List.of(
            new CheckoutSkillSpec(
                    AgenticCommerceWayang.SKILL_CREATE_CHECKOUT,
                    "Create Checkout Session",
                    "Create an Agentic Commerce checkout session for a buyer cart.",
                    AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                    "POST",
                    AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS,
                    List.of("buyer", "line_items", "currency")),
            new CheckoutSkillSpec(
                    AgenticCommerceWayang.SKILL_RETRIEVE_CHECKOUT,
                    "Retrieve Checkout Session",
                    "Retrieve the latest state for an Agentic Commerce checkout session.",
                    AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                    "GET",
                    AgenticCommerceProtocol.PATH_CHECKOUT_SESSION,
                    List.of("checkout_session_id")),
            new CheckoutSkillSpec(
                    AgenticCommerceWayang.SKILL_UPDATE_CHECKOUT,
                    "Update Checkout Session",
                    "Update checkout buyer, line item, fulfillment, coupon, or discount details.",
                    AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                    "POST",
                    AgenticCommerceProtocol.PATH_CHECKOUT_SESSION,
                    List.of("checkout_session_id")),
            new CheckoutSkillSpec(
                    AgenticCommerceWayang.SKILL_COMPLETE_CHECKOUT,
                    "Complete Checkout Session",
                    "Complete an Agentic Commerce checkout session with payment and risk context.",
                    AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                    "POST",
                    AgenticCommerceProtocol.PATH_CHECKOUT_SESSION_COMPLETE,
                    List.of("checkout_session_id", "payment_data")),
            new CheckoutSkillSpec(
                    AgenticCommerceWayang.SKILL_CANCEL_CHECKOUT,
                    "Cancel Checkout Session",
                    "Cancel an Agentic Commerce checkout session.",
                    AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION,
                    "POST",
                    AgenticCommerceProtocol.PATH_CHECKOUT_SESSION_CANCEL,
                    List.of("checkout_session_id")));

    public List<SkillDefinition> checkoutSkills() {
        return CHECKOUT_SKILLS.stream().map(this::skill).toList();
    }

    public Optional<SkillDefinition> skillForId(String skillId) {
        String normalized = AgenticCommerceWayangMaps.text(skillId);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return CHECKOUT_SKILLS.stream()
                .filter(spec -> spec.id().equals(normalized))
                .findFirst()
                .map(this::skill);
    }

    public Optional<SkillDefinition> skillForOperation(String operation) {
        String normalized = AgenticCommerceWayangMaps.text(operation);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return CHECKOUT_SKILLS.stream()
                .filter(spec -> spec.operation().equals(normalized))
                .findFirst()
                .map(this::skill);
    }

    private SkillDefinition skill(CheckoutSkillSpec spec) {
        return SkillDefinition.builder()
                .id(spec.id())
                .name(spec.name())
                .description(spec.description())
                .category(SkillCategory.EXECUTION.name())
                .systemPrompt(systemPrompt(spec))
                .userPromptTemplate("Execute " + spec.operation() + " through the configured Wayang Agentic Commerce connector.")
                .temperature(0.0)
                .maxTokens(1024)
                .tools(List.of(AgenticCommerceWayang.PROTOCOL_ID))
                .metadata(metadata(spec))
                .build();
    }

    private static String systemPrompt(CheckoutSkillSpec spec) {
        return "Use the Agentic Commerce Protocol checkout operation "
                + spec.operation()
                + " through the configured Wayang connector. Return the decoded checkout HTTP result as JSON.";
    }

    private static Map<String, Object> metadata(CheckoutSkillSpec spec) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SkillMetadataKeys.KEY_CATEGORY, SkillCategory.EXECUTION.name());
        values.put(SkillMetadataKeys.KEY_DOMAINS, List.of("commerce", "checkout"));
        values.put(SkillMetadataKeys.KEY_TAGS, List.of("commerce", "checkout", "agentic-commerce"));
        values.put(SkillMetadataKeys.KEY_OUTPUT_FORMAT, "json");
        values.put(SkillMetadataKeys.KEY_VERSION, AgenticCommerceProtocol.SPEC_VERSION);
        values.put(SkillMetadataKeys.KEY_INPUT_SCHEMA, inputSchema(spec));
        values.put(AgenticCommerceWayang.METADATA_PROTOCOL, AgenticCommerceWayang.PROTOCOL_ID);
        values.put(AgenticCommerceWayang.METADATA_SPEC_VERSION, AgenticCommerceProtocol.SPEC_VERSION);
        values.put(AgenticCommerceWayang.METADATA_OPERATION, spec.operation());
        values.put(AgenticCommerceWayang.METADATA_HTTP_METHOD, spec.httpMethod());
        values.put(AgenticCommerceWayang.METADATA_PATH_TEMPLATE, spec.pathTemplate());
        return Map.copyOf(values);
    }

    private static Map<String, Object> inputSchema(CheckoutSkillSpec spec) {
        return Map.of(
                "type",
                "object",
                "required",
                spec.requiredFields(),
                "additionalProperties",
                true);
    }

    private record CheckoutSkillSpec(
            String id,
            String name,
            String description,
            String operation,
            String httpMethod,
            String pathTemplate,
            List<String> requiredFields) {
    }
}
