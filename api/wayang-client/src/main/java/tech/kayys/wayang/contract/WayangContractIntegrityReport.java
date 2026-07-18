package tech.kayys.wayang.contract;

import java.util.List;

public record WayangContractIntegrityReport(
        int totalContracts,
        int totalCommands,
        int contractCommandLinks,
        int commandContractLinks,
        List<WayangContractIntegrityIssue> issues) {

    public WayangContractIntegrityReport {
        totalContracts = Math.max(0, totalContracts);
        totalCommands = Math.max(0, totalCommands);
        contractCommandLinks = Math.max(0, contractCommandLinks);
        commandContractLinks = Math.max(0, commandContractLinks);
        issues = SdkLists.copy(issues);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }
}
