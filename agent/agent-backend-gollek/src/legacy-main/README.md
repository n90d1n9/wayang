# Legacy Gollek Backend Adapter

This source tree is intentionally not compiled by Maven.

The preserved adapter used a removed intermediate
`tech.kayys.wayang.agent.spi.inference` package and assumed a
`GollekSdk.builder()` API. The active adapter in `src/main/java` now maps the
Wayang `agent-spi` contracts directly to the current Gollek SDK/SPI boundary.

Keep this directory only as migration reference material. New behavior should
be added to the active adapter or a focused mapper class.
