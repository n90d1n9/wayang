package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Option;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;

final class WayangCommandQueryOptions {

    @Option(names = "--surface", description = "Filter command recommendations to a product surface.")
    String surfaceId;

    @Option(names = "--profile", description = "Filter command recommendations through a product profile.")
    String profileId;

    @Option(names = "--category", description = "Filter command recommendations to one category.")
    String category;

    @Option(names = "--id", description = "Show one command by stable command id.")
    String commandId;

    @Option(names = "--contract-json-schema-id", description = "Filter commands by output contract JSON Schema id.")
    String contractJsonSchemaId;

    WorkbenchCommandQuery toQuery() {
        return WorkbenchCommandQuery.of(
                surfaceId,
                profileId,
                category,
                commandId,
                contractJsonSchemaId);
    }
}
