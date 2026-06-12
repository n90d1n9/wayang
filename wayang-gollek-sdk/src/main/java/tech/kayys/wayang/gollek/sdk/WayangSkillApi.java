package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

/**
 * Dynamic skill API for RAG, MCP, workflow, and assistant capabilities.
 *
 * <p>This facade keeps skill discovery and skill JSON envelopes in the SDK so
 * product wrappers can expose capabilities without duplicating registry or
 * wire-format logic.</p>
 */
public final class WayangSkillApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    WayangSkillApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public AgentSkillDiscovery discover(AgentSkillQuery query) {
        return discover(query, "");
    }

    public AgentSkillDiscovery discover(AgentSkillQuery query, String search) {
        return sdk.skillDiscovery(query, search);
    }

    public List<RegisteredSkill> list(AgentSkillQuery query) {
        return discover(query).skills();
    }

    public RegisteredSkill get(String skillId) {
        return sdk.skill(skillId);
    }

    public Map<String, Object> discoveryEnvelope(AgentSkillQuery query) {
        return discoveryEnvelope(discover(query));
    }

    public Map<String, Object> discoveryEnvelope(AgentSkillQuery query, String search) {
        return discoveryEnvelope(discover(query, search));
    }

    public Map<String, Object> discoveryEnvelope(AgentSkillDiscovery discovery) {
        return AgentSkillEnvelopes.discovery(productName(), discovery);
    }

    public Map<String, Object> detailEnvelope(RegisteredSkill skill) {
        return AgentSkillEnvelopes.detail(productName(), skill);
    }

    public String discoveryJson(AgentSkillQuery query) {
        return discoveryJson(discover(query));
    }

    public String discoveryJson(AgentSkillQuery query, String search) {
        return discoveryJson(discover(query, search));
    }

    public String discoveryJson(AgentSkillDiscovery discovery) {
        return wire.object(discoveryEnvelope(discovery));
    }

    public String detailJson(RegisteredSkill skill) {
        return wire.object(detailEnvelope(skill));
    }

    private String productName() {
        return sdk.status().productName();
    }
}
