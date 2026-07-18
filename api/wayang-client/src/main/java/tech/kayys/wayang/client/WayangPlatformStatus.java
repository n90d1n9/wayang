package tech.kayys.wayang.client;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;

public record WayangPlatformStatus(
        String productName,
        String version,
        ComponentStatus gollek,
        ComponentStatus gamelan,
        ComponentStatus agentCore,
        ComponentStatus rag,
        ComponentStatus mcp,
        int activeSkills,
        List<String> notes) {

    public WayangPlatformStatus {
        productName = SdkText.trimToDefault(productName, "Wayang");
        version = SdkText.trimToDefault(version, "unknown");
        gollek = gollek == null ? unknown("Gollek") : gollek;
        gamelan = gamelan == null ? unknown("Gamelan") : gamelan;
        agentCore = agentCore == null ? unknown("Agent Core") : agentCore;
        rag = rag == null ? unknown("RAG Runtime") : rag;
        mcp = mcp == null ? unknown("MCP") : mcp;
        activeSkills = Math.max(0, activeSkills);
        notes = SdkLists.copy(notes);
    }

    private static ComponentStatus unknown(String name) {
        return new ComponentStatus(name, "", "unknown", "", 0);
    }
}
