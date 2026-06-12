package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.AgentSkillState;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.WayangContractQuery;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliQueryOptionsTest {

    @Test
    void skillOptionsMapToSdkQuery() {
        WayangSkillQueryOptions options = new WayangSkillQueryOptions();
        options.surfaceId = "Assistant-Agent";
        options.profileId = "assistant-agent";
        options.category = "Knowledge";
        options.source = "RAG";
        options.state = "preview";
        options.tag = "Docs";
        options.inputKey = "prompt";
        options.outputKey = "answer";

        AgentSkillQuery query = options.toQuery("RAG.Retrieve");

        assertThat(query.surfaceId()).isEqualTo("assistant-agent");
        assertThat(query.profileId()).isEqualTo("assistant-agent");
        assertThat(query.category()).isEqualTo("Knowledge");
        assertThat(query.source()).isEqualTo("rag");
        assertThat(query.state()).isEqualTo(AgentSkillState.PREVIEW);
        assertThat(query.skillId()).isEqualTo("rag.retrieve");
        assertThat(query.tag()).isEqualTo("docs");
        assertThat(query.inputKey()).isEqualTo("prompt");
        assertThat(query.outputKey()).isEqualTo("answer");
    }

    @Test
    void contractOptionsMapToSdkQuery() {
        WayangContractQueryOptions options = new WayangContractQueryOptions();
        options.schema = " wayang.run.planning ";
        options.envelope = " run-preview ";
        options.commandId = " run-dry-json ";
        options.domain = " planning ";
        options.jsonSchemaId = " urn:wayang:contract:wayang.run.planning:v1:run-preview ";

        WayangContractQuery query = options.toQuery();

        assertThat(query.schema()).isEqualTo("wayang.run.planning");
        assertThat(query.envelope()).isEqualTo("run-preview");
        assertThat(query.commandId()).isEqualTo("run-dry-json");
        assertThat(query.domain()).isEqualTo("planning");
        assertThat(query.jsonSchemaId()).isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(query.filtered()).isTrue();
    }

    @Test
    void commandOptionsMapToSdkQuery() {
        WayangCommandQueryOptions options = new WayangCommandQueryOptions();
        options.surfaceId = " assistant-agent ";
        options.profileId = " assistant-agent ";
        options.category = " Runs ";
        options.commandId = " run-session-context ";
        options.contractJsonSchemaId = " urn:wayang:contract:wayang.run.lifecycle:v1:run-status ";

        WorkbenchCommandQuery query = options.toQuery();

        assertThat(query.surfaceId()).isEqualTo("assistant-agent");
        assertThat(query.profileId()).isEqualTo("assistant-agent");
        assertThat(query.category()).isEqualTo("Runs");
        assertThat(query.commandId()).isEqualTo("run-session-context");
        assertThat(query.contractJsonSchemaId()).isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-status");
        assertThat(query.filtered()).isTrue();
    }

    @Test
    void runEventOptionsMapToSdkQuery() {
        WayangRunEventQueryOptions options = new WayangRunEventQueryOptions();
        options.state = "completed";
        options.type = " Run.Completed ";
        options.afterSequence = 10L;
        options.limit = 20;

        AgentRunEventsQuery query = options.toQuery();

        assertThat(query.state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(query.type()).isEqualTo("run.completed");
        assertThat(query.afterSequence()).isEqualTo(10L);
        assertThat(query.limit()).isEqualTo(20);
        assertThat(query.filtered()).isTrue();
    }

    @Test
    void runHistoryOptionsMapToSdkQuery() {
        WayangRunHistoryQueryOptions options = new WayangRunHistoryQueryOptions();
        options.state = "failed";
        options.limit = 10;
        options.offset = 5;
        options.tenantId = " tenant-a ";
        options.sessionId = " session-a ";
        options.surfaceId = " assistant-agent ";
        options.profileId = " assistant-agent ";

        AgentRunHistoryQuery query = options.toQuery();

        assertThat(query.state()).isEqualTo(AgentRunState.FAILED);
        assertThat(query.limit()).isEqualTo(10);
        assertThat(query.offset()).isEqualTo(5);
        assertThat(query.tenantId()).isEqualTo("tenant-a");
        assertThat(query.sessionId()).isEqualTo("session-a");
        assertThat(query.surfaceId()).isEqualTo("assistant-agent");
        assertThat(query.profileId()).isEqualTo("assistant-agent");
        assertThat(query.filtered()).isTrue();
    }
}
