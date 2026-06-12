package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * Compact diagnostics-friendly summary of the A2A spec-alignment report.
 */
public record WayangA2aSpecAlignmentSnapshot(
        String protocol,
        String protocolVersion,
        String binding,
        boolean aligned,
        int requirementCount,
        int alignedCount,
        int gapCount,
        List<String> gapIds,
        List<WayangA2aSpecAlignmentCategorySummary> categorySummaries) {

    public WayangA2aSpecAlignmentSnapshot(
            String protocol,
            String protocolVersion,
            String binding,
            boolean aligned,
            int requirementCount,
            int alignedCount,
            int gapCount,
            List<String> gapIds) {
        this(protocol, protocolVersion, binding, aligned, requirementCount, alignedCount, gapCount, gapIds, List.of());
    }

    public WayangA2aSpecAlignmentSnapshot {
        protocol = protocol == null || protocol.isBlank() ? "a2a" : protocol.trim();
        protocolVersion = protocolVersion == null || protocolVersion.isBlank()
                ? A2aProtocol.VERSION
                : protocolVersion.trim();
        binding = binding == null || binding.isBlank() ? A2aProtocol.BINDING_JSONRPC : binding.trim();
        requirementCount = Math.max(0, requirementCount);
        alignedCount = Math.max(0, alignedCount);
        gapCount = Math.max(Math.max(0, gapCount), gapIds == null ? 0 : gapIds.size());
        gapIds = gapIds == null
                ? List.of()
                : gapIds.stream()
                        .map(WayangA2aMaps::optional)
                        .filter(Objects::nonNull)
                        .toList();
        categorySummaries = categorySummaries == null
                ? List.of()
                : categorySummaries.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public static WayangA2aSpecAlignmentSnapshot defaults() {
        return from(WayangA2aJsonRpcMethods.specAlignmentReport());
    }

    public static WayangA2aSpecAlignmentSnapshot from(WayangA2aSpecAlignmentReport report) {
        WayangA2aSpecAlignmentReport resolved = report == null
                ? WayangA2aSpecAlignmentReport.defaults()
                : report;
        return new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                resolved.aligned(),
                resolved.requirementCount(),
                resolved.alignedCount(),
                resolved.gapCount(),
                resolved.gapIds(),
                resolved.categorySummaries());
    }

    public static WayangA2aSpecAlignmentSnapshot fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aSpecAlignmentSnapshot(
                text(copy.get("protocol"), "a2a"),
                text(copy.get("protocolVersion"), A2aProtocol.VERSION),
                text(copy.get("binding"), A2aProtocol.BINDING_JSONRPC),
                bool(copy.get("aligned"), false),
                number(copy.get("requirementCount"), 0),
                number(copy.get("alignedCount"), 0),
                number(copy.get("gapCount"), 0),
                WayangA2aMaps.stringList(copy.get("gapIds")),
                WayangA2aMaps.objectList(copy.get("categorySummaries")).stream()
                        .map(WayangA2aSpecAlignmentCategorySummary::fromMap)
                        .toList());
    }

    public Optional<WayangA2aSpecAlignmentCategorySummary> categorySummary(String category) {
        return categorySummarySet().find(category);
    }

    public List<WayangA2aSpecAlignmentCategorySummary> gapCategorySummaries() {
        return categorySummarySet().gaps();
    }

    public List<String> gapCategories() {
        return categorySummarySet().gapCategories();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", protocol);
        values.put("protocolVersion", protocolVersion);
        values.put("binding", binding);
        values.put("standard", standardDescriptor());
        values.put("aligned", aligned);
        values.put("requirementCount", requirementCount);
        values.put("alignedCount", alignedCount);
        values.put("gapCount", gapCount);
        values.put("gapIds", gapIds);
        values.put("gapCategories", gapCategories());
        values.put("categorySummaries", categorySummarySet().maps());
        return WayangA2aMaps.copyMap(values);
    }

    private Map<String, Object> standardDescriptor() {
        return WayangA2aSpecAlignmentStandardDescriptor.from(protocolVersion, binding).toMap();
    }

    private WayangA2aSpecAlignmentCategorySummaries categorySummarySet() {
        return WayangA2aSpecAlignmentCategorySummaries.fromSummaries(categorySummaries);
    }
}
