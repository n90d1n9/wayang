---
name: git-workflow
description: Git operations, branch management, commit messages, PR descriptions, and merge conflict resolution. Activate when the user asks about git, commits, branches, diffs, history, or merges.
license: Apache-2.0
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: git run_command read_file
---

# Git Workflow Skill

## When to activate
- "commit this", "what changed", "show the diff"
- "create a branch", "how do I merge"
- "write a PR description"
- "resolve this conflict"

## Git operations

### Before committing
Always check what's staged: `git status` then `git diff --staged`

### Commit messages
Follow Conventional Commits:
```
<type>(<scope>): <short summary>

<optional body explaining why, not what>

<optional footer: BREAKING CHANGE, Fixes #123>
```

Types: `feat` `fix` `refactor` `test` `docs` `chore` `perf` `ci`

### Branch naming
`feature/description`, `fix/issue-description`, `chore/description`

### PR descriptions
Structure:
1. **What**: one sentence summary
2. **Why**: motivation / problem solved
3. **How**: key implementation decisions
4. **Testing**: how to verify
5. **Screenshots** (if UI change)

### Merge conflict resolution
1. Read both versions carefully (ours vs theirs)
2. Understand the intent of both changes
3. Merge semantically, not mechanically
4. Never blindly accept "ours" or "theirs" for logic conflicts

### Commit signing
Use `git commit -S` for signed commits when working on sensitive projects.
