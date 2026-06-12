# Legacy Built-In Skills

This source tree is intentionally not compiled by Maven.

The preserved implementations targeted an older runtime skill surface based on
`SkillContext`, `SkillResult`, `tech.kayys.gollek.engine.inference.InferenceService`,
and removed agent-local embedding request types. The active skills under
`src/main/java` now implement the current `tech.kayys.wayang.agent.spi.AgentSkill`
map-based contract and use active Wayang boundaries such as `InferenceBackend`
and `EmbeddingService`.

Keep this directory only as migration reference material.
