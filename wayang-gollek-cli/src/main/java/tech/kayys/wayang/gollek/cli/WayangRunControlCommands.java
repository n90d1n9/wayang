package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitResult;
import tech.kayys.wayang.gollek.sdk.WayangRunApi;

import java.util.concurrent.Callable;

final class WayangRunControlCommands {

    private WayangRunControlCommands() {
    }

    @Command(
            name = "wait",
            description = "Poll run status until the run reaches a terminal state.",
            mixinStandardHelpOptions = true)
    static final class WaitCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Parameters(index = "0", description = "Run id to wait for.")
        String runId;

        @Option(names = "--timeout-seconds", description = "Maximum seconds to wait. Use 0 for one status check.")
        Integer timeoutSeconds;

        @Option(names = "--poll-millis", description = "Milliseconds between status polls.")
        Integer pollMillis;

        @Option(names = "--json", description = "Render wait result as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                AgentRunWaitResult result = runs.waitFor(
                        runId,
                        AgentRunWaitOptions.of(timeoutSeconds, pollMillis));
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.waitJson(result),
                        () -> WayangRunControlTextFormat.waitText(result));
                return result.terminal() ? 0 : 1;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(
            name = "cancel",
            description = "Request cancellation for a non-terminal run.",
            mixinStandardHelpOptions = true)
    static final class CancelCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Parameters(index = "0", description = "Run id to cancel.")
        String runId;

        @Option(names = "--reason", description = "Reason to attach to the cancellation request.")
        String reason;

        @Option(names = "--json", description = "Render cancel result as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                AgentRunCancelResult result = runs.cancel(runId, reason);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.cancelJson(result),
                        () -> WayangRunControlTextFormat.cancelText(result));
                return result.cancelled() ? 0 : 1;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(
            name = "forget",
            description = "Forget a locally recorded run status snapshot.",
            mixinStandardHelpOptions = true)
    static final class ForgetCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Parameters(index = "0", description = "Run id to forget.")
        String runId;

        @Option(names = "--json", description = "Render forget result as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                AgentRunForgetResult result = runs.forget(runId);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.forgetJson(result),
                        () -> WayangRunControlTextFormat.forgetText(result));
                return result.forgotten() ? 0 : 1;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }
}
