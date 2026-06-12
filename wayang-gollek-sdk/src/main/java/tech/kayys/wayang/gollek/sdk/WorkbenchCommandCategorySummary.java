package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record WorkbenchCommandCategorySummary(
        String name,
        int count,
        List<String> commandIds) {

    public WorkbenchCommandCategorySummary {
        name = SdkText.trimToEmpty(name);
        count = Math.max(0, count);
        commandIds = SdkLists.copy(commandIds);
    }
}
