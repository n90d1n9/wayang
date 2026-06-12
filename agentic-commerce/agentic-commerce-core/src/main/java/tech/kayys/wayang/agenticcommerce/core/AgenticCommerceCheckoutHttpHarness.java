package tech.kayys.wayang.agenticcommerce.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free checkout HTTP scenario runner.
 */
public final class AgenticCommerceCheckoutHttpHarness {

    private final AgenticCommerceRequestValidator requestValidator;
    private final AgenticCommerceResponseValidator responseValidator;

    public AgenticCommerceCheckoutHttpHarness() {
        this(AgenticCommerceRequestValidator.checkout(), AgenticCommerceResponseValidator.checkout());
    }

    public AgenticCommerceCheckoutHttpHarness(
            AgenticCommerceRequestValidator requestValidator,
            AgenticCommerceResponseValidator responseValidator) {
        this.requestValidator = requestValidator == null
                ? AgenticCommerceRequestValidator.checkout()
                : requestValidator;
        this.responseValidator = responseValidator == null
                ? AgenticCommerceResponseValidator.checkout()
                : responseValidator;
    }

    public static AgenticCommerceCheckoutHttpHarness checkout() {
        return new AgenticCommerceCheckoutHttpHarness();
    }

    public AgenticCommerceCheckoutHttpScenarioResult run(
            AgenticCommerceCheckoutHttpScenario scenario,
            AgenticCommerceCheckoutHttpResponder responder) {
        AgenticCommerceCheckoutHttpScenario resolvedScenario = Objects.requireNonNull(scenario, "scenario");
        AgenticCommerceCheckoutHttpResponder resolvedResponder = Objects.requireNonNull(responder, "responder");
        List<AgenticCommerceCheckoutHttpExchange> exchanges = new ArrayList<>();
        for (AgenticCommerceCheckoutHttpScenarioStep step : resolvedScenario.steps()) {
            exchanges.add(runStep(step, resolvedResponder));
        }
        return new AgenticCommerceCheckoutHttpScenarioResult(
                resolvedScenario,
                exchanges,
                List.of(),
                Map.of(
                        "protocol",
                        "agentic-commerce",
                        "specVersion",
                        AgenticCommerceProtocol.SPEC_VERSION));
    }

    private AgenticCommerceCheckoutHttpExchange runStep(
            AgenticCommerceCheckoutHttpScenarioStep step,
            AgenticCommerceCheckoutHttpResponder responder) {
        AgenticCommerceValidationReport requestReport = requestValidator.validate(step.request());
        AgenticCommerceHttpResponse response;
        String transportError = "";
        try {
            response = responder.respond(step.request());
            if (response == null) {
                throw new IllegalStateException("Agentic Commerce responder returned null response");
            }
        } catch (RuntimeException exception) {
            transportError = exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage();
            response = new AgenticCommerceHttpResponse(
                    599,
                    "",
                    Map.of(),
                    Map.of("transportError", transportError));
        }
        AgenticCommerceCheckoutHttpResult result = AgenticCommerceCheckoutHttpResponses.decode(
                step.request(),
                response,
                responseValidator);
        List<AgenticCommerceValidationIssue> issues = issues(step, requestReport, result, transportError);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestValid", requestReport.valid());
        metadata.put("responseValid", result.validation().valid());
        metadata.put("bodyDecoded", !result.body().isEmpty());
        metadata.put("transportErrorPresent", !transportError.isBlank());
        return new AgenticCommerceCheckoutHttpExchange(
                step,
                requestReport,
                result,
                transportError,
                issues,
                metadata);
    }

    private static List<AgenticCommerceValidationIssue> issues(
            AgenticCommerceCheckoutHttpScenarioStep step,
            AgenticCommerceValidationReport requestReport,
            AgenticCommerceCheckoutHttpResult result,
            String transportError) {
        List<AgenticCommerceValidationIssue> issues = new ArrayList<>();
        issues.addAll(requestReport.issues());
        issues.addAll(result.issues());
        if (!transportError.isBlank()) {
            issues.add(AgenticCommerceValidationIssue.of(
                    "transport_error",
                    "transport",
                    "Checkout HTTP scenario step failed before a response was returned.",
                    "HTTP response",
                    transportError));
        }
        if (result.response().statusCode() != step.expectedStatusCode()) {
            issues.add(AgenticCommerceValidationIssue.of(
                    "unexpected_step_status",
                    "statusCode",
                    "Checkout HTTP scenario response status did not match the step expectation.",
                    String.valueOf(step.expectedStatusCode()),
                    String.valueOf(result.response().statusCode())));
        }
        if (result.successful() != step.expectedSuccessful()) {
            issues.add(AgenticCommerceValidationIssue.of(
                    "unexpected_step_success",
                    "successful",
                    "Checkout HTTP scenario success state did not match the step expectation.",
                    String.valueOf(step.expectedSuccessful()),
                    String.valueOf(result.successful())));
        }
        return List.copyOf(issues);
    }
}
