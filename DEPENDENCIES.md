# Wayang-Gollek Dependency Policy

**Last updated**: 2026-04-06  
**Version**: 1.0  
**Enforced by**: Maven Enforcer Plugin (v3.5.0)

---

## Purpose

This document defines the allowed and banned dependencies for `wayang-gollek` modules to ensure:
- ✅ **Backend agnosticism** — Modules depend on SDK/SPI, not implementations
- ✅ **Standalone capability** — Can run without full engine runtime
- ✅ **Minimal footprint** — No unnecessary transitive dependencies
- ✅ **Clean architecture** — SPI-first, implementations as adapters

---

## Dependency Rules

### 🔴 Banned Dependencies

| Dependency | Reason | Replacement |
|------------|--------|-------------|
| `gamelan-engine-core` | Pulls full Quarkus engine runtime, prevents standalone usage | `gamelan-engine-spi` + `gamelan-sdk-executor-core` |
| `gamelan-engine-grpc` | Implementation detail, not needed by executor modules | Use SDK client interfaces |
| `gamelan-engine-kafka` | Implementation detail, not needed by executor modules | Use SDK client interfaces |
| `gollek-engine` (implementation) | Pulls full inference runtime | `gollek-spi*` modules + `gollek-sdk` |

### ✅ Allowed Dependencies

#### Gamelan SDK Modules

| Module | Purpose | When to Use |
|--------|---------|-------------|
| `gamelan-engine-spi` | Node/workflow/executor interfaces | All modules needing workflow types |
| `gamelan-sdk-executor-core` | `WorkflowExecutor` base interface, transport abstraction | All executor implementations |
| `gamelan-sdk-client-core` | `GamelanClient` fluent API, workflow operations | Modules creating/starting workflows |
| `gamelan-sdk-client-local` | In-JVM client (direct engine calls) | Runtime modules deployed with engine |
| `gamelan-sdk-client-remote` | REST/gRPC client | Distributed deployments |
| `gamelan-sdk-executor-local` | Vert.x EventBus transport | In-JVM executor deployments |
| `gamelan-sdk-executor-remote` | gRPC/Kafka transport | Distributed executor deployments |
| `gamelan-plugin-spi` | Plugin interfaces and types | Plugin-based modules |

#### Gollek SDK Modules

| Module | Purpose | When to Use |
|--------|---------|-------------|
| `gollek-spi` | Core inference types | All inference-related modules |
| `gollek-spi-inference` | Inference request/response | Inference executors |
| `gollek-spi-provider` | Provider interfaces | Provider routing modules |
| `gollek-spi-multimodal` | Multimodal types | Vision/audio modules |
| `gollek-spi-plugin` | Plugin interfaces | Plugin-based modules |
| `gollek-sdk` | Gollek SDK facade | Inference client modules |
| `gollek-sdk-java-local` | Local inference implementation | In-JVM inference deployments |
| `gollek-sdk-java-remote` | Remote inference implementation | Distributed inference |

#### Internal Wayang-Gollek Modules

| Module | Purpose | When to Use |
|--------|---------|-------------|
| `wayang-memory-core` | Memory interfaces | Modules needing memory access |
| `wayang-tool-core` | Tool interfaces | Tool-related modules |
| `wayang-vector-core` | Vector store interfaces | Vector search modules |
| `wayang-plugin-spi` | Plugin SPI | Plugin modules |
| `wayang-schema-core` | Schema definitions | Modules needing schema validation |
| `wayang-error-spi` | Error types | All modules (standardized errors) |

### ⚠️ Conditional Dependencies

| Dependency | Condition | Notes |
|------------|-----------|-------|
| `quarkus-arc` | Only in `runtime-*` modules | CDI container — must be `provided` scope in libraries |
| `quarkus-rest*` | Only in modules exposing REST APIs | Not needed for pure executor modules |
| `quarkus-hibernate-reactive-panache` | Only in modules with database access | Use `provided` scope in libraries |
| `io.smallrye.reactive:mutiny` | All modules | Reactive programming — core dependency |

---

## Module Dependency Profiles

### Profile 1: Pure SPI Modules

**Examples**: `agent-spi/`, `skill-spi/`, `tools-spi/`

**Allowed**:
- Other SPI modules only
- `jakarta.enterprise.cdi-api` (scope: `provided`)
- `io.smallrye.reactive:mutiny` (if reactive types needed)
- Standard Java libraries (SLF4J, Jackson, etc.)

**Banned**:
- Any `*-core` implementation modules
- Quarkus runtime extensions
- Engine implementations

---

### Profile 2: Executor Modules

**Examples**: `wayang-tool-runtime/`, `wayang-vector-runtime/`, memory executors

**Allowed**:
- `gamelan-sdk-executor-core` (required)
- `gamelan-engine-spi` (for node/workflow types)
- Internal SPI modules (`wayang-tool-core`, etc.)
- `quarkus-arc` (scope: `compile` — Quarkus extension)
- Mutiny

**Banned**:
- `gamelan-engine-core`
- `gamelan-engine-grpc`
- `gamelan-engine-kafka`

---

### Profile 3: Runtime/Integration Modules

**Examples**: `memory-runtime/`, `rag-runtime/`

**Allowed**:
- Everything in Profile 2
- `gamelan-sdk-client-core` or `gamelan-sdk-client-local`
- Quarkus extensions (REST, Hibernate, etc.)
- Database clients (PostgreSQL, Redis, etc.)

**Banned**:
- `gamelan-engine-core` (still banned — use SDK client)

---

### Profile 4: Agent Core Modules

**Examples**: `agent/agent-core/`, `agent/wayang-agent-core/`

**Allowed**:
- Gollek SDK (`gollek-sdk`, `gollek-spi*`)
- Gamelan SDK (`gamelan-sdk-client-core`, `gamelan-sdk-executor-core`)
- Internal wayang-gollek modules (memory, tools, skills, etc.)
- Quarkus extensions (for Quarkus deployment)

**Banned**:
- `gamelan-engine-core`
- Direct engine implementation classes

---

## Maven Enforcer Configuration

The following enforcer rule is configured in the parent POM:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>enforce-sdk-only-dependencies</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <bannedDependencies>
                        <searchTransitive>true</searchTransitive>
                        <excludes>
                            <exclude>tech.kayys.gamelan:gamelan-engine-core</exclude>
                        </excludes>
                        <message>
                            ❌ VIOLATION: wayang-gollek modules must NOT depend on gamelan-engine-core.
                            Use gamelan-engine-spi and gamelan-sdk-*-core instead.
                        </message>
                    </bannedDependencies>
                </rules>
                <fail>true</fail>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Running Enforcement

```bash
# Check dependencies
mvn enforcer:enforce

# Full build with enforcement
mvn clean install

# Check dependency tree
mvn dependency:tree -Dincludes=tech.kayys.gamelan:*
```

---

## Violation Resolution Guide

### If you see this error:

```
[ERROR] Rule 0: org.apache.maven.plugins.enforcer.BannedDependencies failed with message:
❌ VIOLATION: wayang-gollek modules must NOT depend on gamelan-engine-core.
```

### Resolution Steps:

1. **Identify the violation source**:
   ```bash
   mvn dependency:tree | grep gamelan-engine-core
   ```

2. **Replace with SPI/SDK**:
   - Remove: `gamelan-engine-core`
   - Add: `gamelan-engine-spi` + `gamelan-sdk-executor-core` (or `gamelan-sdk-client-core`)

3. **Update imports**:
   - Change: `import tech.kayys.gamelan.core.*` → `import tech.kayys.gamelan.engine.*` (SPI package)
   - Most node/workflow/executor types are in `gamelan-engine-spi`

4. **Verify**:
   ```bash
   mvn clean install
   ```

---

## Transitive Dependency Management

### Excluding Transitive Violations

If a dependency pulls in `gamelan-engine-core` transitively:

```xml
<dependency>
    <groupId>tech.kayys.wayang</groupId>
    <artifactId>some-module</artifactId>
    <version>${project.version}</version>
    <exclusions>
        <exclusion>
            <groupId>tech.kayys.gamelan</groupId>
            <artifactId>gamelan-engine-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### Dependency Convergence

Run convergence checks to ensure consistent versions:

```bash
mvn enforcer:enforce -Drules=requireUpperBoundDeps
```

---

## Dependency Audit Trail

### Changes Log

| Date | Change | Reason |
|------|--------|--------|
| 2026-04-06 | Initial policy created | Phase 1 of agnostic framework roadmap |
| 2026-04-06 | Banned `gamelan-engine-core` | Prevents standalone agent generation |
| 2026-04-06 | Added Maven Enforcer rules | Automated enforcement |

### Modules Fixed

| Module | Previous Dependency | Fixed To | Status |
|--------|-------------------|----------|--------|
| `memory/memory-core` | `gamelan-engine-core` | `gamelan-engine-spi` | ✅ Fixed |
| `memory/memory-runtime` | `gamelan-engine-core` | `gamelan-engine-spi` | ✅ Fixed |
| `tools/wayang-tool-runtime` | `gamelan-engine-core` | `gamelan-engine-spi` | ✅ Fixed |

---

## Questions?

- **Why is `gamelan-engine-core` banned?** — It pulls the full Quarkus engine runtime, making standalone agent generation impossible.
- **What if I need engine features not in SPI?** — Add them to `gamelan-engine-spi` first, then use in your module.
- **Can I request an exception?** — Open an issue with justification. Exceptions require architecture review.

---

*This policy is automatically enforced by the Maven Enforcer plugin. Violations will cause build failures.*
