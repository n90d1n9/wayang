
## 🛡️ **10. Guardrails Service (wayang-guardrails)**

### **Purpose**
Safety, compliance, PII detection, content moderation, and policy enforcement.

### **Project Structure**

```
wayang-guardrails/
├── pom.xml
└── src/main/java/tech/kayys/wayang/guardrails/
    ├── resource/
    │   └── GuardrailsResource.java
    ├── service/
    │   ├── GuardrailsService.java
    │   ├── PolicyEngine.java
    │   └── DetectorOrchestrator.java
    ├── detector/
    │   ├── PIIDetector.java
    │   ├── ToxicityDetector.java
    │   ├── BiasDetector.java
    │   └── HallucinationDetector.java
    ├── policy/
    │   ├── CELPolicyEvaluator.java
    │   └── PolicyRepository.java
    └── redactor/
        └── ContentRedactor.java
```
