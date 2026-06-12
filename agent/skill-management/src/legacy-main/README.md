# Legacy Skill Management

This source tree is intentionally not compiled by Maven.

The preserved implementation targeted the old `tech.kayys.gollek` package tree
and a removed `agent-skills-repo` repository SPI. The active module now provides
a smaller Wayang-native lifecycle service around the current
`tech.kayys.wayang.agent.spi.skills.SkillRegistry` contract.

Keep this directory only as migration reference material.
