# Legacy Agent Orchestration Sources

This source tree is intentionally not compiled by Maven.

The classes here still depend on removed Gollek-native SDK, inference, tool, and
memory contracts. Keep them as migration reference material, then move behavior
back into `src/main/java` only after it is expressed through the active Wayang
boundaries:

- `agent-spi` for agent requests, responses, state, events, and inference-facing
  value records.
- `tools/tools-spi` for executable tool contracts.
- `agent-core` for concrete runtime services and adapters.

Avoid adding new dependencies to make these files compile in place. The active
module should stay backend-agnostic and provider-neutral.
