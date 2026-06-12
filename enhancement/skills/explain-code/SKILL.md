---
name: explain-code
description: Explain what code does, how it works, why it's designed a certain way. Activate when the user asks to explain, understand, describe, or clarify how code works.
license: Apache-2.0
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file search_files list_dir
---

# Explain Code Skill

## When to activate
- "what does this do?", "explain this code"
- "how does X work?", "walk me through this"
- "what is this class/function for?"
- "I don't understand this part"

## Explanation approach

### For a function/method
1. **Purpose**: what problem does it solve? (1 sentence)
2. **Inputs**: what does each parameter represent?
3. **Algorithm**: step-by-step walk-through of the logic
4. **Output**: what does it return and when?
5. **Edge cases**: what happens with null, empty, negative, boundary values?

### For a class/module
1. **Responsibility**: single sentence using "This class is responsible for..."
2. **Key fields**: what state does it hold and why?
3. **Key methods**: what are the most important operations?
4. **Relationships**: what does it depend on? What depends on it?

### For an architecture
1. **Components**: list the main pieces
2. **Data flow**: trace how data moves from input to output
3. **Design decisions**: explain non-obvious choices (why not X instead?)

## Style rules
- Use analogies when appropriate ("this is like a cache — it stores results so we don't recompute")
- Show concrete examples with real values
- Highlight gotchas and non-obvious behavior
- When the code is bad, say so diplomatically and suggest better alternatives
