# Legacy Agent-Core Tests

This tree keeps pre-reorganization agent-core tests that still target removed or
renamed contracts such as old `core.spi`, inference DTO, orchestration, and
skill adapter APIs.

Tests in this directory are intentionally not compiled by Maven. Migrate them
back into `src/test/java` one API boundary at a time after the covered runtime
surface is aligned with the active `agent-spi`, `agent-core`, and tool SPI
contracts.
