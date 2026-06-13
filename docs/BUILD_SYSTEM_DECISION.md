# Build System Decision: Maven vs Gradle

## Context
Wayang-Gollek runs embedded on top of Gollek (https://github.com/bhangun/gollek), which uses Gradle with modular/custom build modules.

## Decision: Keep Maven (But Modernize)

### Why NOT Switch to Gradle?

#### 1. Already Solved the Hard Problems ✅
- **Dependency Policy**: Excellent `DEPENDENCIES.md` with automated enforcement via Maven Enforcer
- **SPI/SDK Separation**: Clean architecture already enforced at build level
- **Multi-module Reactor**: 20+ modules working correctly
- **Profile-based Editions**: Community vs Enterprise addons working via Maven profiles

#### 2. Maven Strengths for This Architecture
- **BOM Management**: Quarkus BOM import works flawlessly
- **Enforcement Rules**: Maven Enforcer v3.5.0 mature for banned dependencies
- **Reactor Stability**: Proven for large multi-module Java projects
- **Enterprise CI/CD**: Better integration with Jenkins, Artifactory, Nexus

#### 3. Switching Costs Outweigh Benefits
- **Conversion Effort**: 20+ modules × complex dependency rules = weeks of work
- **Risk**: Could break carefully crafted dependency boundaries
- **Learning Curve**: Team familiarity with current Maven setup
- **No Functional Gain**: Gradle wouldn't enable anything Maven can't do here

### What TO Improve in Maven

#### Priority 1: Add Gradle-like Convenience Features

```bash
# Create wrapper scripts (like Gradle Wrapper)
mvn -N wrapper:wrapper -Dmaven=3.9.6
```

**Benefits:**
- Consistent Maven version across dev/CI
- No manual Maven installation needed
- Same DX as `./gradlew`

#### Priority 2: Custom Build Logic via Maven Plugins

Instead of Gradle's custom build logic, use:

1. **Build Helper Plugin** - For custom source sets
2. **Exec Plugin** - For custom tasks
3. **AntRun Plugin** - For complex scripting
4. **Groovy/BeanShell** - For dynamic logic in POMs

Example: Add to parent POM:
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>custom-validation</id>
            <phase>validate</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>./scripts/validate-modules.sh</executable>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### Priority 3: Faster Builds

```xml
<!-- Add to parent POM -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <useIncrementalCompilation>false</useIncrementalCompilation>
        <optimize>true</optimize>
    </configuration>
</plugin>
```

**Build parallelism:**
```bash
mvn -T 1C clean install  # One thread per core
```

#### Priority 4: Better Developer Experience

Create `Makefile` or shell scripts for common tasks:

```makefile
.PHONY: build test validate clean

build:
	./mvnw -T 1C clean install -DskipTests

test:
	./mvnw -T 1C test

validate:
	./mvnw enforcer:enforce

clean:
	./mvnw clean

# Module-specific builds
build-agent:
	./mvnw -pl agent/agent-core,agent/agent-spi -am build

build-runtime:
	./mvnw -pl runtime-quarkus -am build
```

#### Priority 5: Consider Gradle for Specific Subprojects ONLY

If certain modules need Gradle-specific features:
- **Dynamic code generation**
- **Complex custom build logic**
- **Android/Kotlin Multiplatform**

Then consider a **hybrid approach**:
- Keep core framework in Maven
- Use Gradle for specific tooling/CLI modules
- Publish both to same repo

### When WOULD Switching to Gradle Make Sense?

Consider Gradle if:
1. ❌ You need **incremental compilation** across modules (Maven's is limited)
2. ❌ You need **complex custom build logic** that can't be done with plugins
3. ❌ You're adding **Kotlin/Android** modules
4. ❌ Your team **already knows Gradle better**
5. ❌ You need **faster cold builds** (Gradle's cache is superior)

### Hybrid Approach (Best of Both Worlds)

If Gollek integration requires tighter coupling:

```
wayang-gollek/          (Maven - core framework)
├── agent/
├── tools/
└── ...

gollek-integration/     (Gradle - integration layer)
├── wayang-gollek-bridge/
└── custom-build-tasks/
```

**Benefits:**
- Keep Maven for stable framework
- Use Gradle for experimental/integration work
- Publish both to same artifact repository

## Implementation Plan

### Week 1: Maven Wrapper + Scripts ✅ COMPLETED
- [x] Add Maven Wrapper (v3.9.6)
- [x] Create `Makefile` with common tasks
- [x] Fixed Quarkus BOM property references in 8 POM files
- [x] Validated build works: `mvn validate` passes in ~35s offline

### Week 2: Build Performance
- [ ] Enable parallel builds by default
- [ ] Add build scan support (Develocity)
- [ ] Profile slow modules

### Week 3: Developer Experience
- [ ] Add IDE configuration scripts
- [ ] Create module-specific build commands
- [ ] Document common troubleshooting

### Month 2: Evaluate Integration Needs
- [ ] Assess Gollek integration pain points
- [ ] Consider hybrid approach if needed
- [ ] Test Gradle for specific subprojects

## Conclusion

**Stay with Maven** but invest in making it as developer-friendly as Gradle:
- ✅ Add wrapper scripts
- ✅ Create convenience commands
- ✅ Enable parallel builds
- ✅ Add build scans for performance insights
- ✅ Document common patterns

Only consider Gradle if you hit specific limitations that can't be solved with Maven plugins.

---

*Last updated: 2026-06-12*
*Decision owner: Architecture Team*
