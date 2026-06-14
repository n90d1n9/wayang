# Wayang SDK

`wayang-gollek-sdk` is the shared Java contract for the Wayang agentic platform.
It plays the same role for Wayang products that Gollek SDK plays for
Gollek inference clients: one source of truth for requests, product surfaces,
status, workbench state, and the bridge into the core agent SPI.

## Boundary

- Gollek SDK remains the contract for inference, serving, and training.
- Wayang SDK is the contract for agents, skills, tools, MCP, RAG,
  memory, workflows, product workbenches, and harness operations.
- CLI, TUI, API, coding-agent products, assistant-agent products, and low-code
  workflow products should consume this SDK instead of re-declaring agent
  models.

`WayangSdkBoundaryCatalog` describes the SDK's intended ownership map before
the current flat public package is physically split. It names the core, run,
context, capability, platform, contract, workbench, storage, and remote
boundaries with their future package targets, class-prefix ownership, contract
schemas, and dependencies. Use it as the source of truth for future package
refactors and for API/CLI wrappers that need to explain which part of the SDK a
model belongs to. SDK wrappers can access the same map through
`WayangClient.platform().sdkBoundaries()`, `sdkBoundary("<id>")`,
`sdkBoundaryCatalogJson()`, and `sdkBoundaryJson("<id>")`.

## Entry Points

```java
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.Wayang;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

WayangGollekSdk sdk = Wayang.local();

var result = sdk.run(new AgentRunRequest(
        "Plan the next RAG evaluation",
        "default",
        "qwen",
        "planner",
        java.util.List.of("rag"),
        true,
        12,
        ".",
        true,
        80,
        true,
        8,
        true));
```

For new code, prefer the named builder as the request grows:

```java
var result = sdk.run(AgentRunRequest.builder()
        .prompt("Plan the next RAG evaluation")
        .systemPrompt("Stay inside the Wayang product boundary.")
        .tenantId("default")
        .modelId("qwen")
        .workflowId("planner")
        .surfaceId("coding-agent")
        .sessionId("session-a")
        .userId("user-a")
        .context("rag.collection", "docs")
        .context("mcp.server", "filesystem")
        .skill("rag")
        .workspace(".")
        .harness(8, true)
        .build());
```

`WayangGollekSdkProvider` and `WayangGollekSdkFactory` are prepared for future
local and remote implementations. The current local SDK is deterministic and
keeps the CLI/API contract stable while runtime adapters are wired underneath.
The local provider is registered through `ServiceLoader`; remote providers can
be added later with the same `WayangGollekSdkProvider` contract.

The first remote provider lives in `wayang-gollek-sdk-remote` so applications
can opt into HTTP behavior without changing the core SDK or default CLI jar.

`inspectWorkspace(...)` provides the shared workspace context contract for
coding-agent products. Local SDK inspection covers filesystem and git/build
signals; remote SDK implementations can expose the same model through platform
APIs. `AgentRunRequest` can request workspace attachment directly, and local
SDK runs map that snapshot into the core `AgentRequest` context under the
`workspace` key.

`planHarness(...)` turns workspace descriptors into planned verification
checks such as Maven, Gradle, Go, Cargo, Python, JavaScript, and Make targets.
The SDK returns command arrays and metadata, but does not execute checks; that
keeps execution policy inside the calling product, CI runner, or harness
adapter. `AgentRunRequest` can also ask local SDK runs to attach the planned
harness into the core `AgentRequest` context under the `harness` key.

`surfaceId` makes each run explicit about the product consuming the shared
agent engine. It defaults to `coding-agent` and can be set to surfaces such as
`assistant-agent` or `workflow-platform`. Runs validate the id against
`WayangProductCatalog`, so future products extend the catalog instead of
forking the core agent request contract.

Each catalog surface also has a `ProductSurfacePolicy`. The policy is descriptive
metadata for product shells and adapters: preferred context families, suggested
skills, required context keys, and routing hints. Local and remote SDK runs add
that policy to result metadata, and the mapper attaches it to the core
`AgentRequest` context under `surfacePolicy`.

`assessRunPolicy(...)` runs a descriptive preflight against an
`AgentRunRequest`. It reports whether required context is present, which keys
are missing, and recommendations such as `--workspace`, `--harness`, or
`--workflow` before a product shell decides whether to continue. Runs expose the
same result under `surfacePolicyAssessment`.

`sessionId`, `userId`, and `context(...)` form the SDK-owned run envelope for
product-specific state. The mapper forwards session/user identity to the core
`AgentRequest` fields and attaches caller context before SDK-owned reserved
context such as `surfaceId`, `workspace`, `harness`, and `surfacePolicy`, so
products can pass RAG namespaces, MCP server hints, workflow node ids, ticket
ids, or UI state without forking the request model.

`systemPrompt` is also part of the SDK contract and maps directly into the core
`AgentRequest` system prompt. CLI and product shells can source it inline or
from a file while keeping prompt assembly outside backend-specific adapters.

`previewRun(...)` returns an SDK-owned `AgentRunPreview` without submitting a
run. The preview contains the normalized core request id, tenant/model/workflow
routing, prompt/system-prompt character counts, memory/step settings, skill
filters, context keys, core context/parameters, workspace/harness attachment
flags, and the same surface policy assessment used by `run --preflight`.

`AgentRunPlanner` is the local SDK service that keeps run preparation aligned
across preview, preflight, core request mapping, and execution. It applies SDK
defaults, validates product surfaces, attaches workspace and harness context,
maps into the core `AgentRequest`, and returns an `AgentRunPreparation` with
the shared readiness result.

`workbench()` returns both the compatibility `commandPalette()` strings and
structured `commands()` metadata from `WayangWorkbenchCatalog`. Product shells
can use command ids, categories, descriptions, surface hints, and local-only
flags without scraping rendered terminal text.
`WayangWorkbenchCatalog.*CommandsForSurface(...)` filters those commands to the
general entries plus one product surface's recommendations.

`AgentRunSpec.fromProperties(...)` maps Java properties into the same
`AgentRunRequest` builder. This gives CI jobs, Gamelan workflow nodes, and other
product shells a dependency-light way to share saved Wayang run defaults while
still letting CLI flags override individual fields.
`AgentRunSpec.formatProperties(...)` performs the reverse projection with a
stable key order so resolved requests can be inspected or passed to other
tooling as plain properties.
`AgentRunSpec.template(...)` creates a starter request from the SDK-owned
surface policy, including suggested skills plus preferred workspace, harness,
and workflow defaults for that surface.
`WayangRunSpec` wraps that request projection with launch-level fields:
`specVersion=1`, optional `profileId=<id>`, and optional `requireReady=true`.
`profileId` selects reusable product defaults from `ProductProfile` entries such
as `coding-agent`, `openclaw-agent`, `assistant-agent`, `workflow-agent`,
`low-code-agent`, or `platform-admin`; explicit request keys override the
profile template. Missing `specVersion` remains compatible with legacy
request-only specs, while unsupported versions fail early.
`WayangRunSpecService` wraps those parser/formatter contracts with UTF-8 file
read/write, template writing, parent-directory creation, and explicit overwrite
protection so CLI, TUI, API launchers, and workflow nodes share one spec
lifecycle.
