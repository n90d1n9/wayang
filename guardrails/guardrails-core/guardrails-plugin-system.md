# Modular Guardrails Plugin System

This document describes the modular guardrails plugin system that enables easy addition of new guardrail features in the Wayang platform.

## Overview

The guardrails system has been refactored to support a plugin architecture that allows for:
- Easy addition of new detection capabilities
- Dynamic loading of guardrail plugins
- Flexible configuration of when and how guardrails are applied
- Support for both detection and policy enforcement plugins

## Architecture

### Core Components

1. **GuardrailDetectorPlugin** - Interface for content detection plugins
2. **GuardrailPolicyPlugin** - Interface for policy enforcement plugins
3. **GuardrailPluginRegistry** - Central registry for discovering and managing plugins
4. **DetectorOrchestrator** - Orchestrates the execution of detector plugins
5. **PolicyEngine** - Evaluates policy plugins alongside traditional policies

### Plugin Types

#### Detector Plugins
- Implement `GuardrailDetectorPlugin` interface
- Used for content analysis (PII, toxicity, bias, etc.)
- Can operate in PRE_EXECUTION or POST_EXECUTION phases
- Return `DetectionResult` indicating safety level

#### Policy Plugins
- Implement `GuardrailPolicyPlugin` interface
- Used for access control, rate limiting, etc.
- Can operate in PRE_EXECUTION or POST_EXECUTION phases
- Return `PolicyCheckResult` indicating whether action is allowed

## Creating New Guardrail Plugins

### Creating a Detector Plugin

To create a new detector plugin, implement the `GuardrailDetectorPlugin` interface:

```java
@ApplicationScoped
public class CustomDetectorPlugin implements GuardrailDetectorPlugin {
    
    @Override
    public String id() {
        return "custom-detector-plugin";
    }

    @Override
    public String name() {
        return "Custom Detector Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Description of what this detector does";
    }

    @Override
    public CheckPhase[] applicablePhases() {
        return new CheckPhase[]{CheckPhase.PRE_EXECUTION, CheckPhase.POST_EXECUTION};
    }

    @Override
    public Uni<DetectionResult> detect(String text) {
        // Implement detection logic here
        // Return appropriate DetectionResult
    }

    @Override
    public String getCategory() {
        return "custom-category";
    }

    @Override
    public DetectionSeverity getSeverity() {
        return DetectionSeverity.MEDIUM;
    }
}
```

### Creating a Policy Plugin

To create a new policy plugin, implement the `GuardrailPolicyPlugin` interface:

```java
@ApplicationScoped
public class CustomPolicyPlugin implements GuardrailPolicyPlugin {
    
    @Override
    public String id() {
        return "custom-policy-plugin";
    }

    @Override
    public String name() {
        return "Custom Policy Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Description of what this policy enforces";
    }

    @Override
    public Uni<PolicyCheckResult> evaluate(NodeContext context) {
        // Implement policy evaluation logic here
        // Return appropriate PolicyCheckResult
    }

    @Override
    public String getCategory() {
        return "custom-policy";
    }

    @Override
    public CheckPhase[] applicablePhases() {
        return new CheckPhase[]{CheckPhase.PRE_EXECUTION};
    }
}
```

## Built-in Plugins

The system comes with several built-in plugins:

1. **PIIDetectorPlugin** - Detects personally identifiable information
2. **ToxicityDetectorPlugin** - Detects toxic content
3. **RateLimitPolicyPlugin** - Enforces rate limiting policies

## Configuration

Plugins are automatically discovered through CDI injection. Simply annotate your plugin implementation with `@ApplicationScoped` and implement the appropriate interface.

## Extensibility Points

The plugin system provides several extensibility points:

1. **New Detection Categories** - Add new types of content analysis
2. **New Policy Types** - Add new access control mechanisms
3. **Custom Enforcement Actions** - Define custom responses to violations
4. **Integration Points** - Connect with external systems for enhanced capabilities

## Best Practices

1. Always implement proper error handling in plugin methods
2. Use asynchronous operations (Uni) for I/O-bound operations
3. Implement appropriate caching for performance-sensitive operations
4. Follow the existing patterns for logging and monitoring
5. Ensure thread safety in plugin implementations