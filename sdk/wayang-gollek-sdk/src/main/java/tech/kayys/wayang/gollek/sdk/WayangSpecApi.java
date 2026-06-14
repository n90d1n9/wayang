package tech.kayys.wayang.gollek.sdk;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Run-spec API for portable templates, validation, persistence, and JSON envelopes.
 *
 * <p>This facade makes run specs a stable SDK concern instead of a CLI-only
 * helper, which lets agent products reuse the same templates and validation
 * contracts from command-line, TUI, HTTP, and low-code surfaces.</p>
 */
public final class WayangSpecApi {

    private final WayangRunSpecService runSpecs;
    private final WayangRunApi runs;
    private final WayangWireApi wire;

    WayangSpecApi(WayangRunSpecService runSpecs, WayangRunApi runs, WayangWireApi wire) {
        this.runSpecs = runSpecs == null ? WayangRunSpecService.create() : runSpecs;
        this.runs = Objects.requireNonNull(runs, "runs");
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public AgentRunRequest read(String specPath) {
        return readSpec(specPath).request();
    }

    public WayangRunSpec readSpec(String specPath) {
        return runSpecs.readSpec(specPath);
    }

    public WayangRunSpec readSpec(String specPath, String profileId) {
        return runSpecs.readSpec(specPath, profileId);
    }

    public AgentRunRequest read(Path specPath) {
        return readSpec(specPath).request();
    }

    public WayangRunSpec readSpec(Path specPath) {
        return runSpecs.readSpec(specPath);
    }

    public WayangRunSpec readSpec(Path specPath, String profileId) {
        return runSpecs.readSpec(specPath, profileId);
    }

    public AgentRunRequest readOrDefault(String specPath) {
        return readSpecOrDefault(specPath).request();
    }

    public WayangRunSpec readSpecOrDefault(String specPath) {
        return runSpecs.readSpecOrDefault(specPath);
    }

    public WayangRunSpec readSpecOrDefault(String specPath, String profileId) {
        return runSpecs.readSpecOrDefault(specPath, profileId);
    }

    public AgentRunPreview validate(String specPath) {
        return validate(readSpec(specPath));
    }

    public AgentRunPreview validate(Path specPath) {
        return validate(readSpec(specPath));
    }

    public AgentRunPreview validate(WayangRunSpec spec) {
        WayangRunSpec source = spec == null ? WayangRunSpec.of(AgentRunRequest.builder().build()) : spec;
        return runs.preview(source.request());
    }

    public AgentRunRequest template(String surfaceId) {
        return templateSpec(surfaceId).request();
    }

    public WayangRunSpec templateSpec(String surfaceId) {
        return runSpecs.templateSpec(surfaceId);
    }

    public WayangRunSpec profileTemplateSpec(String profileId) {
        return runSpecs.profileTemplateSpec(profileId);
    }

    public String format(AgentRunRequest request) {
        return runSpecs.format(request);
    }

    public String format(WayangRunSpec spec) {
        return runSpecs.format(spec);
    }

    public String templateProperties(String surfaceId) {
        return runSpecs.templateProperties(surfaceId);
    }

    public String profileTemplateProperties(String profileId) {
        return runSpecs.profileTemplateProperties(profileId);
    }

    public void write(String specPath, AgentRunRequest request, boolean force) {
        runSpecs.write(specPath, request, force);
    }

    public void write(Path specPath, AgentRunRequest request, boolean force) {
        runSpecs.write(specPath, request, force);
    }

    public void writeSpec(String specPath, WayangRunSpec spec, boolean force) {
        runSpecs.writeSpec(specPath, spec, force);
    }

    public void writeSpec(Path specPath, WayangRunSpec spec, boolean force) {
        runSpecs.writeSpec(specPath, spec, force);
    }

    public void writeTemplate(String specPath, String surfaceId, boolean force) {
        runSpecs.writeTemplate(specPath, surfaceId, force);
    }

    public void writeTemplate(Path specPath, String surfaceId, boolean force) {
        runSpecs.writeTemplate(specPath, surfaceId, force);
    }

    public void writeProfileTemplate(String specPath, String profileId, boolean force) {
        runSpecs.writeProfileTemplate(specPath, profileId, force);
    }

    public void writeProfileTemplate(Path specPath, String profileId, boolean force) {
        runSpecs.writeProfileTemplate(specPath, profileId, force);
    }

    public void writeProperties(String label, String specPath, String properties, boolean force) {
        runSpecs.writeProperties(label, specPath, properties, force);
    }

    public void writeProperties(String label, Path specPath, String properties, boolean force) {
        runSpecs.writeProperties(label, specPath, properties, force);
    }

    public Map<String, Object> validationEnvelope(String path, AgentRunPreview preview) {
        return WayangRunSpecEnvelopes.validation(path, preview);
    }

    public String validationJson(String path, AgentRunPreview preview) {
        return wire.object(validationEnvelope(path, preview));
    }
}
