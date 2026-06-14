package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

/**
 * Plain-text renderer for local workspace inspection snapshots.
 */
final class WayangWorkspaceTextFormat {

    private static final String NL = System.lineSeparator();

    private WayangWorkspaceTextFormat() {
    }

    static String text(WorkspaceSnapshot snapshot) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang workspace").append(NL);
        output.append("root: ").append(snapshot.rootPath()).append(NL);
        output.append("exists: ").append(snapshot.exists()).append(NL);
        output.append("directory: ").append(snapshot.directory()).append(NL);
        output.append("git: ").append(CliText.yesNo(snapshot.gitRepository()));
        if (!snapshot.branch().isBlank()) {
            output.append(" [").append(snapshot.branch()).append("]");
        }
        output.append(NL);
        appendInline(output, "build", snapshot.buildFiles());
        appendInline(output, "package managers", snapshot.packageManagers());
        CliText.appendBulletBlock(output, "modules", snapshot.modules());
        CliText.appendBulletBlock(output, "paths", snapshot.importantPaths());
        CliText.appendBulletBlock(output, "notes", snapshot.notes());
        return output.toString();
    }

    private static void appendInline(StringBuilder output, String label, Iterable<String> values) {
        StringBuilder line = new StringBuilder();
        for (String value : values) {
            if (!line.isEmpty()) {
                line.append(", ");
            }
            line.append(value);
        }
        output.append(label).append(": ").append(line.isEmpty() ? "none" : line).append(NL);
    }
}
