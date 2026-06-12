package tech.kayys.gamelan.agent.agui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class AguiEventTest {

    @Test void runStartedType()       { assertThat(AguiEvent.runStarted("r").type()).isEqualTo("RUN_STARTED"); }
    @Test void textDeltaDelta()       { assertThat(AguiEvent.textDelta("r","m","tok").delta()).isEqualTo("tok"); }
    @Test void toolCallStartName()    { assertThat(AguiEvent.toolCallStart("r","tc","read_file").toolName()).isEqualTo("read_file"); }
    @Test void errorType()            { assertThat(AguiEvent.error("r","oops").error()).isEqualTo("oops"); }
    @Test void stepStartedStep()      { assertThat(AguiEvent.stepStarted("r", 3).step()).isEqualTo(3); }
    @Test void runFinishedNoError()   { assertThat(AguiEvent.runFinished("r",true,null).error()).isNull(); }
    @Test void runFinishedWithError() { assertThat(AguiEvent.runFinished("r",false,"fail").error()).isEqualTo("fail"); }
    @Test void timestampIsSet()       { assertThat(AguiEvent.runStarted("r").timestamp()).isGreaterThan(0); }

    @Test
    void sseFrameFormat() {
        String frame = AguiEvent.textDelta("run-1", "msg-1", "hello").toSseFrame();
        assertThat(frame).startsWith("data: {");
        assertThat(frame).contains("TEXT_MESSAGE_CONTENT");
        assertThat(frame).contains("run-1");
        assertThat(frame).endsWith("\n\n");
    }

    @Test
    void toJsonIsObject() {
        String json = AguiEvent.toolCallStart("r","tc","glob").toJson();
        assertThat(json).startsWith("{").endsWith("}").contains("\"type\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "RUN_STARTED","RUN_FINISHED","TEXT_MESSAGE_START","TEXT_MESSAGE_CONTENT",
        "TEXT_MESSAGE_END","TOOL_CALL_START","TOOL_CALL_END",
        "STEP_STARTED","STEP_FINISHED","STATE_SNAPSHOT","ERROR"
    })
    void allTypesSerialize(String type) {
        AguiEvent e = AguiEvent.builder(type).runId("r").build();
        assertThat(e.toSseFrame()).contains(type);
    }

    @Test
    void stateSnapshotHasState() {
        AguiEvent e = AguiEvent.stateSnapshot("r", Map.of("task","test"));
        assertThat(e.state()).containsKey("task");
    }
}
