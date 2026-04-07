

# golok Code Agent — Implementation Walkthrough

## Summary

Implemented the `golok-code-core` module with **11 Java source files** across 5 packages, enabling external skill management via the Agent Skills specification (SKILL.md format).

## Architecture

```mermaid
graph TD
    CLI["SkillsCommand<br/>(Picocli)"] --> Handler["SkillsCommandHandler"]
    Handler --> Loader["GitSkillLoader<br/>(JGit clone/pull)"]
    Handler --> Store["ExternalSkillStore<br/>(in-memory index)"]
    
    Agent["AgentSkillUtil"] --> Discovery["SkillDiscoveryService"]
    Agent --> Store
    Agent --> Loader
    
    Discovery --> Store
    Discovery -->|scans| Paths["~/.golok/skills/<br/>.golok/skills/<br/>.agents/skills/"]
    
    Store --> Parser["SkillManifestParser<br/>(YAML frontmatter)"]
    Parser --> Manifest["SkillManifest"]
    
    Agent -->|builds LLM context| Adapter["ExternalSkillAdapter"]
    Adapter --> Manifest
```


## golok Code Agent Architecture


```mermaid
graph TD
    CLI["SkillsCommand<br/>(Picocli)"] --> Handler["SkillsCommandHandler"]
    Handler --> Loader["GitSkillLoader<br/>(JGit clone/pull)"]
    Handler --> Store["ExternalSkillStore"]
    Agent["AgentSkillUtil"] --> Discovery["SkillDiscoveryService"]
    Agent --> Store
    Discovery -->|scans| Paths["~/.golok/skills/<br/>.agents/skills/"]
    Store --> Parser["SkillManifestParser"]
    Agent -->|LLM context| Adapter["ExternalSkillAdapter"]
```


## Usage

```bash
# Install skills from a git repo
golok skills add https://github.com/samber/cc-skills

# Install only specific skills
golok skills add https://github.com/samber/cc-skills --skill 'golang-*'

# List installed skills
golok skills list

# Show skill details
golok skills info conventional-git

# Update all installed repos
golok skills update

# Remove a skill repo
golok skills remove cc-skills
```