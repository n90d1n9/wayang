# Legacy Agent Adapters

This source tree is intentionally not compiled by Maven.

The preserved adapters targeted pre-reorganization compatibility packages,
including alias-style imports that are not valid Java, removed audit/analytics
contracts, and placeholder skill conversion methods. The active adapter module
now stays on current Wayang SPI contracts and avoids CDI bean registration by
default so it cannot conflict with the canonical `agent-core` services.

Keep this directory only as migration reference material.
