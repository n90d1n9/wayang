package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-store-backed approval store for cloud and S3-compatible storage.
 */
public final class ObjectStorageHermesSkillLineageRepairApprovalStore
        implements HermesSkillLineageRepairApprovalStore {

    public static final String DEFAULT_PREFIX = "hermes/repair-approvals";

    private final ObjectStorageService objectStorageService;
    private final String prefix;

    public ObjectStorageHermesSkillLineageRepairApprovalStore(ObjectStorageService objectStorageService) {
        this(objectStorageService, DEFAULT_PREFIX);
    }

    public ObjectStorageHermesSkillLineageRepairApprovalStore(
            ObjectStorageService objectStorageService,
            String prefix) {
        this.objectStorageService = Objects.requireNonNull(objectStorageService, "objectStorageService");
        this.prefix = HermesObjectStorageLayout.normalizePrefix(prefix, DEFAULT_PREFIX);
    }

    public String prefix() {
        return prefix;
    }

    @Override
    public synchronized Optional<HermesSkillLineageRepairApproval> find(String approvalId) {
        String id = approvalId == null ? "" : HermesText.oneLine(approvalId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        List<HermesSkillLineageRepairApproval> approvals = approvals();
        for (int index = approvals.size() - 1; index >= 0; index--) {
            HermesSkillLineageRepairApproval approval = approvals.get(index);
            if (id.equals(approval.approvalId())) {
                return Optional.of(approval);
            }
        }
        return Optional.empty();
    }

    public synchronized List<HermesSkillLineageRepairApproval> approvals() {
        return approvalKeys().stream()
                .map(this::readApprovals)
                .flatMap(List::stream)
                .toList();
    }

    public synchronized int approvalCount() {
        return approvals().size();
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("storeType", "object-storage");
        metadata.put("configured", true);
        metadata.put("approvalPrefix", prefix);
        metadata.put("approvalCount", approvalCount());
        return Map.copyOf(metadata);
    }

    private List<String> approvalKeys() {
        return HermesObjectStorageLayout.listKeys(objectStorageService, prefix, ".jsonl", ".json");
    }

    private List<HermesSkillLineageRepairApproval> readApprovals(String key) {
        Optional<byte[]> content = HermesObjectStorageLayout.read(objectStorageService, key);
        if (content.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(new String(content.orElseThrow(), StandardCharsets.UTF_8).split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(HermesRuntimeEventJsonCodec::objectFromJsonLine)
                .map(HermesSkillLineageRepairApproval::fromMetadata)
                .filter(approval -> !approval.approvalId().isBlank())
                .toList();
    }
}
