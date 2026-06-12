package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime-facing diagnostics for Hermes capabilities, adapters, and learned-skill persistence.
 */
public record HermesRuntimeDiagnostics(
        HermesRuntimeCapabilities capabilities,
        HermesRuntimePorts runtimePorts,
        HermesLearnedSkillPersistencePreflightReport skillPersistencePreflight,
        HermesRuntimeAssemblyReport assemblyReport,
        HermesRuntimeLifecycle lifecycle,
        List<String> attention) {

    public HermesRuntimeDiagnostics {
        capabilities = capabilities == null
                ? HermesAgentModeConfig.defaults().runtimeCapabilities()
                : capabilities;
        runtimePorts = runtimePorts == null ? HermesRuntimePorts.noop() : runtimePorts;
        skillPersistencePreflight = skillPersistencePreflight == null
                ? HermesLearnedSkillPersistencePreflight.inspect(HermesAgentModeConfig.defaults())
                : skillPersistencePreflight;
        assemblyReport = assemblyReport == null ? HermesRuntimeAssemblyReport.empty() : assemblyReport;
        attention = HermesCollections.copyNonNull(
                attention == null ? attentionMessages(runtimePorts, skillPersistencePreflight) : attention);
        lifecycle = lifecycle == null
                ? HermesRuntimeLifecycle.from(
                        HermesAgentModeConfig.defaults(),
                        runtimePorts,
                        skillPersistencePreflight,
                        attention)
                : lifecycle;
    }

    public static HermesRuntimeDiagnostics from(
            HermesAgentModeConfig config,
            HermesRuntimePorts runtimePorts) {
        return from(config, runtimePorts, Optional.empty(), Optional.empty());
    }

    public static HermesRuntimeDiagnostics from(
            HermesAgentModeConfig config,
            HermesRuntimePorts runtimePorts,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        return from(
                config,
                runtimePorts,
                objectStorageService,
                dataSource,
                HermesRuntimeAssemblyReport.empty());
    }

    public static HermesRuntimeDiagnostics from(
            HermesAgentModeConfig config,
            HermesRuntimePorts runtimePorts,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource,
            HermesRuntimeAssemblyReport assemblyReport) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesRuntimePorts effectivePorts = runtimePorts == null ? HermesRuntimePorts.noop() : runtimePorts;
        HermesLearnedSkillPersistencePreflightReport skillPersistencePreflight =
                HermesLearnedSkillPersistencePreflight.inspect(effectiveConfig, objectStorageService, dataSource);
        List<String> attention = attentionMessages(effectivePorts, skillPersistencePreflight);
        return new HermesRuntimeDiagnostics(
                effectiveConfig.runtimeCapabilities(),
                effectivePorts,
                skillPersistencePreflight,
                assemblyReport,
                HermesRuntimeLifecycle.from(effectiveConfig, effectivePorts, skillPersistencePreflight, attention),
                attention);
    }

    public boolean ready() {
        return runtimePortsReady() && skillPersistenceReady();
    }

    public boolean runtimePortsReady() {
        return runtimePorts.descriptors().stream().allMatch(HermesRuntimePortDescriptor::ready);
    }

    public boolean skillPersistenceReady() {
        return skillPersistencePreflight.ready();
    }

    public boolean learningAuditConfigured() {
        HermesRuntimePortDescriptor descriptor = learningAuditDescriptor();
        return descriptor.configured() && !descriptor.noop();
    }

    public boolean learningAuditReady() {
        HermesRuntimePortDescriptor descriptor = learningAuditDescriptor();
        return learningAuditConfigured() && descriptor.ready();
    }

    public long configuredPortCount() {
        return runtimePorts.descriptors().stream().filter(HermesRuntimePortDescriptor::configured).count();
    }

    public long readyPortCount() {
        return runtimePorts.descriptors().stream().filter(HermesRuntimePortDescriptor::ready).count();
    }

    public long noopPortCount() {
        return runtimePorts.descriptors().stream().filter(HermesRuntimePortDescriptor::noop).count();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("runtimePortsReady", runtimePortsReady());
        values.put("skillPersistenceReady", skillPersistenceReady());
        values.put("learningAuditConfigured", learningAuditConfigured());
        values.put("learningAuditReady", learningAuditReady());
        values.put("configuredPortCount", configuredPortCount());
        values.put("readyPortCount", readyPortCount());
        values.put("noopPortCount", noopPortCount());
        values.put("attention", attention);
        values.put("capabilities", capabilities.toMetadata());
        values.put("assembly", assemblyReport.toMetadata());
        values.put("lifecycle", lifecycle.toMetadata());
        values.put("runtimePorts", runtimePorts.toMetadata());
        values.put("skillPersistencePreflight", skillPersistencePreflight.toMetadata());
        values.put("learningAudit", learningAuditMetadata());
        return Map.copyOf(values);
    }

    public Map<String, Object> learningAuditMetadata() {
        HermesRuntimePortDescriptor descriptor = learningAuditDescriptor();
        Map<String, Object> retentionStatus = objectMap(descriptor.metadata().get("retentionStatus"));
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("configured", learningAuditConfigured());
        values.put("ready", learningAuditReady());
        values.put("noop", descriptor.noop());
        values.put("status", descriptor.status());
        values.put("reason", descriptor.reason());
        values.put("adapterId", descriptor.adapterId());
        values.put("adapterType", descriptor.adapterType());
        values.put("retentionStatus", retentionStatus);
        values.put("retentionSeverity", text(retentionStatus.get("severity")));
        values.put("retentionPriority", intValue(retentionStatus.get("priority")));
        values.put("retentionRequiresAttention", booleanValue(retentionStatus.get("requiresAttention")));
        values.put("retentionAttention", strings(retentionStatus.get("attention")));
        values.put("retentionRecommendedActions", strings(retentionStatus.get("recommendedActions")));
        values.put("port", descriptor.toMetadata());
        return Map.copyOf(values);
    }

    private HermesRuntimePortDescriptor learningAuditDescriptor() {
        return runtimePorts.learningAuditPort().descriptor();
    }

    private static List<String> attentionMessages(
            HermesRuntimePorts runtimePorts,
            HermesLearnedSkillPersistencePreflightReport skillPersistencePreflight) {
        List<String> messages = new ArrayList<>();
        if (skillPersistencePreflight != null) {
            messages.addAll(skillPersistencePreflight.attention());
        }
        if (runtimePorts != null) {
            List<HermesRuntimePortDescriptor> descriptors = runtimePorts.descriptors();
            descriptors.stream()
                    .filter(descriptor -> !descriptor.ready())
                    .map(descriptor -> "Hermes runtime port unavailable: "
                            + descriptor.port()
                            + " ("
                            + descriptor.status()
                            + ")")
                    .forEach(messages::add);
            descriptors.stream()
                    .filter(descriptor -> HermesRuntimePortCatalog.LEARNING_AUDIT.equals(descriptor.port()))
                    .flatMap(descriptor -> learningAuditRetentionAttention(descriptor).stream())
                    .forEach(messages::add);
        }
        return HermesText.distinctOneLineList(messages);
    }

    private static List<String> learningAuditRetentionAttention(HermesRuntimePortDescriptor descriptor) {
        Map<String, Object> retentionStatus = descriptor == null
                ? Map.of()
                : objectMap(descriptor.metadata().get("retentionStatus"));
        if (!booleanValue(retentionStatus.get("requiresAttention"))) {
            return List.of();
        }
        List<String> attention = strings(retentionStatus.get("attention"));
        if (!attention.isEmpty()) {
            return attention;
        }
        String status = text(retentionStatus.get("status"));
        return status.isBlank()
                ? List.of("Hermes learning-audit retention requires attention.")
                : List.of("Hermes learning-audit retention requires attention: " + status);
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(String::valueOf)
                .map(HermesText::oneLine)
                .filter(text -> !text.isBlank())
                .toList();
    }

    private static String text(Object value) {
        return value == null ? "" : HermesText.oneLineOr(String.valueOf(value), "");
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> {
            if (key != null) {
                values.put(String.valueOf(key), mapValue);
            }
        });
        return Map.copyOf(values);
    }
}
