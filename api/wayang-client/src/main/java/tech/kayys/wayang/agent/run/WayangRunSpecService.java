package tech.kayys.wayang.agent.run;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import tech.kayys.wayang.client.SdkText;

/**
 * File-backed service for reading, writing, and formatting portable Wayang run specs.
 *
 * <p>The service intentionally owns only property-file parsing and persistence;
 * higher-level validation and JSON rendering live in {@link WayangSpecApi}.</p>
 */
public final class WayangRunSpecService {

    private static final WayangRunSpecService INSTANCE = new WayangRunSpecService();

    private WayangRunSpecService() {
    }

    public static WayangRunSpecService create() {
        return INSTANCE;
    }

    public AgentRunRequest readOrDefault(String specPath) {
        return readSpecOrDefault(specPath).request();
    }

    public WayangRunSpec readSpecOrDefault(String specPath) {
        return readSpecOrDefault(specPath, "");
    }

    public WayangRunSpec readSpecOrDefault(String specPath, String profileId) {
        String normalizedProfileId = SdkText.trimToEmpty(profileId);
        if (specPath == null || specPath.isBlank()) {
            if (!normalizedProfileId.isBlank()) {
                return profileTemplateSpec(normalizedProfileId);
            }
            return WayangRunSpec.of(AgentRunRequest.builder().build());
        }
        return readSpec(specPath, normalizedProfileId);
    }

    public AgentRunRequest read(String specPath) {
        return readSpec(specPath).request();
    }

    public WayangRunSpec readSpec(String specPath) {
        return readSpec(specPath, "");
    }

    public WayangRunSpec readSpec(String specPath, String profileId) {
        return readSpec(path("Run spec", specPath), profileId);
    }

    public AgentRunRequest read(Path specPath) {
        return readSpec(specPath).request();
    }

    public WayangRunSpec readSpec(Path specPath) {
        return readSpec(specPath, "");
    }

    public WayangRunSpec readSpec(Path specPath, String profileId) {
        Path source = path("Run spec", specPath);
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(source)) {
            properties.load(input);
            return WayangRunSpec.fromProperties(properties, profileId);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read run spec file: " + source, e);
        }
    }

    public AgentRunRequest template(String surfaceId) {
        return templateSpec(surfaceId).request();
    }

    public WayangRunSpec templateSpec(String surfaceId) {
        return WayangRunSpec.template(surfaceId);
    }

    public WayangRunSpec profileTemplateSpec(String profileId) {
        return WayangRunSpec.profileTemplate(profileId);
    }

    public String format(AgentRunRequest request) {
        return format(WayangRunSpec.of(request));
    }

    public String format(WayangRunSpec spec) {
        return WayangRunSpec.formatProperties(spec);
    }

    public String templateProperties(String surfaceId) {
        return format(templateSpec(surfaceId));
    }

    public String profileTemplateProperties(String profileId) {
        return format(profileTemplateSpec(profileId));
    }

    public void write(String specPath, AgentRunRequest request, boolean force) {
        write(path("Run spec", specPath), request, force);
    }

    public void write(Path specPath, AgentRunRequest request, boolean force) {
        writeSpec(specPath, WayangRunSpec.of(request), force);
    }

    public void writeSpec(String specPath, WayangRunSpec spec, boolean force) {
        writeSpec(path("Run spec", specPath), spec, force);
    }

    public void writeSpec(Path specPath, WayangRunSpec spec, boolean force) {
        writeProperties("Wayang run spec", specPath, format(spec), force);
    }

    public void writeTemplate(String specPath, String surfaceId, boolean force) {
        writeTemplate(path("Run spec template", specPath), surfaceId, force);
    }

    public void writeTemplate(Path specPath, String surfaceId, boolean force) {
        writeProperties("Wayang run spec template", specPath, templateProperties(surfaceId), force);
    }

    public void writeProfileTemplate(String specPath, String profileId, boolean force) {
        writeProfileTemplate(path("Run spec profile template", specPath), profileId, force);
    }

    public void writeProfileTemplate(Path specPath, String profileId, boolean force) {
        writeProperties("Wayang run spec profile template", specPath, profileTemplateProperties(profileId), force);
    }

    public void writeProperties(String label, String specPath, String properties, boolean force) {
        writeProperties(label, path(label, specPath), properties, force);
    }

    public void writeProperties(String label, Path specPath, String properties, boolean force) {
        Path target = path(label, specPath);
        String normalizedLabel = label == null || label.isBlank() ? "Wayang run spec" : label.trim();
        if (Files.exists(target) && !force) {
            throw new IllegalArgumentException(normalizedLabel + " already exists: " + target + ". Use --force to overwrite.");
        }
        try {
            Path parent = target.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, properties == null ? "" : properties, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to write " + normalizedLabel.toLowerCase() + ": " + target, e);
        }
    }

    private static Path path(String label, String specPath) {
        if (specPath == null || specPath.isBlank()) {
            throw new IllegalArgumentException(label + " path is required.");
        }
        return Path.of(specPath);
    }

    private static Path path(String label, Path specPath) {
        if (specPath == null) {
            throw new IllegalArgumentException(label + " path is required.");
        }
        return specPath;
    }
}
