package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WorkbenchCommand;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandCategorySummary;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandContract;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandDiscovery;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandEnvelopes;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Text renderer for command discovery responses shown by the Wayang CLI.
 */
final class WayangCommandTextFormat {

    private WayangCommandTextFormat() {
    }

    static String text(WorkbenchCommandQuery query, List<WorkbenchCommand> commands) {
        WorkbenchCommandQuery normalized = query == null ? WorkbenchCommandQuery.all() : query;
        StringBuilder output = new StringBuilder("Wayang commands\n");
        appendQueryLines(output, normalized);
        for (Map.Entry<String, List<WorkbenchCommand>> entry : groupedByCategory(commands).entrySet()) {
            output.append('\n').append(entry.getKey()).append('\n');
            for (WorkbenchCommand command : entry.getValue()) {
                output.append("  - ")
                        .append(command.id())
                        .append(": ")
                        .append(command.command())
                        .append('\n');
                if (!command.description().isBlank()) {
                    output.append("    ").append(command.description()).append('\n');
                }
                CliText.appendIndentedListLine(output, "surfaces", command.surfaceIds());
                CliText.appendIndentedListLine(output, "contracts", contractLabels(command.contracts()));
                if (command.localOnly()) {
                    output.append("    local only\n");
                }
            }
        }
        return output.append('\n').toString();
    }

    static String indexText(String productName, WorkbenchCommandDiscovery discovery) {
        WorkbenchCommandDiscovery model = WorkbenchCommandEnvelopes.normalize(discovery);
        StringBuilder output = new StringBuilder("Wayang command index\n");
        output.append("product: ").append(productName).append('\n');
        appendQueryLines(output, model.query());
        output.append("totalCommands: ").append(model.totalCommands()).append('\n');
        output.append("matchingCommands: ").append(model.matchingCommands()).append('\n');

        output.append('\n').append("Categories").append('\n');
        for (WorkbenchCommandCategorySummary summary : model.categorySummaries()) {
            output.append("  - ")
                    .append(summary.name())
                    .append(" (")
                    .append(summary.count())
                    .append(")")
                    .append('\n');
            CliText.appendIndentedListLine(output, "ids", summary.commandIds());
        }

        output.append('\n').append("Command IDs").append('\n');
        for (String commandId : model.commandIds()) {
            output.append("  - ").append(commandId).append('\n');
        }
        return output.append('\n').toString();
    }

    private static void appendQueryLines(StringBuilder output, WorkbenchCommandQuery query) {
        if (query.surfaceId() != null) {
            output.append("surface: ").append(query.surfaceId()).append('\n');
        }
        if (query.profileId() != null) {
            output.append("profile: ").append(query.profileId()).append('\n');
        }
        String resolvedSurfaceId = query.resolvedSurfaceId();
        if (resolvedSurfaceId != null && !resolvedSurfaceId.equals(query.surfaceId())) {
            output.append("resolvedSurface: ").append(resolvedSurfaceId).append('\n');
        }
        if (query.category() != null) {
            output.append("category: ").append(query.category()).append('\n');
        }
        if (query.commandId() != null) {
            output.append("commandId: ").append(query.commandId()).append('\n');
        }
        if (query.contractJsonSchemaId() != null) {
            output.append("contractJsonSchemaId: ").append(query.contractJsonSchemaId()).append('\n');
        }
    }

    private static Map<String, List<WorkbenchCommand>> groupedByCategory(List<WorkbenchCommand> commands) {
        Map<String, List<WorkbenchCommand>> grouped = new LinkedHashMap<>();
        for (WorkbenchCommand command : commands == null ? List.<WorkbenchCommand>of() : commands) {
            grouped.computeIfAbsent(command.category(), ignored -> new ArrayList<>()).add(command);
        }
        return grouped;
    }

    private static List<String> contractLabels(List<WorkbenchCommandContract> contracts) {
        return contracts.stream()
                .map(contract -> contract.schema() + "/" + contract.envelope())
                .toList();
    }
}
