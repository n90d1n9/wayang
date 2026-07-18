package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.Wayang;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliContextTest {

    @Test
    void contextDoesNotResolveClientUntilClientIsRequested() {
        AtomicInteger calls = new AtomicInteger();

        WayangCliContext context = new WayangCliContext(
                () -> {
                    calls.incrementAndGet();
                    return Wayang.client();
                },
                ByteArrayInputStream.nullInputStream(),
                stream(new ByteArrayOutputStream()),
                stream(new ByteArrayOutputStream()));

        assertThat(calls).hasValue(0);
        assertThat(context.in()).isNotNull();
        assertThat(calls).hasValue(0);
    }

    @Test
    void contextResolvesClientLazilyAndCachesIt() {
        WayangGollekSdk sdk = WayangGollekSdk.local();
        WayangClient client = Wayang.client(sdk);
        AtomicInteger calls = new AtomicInteger();

        WayangCliContext context = new WayangCliContext(
                () -> {
                    calls.incrementAndGet();
                    return client;
                },
                ByteArrayInputStream.nullInputStream(),
                stream(new ByteArrayOutputStream()),
                stream(new ByteArrayOutputStream()));

        assertThat(calls).hasValue(0);
        assertThat(context.client()).isSameAs(client);
        assertThat(context.client()).isSameAs(context.client());
        assertThat(calls).hasValue(1);
    }

    @Test
    void contextCarriesCliStreams() {
        ByteArrayInputStream in = new ByteArrayInputStream("prompt".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        WayangCliContext context = new WayangCliContext(
                Wayang::client,
                in,
                stream(out),
                stream(err));

        assertThat(context.in()).isSameAs(in);
        assertThat(context.out()).isNotNull();
        assertThat(context.err()).isNotNull();
    }

    @Test
    void contextRendersStandardCommandFailure() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        WayangCliContext context = new WayangCliContext(
                Wayang::client,
                ByteArrayInputStream.nullInputStream(),
                stream(new ByteArrayOutputStream()),
                stream(err));

        int exitCode = context.commandFailure(new IllegalArgumentException("bad input"));

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString(StandardCharsets.UTF_8))
                .isEqualTo("Wayang command failed: bad input" + System.lineSeparator());
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
