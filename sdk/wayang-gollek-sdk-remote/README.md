# Wayang Remote SDK

`wayang-gollek-sdk-remote` provides the remote `WayangGollekSdkProvider`.
It is intentionally separate from the default CLI module so local installs keep
working without remote API behavior on the classpath.

## HTTP Shape

- `GET /status` verifies the remote Wayang API is reachable.
- `POST /workspace/inspect` requests workspace context from the remote platform.
- `POST /harness/plan` requests verification-check planning from the remote platform.
- `POST /runs` submits an agent run request using the SDK request model,
  including prompt/system prompt, surface id, session/user identity, caller
  context, workspace flags, harness flags, memory settings, skills, tenant,
  model, and workflow ids.

Workbench and command discovery use the SDK-owned remote-safe command catalog.
`workbench(query)` and `commandDiscovery(query)` therefore preserve local/remote
parity while excluding local-only entries such as `tui`.

The first implementation keeps response mapping deliberately small: it returns
stable SDK envelopes and includes HTTP status plus response preview metadata.
Future API modules can expand this without changing CLI command code.

Remote SDK dry-runs use `previewRun(...)` to normalize the request with remote
tenant/model defaults without posting to `/runs`, making CLI `--dry-run` safe
for API-backed deployments.
