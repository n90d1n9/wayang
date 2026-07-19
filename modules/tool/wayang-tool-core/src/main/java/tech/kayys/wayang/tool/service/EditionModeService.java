package tech.kayys.wayang.tool.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EditionModeService {

    @ConfigProperty(name = "wayang.edition", defaultValue = "community")
    String edition;

    @ConfigProperty(name = "wayang.enterprise.enabled", defaultValue = "false")
    boolean enterpriseEnabled;

    @ConfigProperty(name = "wayang.multitenancy.enabled", defaultValue = "false")
    boolean multitenancyEnabled;

    public boolean isEnterpriseMode() {
        return enterpriseEnabled
                || multitenancyEnabled
                || "enterprise".equalsIgnoreCase(edition);
    }

    public boolean supportsMcpRegistryDatabase() {
        return isEnterpriseMode();
    }

    public void assertMcpRegistryDatabaseEnabled() {
        if (!supportsMcpRegistryDatabase()) {
            throw new ForbiddenException("MCP registry database mode is enterprise-only. Use file-based MCP config in community mode.");
        }
    }
}
