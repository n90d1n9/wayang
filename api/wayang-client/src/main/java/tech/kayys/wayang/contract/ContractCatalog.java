package tech.kayys.wayang.contract;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import tech.kayys.wayang.catalog.AbstractCatalog;

/**
 * Unified catalog for Wayang contract implementations.
 * Extends AbstractCatalog to provide standardized contract discovery and management.
 */
public class ContractCatalog extends AbstractCatalog<ContractItem, String> {

    private static final String CATALOG_ID = "contract";

    public ContractCatalog() {
        super(CATALOG_ID);
    }

    @Override
    protected String getId(ContractItem item) {
        return item.getId();
    }

    @Override
    protected Map<String, Object> toIndexEntry(ContractItem item) {
        return item.toMap();
    }

    @Override
    protected Optional<ContractItem> fromIndexEntry(Map<String, Object> entry) {
        try {
            @SuppressWarnings("unchecked")
            String id = (String) entry.get("id");
            String envelope = (String) entry.get("envelope");
            String description = (String) entry.get("description");
            @SuppressWarnings("unchecked")
            List<String> commandIds = (List<String>) entry.get("commandIds");
            String contractType = (String) entry.get("contractType");
            @SuppressWarnings("unchecked")
            String[] commands = ((List<String>) entry.get("commands")).toArray(new String[0]);

            return Optional.of(new ContractItem(
                    id,
                    envelope,
                    description,
                    commandIds,
                    contractType,
                    commands));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Discovers contracts matching the given query.
     * This maintains API compatibility with existing code.
     *
     * @param query the contract query
     * @return discovery result
     */
    public WayangContractDiscovery discover(WayangContractQuery query) {
        WayangContractQuery normalized = query == null ? WayangContractQuery.all() : query;
        List<ContractItem> allItems = findAll();

        List<ContractItem> filtered = allItems.stream()
                .filter(item -> matchesQuery(item, normalized))
                .toList();

        return WayangContractDiscovery.of(normalized, filtered, allItems.size());
    }

    /**
     * Checks if a contract item matches the query criteria.
     */
    private boolean matchesQuery(ContractItem item, WayangContractQuery query) {
        // Basic matching logic - can be enhanced based on WayangContractQuery structure
        if (query == null) {
            return true;
        }
        // TODO: Implement actual query matching based on query parameters
        return true;
    }
}
