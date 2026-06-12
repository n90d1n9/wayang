# Legacy Skills CLI

This source tree is intentionally not compiled by Maven.

The preserved CLI targeted the old `tech.kayys.gollek` package tree and removed
external skill store/installer helpers. The active CLI now stays Wayang-native
and delegates to `SkillManagementService` plus the current `SkillRegistry` SPI.

Keep this directory only as migration reference material.
