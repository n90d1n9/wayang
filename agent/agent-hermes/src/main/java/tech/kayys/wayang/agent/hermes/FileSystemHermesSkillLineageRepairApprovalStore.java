package tech.kayys.wayang.agent.hermes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JSONL file-backed approval store for repair mutations.
 */
public final class FileSystemHermesSkillLineageRepairApprovalStore
        implements HermesSkillLineageRepairApprovalStore {

    public static final String DEFAULT_PATH = "var/hermes/repair-approvals.jsonl";

    private final Path approvalsPath;

    public FileSystemHermesSkillLineageRepairApprovalStore(Path approvalsPath) {
        this.approvalsPath = Objects.requireNonNull(approvalsPath, "approvalsPath");
    }

    public Path approvalsPath() {
        return approvalsPath;
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
        if (!Files.exists(approvalsPath)) {
            return List.of();
        }
        try {
            return Files.readAllLines(approvalsPath, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(HermesRuntimeEventJsonCodec::objectFromJsonLine)
                    .map(HermesSkillLineageRepairApproval::fromMetadata)
                    .filter(approval -> !approval.approvalId().isBlank())
                    .toList();
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read Hermes repair approval store", error);
        }
    }

    public synchronized int approvalCount() {
        return approvals().size();
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("storeType", "file-system");
        metadata.put("configured", true);
        metadata.put("approvalPath", approvalsPath.toString());
        metadata.put("approvalCount", approvalCount());
        return Map.copyOf(metadata);
    }
}
