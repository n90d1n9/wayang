# A2UI Surface Architecture

This note documents the current Wayang A2UI surface boundary after the renderer
split. The goal is to keep model-to-surface rendering modular, testable, and
easy to extend without growing `WayangA2uiSurfaces` back into a large class.

## Boundary

The A2UI Wayang adapter renders SDK models into A2UI server message streams.
The public compatibility entry points stay small, while package-local classes
own concrete layout, action, identity, text, data, and message assembly.

Primary module:

`a2ui/a2ui-wayang/src/main/java/tech/kayys/wayang/a2ui/wayang`

## Ownership Map

| Area | Owner | Responsibility |
| --- | --- | --- |
| Public facade | `WayangA2uiSurfaces` | Stable static methods for SDK callers. Delegates to dedicated renderers. |
| Renderer registry | `WayangA2uiSurfaceRegistry` | Model-type to surface-kind binding, replacement, lookup, and extension. |
| Run status | `WayangA2uiRunStatusSurface` | Single-run layout and action placement. |
| Run inspection | `WayangA2uiRunInspectionSurface` | Composition of status plus optional events surfaces. |
| Run events | `WayangA2uiRunEventsSurface` | Event stream layout, rows, and refresh action. |
| Run history | `WayangA2uiRunHistorySurface` | History page layout and per-run row placement. |
| Action result | `WayangA2uiResultSurfaces` | Fallback/rejected/handled action feedback surfaces. |
| Surface ids | `WayangA2uiSurfaceIds` | Stable id formats and safe id segment normalization. |
| Text | `WayangA2uiSurfaceText` | User-visible run/history/event text and fallback messages. |
| Run data models | `WayangA2uiRunDataModels` | Top-level data-model entries for status, events, and history. |
| Nested data entries | `WayangA2uiSurfaceData` | Nested run/event/count data-entry lists. |
| Action vocabulary | `WayangA2uiActions` | Stable action ids, default action groups, and action taxonomy. |
| Action admission | `WayangA2uiActionGate` | Policy checks and pre-dispatch rejection results for inbound actions. |
| Action dispatch | `WayangA2uiActionRouter`, `WayangA2uiActionHandlers`, `WayangA2uiActionHandler` | User-action orchestration and action-to-handler lookup. |
| Action dispatch diagnostics | `WayangA2uiActionBindingReport` | Coverage report between allowed policy actions and registered action handlers. |
| Action report decoding | `WayangA2uiActionBindingReportDecoder` | Stored, remote, transport, and HTTP response decoding for action policy/handler diagnostics. |
| Action binding transport | `WayangA2uiTransportRequest.actionBindingReport`, `WayangA2uiTransportAdapter.exchangeActionBindingReport` | Transport-neutral access to action policy/handler diagnostics through the same bridge envelope as other A2UI operations. |
| Action binding readiness | `WayangA2uiHttpActionBindingProbeResult` | HTTP readiness probe for allowed-action handler coverage; missing allowed handlers block readiness while orphan handlers stay visible as non-blocking diagnostics. |
| HTTP operational diagnostics | `WayangA2uiHttpOperationalDiagnostics` | Single typed diagnostics facade over readiness, route binding, action binding, and smoke probes for CLI, REST, and harness callers. |
| HTTP operational summary | `WayangA2uiHttpOperationalDiagnosticsSummary` | Compact operator-facing summary of pass/fail state, binding/action/smoke counts, issue codes, and readiness identity. |
| Action query mapping | `WayangA2uiActionQueries` | Inbound action context to SDK history/event/wait/cancel request values. |
| Action responses | `WayangA2uiActionResponses` | SDK lifecycle responses to handled action results, surfaces, and metadata. |
| Action controls | `WayangA2uiSurfaceActions`, `WayangA2uiRunActionControls` | A2UI action construction and repeated run control wiring. |
| Session profiles | `WayangA2uiSessionProfiles` | Named session modes, aliases, and default action policies. |
| Root layout helpers | `WayangA2uiSurfaceLayouts` | Mutable component/child lists and root-column insertion. |
| Message assembly | `WayangA2uiSurfaceMessages` | Standard data-model update, surface update, begin-rendering sequence. |

## Render Flow

```text
SDK model
  -> WayangA2uiSurfaceRegistry
  -> WayangA2uiSurfaces facade
  -> dedicated renderer/composer
  -> ids/text/layout/actions/data helpers
  -> WayangA2uiSurfaceMessages.standard(...)
  -> List<A2uiServerMessage>
```

`WayangA2uiSurfaces` should remain a facade. New layout, data, or action logic
belongs in a focused renderer or helper.

## Extension Rules

1. Add a dedicated renderer for each new domain surface.
2. Keep the public facade method as delegation only.
3. Register the model type and surface kind in `WayangA2uiSurfaceRegistry`.
4. Reuse `WayangA2uiSurfaceIds` for stable ids instead of hand-building ids in
   multiple places.
5. Reuse `WayangA2uiSurfaceMessages.standard(...)` for the common three-message
   response sequence.
6. Put repeated user-visible text in `WayangA2uiSurfaceText`.
7. Put repeated data-entry shape in `WayangA2uiRunDataModels` or
   `WayangA2uiSurfaceData`, depending on whether it is top-level or nested.
8. Add focused tests for the new helper/renderer plus registry and contract
   coverage when the public surface shape changes.

## Test Shape

Focused tests live beside the surface package:

| Test | Purpose |
| --- | --- |
| `WayangA2uiSurfacesTest` | Public facade compatibility and representative JSONL shape. |
| `WayangA2uiRunStatusSurfaceTest` | Status renderer contract and lifecycle action gating. |
| `WayangA2uiRunInspectionSurfaceTest` | Status plus events composition rules. |
| `WayangA2uiRunEventsSurfaceTest` | Event renderer contract and refresh action gating. |
| `WayangA2uiRunHistorySurfaceTest` | History renderer contract and terminal row action gating. |
| `WayangA2uiRunActionControlsTest` | Shared run action labels and gating. |
| `WayangA2uiSurfaceMessagesTest` | Standard message order. |
| `WayangA2uiSurfaceLayoutsTest` | Root-column helper behavior. |
| `WayangA2uiRunDataModelsTest` | Top-level run data-model entry order. |
| `WayangA2uiActionsTest` | Action names, default action groups, and action taxonomy. |
| `WayangA2uiActionGateTest` | Pre-dispatch policy admission and rejection ordering. |
| `WayangA2uiActionHandlersTest` | Built-in/custom handler lookup, SDK dispatch, and unsupported fallback. |
| `WayangA2uiActionBindingReportTest` | Policy/handler binding coverage and projection order. |
| `WayangA2uiActionBindingReportDecoderTest` | Stored, JSON, transport, and HTTP response decoding for action binding diagnostics. |
| `WayangA2uiHttpActionBindingProbeResultTest` | HTTP exchange probe behavior for action binding readiness and custom missing-handler failures. |
| `WayangA2uiHttpOperationalDiagnosticsTest` | Aggregate A2UI HTTP diagnostics facade, compact summary decoding, JSON/map decoding, and readiness failure propagation. |
| `WayangA2uiActionRouterTest` | Public action routing contract across gate, handlers, and response surfaces. |
| `WayangA2uiActionQueriesTest` | Inbound action context to SDK query/option mapping. |
| `WayangA2uiActionResponsesTest` | SDK lifecycle responses to handled A2UI action results. |
| `WayangA2uiSessionProfilesTest` | Session mode aliases and default policy/config resolution. |
| `WayangA2uiSurfaceContractTest` | Pinned parsed JSONL contract fixtures. |
| `WayangA2uiSurfaceRegistryTest` | Surface kind/model registry behavior. |

Shared run-surface test samples live in `WayangA2uiRunSurfaceFixtures`. Use it
for canonical running/completed statuses, event pages, history pages, and
inspection samples before adding another local sample builder.

Shared parsed-JSONL helpers live in `A2uiJsonlTestSupport`. Use it when a test
needs to stream A2UI messages or inspect the standard data-model update, surface
update, and begin-rendering lines.

The contract fixtures currently pin the read-only run status, run events, run
history, composite inspection, action feedback, and surface catalog shapes.
When a public surface intentionally changes, update the focused renderer test and
the matching fixture in the same change.

Useful focused command:

```bash
mvn -q -Ppro-enterprise-addons -pl a2ui/a2ui-wayang -Dtest=WayangA2uiRunStatusSurfaceTest,WayangA2uiRunInspectionSurfaceTest,WayangA2uiRunEventsSurfaceTest,WayangA2uiRunHistorySurfaceTest,WayangA2uiSurfaceContractTest,WayangA2uiSurfaceRegistryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Full A2UI verification:

```bash
mvn -q -Ppro-enterprise-addons -pl a2ui/a2ui-core,a2ui/a2ui-wayang -am test
```

## Guardrails

- Do not add layout branches to `WayangA2uiSurfaces`; add a renderer.
- Do not make one generic renderer for all domains; keep renderers tied to SDK
  model concepts.
- Do not duplicate A2UI id formats, action labels, or message sequencing.
- Keep action ids, default action groups, and action taxonomy in
  `WayangA2uiActions`.
- Keep pre-dispatch action rejection in `WayangA2uiActionGate`; the router should
  dispatch accepted actions.
- Keep built-in action-to-SDK dispatch in `WayangA2uiActionHandlers`; avoid
  growing `WayangA2uiActionRouter` with per-action branches.
- Add custom action dispatch through `WayangA2uiActionHandlers.builder()` or
  `standardBuilder(...).register(...)`; remember the action policy must allow
  the custom action name before the handler can run.
- Use `WayangA2uiActionBindingReport` when wiring custom action policies and
  handlers so missing or orphan action bindings are visible before runtime.
- Use `WayangA2uiActionBindingReport.from(...)`/`fromJson(...)` rather than
  hand-parsing report response bodies.
- Use `WayangA2uiTransportRequest.actionBindingReport()` or
  `WayangA2uiTransportAdapter.exchangeActionBindingReport()` when exposing that
  report through bridges; avoid adding duplicate framework-specific report
  shapes.
- Treat missing allowed action handlers as readiness-blocking. Orphan handlers
  remain visible in action binding diagnostics, but they are not readiness
  failures because policy gating prevents those actions from dispatching.
- Use `WayangA2uiHttpOperationalDiagnostics` when adapter, CLI, REST, or harness
  code needs one A2UI HTTP operational object. It derives route binding,
  action binding, and smoke probes from readiness instead of running duplicate
  probes.
- Use `diagnostics.summary()` or
  `WayangA2uiHttpOperationalDiagnosticsSummary.from(...)` for compact
  operator-facing payloads. The golden fixture pins this smaller shape while
  the full diagnostics object remains available for drill-down.
- Keep session mode aliases and default action-policy profiles in
  `WayangA2uiSessionProfiles`.
- Keep inbound action context parsing in `WayangA2uiActionContextReader` and
  SDK query/option construction in `WayangA2uiActionQueries`.
- Keep handled action result assembly in `WayangA2uiActionResponses`.
- Do not change JSONL field order without focused tests and contract fixture
  updates.
- Prefer package-local helpers for shared mechanics and public classes only for
  compatibility or extension points.
