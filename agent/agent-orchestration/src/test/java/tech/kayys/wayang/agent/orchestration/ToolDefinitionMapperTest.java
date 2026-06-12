package tech.kayys.wayang.agent.orchestration;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.InferenceTypes.ToolDefinition;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolDefinitionMapperTest {

    @Test
    void mapsCanonicalToolToAgentToolDefinition() {
        ToolDefinition definition = ToolDefinitionMapper.fromTool(new EchoTool());

        assertThat(definition.name()).isEqualTo("echo");
        assertThat(definition.description()).isEqualTo("Echo input text");
        assertThat(definition.parameters()).containsEntry("type", "object");
    }

    private static final class EchoTool implements Tool {
        @Override
        public String id() {
            return "echo";
        }

        @Override
        public String name() {
            return "Echo";
        }

        @Override
        public String description() {
            return "Echo input text";
        }

        @Override
        public Map<String, Object> inputSchema() {
            return Map.of("type", "object");
        }

        @Override
        public ToolResult execute(Map<String, Object> params, ToolContext context) {
            return ToolResult.success(params);
        }
    }
}
