# Legacy Gamelan Backend Adapter

This source tree is intentionally not compiled by Maven.

The preserved adapter used older Gamelan operation names such as
`createRun`, `startRun`, and record-style DTO accessors. The active adapter in
`src/main/java` now targets the current Gamelan SDK client shape:

- `runs().create(workflowId).execute()`
- `runs().start(runId)`
- JavaBean-style `RunResponse` and `ExecutionHistory` accessors
- nested Wayang `WorkflowTypes` value records

Keep this directory only as migration reference material.
