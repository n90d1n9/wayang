package tech.kayys.wayang.gollek.cli;

import java.io.PrintStream;
import java.util.function.Supplier;

final class WayangCliRender {

    private WayangCliRender() {
    }

    static void jsonOrText(
            PrintStream out,
            boolean json,
            Supplier<String> jsonOutput,
            Supplier<String> textOutput) {
        if (json) {
            out.println(jsonOutput.get());
        } else {
            out.print(textOutput.get());
        }
    }
}
