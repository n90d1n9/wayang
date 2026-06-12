package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangClient;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Runtime context shared by CLI command modules.
 *
 * <p>The context owns streams and lazy client resolution only; domain services
 * are reached through {@link WayangClient} facades so the CLI remains a wrapper
 * around the SDK boundary.</p>
 */
final class WayangCliContext {

    private final Supplier<WayangClient> clients;
    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private WayangClient client;

    WayangCliContext(
            Supplier<WayangClient> clients,
            InputStream in,
            PrintStream out,
            PrintStream err) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.in = in == null ? InputStream.nullInputStream() : in;
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    WayangClient client() {
        if (client == null) {
            client = Objects.requireNonNull(clients.get(), "client");
        }
        return client;
    }

    InputStream in() {
        return in;
    }

    PrintStream out() {
        return out;
    }

    PrintStream err() {
        return err;
    }

    int commandFailure(RuntimeException e) {
        err.println("Wayang command failed: " + e.getMessage());
        return 2;
    }
}
