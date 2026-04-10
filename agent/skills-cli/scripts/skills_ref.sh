#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════
# skills_ref — AgentSkills.io Validation CLI
# ═══════════════════════════════════════════════════════════
#
# Usage:
#   skills_ref validate <skill-dir>     Validate a skill directory
#   skills_ref validate-all <skills-dir>  Validate all skills in a directory
#   skills_ref help                     Show this help
#
# Examples:
#   skills_ref validate ./skills/my-skill
#   skills_ref validate-all ./skills
#

set -euo pipefail

# ── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ── Functions ───────────────────────────────────────────────────────────────

usage() {
    echo "Usage: skills_ref <command> [args]"
    echo ""
    echo "Commands:"
    echo "  validate <skill-dir>        Validate a single skill directory"
    echo "  validate-all <skills-dir>   Validate all skills in a directory"
    echo "  help                        Show this help"
    exit 0
}

validate_name() {
    local name="$1"
    local dir_name="$2"
    local errors=()

    # Length check
    if [ ${#name} -lt 1 ] || [ ${#name} -gt 64 ]; then
        errors+=("Name must be 1-64 characters, got ${#name}: $name")
    fi

    # Pattern check (lowercase alphanumeric + hyphens)
    if ! [[ "$name" =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
        errors+=("Name must be lowercase alphanumeric with hyphens only: $name")
    fi

    # Directory match check
    if [ "$name" != "$dir_name" ]; then
        errors+=("Name must match parent directory name. Name='$name', Directory='$dir_name'")
    fi

    if [ ${#errors[@]} -gt 0 ]; then
        for err in "${errors[@]}"; do
            echo -e "  ${RED}❌ $err${NC}"
        done
        return 1
    fi
    return 0
}

validate_description() {
    local description="$1"
    local errors=()

    # Length check
    if [ ${#description} -lt 1 ] || [ ${#description} -gt 1024 ]; then
        errors+=("Description must be 1-1024 characters, got ${#description}")
    fi

    # Word count check (at least 5 words)
    local word_count=$(echo "$description" | wc -w)
    if [ "$word_count" -lt 5 ]; then
        errors+=("Description too brief. Must include what the skill does and when to use it (at least 5 words)")
    fi

    # Keyword check
    local lower=$(echo "$description" | tr '[:upper:]' '[:lower:]')
    if ! [[ "$lower" =~ (use|when|for|execute|run|create) ]]; then
        errors+=("Description should include task-identifying keywords (use, when, for, execute, run, create)")
    fi

    if [ ${#errors[@]} -gt 0 ]; then
        for err in "${errors[@]}"; do
            echo -e "  ${RED}❌ $err${NC}"
        done
        return 1
    fi
    return 0
}

validate_skill() {
    local skill_dir="$1"
    local skill_md="$skill_dir/SKILL.md"
    local error_count=0
    local warning_count=0

    # Check directory exists
    if [ ! -d "$skill_dir" ]; then
        echo -e "${RED}❌ Skill directory not found: $skill_dir${NC}"
        return 1
    fi

    # Check SKILL.md exists
    if [ ! -f "$skill_md" ]; then
        echo -e "${RED}❌ SKILL.md not found in: $skill_dir${NC}"
        return 1
    fi

    local dir_name=$(basename "$skill_dir")
    echo "Validating: $dir_name"

    # Parse frontmatter
    local content=$(cat "$skill_md")

    # Check frontmatter delimiters
    if [[ ! "$content" == ---* ]]; then
        echo -e "  ${RED}❌ SKILL.md must start with YAML frontmatter delimiter (---)${NC}"
        ((error_count++))
        return 1
    fi

    # Extract name and description (simplified parsing)
    local name=$(grep -m1 "^name:" "$skill_md" | sed 's/^name: *//;s/^"\(.*\)"$/\1/')
    local description=$(grep -m1 "^description:" "$skill_md" | sed 's/^description: *//;s/^"\(.*\)"$/\1/')

    # Validate name
    if [ -n "$name" ]; then
        if ! validate_name "$name" "$dir_name"; then
            ((error_count++))
        fi
    else
        echo -e "  ${RED}❌ Missing required field: name${NC}"
        ((error_count++))
    fi

    # Validate description
    if [ -n "$description" ]; then
        if ! validate_description "$description"; then
            ((error_count++))
        fi
    else
        echo -e "  ${RED}❌ Missing required field: description${NC}"
        ((error_count++))
    fi

    # Check body content
    local body=$(sed -n '/^---$/,/^---$/!p' "$skill_md" | sed '1,/^---$/d')
    if [ -z "$body" ] || [ -z "$(echo "$body" | tr -d '[:space:]')" ]; then
        echo -e "  ${RED}❌ SKILL.md body content is empty${NC}"
        ((error_count++))
    fi

    # Token count estimation
    local char_count=${#body}
    local estimated_tokens=$((char_count / 4))
    if [ $estimated_tokens -gt 5000 ]; then
        echo -e "  ${YELLOW}⚠️  SKILL.md body is very large (~$estimated_tokens tokens). Consider moving details to references/${NC}"
        ((warning_count++))
    fi

    # Report results
    if [ $error_count -eq 0 ] && [ $warning_count -eq 0 ]; then
        echo -e "  ${GREEN}✅ Valid skill${NC}"
    elif [ $error_count -eq 0 ]; then
        echo -e "  ${GREEN}✅ Valid skill (with $warning_count warnings)${NC}"
    else
        echo -e "  ${RED}❌ Invalid skill ($error_count errors, $warning_count warnings)${NC}"
    fi

    return $error_count
}

cmd_validate() {
    local skill_dir="${1:-.}"
    validate_skill "$skill_dir"
}

cmd_validate_all() {
    local skills_dir="${1:-./skills}"
    local total=0
    local valid=0
    local invalid=0

    if [ ! -d "$skills_dir" ]; then
        echo -e "${RED}❌ Skills directory not found: $skills_dir${NC}"
        exit 1
    fi

    echo "Validating all skills in: $skills_dir"
    echo "─────────────────────────────────────────────"

    for skill_dir in "$skills_dir"/*/; do
        if [ -d "$skill_dir" ]; then
            ((total++))
            if validate_skill "$skill_dir"; then
                ((valid++))
            else
                ((invalid++))
            fi
        fi
    done

    echo "─────────────────────────────────────────────"
    echo -e "Total: $total | ${GREEN}Valid: $valid${NC} | ${RED}Invalid: $invalid${NC}"

    if [ $invalid -gt 0 ]; then
        exit 1
    fi
}

# ── Main ────────────────────────────────────────────────────────────────────

command="${1:-help}"
shift || true

case "$command" in
    validate)
        cmd_validate "$@"
        ;;
    validate-all)
        cmd_validate_all "$@"
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        echo -e "${RED}Unknown command: $command${NC}"
        usage
        ;;
esac
