package tech.kayys.wayang.workbench;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;

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
