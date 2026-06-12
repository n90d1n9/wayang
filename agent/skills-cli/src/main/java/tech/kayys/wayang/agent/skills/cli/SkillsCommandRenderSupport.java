package tech.kayys.wayang.agent.skills.cli;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class SkillsCommandRenderSupport {

    private final PrintStream out;
    private final PrintStream err;

    SkillsCommandRenderSupport(PrintStream out, PrintStream err) {
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    <T> int render(
            T report,
            boolean json,
            Function<T, String> jsonRenderer,
            BiConsumer<T, PrintStream> textRenderer) {
        return render(report, json, jsonRenderer, textRenderer, ignored -> true);
    }

    <T> int render(
            T report,
            boolean json,
            Function<T, String> jsonRenderer,
            BiConsumer<T, PrintStream> textRenderer,
            Predicate<T> success) {
        T resolvedReport = Objects.requireNonNull(report, "report");
        Objects.requireNonNull(jsonRenderer, "jsonRenderer");
        Objects.requireNonNull(textRenderer, "textRenderer");
        Predicate<T> resolvedSuccess = Objects.requireNonNull(success, "success");
        if (json) {
            out.println(jsonRenderer.apply(resolvedReport));
        } else {
            textRenderer.accept(resolvedReport, out);
        }
        return resolvedSuccess.test(resolvedReport) ? 0 : 1;
    }

    <T> int renderSafely(
            Supplier<T> reportSupplier,
            boolean json,
            Function<T, String> jsonRenderer,
            BiConsumer<T, PrintStream> textRenderer) {
        return renderSafely(reportSupplier, json, jsonRenderer, textRenderer, ignored -> true);
    }

    <T> int renderSafely(
            Supplier<T> reportSupplier,
            boolean json,
            Function<T, String> jsonRenderer,
            BiConsumer<T, PrintStream> textRenderer,
            Predicate<T> success) {
        try {
            return render(
                    Objects.requireNonNull(reportSupplier, "reportSupplier").get(),
                    json,
                    jsonRenderer,
                    textRenderer,
                    success);
        } catch (IllegalArgumentException error) {
            err.println(error.getMessage());
            return 1;
        }
    }
}
