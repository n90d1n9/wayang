Level	Description	When to use	Considerations

# Direct model call	
A single language model call with a well-crafted prompt. No agent logic, no tool access.	
## When to use
Classification, summarization, translation, and other single-step tasks that the model can complete in one pass.	
## Considerations
The least complex option. If prompt engineering can solve the problem, you don't need an agent.


# Single agent with tools	
One agent that reasons and acts by selecting from available tools, knowledge sources, and APIs. 
## When to use
The agent can loop through multiple model calls and tool invocations to refine results.	Varied queries within a single domain where some requests require dynamic tool use, such as looking up order status or querying a database.	
## Considerations
Often the right default for enterprise use cases. Simpler to debug and test than multi-agent setups, while still allowing dynamic logic. Guard against infinite tool-call loops by setting iteration limits.


# Multi-agent orchestration	
Multiple specialized agents coordinate to solve a problem. An orchestrator or peer-based protocol manages work distribution, context sharing, and result aggregation.	
## When to use
Cross-functional or cross-domain problems, scenarios that require distinct security boundaries per agent, or tasks that benefit from parallel specialization.	
## Considerations
Adds coordination overhead, latency, and failure modes. Justify the added complexity by demonstrating that a single agent can't reliably handle the task due to prompt complexity, tool overload, or security requirements.