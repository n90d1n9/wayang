package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.QueryParam;

/**
 * Query parameters for agent skill catalog listing.
 */
public final class AgentSkillsRequest {

    private String category;

    public AgentSkillsRequest() {
        this(null);
    }

    AgentSkillsRequest(String category) {
        this.category = category;
    }

    public String category() {
        return category;
    }

    @QueryParam("category")
    public void setCategory(String category) {
        this.category = category;
    }
}
