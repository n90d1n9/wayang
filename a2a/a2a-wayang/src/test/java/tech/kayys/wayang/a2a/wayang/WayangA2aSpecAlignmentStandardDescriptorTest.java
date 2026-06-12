package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentStandardDescriptorTest {

    @Test
    void projectsPinnedDescriptorInStableOrder() {
        String json = WayangA2aHttpJson.write(
                WayangA2aSpecAlignmentStandardDescriptor.pinned().toMap());

        assertThat(WayangA2aSpecAlignmentStandardDescriptor.pinned().toMap())
                .containsEntry("standardId", WayangA2aSpecAlignmentReport.STANDARD_ID)
                .containsEntry("name", WayangA2aSpecAlignmentReport.STANDARD_NAME)
                .containsEntry("version", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("specUrl", WayangA2aSpecAlignmentReport.SPEC_URL);
        assertThat(json).startsWith("{\"standardId\":");
        assertThat(json.indexOf("\"specUrl\"")).isGreaterThan(json.indexOf("\"binding\""));
    }

    @Test
    void normalizesBlankFieldsToPinnedDefaults() {
        WayangA2aSpecAlignmentStandardDescriptor descriptor =
                new WayangA2aSpecAlignmentStandardDescriptor(" ", " ", " ", " ", " ");

        assertThat(descriptor.toMap())
                .containsEntry("standardId", WayangA2aSpecAlignmentReport.STANDARD_ID)
                .containsEntry("name", WayangA2aSpecAlignmentReport.STANDARD_NAME)
                .containsEntry("version", A2aProtocol.VERSION)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("specUrl", WayangA2aSpecAlignmentReport.SPEC_URL);
    }

    @Test
    void keepsSnapshotVersionAndBindingOverrides() {
        WayangA2aSpecAlignmentStandardDescriptor descriptor =
                WayangA2aSpecAlignmentStandardDescriptor.from("1.1", A2aProtocol.BINDING_HTTP_JSON);

        assertThat(descriptor.toMap())
                .containsEntry("version", "1.1")
                .containsEntry("binding", A2aProtocol.BINDING_HTTP_JSON)
                .containsEntry("standardId", WayangA2aSpecAlignmentReport.STANDARD_ID);
    }
}
