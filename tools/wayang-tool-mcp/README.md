MCP module provides MCP server tooling for Wayang.

## Tool Call History Retention

MCP tool-call history is retained in memory per run and is pruned lazily on reads/writes.
The module also runs a scheduled all-run prune so quiet runs do not keep expired entries
until the next API request.

Configuration:

| Property | Default | Description |
| --- | --- | --- |
| `wayang.mcp.tool-call-history.retention` | `P7D` | Retention window for tool-call history entries. ISO-8601 durations and simple values like `5m`, `12h`, or `7d` are accepted. |
| `wayang.mcp.tool-call-history.max-entries-per-run` | `500` | Maximum retained entries per run, capped at `10000`. |
| `wayang.mcp.tool-call-history.scheduled-prune.enabled` | `true` | Enables the background all-run prune. |
| `wayang.mcp.tool-call-history.scheduled-prune.interval` | `5m` | Scheduler interval for pruning expired history. |
| `wayang.mcp.tool-call-history.scheduled-prune.delayed` | `45s` | Initial scheduler delay after application startup. |

Storage stats include retained entry and run counts. They are available per run at
`GET /mcp/tools/calls/stats` and across all runs at `GET /mcp/tools/calls/stats/all`.
Expired entries can be pruned per run with `POST /mcp/tools/calls/prune-expired`
or across all runs with `POST /mcp/tools/calls/prune-expired/all`.

## Server Action Execution History Retention

MCP server-action execution history uses the same in-memory retention model per request.
It also has a scheduled all-request prune for expired action execution records.

Configuration:

| Property | Default | Description |
| --- | --- | --- |
| `wayang.mcp.action-history.retention` | `P7D` | Retention window for server-action execution history entries. ISO-8601 durations and simple values like `5m`, `12h`, or `7d` are accepted. |
| `wayang.mcp.action-history.max-entries-per-request` | `500` | Maximum retained entries per request, capped at `10000`. |
| `wayang.mcp.action-history.scheduled-prune.enabled` | `true` | Enables the background all-request prune. |
| `wayang.mcp.action-history.scheduled-prune.interval` | `5m` | Scheduler interval for pruning expired action history. |
| `wayang.mcp.action-history.scheduled-prune.delayed` | `50s` | Initial scheduler delay after application startup. |

Storage stats include retained entry and request counts. They are available per request
at `GET /mcp/servers/actions/executions/stats` and across all requests at
`GET /mcp/servers/actions/executions/stats/all`.
Expired entries can be pruned per request with
`POST /mcp/servers/actions/executions/prune-expired` or across all requests with
`POST /mcp/servers/actions/executions/prune-expired/all`.
