package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsConfigSampleRequestTest {

    @Test
    void defaultsToDefaultPropertiesSample() {
        SkillsConfigSampleRequest request = SkillsConfigSampleRequest.defaults();

        assertThat(request.profileName()).isEqualTo("default");
        assertThat(request.formatName()).isEqualTo("properties");
        assertThat(request.format()).isEqualTo(SkillsConfigSampleFormat.PROPERTIES);
    }

    @Test
    void normalizesProfileAndFormatNames() {
        SkillsConfigSampleRequest request = SkillsConfigSampleRequest.fromOptions(" rustfs ", " env ");

        assertThat(request.profileName()).isEqualTo("rustfs");
        assertThat(request.formatName()).isEqualTo("env");
        assertThat(request.format()).isEqualTo(SkillsConfigSampleFormat.ENV);
    }

    @Test
    void sampleServiceCanReceiveRequestObject() {
        SkillsConfigSampleReport report = new SkillsConfigSampleService().report(
                SkillsConfigSampleRequest.fromOptions("rustfs", "properties"));

        assertThat(report.sample().profile()).isEqualTo("object-storage");
        assertThat(report.format()).isEqualTo(SkillsConfigSampleFormat.PROPERTIES);
    }
}
