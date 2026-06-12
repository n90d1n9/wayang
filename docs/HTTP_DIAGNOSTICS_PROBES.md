# HTTP Diagnostics Probes

Wayang Gollek now has dependency-free HTTP-shaped diagnostics for the A2UI and
A2A adapter surfaces. These probes are intended for CLI commands, REST bridges,
CI jobs, runtime readiness checks, and framework adapters that need one stable
decision object instead of parsing nested transport envelopes directly.

## Scope

Use this guide when you need to expose or validate:

- A2UI HTTP route discovery, binding reports, smoke checks, and readiness checks.
- A2A JSON-RPC endpoint, smoke, route-catalog, diagnostics-report,
  spec-compliance-report, binding-report, and readiness checks.
- Golden JSON contract fixtures for probe payloads.
- Focused Maven test slices for adapter diagnostics.

The diagnostic classes live in the `a2ui/a2ui-wayang` and `a2a/a2a-wayang`
modules. They deliberately stay framework-neutral, so Quarkus, CLI, gateway,
and harness code can delegate to the same adapter/probe boundary.

## A2UI HTTP Surface

The A2UI HTTP bridge uses a route catalog. The default catalog exposes:

| Operation | Method | Path | Purpose |
|-----------|--------|------|---------|
| `a2ui.exchange` | `POST` | `/a2ui/exchange` | Submit one A2UI transport exchange. |
| `a2ui.surfaceCatalog` | `GET` | `/a2ui/surface-catalog` | Inspect available A2UI surfaces. |
| `a2ui.routeCatalog` | `GET` | `/a2ui/route-catalog` | Inspect exposed HTTP routes. |
| `a2ui.bindingReport` | `GET` | `/a2ui/binding-report` | Compare route declarations with registered handlers. |
| `a2ui.smoke` | `GET` | `/a2ui/smoke` | Run the built-in A2UI HTTP smoke suite. |
| `a2ui.readiness` | `GET` | `/a2ui/readiness` | Compose binding-report, action-binding, and smoke probes. |

Every known route also supports `OPTIONS` and returns route metadata headers
such as `Allow` and `X-Wayang-A2UI-Route-Operation`.

Framework adapters can mount the same route catalog at another root without
rewriting handlers:

```java
WayangA2uiHttpRouteCatalog catalog = WayangA2uiHttpRouteCatalog.defaultCatalog()
        .mountedAt("/api/a2ui");
WayangA2uiHttpBridgeAdapter adapter = new WayangA2uiHttpBridgeAdapter(
        bridge,
        catalog,
        WayangA2uiHttpRouteGuard.strict());
```

The mounted catalog reports mounted paths in route-catalog responses, binding
reports, smoke probes, and readiness probes. Blank roots keep the canonical
`/a2ui` surface; an explicit `/` root exposes paths such as `/exchange`.

For framework entry points that receive raw HTTP values, use the endpoint
binding helper:

```java
WayangA2uiHttpEndpointBinding endpoint =
        WayangA2uiHttpEndpointBinding.from(transportAdapter, "/api/a2ui");

WayangA2uiHttpResponse response = endpoint.handle(
        method,
        rawPath,
        body,
        headers,
        attributes);
```

The binding strips query strings and fragments before route matching, preserves
request attributes, and normalizes common multi-value header shapes such as
`Map<String, List<String>>` or header arrays.

Use `endpoint.project(...)` when a framework needs to inspect an inbound request
before dispatch. The request projection reports normalized method/path/body,
headers, attributes, whether the path is known, whether the route binding
matches, and any mounted route operation/`Allow` metadata.

Use `endpoint.respond(...)` when a framework needs an output projection with
status, body, content type, and `Map<String, List<String>>` response headers.
`WayangA2uiHttpEndpointResponse.from(response)` provides the same projection for
responses returned by lower-level bridge calls.

Use `endpoint.exchange(...)` when diagnostics need both projections for one raw
mounted HTTP call. `WayangA2uiHttpEndpointExchange` includes the request,
response, status, outcome, transport-error state, and decoded response envelope
in one JSON-ready projection.

Use `WayangA2uiHttpEndpointDiagnostics.of(endpoint)` when a framework needs a
batch diagnostic run over raw mounted endpoint calls. `run(...)` accepts
`WayangA2uiHttpEndpointDiagnosticRequest` values, while `runDefault()` probes
route catalog, binding report, smoke/readiness, and OPTIONS for each published
route. The resulting report includes known-path, matched-route, status bucket,
transport outcome, issue, and decoded envelope summaries.

Pass `WayangA2uiHttpEndpointDiagnosticConfig` to tune default probes for each
runtime. The config can disable smoke/readiness or OPTIONS probes, keep only
discovery probes, and apply shared default headers/attributes such as tenant,
trace, or gateway context to every generated diagnostic request.
`WayangA2uiHttpEndpointDiagnosticConfig.fromMap(...)` accepts property-style
maps with `profile`, nested `probes`, `defaultHeaders`/`headers`, and
`defaultAttributes`/`attributes`, so framework config binders can create the
same runtime config without bespoke adapter parsing.
`WayangA2uiHttpEndpointDiagnosticRequest.fromMap(...)` and `fromJson(...)`
decode raw request definitions with `method`, `rawPath`/`path`, `body`,
`headers`, and `attributes`; `WayangA2uiHttpEndpointDiagnostics.runFromMaps(...)`
executes lists of those maps through the same endpoint projection/report path.
Use `WayangA2uiHttpEndpointDiagnosticPlan.fromMap(...)` or `fromJson(...)` when
a caller needs to submit one payload containing diagnostics id, config,
attributes, and optional requests. Empty request lists use the configured
default probes; `run(plan)`, `runPlanMap(...)`, and `runPlanJson(...)` execute
the payload through the same report pipeline.
Persisted or remote reports can be decoded through
`WayangA2uiHttpEndpointDiagnosticReport.fromMap(...)` and `fromJson(...)`,
which normalize counts, status-code lists, outcome strings, exchange rows,
issues, and attributes before re-emitting canonical report JSON.
Use `WayangA2uiHttpEndpointDiagnosticSummary.from(report)`, `report.summary()`,
or `result.summary()` when an adapter needs only pass/fail, exit code, core
counts, status/outcome lists, distinct issue categories, distinct error codes,
and attributes.
Use `WayangA2uiHttpEndpointDiagnosticRunner` as the adapter-facing facade when
REST, CLI, gateway, or harness code needs to run default probes, plan maps,
plan JSON, or request maps and emit canonical report/summary JSON from one
small boundary. `WayangA2uiHttpEndpointDiagnosticRun` carries the low-level
result plus report, summary, report JSON, summary JSON, and a combined run
envelope.
Endpoint diagnostic issues are shaped by
`WayangA2uiHttpEndpointDiagnosticIssue`, keeping unknown-path,
route-mismatch, transport-error, and HTTP-status issue policy reusable outside
the aggregate report.
`WayangA2uiHttpEndpointDiagnosticIssueCatalog` centralizes the issue category
and fallback error-code vocabulary so reports, summaries, CLI output, and
gateway views can share the same taxonomy.
The config, raw request, and full plan payloads are pinned by golden fixtures in
`a2ui/a2ui-wayang/src/test/resources/contracts/a2ui`, giving external adapters
a stable JSON target for CLI, REST, and harness integrations.
Individual endpoint diagnostic issue rows also have golden fixtures for
unknown-path and route-mismatch cases, so consumers can validate issue-level
rendering without parsing an entire diagnostic report.
The combined runner envelope has its own golden fixture as well, covering the
adapter-facing `summary` plus `report` payload produced by
`WayangA2uiHttpEndpointDiagnosticRun.toJson()`.
A2UI transport response maps preserve canonical insertion order before JSON
serialization, so embedded transport bodies remain stable across repeated JVM
runs and can be safely pinned by golden fixtures.
Endpoint diagnostic config, request, plan, issue, report, summary, and run
projections use the same immutable linked-map snapshot policy, so adapter-facing
JSON keeps its authored field order while remaining safe from caller mutation.
A2UI binding-report, smoke-summary, smoke-probe, and readiness-probe results use
that same snapshot policy, including generated issue rows, so probe fixtures and
runtime readiness payloads share one serialization discipline.
A2UI HTTP scenario, expectation, suite, and smoke-result payloads follow the same
policy, keeping harness and smoke bodies deterministic from the lowest scenario
rows up to readiness.

Framework routers can also register individual mounted route bindings:

```java
for (WayangA2uiHttpRouteBinding route : endpoint.bindings()) {
    // register route.httpMethod(), route.path(), route.allowedMethods()
}
```

Each binding exposes operation, path, method, media types, request-body
requirements, `Allow` metadata, route matching, and per-route dispatch with
mismatch protection.

Adapters that need a serializable registration manifest can expose
`endpoint.publication().toMap()`. The publication includes route count,
operation order, mounted paths, and the same per-route metadata as individual
bindings, without retaining framework or endpoint objects.

### A2UI Probe Helpers

Use the probe result helpers instead of reading raw HTTP response bodies:

```java
WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(transportAdapter);

WayangA2uiHttpBindingReportProbeResult binding =
        WayangA2uiHttpBindingReportProbeResult.run(adapter);

WayangA2uiHttpActionBindingProbeResult actionBinding =
        WayangA2uiHttpActionBindingProbeResult.run(adapter);

WayangA2uiHttpSmokeProbeResult smoke =
        WayangA2uiHttpSmokeProbeResult.run(adapter);

WayangA2uiHttpReadinessProbeResult readiness = adapter.readinessProbe();
WayangA2uiHttpReadinessProbeResult decoded =
        WayangA2uiHttpReadinessProbeResult.from(adapter.readinessResponse());

WayangA2uiHttpOperationalDiagnostics diagnostics =
        adapter.operationalDiagnostics();

WayangA2uiHttpOperationalDiagnosticsSummary summary =
        diagnostics.summary();
```

`WayangA2uiHttpBindingReportProbeResult.passed()` is true only when the
response is HTTP-successful, routed through `a2ui.bindingReport`, decodes as the
expected JSON binding-report envelope, and the route catalog is complete.

`WayangA2uiHttpSmokeProbeResult.passed()` is true only when the response is
HTTP-successful, routed through `a2ui.smoke`, and the decoded smoke summary has
a successful exit state.

`WayangA2uiHttpActionBindingProbeResult.passed()` is true when the HTTP
exchange returns an action-binding report and every policy-allowed action has a
registered handler. Orphan handlers remain visible in counts and lists, but do
not fail the probe because policy gating prevents those actions from running.

`WayangA2uiHttpReadinessProbeResult.passed()` requires the binding-report probe
and action-binding probe to pass. It also requires the smoke probe to pass when
the route catalog exposes `a2ui.smoke`. `from(response)`, `fromMap(...)`, and
`fromJson(...)` decode the same readiness decision from an HTTP endpoint
response or a stored probe payload.

`WayangA2uiHttpOperationalDiagnostics` is the adapter-facing aggregate for
callers that want one typed object. `adapter.operationalDiagnostics()` runs
readiness once and exposes the derived binding-report, action-binding, smoke,
readiness, and standard readiness views without repeating probe calls.
`WayangA2uiHttpOperationalDiagnosticsSummary` provides the compact
operator-facing payload for CLI, REST, and harness integrations that only need
pass/fail state, exit code, issue codes, binding/action/smoke counts, and the
readiness identity.

Incomplete A2UI binding reports flatten missing/orphan route-handler problems
into `issues` with `issueCount`, so operator-facing surfaces can show clear
diagnostics without walking the raw report body.

## A2A HTTP Discovery

The neutral A2A HTTP dispatcher exposes the standard
`/.well-known/agent-card.json` discovery route from `A2aHttpRouteCatalog`. Agent
Card responses include `Cache-Control` and deterministic `ETag` headers so
gateways and clients can cache discovery metadata. When a request sends a
matching `If-None-Match` header, the dispatcher returns `304 Not Modified`
while preserving route metadata headers such as `Allow` and
`X-Wayang-A2A-Route-Operation`.

The `/extendedAgentCard` route can be protected with
`WayangA2aExtendedAgentCardAuthorizer` without coupling the neutral dispatcher
to a framework security implementation. By default the route remains open for
backward-compatible harnesses; callers can opt into bearer-token or custom
authorization. Unauthorized responses return HTTP `401`,
`extended_agent_card_unauthorized`, and a `WWW-Authenticate` challenge while
preserving A2A route metadata headers.

The JSON-RPC HTTP adapter can use the same authorizer for the
`GetExtendedAgentCard` method. Unauthorized requests are rejected before method
dispatch with HTTP `401`, a JSON-RPC custom authentication error code `-32010`,
and the same `WWW-Authenticate` challenge. Required extension negotiation does
not block Agent Card discovery methods, so authenticated clients can still fetch
the extended contract before invoking operational methods.

## A2A Capability Validation

Optional A2A operations are gated by the capabilities advertised in the Agent
Card. REST and JSON-RPC dispatchers reject streaming operations unless
`capabilities.streaming` is true, reject push notification configuration
operations unless `capabilities.pushNotifications` is true, and reject extended
Agent Card access unless `capabilities.extendedAgentCard` is true. REST
responses use HTTP `400` with stable string codes such as
`push_notification_not_supported`, `unsupported_operation`, and
`extended_agent_card_not_configured`. JSON-RPC responses use A2A-specific codes
including `-32003`, `-32004`, and `-32007`.

## A2A SendMessage Request Validation

SendMessage and SendStreamingMessage requests validate payload semantics before
creating or mutating tasks. Inbound messages must use `ROLE_USER`; agent or
unspecified roles are rejected as request errors. Message parts are projected to
input modes and checked against `AgentCard.defaultInputModes`: text parts use
`text/plain`, data parts use `application/json`, and file parts use their
declared `mediaType` or `application/octet-stream` when omitted. Exact MIME
types plus simple wildcards such as `text/*` and `*/*` are supported.

REST routes return HTTP `400` with `invalid_message_role` or
`unsupported_input_mode` before invoking handlers. JSON-RPC routes map the same
failures to `Invalid params` (`-32602`). This keeps unsupported payloads out of
task history and makes adapter behavior match the advertised Agent Card input
contract.

Wayang-specific skill routing metadata is validated at the same boundary.
Requests may provide `allowedSkills` or `wayang.allowedSkills` in request or
message metadata, but every requested skill ID must appear in
`AgentCard.skills`. REST returns `skill_not_supported`; JSON-RPC again maps the
failure to `Invalid params` before task creation.

When a request selects one or more advertised skills, input-mode validation uses
the union of those skills' `inputModes`. If the selected skills do not declare
input modes, validation falls back to `AgentCard.defaultInputModes`.

The dispatcher runs request and configuration checks through one SendMessage
preflight boundary, so REST and JSON-RPC parse valid SendMessage payloads once
and reuse the decoded request for tenant validation and handler execution while
malformed payloads still flow to the normal request parsing error path.

## A2A SendMessage Configuration

SendMessage and SendStreamingMessage requests validate their optional
`configuration` before agent execution. `acceptedOutputModes` is checked against
`AgentCard.defaultOutputModes` using content-negotiation semantics: at least one
client-accepted mode must be supported. Output mode matching uses the same exact
MIME and simple wildcard rules as input-mode validation. REST requests with no
compatible mode return HTTP `400` and `unsupported_output_mode`; JSON-RPC
requests return `Invalid params` (`-32602`). Requests that include
`taskPushNotificationConfig` require `capabilities.pushNotifications`; otherwise
REST returns `push_notification_not_supported` and JSON-RPC returns
`PushNotificationNotSupportedError` (`-32003`).

When `allowedSkills` narrows routing, output-mode validation uses the selected
skills' `outputModes`, falling back to `AgentCard.defaultOutputModes` when the
selected skills do not declare output modes.

The send-message service persists a valid `taskPushNotificationConfig` with the
created task before execution. `historyLength` is applied only to the returned
task projection, so the stored task keeps its complete history while the
response can carry the requested trailing window. `returnImmediately` is parsed
for protocol compatibility but the current neutral service remains synchronous
and does not advertise an asynchronous execution contract.

## A2A Tenant Validation

The shared tenant guard validates explicit `tenant` hints against tenant values
advertised by `AgentCard.supportedInterfaces`. Tenantless Agent Cards and
tenantless requests remain compatible. When an Agent Card advertises one or more
tenant-scoped interfaces, REST and JSON-RPC dispatchers reject requests that
include a different `tenant` before handler execution.

REST responses return HTTP `400` with `tenant_not_supported` and the supported
tenant list in error metadata. JSON-RPC dispatchers map the same contract
failure to `Invalid params` (`-32602`). The guard intentionally validates only
explicit request hints; framework adapters can still perform richer tenant
resolution and authorization before handing requests to the neutral dispatcher.

Task access also respects explicit tenant hints. `WayangA2aTaskQuery` filters
list results by task metadata tenant, and direct task-id routes hide
tenant-mismatched tasks as not found before reading, canceling, subscribing, or
mutating push notification configs. JSON-RPC uses the same rule for `GetTask`,
`CancelTask`, `SubscribeToTask`, and push notification methods, returning the
same task-not-found surface to avoid leaking cross-tenant task existence.

## A2A Task Lifecycle Validation

The in-memory A2A task store now enforces shared lifecycle invariants before
mutating task state, history, or artifacts. Terminal tasks cannot be canceled,
transitioned, or appended to, and status updates cannot target
`TASK_STATE_UNSPECIFIED`. REST handlers map lifecycle violations to HTTP `400`
with `unsupported_operation`; JSON-RPC maps them to
`UnsupportedOperationError` (`-32004`). Non-terminal cancel paths still return a
normal canceled task response.

## A2A Extension Negotiation

The shared A2A extension negotiator reads required extension declarations from
the Agent Card and validates comma-separated `A2A-Extensions` opt-ins before
agent execution. Public and extended Agent Card discovery remain reachable
without extension headers so clients can learn the advertised contract first.

REST-style A2A operational routes return HTTP `400` with
`extension_support_required` when a required extension is missing. JSON-RPC
requests return the A2A `ExtensionSupportRequiredError` code `-32008` before
dispatching the method. Both responses include the required extension list in an
`A2A-Extensions` response header for probes and gateways.

## A2A JSON-RPC Surface

The A2A JSON-RPC HTTP adapter is configurable through
`WayangA2aJsonRpcHttpConfig`. Defaults are:

| Operation | Method | Path | Enabled by default | Purpose |
|-----------|--------|------|--------------------|---------|
| `JsonRpc` | `POST` | `/` | yes | JSON-RPC 2.0 A2A endpoint. |
| `JsonRpcSmoke` | `GET` | `/a2a/jsonrpc/smoke` | yes | Run the built-in JSON-RPC smoke scenario. |
| `JsonRpcRouteCatalog` | `GET` | `/a2a/jsonrpc/route-catalog` | yes | Inspect configured JSON-RPC HTTP routes. |
| `JsonRpcDiagnostics` | `GET` | `/a2a/jsonrpc/diagnostics` | yes | Publish compact aggregate diagnostics. |
| `JsonRpcSpecCompliance` | `GET` | `/a2a/jsonrpc/spec-compliance` | yes | Publish the A2A JSON-RPC method mapping guardrail. |
| `JsonRpcBindingReport` | `GET` | `/a2a/jsonrpc/binding-report` | yes | Inspect endpoint, methods, media types, and diagnostics routes. |
| `JsonRpcReadiness` | `GET` | `/a2a/jsonrpc/readiness` | yes | Compose binding-report and optional smoke probes. |
| `JsonRpcReadinessIssueSummary` | `GET` | `/a2a/jsonrpc/readiness/issues` | yes | Flatten readiness diagnostic issues. |

Every enabled diagnostic route also supports `OPTIONS` and emits A2A route
metadata headers such as `Allow`, `X-Wayang-A2A-Route-Operation`, and
`X-Wayang-A2A-Protocol-Version`. Responses also include the standard
`A2A-Version` header so framework adapters can expose protocol version metadata
without depending on the Wayang-specific diagnostic header.

The JSON-RPC endpoint accepts the standard `A2A-Version` request header. Missing
or matching versions are accepted. Unsupported versions are rejected before
agent execution with HTTP `400` and the A2A JSON-RPC
`VersionNotSupportedError` code `-32009`.

Runtime configuration can bind from a map using these stable keys and aliases:

| Field | Aliases |
|-------|---------|
| `endpointPath` | `endpoint`, `path` |
| `smokePath` | `smokeEndpointPath` |
| `smokeEnabled` | `enableSmoke` |
| `routeCatalogPath` | `routesPath`, `httpRouteCatalogPath`, `catalogPath` |
| `routeCatalogEnabled` | `enableRouteCatalog`, `routesEnabled`, `httpRouteCatalogEnabled` |
| `diagnosticsReportPath` | `aggregateDiagnosticsPath`, `jsonRpcDiagnosticsPath`, `reportPath` |
| `diagnosticsReportEnabled` | `enableDiagnosticsReport`, `aggregateDiagnosticsEnabled`, `jsonRpcDiagnosticsEnabled` |
| `specComplianceReportPath` | `specCompliancePath`, `jsonRpcSpecCompliancePath`, `complianceReportPath` |
| `specComplianceReportEnabled` | `enableSpecComplianceReport`, `specComplianceEnabled`, `jsonRpcSpecComplianceEnabled` |
| `bindingReportPath` | `bindingPath`, `diagnosticsPath` |
| `bindingReportEnabled` | `enableBindingReport`, `diagnosticsEnabled` |
| `readinessPath` | `healthPath` |
| `readinessEnabled` | `enableReadiness`, `healthEnabled` |
| `readinessIssueSummaryPath` | `readinessIssuesPath`, `healthIssueSummaryPath`, `issueSummaryPath` |
| `readinessIssueSummaryEnabled` | `enableReadinessIssueSummary`, `healthIssueSummaryEnabled`, `issueSummaryEnabled` |

The config normalizes paths and rejects ambiguous collisions between enabled
endpoint, smoke, route-catalog, diagnostics-report, spec-compliance-report,
binding-report, readiness, and readiness issue summary routes.

### A2A Probe Helpers

Use adapter helpers when possible so probes validate the same configured paths
that the runtime serves:

```java
WayangA2aJsonRpcHttpAdapter adapter =
        WayangA2aJsonRpcHttpAdapter.withSmoke(dispatcher, smokeRunner, config);

WayangA2aJsonRpcHttpRouteCatalog routes = adapter.routeCatalog();
WayangA2aJsonRpcHttpPublication publication = adapter.routePublication();
WayangA2aJsonRpcSpecComplianceReport compliance = adapter.specComplianceReport();
WayangA2aJsonRpcSpecComplianceReport decodedCompliance =
        WayangA2aJsonRpcSpecComplianceReport.fromJson(adapter.specComplianceReportResponse().body());
WayangA2aJsonRpcRouteCatalogProbeResult routeProbe = adapter.routeCatalogProbe();
WayangA2aJsonRpcBindingReportProbeResult binding = adapter.bindingReportProbe();
WayangA2aJsonRpcSmokeProbeResult smoke = adapter.smokeProbe();
WayangA2aJsonRpcReadinessProbeResult readiness = adapter.readinessProbe();
WayangA2aJsonRpcReadinessIssueSummary issues = adapter.readinessIssueSummary();
WayangA2aJsonRpcDiagnosticsReport diagnostics = adapter.diagnosticsReport();
WayangA2aJsonRpcDiagnosticsReport decodedDiagnostics =
        WayangA2aJsonRpcDiagnosticsReport.fromJson(adapter.diagnosticsReportResponse().body());
WayangA2aJsonRpcReadinessProbeResult decoded =
        WayangA2aJsonRpcReadinessProbeResult.from(adapter.readinessResponse());
WayangA2aJsonRpcReadinessIssueSummary decodedIssues =
        WayangA2aJsonRpcReadinessIssueSummary.from(adapter.readinessIssueSummaryResponse());
```

`WayangA2aJsonRpcBindingReportProbeResult.passed()` is true only when the
response is HTTP-successful, routed through `JsonRpcBindingReport`, returns
`application/json`, exposes at least one JSON-RPC method, and includes non-blank
endpoint, smoke, route-catalog, diagnostics-report, spec-compliance-report,
binding-report, readiness, and readiness issue summary paths.

`WayangA2aJsonRpcHttpPublication` is the framework/runtime registration view.
It filters the configured route catalog to enabled routes and returns
`WayangA2aJsonRpcHttpRouteBinding` entries with operation, method, path,
allowed methods, media types, and a `handle(request)` delegate. Runtime layers
can register routes without hardcoding paths:

```java
for (WayangA2aJsonRpcHttpRouteBinding binding : adapter.routePublication().bindings()) {
    register(binding.httpMethod(), binding.path(), binding::handle);
    register("OPTIONS", binding.path(), binding::handle);
}
```

`WayangA2aJsonRpcSpecComplianceReport` is the A2A v1.0 JSON-RPC
specification guardrail. It projects the canonical operation matrix from
`A2aHttpRouteCatalog.standard()`, including JSON-RPC method, gRPC method,
HTTP+JSON/REST method and path, streaming status, media types, and support
status. It also checks that the configured JSON-RPC endpoint is present in the
route publication, so CLI/CI can fail before a framework adapter silently omits
the main endpoint. The adapter exposes `specComplianceReportResponse()` plus a
configurable `GET /a2a/jsonrpc/spec-compliance` route with `OPTIONS` metadata,
so REST and gateway layers can publish the same guardrail without rebuilding
the matrix.

`WayangA2aJsonRpcReadinessProbeResult.passed()` requires the binding-report
probe and enabled route-catalog probe to pass. It also requires the smoke probe
to pass when the binding report says smoke is enabled. `from(response)`,
`fromJson(...)`, and `fromMap(...)` rebuild the same readiness decision from
endpoint responses or stored payloads.
`WayangA2aJsonRpcReadinessIssueSummary` condenses readiness, binding-report,
route-catalog, and smoke issues into one flat list with per-probe issue counts
for operator UIs, CLIs, and CI annotations. The adapter exposes both
`readinessIssueSummary()` and `readinessIssueSummaryResponse()` convenience
helpers, plus a configurable `GET /a2a/jsonrpc/readiness/issues` route, so
framework layers can publish the compact summary without duplicating probe
traversal.

`WayangA2aJsonRpcDiagnosticsReport` is the compact one-call projection for
CLI/CI. It runs the configured readiness path, summarizes binding-report,
route-catalog, smoke, and readiness checks, carries the flattened issue list,
and keeps the runtime config snapshot under `attributes.config`. The adapter
also exposes `diagnosticsReportResponse()` plus a configurable
`GET /a2a/jsonrpc/diagnostics` route with `OPTIONS` metadata for REST,
gateway, and CLI integrations.

Incomplete A2A binding reports flatten HTTP, route-operation, content-type,
method-count, and missing-path failures into `issues` with `issueCount`.

## Contract Fixtures

Probe JSON shapes are locked by golden fixtures. Keep external integrations
aligned with these files instead of depending on incidental field order or raw
transport internals.

A2UI fixtures:

- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-binding-report-probe-result.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-binding-report-probe-result-incomplete.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-smoke-probe-result.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-smoke-probe-result-failed.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-readiness-probe-result.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-route-catalog-body.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-binding-report-body.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-config.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-request.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-plan.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-report.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-summary.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-issue-unknown-path.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-issue-route-mismatch.json`
- `a2ui/a2ui-wayang/src/test/resources/contracts/a2ui/wayang-http-endpoint-diagnostic-run.json`

A2A fixtures:

- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-binding-report.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-route-catalog.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-route-catalog-probe-result.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-diagnostics-report.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-spec-compliance-report.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-binding-report-probe-result.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-binding-report-probe-result-incomplete.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-smoke-result.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-smoke-result-passed.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-smoke-summary.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-smoke-summary-passed.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-smoke-probe-result.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-smoke-probe-result-passed.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-readiness-probe-result.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-readiness-probe-result-failed.json`
- `a2a/a2a-wayang/src/test/resources/contracts/a2a/wayang-jsonrpc-readiness-issue-summary-failed.json`

## Focused Verification

Run the adapter slices when changing diagnostics, HTTP routing, probe payloads,
or contract fixtures:

```bash
mvn -q -Ppro-enterprise-addons -pl a2ui/a2ui-core,a2ui/a2ui-wayang -am test
mvn -q -pl a2a/a2a-core,a2a/a2a-wayang -am test
```

A known JBoss LogManager warning may appear during Maven test startup. It is
not currently treated as a probe failure; rely on the Maven exit code and the
golden fixture assertions.

## Integration Guidance

- Expose probe JSON as-is from CLI, REST, gateway, and readiness surfaces.
- Prefer `adapter.bindingReportProbe()`, `adapter.smokeProbe()`,
  `adapter.routeCatalogProbe()`, `adapter.readinessProbe()`, and
  `adapter.readinessIssueSummary()` on A2A so runtime config is validated end to
  end.
- Prefer `adapter.routePublication().bindings()` when a framework, gateway, or
  runtime module needs to register the enabled A2A JSON-RPC routes from one
  catalog-backed source.
- Prefer `adapter.specComplianceReport()` when CLI/CI needs a compact
  [A2A specification](https://a2a-protocol.org/latest/specification/) mapping
  guardrail for JSON-RPC, gRPC, HTTP+JSON/REST, streaming media types, and
  endpoint publication before deploying a route adapter; publish
  `adapter.specComplianceReportResponse()` when an HTTP route should expose the
  same payload.
- Forward the standard `A2A-Version` request header into
  `WayangA2aJsonRpcHttpAdapter.dispatch(...)`. The adapter validates it against
  the supported protocol version and emits both `A2A-Version` and
  `X-Wayang-A2A-Protocol-Version` on route responses.
- Prefer `adapter.diagnosticsReport()` when CLI/CI needs one compact pass/fail
  payload instead of the full nested readiness body; publish
  `adapter.diagnosticsReportResponse()` when an HTTP route should expose the
  same report.
- Prefer `WayangA2uiHttpBindingReportProbeResult.run(adapter)` and
  `WayangA2uiHttpSmokeProbeResult.run(adapter)` on A2UI, or
  `adapter.readinessProbe()` when one composed readiness decision is enough, so
  the route catalog and dispatcher stay the single source of truth.
- Treat `passed`, `exitCode`, `issueCount`, and `issues` as the operator-facing
  decision fields.
- Treat `body`, `metadata`, and `headers` as diagnostic context for debugging
  and contract compatibility.
