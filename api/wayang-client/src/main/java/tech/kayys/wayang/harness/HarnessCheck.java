package tech.kayys.wayang.harness;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;

public record HarnessCheck(
        String id,
        String label,
        List<String> command,
        String workingDirectory,
        boolean optional,
        String reason) {

    public HarnessCheck {
        id = SdkText.trimToDefault(id, "check");
        label = SdkText.trimToDefault(label, id);
        command = SdkLists.copy(command);
        workingDirectory = SdkText.trimToDefault(workingDirectory, ".");
        reason = SdkText.trimToEmpty(reason);
    }

    public String commandLine() {
        return String.join(" ", command);
    }
}
