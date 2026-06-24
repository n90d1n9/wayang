# Wayang Code Extension Strategy

Wayang Code is the terminal coding-agent surface for the Wayang agentic platform. It runs on top of Gollek for inference and can use Gamelan for workflow execution, but the extension contract belongs to the Wayang SDK so CLI, API, server, and future UI surfaces share one source of truth.

## Separation Of Concern

- `sdk/wayang-gollek-sdk`: owns stable contracts such as `WayangCodeAgentContext`, `WayangCodeAgentExtension`, `WayangCodeAgentContribution`, and diagnostics/discovery types.
- `cli/wayang-gollek-cli`: owns terminal rendering, REPL commands, local process bridging, and prompt application.
- Extension jars: own pro or enterprise behavior such as billing, metrics, multitenant policy, advanced skills, richer tools, audit, or hosted control-plane integration.
- Gollek: remains the inference, serving, and training engine under Wayang.
- Gamelan: remains the workflow engine used by Wayang when agentic runs need workflow orchestration.

## Extension Loading

Extensions are discovered with Java `ServiceLoader`:

```text
META-INF/services/tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtension
```

The implementation class can inspect `WayangCodeAgentContext`, decide whether it supports that session, then return a declarative `WayangCodeAgentContribution`.

## Contribution Boundaries

Extensions should contribute declarative behavior:

- system prompt additions
- slash command hints
- metadata for metrics, billing, policy, or audit
- diagnostics for discoverability

Extensions should not directly own the CLI loop, stdout formatting, local file editing, or SDK persistence. Those concerns stay with the active surface and core SDK services.

## Pro And Enterprise Examples

- `wayang-pro-billing-extension`: quota checks, plan metadata, billing command hints.
- `wayang-enterprise-audit-extension`: audit trail prompts, tenant labels, event sink metadata.
- `wayang-enterprise-policy-extension`: governance requirements and restricted-tool guidance.
- `wayang-pro-observability-extension`: metrics tags and hosted dashboard links.
- `wayang-enterprise-skill-pack`: richer coding, data, workflow, or domain-specific tools.

This keeps the OSS engine useful on its own while allowing richer editions to attach without forking `wayang code`.
