package tech.kayys.wayang.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Domain model representing a contract item in the contract catalog.
 */
public class ContractItem {
    private final String id;
    private final String envelope;
    private final String description;
    private final List<String> commandIds;
    private final String contractType;
    private final String[] commands;

    public ContractItem(
            String id,
            String envelope,
            String description,
            List<String> commandIds,
            String contractType,
            String[] commands) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.envelope = envelope;
        this.description = description;
        this.commandIds = commandIds != null ? List.copyOf(commandIds) : List.of();
        this.contractType = contractType;
        this.commands = commands != null ? commands.clone() : new String[0];
    }

    public String getId() {
        return id;
    }

    public String getEnvelope() {
        return envelope;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCommandIds() {
        return commandIds;
    }

    public String getContractType() {
        return contractType;
    }

    public String[] getCommands() {
        return commands.clone();
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "envelope", envelope,
                "description", description,
                "commandIds", commandIds,
                "contractType", contractType,
                "commands", commands);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractItem that = (ContractItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ContractItem{" +
                "id='" + id + '\'' +
                ", envelope='" + envelope + '\'' +
                '}';
    }
}
