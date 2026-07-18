package tech.kayys.wayang.alignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

/**
 * Provenance for a standard-alignment summary or carrier report.
 */
public record WayangStandardAlignmentSource(
        String sourceType,
        String sourceId,
        String reportId,
        Map<String, Object> attributes) {

    public WayangStandardAlignmentSource {
        sourceType = SdkText.trimToEmpty(sourceType);
        sourceId = SdkText.trimToEmpty(sourceId);
        reportId = SdkText.trimToEmpty(reportId);
        attributes = WayangStandardAlignmentMaps.copy(attributes);
    }

    static List<WayangStandardAlignmentSource> fromReportMap(Map<?, ?> report) {
        Map<String, Object> copied = WayangStandardAlignmentMaps.copy(report);
        List<WayangStandardAlignmentSource> sources = new ArrayList<>();
        Object existingSources = copied.get("sources");
        if (existingSources instanceof List<?> list) {
            list.stream()
                    .map(WayangStandardAlignmentMaps::map)
                    .map(WayangStandardAlignmentSource::fromSourceMap)
                    .flatMap(Optional::stream)
                    .forEach(sources::add);
        }
        fromSourceMap(WayangStandardAlignmentMaps.map(copied.get("source"))).ifPresent(sources::add);
        fromCarrierMap(copied).ifPresent(sources::add);
        return sources.stream()
                .collect(LinkedHashMap<String, WayangStandardAlignmentSource>::new,
                        (values, source) -> values.putIfAbsent(source.key(), source),
                        LinkedHashMap::putAll)
                .values()
                .stream()
                .toList();
    }

    public boolean empty() {
        return sourceType.isEmpty() && sourceId.isEmpty() && reportId.isEmpty() && attributes.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceType", sourceType);
        values.put("sourceId", sourceId);
        values.put("reportId", reportId);
        values.put("attributes", attributes);
        return SdkMaps.orderedCopy(values);
    }

    private static Optional<WayangStandardAlignmentSource> fromSourceMap(Map<String, Object> source) {
        if (source.isEmpty()) {
            return Optional.empty();
        }
        WayangStandardAlignmentSource resolved = new WayangStandardAlignmentSource(
                WayangStandardAlignmentMaps.firstText(source, "sourceType", "type", "kind"),
                WayangStandardAlignmentMaps.firstText(source, "sourceId", "id"),
                WayangStandardAlignmentMaps.firstText(source, "reportId"),
                WayangStandardAlignmentMaps.map(source.get("attributes")));
        return resolved.empty() ? Optional.empty() : Optional.of(resolved);
    }

    private static Optional<WayangStandardAlignmentSource> fromCarrierMap(Map<String, Object> carrier) {
        if (carrier.isEmpty()) {
            return Optional.empty();
        }
        String reportId = WayangStandardAlignmentMaps.firstText(
                carrier,
                "reportId",
                "diagnosticsId",
                "specComplianceId",
                "complianceId",
                "portfolioId");
        String sourceId = WayangStandardAlignmentMaps.firstText(
                carrier,
                "sourceId",
                "diagnosticsId",
                "specComplianceId",
                "complianceId",
                "portfolioId");
        String sourceType = WayangStandardAlignmentMaps.firstText(carrier, "sourceType", "reportType", "kind", "type");
        Map<String, Object> metadata = WayangStandardAlignmentMaps.map(carrier.get("metadata"));
        if (sourceType.isEmpty()) {
            sourceType = inferSourceType(carrier, metadata);
        }
        if (sourceId.isEmpty()) {
            sourceId = WayangStandardAlignmentMaps.firstText(metadata, "source", "sourceId");
        }
        WayangStandardAlignmentSource resolved = new WayangStandardAlignmentSource(
                sourceType,
                sourceId,
                reportId,
                metadata);
        return resolved.empty() ? Optional.empty() : Optional.of(resolved);
    }

    private static String inferSourceType(Map<String, Object> carrier, Map<String, Object> metadata) {
        if (carrier.containsKey("diagnosticsId")) {
            return "diagnostics";
        }
        if (carrier.containsKey("specComplianceId") || carrier.containsKey("complianceId")) {
            return "specCompliance";
        }
        if (carrier.containsKey("portfolioId")) {
            return "portfolio";
        }
        if (carrier.containsKey("reportId")) {
            return "report";
        }
        return WayangStandardAlignmentMaps.firstText(metadata, "source", "sourceType");
    }

    private String key() {
        return sourceType + "|" + sourceId + "|" + reportId + "|" + attributes;
    }
}
