package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreCompactionPreview;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreCompactionResult;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreDiagnostics;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreVerification;
import tech.kayys.wayang.gollek.sdk.AgentRunStoreVerificationPolicy;
import tech.kayys.wayang.gollek.sdk.WayangRunApi;

import java.util.concurrent.Callable;

/**
 * CLI command module for inspecting the configured run-store backend.
 */
final class WayangRunStoreCommands {

    private WayangRunStoreCommands() {
    }

    @Command(
            name = "store",
            description = "Inspect configured run-store diagnostics.",
            mixinStandardHelpOptions = true)
    static final class StoreCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Option(names = "--json", description = "Render run-store diagnostics as compact JSON.")
        boolean json;

        @Option(names = "--verify", description = "Verify the run-store snapshot without mutating it.")
        boolean verify;

        @Option(names = "--compact", description = "Preview or run run-store retention compaction.")
        boolean compact;

        @Option(names = "--dry-run", description = "Preview compaction without mutating the run store.")
        boolean dryRun;

        @Option(names = "--apply", description = "Apply compaction to the run store.")
        boolean apply;

        @Option(names = "--strict", description = "Fail verification when warning-level issues are present.")
        boolean strict;

        @Option(
                names = "--verification-policy",
                description = "Verification policy: lenient or strict. Env: WAYANG_RUN_STORE_VERIFICATION_POLICY.")
        String verificationPolicy;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                if (compact) {
                    if (dryRun && apply) {
                        throw new IllegalArgumentException(
                                "Use only one of --dry-run or --apply with --compact.");
                    }
                    if (!dryRun && !apply) {
                        throw new IllegalArgumentException(
                                "Run-store compaction requires --dry-run or --apply.");
                    }
                    if (apply) {
                        AgentRunStoreCompactionResult result = runs.compact();
                        WayangCliRender.jsonOrText(
                                context.out(),
                                json,
                                () -> runs.compactionResultJson(result),
                                () -> WayangRunStoreTextFormat.compactionResult(result));
                        return result.exitCode();
                    }
                    AgentRunStoreCompactionPreview preview = runs.compactionPreview();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> runs.compactionPreviewJson(preview),
                            () -> WayangRunStoreTextFormat.compactionPreview(preview));
                    return preview.exitCode();
                }
                if (dryRun || apply) {
                    throw new IllegalArgumentException("Use --compact with --dry-run or --apply.");
                }
                if (verify) {
                    AgentRunStoreVerification verification = runs.verification();
                    AgentRunStoreVerificationPolicy policy = policy();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> runs.verificationJson(verification, policy),
                            () -> WayangRunStoreTextFormat.verification(verification, policy));
                    return verification.exitCode(policy);
                }
                AgentRunStoreDiagnostics diagnostics = runs.diagnostics();
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.diagnosticsJson(diagnostics),
                        () -> WayangRunStoreTextFormat.text(diagnostics));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        private AgentRunStoreVerificationPolicy policy() {
            if (strict) {
                return AgentRunStoreVerificationPolicy.strict();
            }
            String mode = CliText.trimToEmpty(verificationPolicy);
            if (mode.isEmpty()) {
                mode = CliText.trimToEmpty(System.getenv("WAYANG_RUN_STORE_VERIFICATION_POLICY"));
            }
            return AgentRunStoreVerificationPolicy.fromMode(mode);
        }
    }
}
