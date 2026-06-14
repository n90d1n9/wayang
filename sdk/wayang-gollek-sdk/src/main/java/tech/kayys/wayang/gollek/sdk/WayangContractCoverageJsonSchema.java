package tech.kayys.wayang.gollek.sdk;

import java.util.List;

final class WayangContractCoverageJsonSchema {

    private WayangContractCoverageJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return WayangContractCoverageContract.SCHEMA.equals(contract.schema())
                && WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE.equals(contract.envelope());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                "Wayang contract command coverage envelope",
                required(),
                WayangContractCoverageJsonSchemaProperties.properties());
    }

    private static List<String> required() {
        return List.of(
                "product",
                "totalContracts",
                "totalCommands",
                "commandLinkedContracts",
                "commandlessContracts",
                "incompleteContracts",
                "commandContractLinks",
                "commandlessEntries",
                "incompleteEntries");
    }
}
