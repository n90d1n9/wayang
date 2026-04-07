# Modular Guardrails Plugin System - Multi-Module Architecture

This document describes the multi-module architecture for the guardrails plugin system in the Wayang platform.

## Overview

The guardrails system has been restructured into a multi-module architecture that separates concerns and allows for independent development, testing, and deployment of different guardrail capabilities.

## Module Structure

### Core Modules

1. **guardrails-core** - Contains the core interfaces, registries, and orchestrators
2. **guardrails-runtime** - Contains runtime execution logic
3. **guardrails-plugins** - Parent module for all guardrail plugins

### Plugin Modules

Each plugin now has its own module:

1. **guardrails-plugin-api** - Shared interfaces for all guardrail plugins
2. **pii-detector-plugin** - PII detection functionality
3. **toxicity-detector-plugin** - Toxicity detection functionality
4. **rate-limit-policy-plugin** - Rate limiting policy enforcement

## Benefits of Multi-Module Architecture

1. **Independent Development** - Each plugin can be developed separately
2. **Selective Deployment** - Deploy only the plugins you need
3. **Easier Testing** - Each module can be tested independently
4. **Better Dependency Management** - Clear separation of concerns
5. **Scalability** - Easy to add new plugins without affecting others

## Adding New Plugins

To add a new plugin:

1. Create a new module under `guardrails-plugins/`
2. Follow the naming convention: `{plugin-name}-plugin`
3. Implement the appropriate interface (`GuardrailDetectorPlugin` or `GuardrailPolicyPlugin`)
4. Add the module to the parent `pom.xml` in `guardrails-plugins`
5. Add the dependency to `guardrails-core/pom.xml` if you want it included by default

Example plugin module structure:
```
my-new-plugin/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── tech/
                └── kayys/
                    └── wayang/
                        └── guardrails/
                            └── detector/
                                └── impl/
                                    └── MyNewDetectorPlugin.java
```

Example pom.xml for a new plugin:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>tech.kayys.wayang</groupId>
        <artifactId>wayang-guardrails-plugins</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>wayang-my-new-plugin</artifactId>
    <name>Wayang :: Guardrails :: My New Plugin</name>

    <dependencies>
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-guardrails</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>io.smallrye.reactive</groupId>
            <artifactId>mutiny</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.enterprise</groupId>
            <artifactId>jakarta.enterprise.cdi-api</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>

</project>
```

## Plugin Development Guidelines

### Detector Plugins
- Implement `GuardrailDetectorPlugin` interface
- Annotate with `@ApplicationScoped` for CDI discovery
- Specify applicable phases (PRE_EXECUTION, POST_EXECUTION, or both)
- Return appropriate `DetectionResult` based on findings

### Policy Plugins
- Implement `GuardrailPolicyPlugin` interface
- Annotate with `@ApplicationScoped` for CDI discovery
- Specify applicable phases (PRE_EXECUTION, POST_EXECUTION, or both)
- Return appropriate `PolicyCheckResult` based on evaluation

## Build and Deployment

To build the entire guardrails system:
```bash
mvn clean install -f wayang/executors/guardrails/pom.xml
```

To build only specific plugins:
```bash
mvn clean install -f wayang/executors/guardrails/guardrails-plugins/pii-detector-plugin/pom.xml
```

## Configuration

Plugins are automatically discovered through CDI injection. Simply include the plugin's JAR in the classpath and it will be picked up by the `GuardrailPluginRegistry`.

For selective plugin loading, you can customize the CDI producer or use CDI qualifiers to enable/disable specific plugins based on configuration.