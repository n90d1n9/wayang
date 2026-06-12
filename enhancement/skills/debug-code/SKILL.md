---
name: debug-code
description: Debug errors, exceptions, stack traces, and test failures. Activate when the user reports a bug, error, crash, exception, test failure, or unexpected behavior.
license: Apache-2.0
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file search_files run_command git
---

# Debug Code Skill

## When to activate
- "I'm getting an error / exception / NPE / stack trace"
- "This test is failing"
- "Why is this crashing?"
- "Something is wrong with X"

## Debugging methodology

### Step 1: Understand the error
Always read the full error message and stack trace. Never guess without seeing it.

### Step 2: Locate the source
Use the stack trace to identify the exact file and line. Read that file, plus
any files it calls at that point.

### Step 3: Form hypotheses
Based on the code:
1. What assumptions does the code make that might be violated?
2. What is the actual value vs. expected value?
3. Is this a logic error, null check, type mismatch, concurrency issue, or config error?

### Step 4: Verify with data
- For runtime errors: add logging or run with debug flags
- For test failures: read the test and the production code together
- For intermittent issues: check for concurrency, shared mutable state, race conditions

### Step 5: Fix minimally
Use `apply_patch` for targeted fixes. Do NOT rewrite working code.

## Common patterns
- **NPE**: Check for null before use. Consider Optional.
- **ClassCastException**: Check generics, instanceof before cast.
- **ConcurrentModificationException**: Never modify a collection you're iterating.
- **StackOverflowError**: Infinite recursion — find the missing base case.
- **OutOfMemoryError**: Look for unbounded collections, memory leaks, infinite streams.
- **Connection refused**: Check if the service is running and on the expected port.

## After fixing
Always verify: run the failing test or reproduce the bug scenario again.
Report what was wrong and what you changed.
