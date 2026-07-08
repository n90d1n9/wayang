package tech.kayys.wayang.tui.ui;

import java.util.List;

public interface ProviderManager {
    record ProviderRow(String id, String name, String version, String status, String defaultModel) {}
    
    List<ProviderRow> listProviders();
}
