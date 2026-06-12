package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Option;
import tech.kayys.wayang.gollek.sdk.WayangContractQuery;

final class WayangContractQueryOptions {

    @Option(names = "--schema", description = "Filter by schema, for example wayang.run.lifecycle.")
    String schema;

    @Option(names = "--envelope", description = "Filter by envelope, for example run-preview.")
    String envelope;

    @Option(names = "--command-id", description = "Filter by command id, for example run-dry-json.")
    String commandId;

    @Option(names = "--domain", description = "Filter by contract domain, for example lifecycle.")
    String domain;

    @Option(names = "--json-schema-id", description = "Filter by JSON Schema id, for example urn:wayang:contract:wayang.run.planning:v1:run-preview.")
    String jsonSchemaId;

    WayangContractQuery toQuery() {
        return WayangContractQuery.of(schema, envelope, commandId, domain, jsonSchemaId);
    }
}
