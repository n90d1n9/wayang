package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSkillsRequestTest {

    @Test
    void defaultsToNoCategoryFilter() {
        assertThat(new AgentSkillsRequest().category()).isNull();
    }

    @Test
    void acceptsCategoryFromBeanSetter() {
        AgentSkillsRequest request = new AgentSkillsRequest();

        request.setCategory("REASONING");

        assertThat(request.category()).isEqualTo("REASONING");
    }
}
