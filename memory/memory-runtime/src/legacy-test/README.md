Legacy memory runtime tests live here while the specialized memory slice
executors remain outside the active memory parent.

The tests under `java/tech/kayys/wayang/memory/executor` target inactive
short-term, long-term, and working-memory executors that currently live in
separate legacy modules. Move them back into `src/test/java` only when those
executor modules are migrated onto active Wayang memory contracts.
