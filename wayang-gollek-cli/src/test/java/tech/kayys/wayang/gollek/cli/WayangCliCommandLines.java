package tech.kayys.wayang.gollek.cli;

import java.util.List;
import java.util.stream.Collectors;

final class WayangCliCommandLines {

    private WayangCliCommandLines() {
    }

    static String render(List<String> args) {
        return args.stream()
                .map(WayangCliCommandLines::renderArg)
                .collect(Collectors.joining(" "));
    }

    private static String renderArg(String arg) {
        String value = arg == null ? "" : arg;
        if (value.matches("[A-Za-z0-9_@%+=:,./-]+")) {
            return value;
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
