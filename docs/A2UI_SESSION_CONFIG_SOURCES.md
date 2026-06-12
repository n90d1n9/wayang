# A2UI Session Config Sources

Wayang A2UI session configuration can be loaded from inline JSON, files,
classpath resources, or application-owned providers such as database, S3, or
RustFS adapters. The A2UI module owns the declarative contract and fallback
composition; storage clients stay outside the module.

## Core Types

| Type | Responsibility |
| --- | --- |
| `SessionConfigSource` | Reads optional JSON and decodes it into `WayangA2uiSessionConfig`. |
| `SessionConfigSources` | Factory helpers for inline, file, classpath, and first-available sources. |
| `SessionConfigSourceCapability` | Discovery metadata for required fields, aliases, and diagnostic-safe fields. |
| `SessionConfigSourceDiagnostics` | Operator-facing payload with redacted source specs, policy state, capabilities, and load result. |
| `SessionConfigSourcePolicy` | Allow/deny policy for deployment-safe dynamic source types. |
| `SessionConfigSourceSpec` | Structured, storage-neutral map specification with built-in validation. |
| `SessionConfigSourceSpecs` | Typed factories for common database, config-service, S3, and RustFS specs. |
| `SessionConfigSourceRegistry` | Resolves specs into sources and hosts custom providers. |
| `SessionConfigSourceRegistries` | Composition helpers for common registry/provider wiring. |
| `SessionConfigSourceRedactor` | Shared diagnostic redaction for source specs that may include credentials or endpoints. |
| `SessionConfigSourceProvider` | Adapter hook for database, object-storage, or service-backed sources. |
| `SessionConfigRequestDiagnostics` | Request-level envelope explaining direct config, dynamic source, active input, and load outcome. |
| `SessionConfigRequestDiagnosticsDecoder` | Stored, remote, and JSON decoding for request-level session diagnostics. |
| `SessionConfigRequestDiagnosticsSummary` | Compact pass/fail summary for request-level diagnostics, suitable for CLI, readiness, and dashboard rows. |
| `SessionConfigRequestDiagnosticsSummaryDecoder` | Stored, remote, and JSON decoding for compact request diagnostic summaries. |
| `SessionConfigLoadAttempt` | Per-source diagnostic row for fallback chains. |
| `SessionConfigLoadResult` | Diagnostic result for loaded, missing, or failed source reads. |
| `SessionConfigLoadResultDecoder` | Stored, remote, and JSON decoding for load-result diagnostics. |
| `SessionConfigSourceDiagnosticsDecoder` | Stored, remote, and JSON decoding for source diagnostic payloads. |

## Built-In Specs

Inline JSON:

```json
{
  "type": "inline",
  "json": "{ \"mode\": \"read-only\" }"
}
```

File:

```json
{
  "type": "file",
  "path": "/etc/wayang/a2ui-session.json"
}
```

Classpath resource:

```json
{
  "type": "classpath",
  "resource": "a2ui/session-config-readonly.json"
}
```

First available source:

```json
{
  "type": "fallback",
  "sources": [
    {
      "type": "database",
      "tenantId": "tenant-a"
    },
    {
      "type": "s3",
      "bucket": "wayang-config",
      "key": "tenants/tenant-a/a2ui-session.json"
    },
    {
      "type": "file",
      "path": "/etc/wayang/a2ui-session-fallback.json"
    }
  ]
}
```

The same shape can be built without raw maps:

```java
SessionConfigSourceSpec spec = SessionConfigSourceSpec.firstAvailable(
        SessionConfigSourceSpecs.database("tenant-a"),
        SessionConfigSourceSpecs.s3("wayang-config", "tenants/tenant-a/a2ui-session.json"),
        SessionConfigSourceSpec.file(Path.of("/etc/wayang/a2ui-session-fallback.json")));
```

Use `toDiagnosticMap()` when exporting source specs to logs, readiness probes,
or operator diagnostics. The executable `toMap()` remains unchanged, while
credential-like fields and service endpoints are masked:

```java
Map<String, Object> diagnosticSource = spec.toDiagnosticMap();
```

## Provider Registration

Register storage-backed providers at the application boundary. The provider
receives a normalized spec map and returns a JSON-backed source.

```java
SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
        .register("database", values -> SessionConfigSources.json(
                "database:" + values.get("tenantId"),
                () -> database.loadSessionConfigJson(String.valueOf(values.get("tenantId")))))
        .register("s3", values -> SessionConfigSources.json(
                "s3:" + values.get("bucket") + "/" + values.get("key"),
                () -> objectStorage.readText(
                        String.valueOf(values.get("bucket")),
                        String.valueOf(values.get("key")))))
        .build();
```

Use `SessionConfigSourceRegistries` when composing common providers:

```java
SessionConfigSourceRegistry registry = SessionConfigSourceRegistries
        .registerS3(
                SessionConfigSourceRegistries.registerDatabase(
                        SessionConfigSourceRegistry.standardBuilder(),
                        (tenantId, lookupKey, values) -> database.readSessionConfig(tenantId, lookupKey)),
                (bucket, key, values) -> s3Client.readText(bucket, key))
        .build();
```

Registry capabilities make source forms discoverable for SDK, CLI, gateway, and
operator UI validation:

```java
SessionConfigSourceCapability s3 = registry.capability("s3").orElseThrow();

boolean requiresBucketKey = s3.requiresBucketKey();
List<String> safeFields = s3.diagnosticSafeFields();
```

Use `providerCapabilities()` when only registered providers are relevant, and
`sourceCapabilities()` when built-in fallback aliases such as `fallback`,
`first-available`, and `chain` should also be advertised.

Use `SessionConfigSourcePolicy` to restrict source types per deployment,
tenant, or gateway. This example allows tenant database lookup, S3, and local
file fallback while rejecting inline request-supplied JSON:

```java
SessionConfigSourceRegistry registry = SessionConfigSourceRegistries
        .registerS3(
                SessionConfigSourceRegistries.registerDatabase(
                        SessionConfigSourceRegistry.standardBuilder(),
                        (tenantId, lookupKey, values) -> database.readSessionConfig(tenantId, lookupKey)),
                (bucket, key, values) -> s3Client.readText(bucket, key))
        .policy(SessionConfigSourcePolicy.allowOnly("fallback", "database", "s3", "file"))
        .build();
```

Use `SessionConfigSourcePolicy.deny("inline", "file")` when most provider
types should remain available but specific source families need to be disabled.
Aliases are canonicalized, so denying `inline` also denies the `json` alias, and
allowing `fallback` also allows `chain` and `first-available`.

Providers can add storage-specific validation without teaching the A2UI core
about database, S3, RustFS, or vendor-specific fields:

```java
SessionConfigSourceProvider s3Provider = new SessionConfigSourceProvider() {
    @Override
    public SessionConfigSource source(Map<String, Object> values) {
        return SessionConfigSources.json(
                "s3:" + values.get("bucket") + "/" + values.get("key"),
                () -> objectStorage.readText(
                        String.valueOf(values.get("bucket")),
                        String.valueOf(values.get("key"))));
    }

    @Override
    public List<String> validationErrors(Map<String, Object> values) {
        List<String> errors = new ArrayList<>();
        if (String.valueOf(values.getOrDefault("bucket", "")).isBlank()) {
            errors.add("s3 source requires bucket");
        }
        if (String.valueOf(values.getOrDefault("key", "")).isBlank()) {
            errors.add("s3 source requires key");
        }
        return errors;
    }
};
```

For database or config-service style lookup, use
`SessionConfigLookupProvider`. Tenant-scoped database sources require
`tenantId` and default to the `default` profile; keyed config-service sources
require `key`, `configKey`, `profile`, `name`, or `id`:

```java
SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
        .register("database", SessionConfigLookupProvider.database(
                (tenantId, lookupKey, values) -> database.readSessionConfig(tenantId, lookupKey)))
        .register("config-service", SessionConfigLookupProvider.configService(
                (tenantId, lookupKey, values) -> configService.readText(lookupKey)))
        .build();
```

For bucket/key object stores, use `SessionConfigObjectStorageProvider` to reuse
standard bucket/key validation while keeping actual S3, RustFS, or compatible
client calls outside the A2UI module:

```java
SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
        .register("s3", SessionConfigObjectStorageProvider.s3(
                (bucket, key, values) -> s3Client.readText(bucket, key)))
        .register("rustfs", SessionConfigObjectStorageProvider.rustfs(
                (bucket, key, values) -> rustfsClient.readText(bucket, key)))
        .build();
```

## Request Context

Request-level overrides can carry a source object or source JSON through
`sessionConfigSource`. This allows an HTTP adapter, gateway, or tenant-aware
application layer to select storage per request while the A2UI session code
continues to consume `WayangA2uiSessionConfig`.

`sessionConfig` can also be supplied as either a decoded object or a JSON string.
Direct `sessionConfig` wins over `sessionConfigSource`, which keeps emergency
or operator-supplied policy overrides predictable.

Call `WayangA2ui.sessionConfigSourceDiagnostics(request, registry)` to inspect
the dynamic source selected by request context. The method returns an optional
diagnostic payload: it is present when `sessionConfigSource` is the active
source, empty when direct `sessionConfig` takes precedence or no source exists,
and failed when source JSON cannot be decoded. This gives gateways and
operator tools a redacted source, policy outcome, capability metadata, and load
result without duplicating request-context parsing.

Call `WayangA2ui.sessionConfigDiagnostics(request, registry)` when a caller
needs one request-level envelope. It reports whether the A2UI context exists,
whether direct config and source inputs are present, which input is active,
the top-level load status, the nested `SessionConfigLoadResult`, and optional
`SessionConfigSourceDiagnostics` when a dynamic source is active. Stored
payloads round-trip through `SessionConfigRequestDiagnostics.fromMap(...)` /
`fromJson(...)`.

Call `WayangA2ui.sessionConfigDiagnosticsSummary(request, registry)` when a
caller needs a compact pass/fail view. The summary carries active input, load
status, successful-exit code, source type, validation/policy error counts,
attempt count, and diagnostic key attributes without forcing dashboards or CLI
commands to parse the full request envelope.
Stored full-envelope payloads can expose the same compact view through
`SessionConfigRequestDiagnosticsSummary.fromDiagnosticsMap(...)` /
`fromDiagnosticsJson(...)`; if an older payload has no embedded `summary`, the
decoder derives the summary from the full diagnostics fields.

The recommended production order is:

1. Database or tenant config service.
2. Object storage such as S3 or RustFS.
3. Local file fallback.
4. Classpath default for packaged read-only behavior.

## Diagnostics

Use `SessionConfigLoadResult` when operators or tests need to know whether the
source loaded, was missing, or failed. The result can be exported as a stable map
or JSON payload and is also available through request-level A2UI session
diagnostics.

Fallback chains include ordered `attempts` when loaded through
`SessionConfigSources.firstAvailable(...)`, `loadFirstResult(...)`, or a
registry fallback source. Each attempt reports the source description, status,
loaded flag, and message, so operators can see which database, object-storage,
or file fallback missed before the selected source loaded.

Use `SessionConfigSourceDiagnostics.load(spec, registry)` when a caller needs a
single operator payload for a dynamic source. The payload includes a redacted
`sourceSpec`, `validationErrors`, `policyErrors`, top-level load `status`,
provider `capability`, registry `sourceCapabilities`, and the nested
`SessionConfigLoadResult`. This is the preferred shape for SDK, CLI, gateway,
or admin UI integrations that need to explain why a source loaded, missed, or
was rejected without exposing credentials.

Stored or remote diagnostics can be read back with
`SessionConfigSourceDiagnostics.fromMap(...)` / `fromJson(...)`. Nested load
results round-trip through `SessionConfigLoadResult.fromMap(...)` /
`fromJson(...)`, and individual fallback attempts can be decoded with
`SessionConfigLoadAttempt.fromMap(...)`.

## Contract Fixtures

The public JSON shapes for hybrid persistence are locked by contract fixtures:

- `contracts/a2ui/wayang-session-config-source-fallback.json`
- `contracts/a2ui/wayang-session-config-source-redacted.json`
- `contracts/a2ui/wayang-session-config-load-result-fallback-trace.json`
- `contracts/a2ui/wayang-session-config-source-diagnostics-policy-rejected.json`
- `contracts/a2ui/wayang-session-config-request-diagnostics-direct.json`
- `contracts/a2ui/wayang-session-config-request-diagnostics-summary-direct.json`

Use these fixtures when aligning SDK, gateway, CLI, or admin UI integrations
with the A2UI session config source contract.
