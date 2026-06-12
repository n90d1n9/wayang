#!/usr/bin/env bash
# Gamelan CLI — Installation & Setup Script
# Usage: ./install.sh [--native] [--prefix /usr/local] [--no-completions]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PREFIX="${GAMELAN_PREFIX:-/usr/local}"
NATIVE=false
NO_COMPLETIONS=false
DEFAULT_MODEL="${GAMELAN_DEFAULT_MODEL:-llama3}"

# ── Parse args ─────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --native)          NATIVE=true;          shift ;;
        --prefix)          PREFIX="$2";          shift 2 ;;
        --no-completions)  NO_COMPLETIONS=true;  shift ;;
        --model)           DEFAULT_MODEL="$2";   shift 2 ;;
        -h|--help)
            echo "Usage: $0 [--native] [--prefix DIR] [--no-completions] [--model NAME]"
            exit 0 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

GAMELAN_HOME="${HOME}/.gamelan"
BIN_DIR="${PREFIX}/bin"

echo "🎶 Installing Gamelan CLI"
echo "   Prefix  : ${PREFIX}"
echo "   Native  : ${NATIVE}"
echo "   Model   : ${DEFAULT_MODEL}"
echo

# ── Check prerequisites ────────────────────────────────────────────────────
command -v java >/dev/null 2>&1 || { echo "❌ Java 21+ is required. Install from https://adoptium.net/"; exit 1; }
JAVA_VER=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)
if [ "${JAVA_VER:-0}" -lt 21 ]; then
    echo "❌ Java 21+ required (found Java ${JAVA_VER})"; exit 1
fi
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven 3.9+ is required."; exit 1; }

# ── Build ──────────────────────────────────────────────────────────────────
cd "${SCRIPT_DIR}"

if [ "${NATIVE}" = true ]; then
    command -v native-image >/dev/null 2>&1 || {
        echo "❌ GraalVM native-image not found. Install GraalVM and run: gu install native-image"
        exit 1
    }
    echo "→ Building native binary (GraalVM)..."
    mvn package -Pnative -DskipTests -q
    INSTALL_BINARY=$(ls target/gamelan-cli-*-runner 2>/dev/null | head -1)
    [ -n "${INSTALL_BINARY}" ] || { echo "❌ Native build failed"; exit 1; }
else
    echo "→ Building JVM package..."
    mvn package -DskipTests -q
    INSTALL_BINARY=""
fi

# ── Create config directory ────────────────────────────────────────────────
echo "→ Creating ~/.gamelan directory structure..."
mkdir -p "${GAMELAN_HOME}/skills"
mkdir -p "${GAMELAN_HOME}/sessions"
mkdir -p "${GAMELAN_HOME}/memory"
mkdir -p "${GAMELAN_HOME}/checkpoints"
mkdir -p "${GAMELAN_HOME}/plans"
mkdir -p "${GAMELAN_HOME}/eval"
mkdir -p "${GAMELAN_HOME}/audit"
mkdir -p "${GAMELAN_HOME}/security"
mkdir -p "${GAMELAN_HOME}/todos"

# ── Write default config ────────────────────────────────────────────────────
CONFIG_FILE="${GAMELAN_HOME}/config.yml"
if [ ! -f "${CONFIG_FILE}" ]; then
    echo "→ Writing default config to ${CONFIG_FILE}..."
    cat > "${CONFIG_FILE}" << YAML
# Gamelan CLI Configuration
# Edit with: gamelan config set <key> <value>
model: ${DEFAULT_MODEL}
temperature: 0.7
max.tokens: 4096
stream: true
engine.mode: auto
YAML
fi

# ── Install binary or launcher ─────────────────────────────────────────────
mkdir -p "${BIN_DIR}"

if [ "${NATIVE}" = true ] && [ -n "${INSTALL_BINARY}" ]; then
    echo "→ Installing native binary to ${BIN_DIR}/gamelan..."
    install -m 755 "${INSTALL_BINARY}" "${BIN_DIR}/gamelan"
else
    echo "→ Installing JVM launcher to ${BIN_DIR}/gamelan..."
    QUARKUS_JAR="${SCRIPT_DIR}/target/quarkus-app/quarkus-run.jar"
    [ -f "${QUARKUS_JAR}" ] || { echo "❌ Build artifact not found at ${QUARKUS_JAR}"; exit 1; }
    cat > "${BIN_DIR}/gamelan" << LAUNCHER
#!/usr/bin/env bash
exec java -jar "${QUARKUS_JAR}" "\$@"
LAUNCHER
    chmod 755 "${BIN_DIR}/gamelan"
fi

# ── Copy bundled skills ────────────────────────────────────────────────────
SKILLS_SRC="${SCRIPT_DIR}/skills"
SKILLS_DST="${GAMELAN_HOME}/skills"
if [ -d "${SKILLS_SRC}" ]; then
    echo "→ Installing bundled skills..."
    for skill_dir in "${SKILLS_SRC}"/*/; do
        skill_name=$(basename "${skill_dir}")
        if [ -f "${skill_dir}/SKILL.md" ]; then
            cp -r "${skill_dir}" "${SKILLS_DST}/${skill_name}"
            echo "   ✓ ${skill_name}"
        fi
    done
fi

# ── Pull default embedding model (if Ollama is available) ─────────────────
if command -v ollama >/dev/null 2>&1; then
    echo "→ Pulling embedding model (nomic-embed-text)..."
    ollama pull nomic-embed-text 2>/dev/null && echo "   ✓ nomic-embed-text" || \
        echo "   ⚠ Could not pull nomic-embed-text (optional — semantic search will use keyword fallback)"
    echo "→ Pulling default model (${DEFAULT_MODEL})..."
    ollama pull "${DEFAULT_MODEL}" 2>/dev/null && echo "   ✓ ${DEFAULT_MODEL}" || \
        echo "   ⚠ Could not pull ${DEFAULT_MODEL} — pull manually with: gamelan models pull ${DEFAULT_MODEL}"
else
    echo "   ℹ Ollama not found — skipping model pull"
    echo "   Install Ollama from https://ollama.ai then run: gamelan models pull ${DEFAULT_MODEL}"
fi

# ── Shell completions ──────────────────────────────────────────────────────
if [ "${NO_COMPLETIONS}" = false ]; then
    echo "→ Installing shell completions..."

    # Bash completion
    BASH_COMP_DIR="${PREFIX}/share/bash-completion/completions"
    if [ -d "$(dirname "${BASH_COMP_DIR}")" ]; then
        mkdir -p "${BASH_COMP_DIR}"
        cat > "${BASH_COMP_DIR}/gamelan" << 'BASH_COMP'
_gamelan_completion() {
    local cur="${COMP_WORDS[COMP_CWORD]}"
    local prev="${COMP_WORDS[COMP_CWORD-1]}"
    local cmds="chat run watch workflow skill models memory checkpoint plan eval config"

    case "${COMP_CWORD}" in
        1) COMPREPLY=($(compgen -W "${cmds} --help --version" -- "${cur}")) ;;
        2) case "${prev}" in
               workflow) COMPREPLY=($(compgen -W "review refactor document test list" -- "${cur}")) ;;
               skill)    COMPREPLY=($(compgen -W "list show install remove validate run verify" -- "${cur}")) ;;
               memory)   COMPREPLY=($(compgen -W "list add forget clear search" -- "${cur}")) ;;
               checkpoint) COMPREPLY=($(compgen -W "list resume delete" -- "${cur}")) ;;
               plan)     COMPREPLY=($(compgen -W "list show search compare delete" -- "${cur}")) ;;
               eval)     COMPREPLY=($(compgen -W "run shadow history audit" -- "${cur}")) ;;
               config)   COMPREPLY=($(compgen -W "list get set reset" -- "${cur}")) ;;
               models)   COMPREPLY=($(compgen -W "list pull rm info" -- "${cur}")) ;;
               run)      COMPREPLY=($(compgen -W "--strategy --plan --plan-only --auto-approve --model --no-stream --json --agui --file" -- "${cur}")) ;;
               chat)     COMPREPLY=($(compgen -W "--model --strategy --no-stream --session --no-color --agui" -- "${cur}")) ;;
           esac ;;
    esac
}
complete -F _gamelan_completion gamelan
BASH_COMP
        echo "   ✓ Bash completions → ${BASH_COMP_DIR}/gamelan"
    fi

    # Zsh completion
    ZSH_COMP_DIR="${PREFIX}/share/zsh/site-functions"
    if command -v zsh >/dev/null 2>&1; then
        mkdir -p "${ZSH_COMP_DIR}"
        cat > "${ZSH_COMP_DIR}/_gamelan" << 'ZSH_COMP'
#compdef gamelan
_gamelan() {
  local -a commands
  commands=(
    'chat:Start interactive REPL'
    'run:Execute a one-shot task'
    'watch:Watch files and trigger tasks on change'
    'workflow:Run multi-step workflow presets'
    'skill:Manage agent skills'
    'models:Manage local LLM models'
    'memory:Manage persistent agent memory'
    'checkpoint:List and resume saved sessions'
    'plan:Manage and compare task plans'
    'eval:Benchmarking and audit commands'
    'config:View and edit configuration'
  )
  local -a strategies
  strategies=(direct react reflexion multi pipeline auto)
  case "$words[2]" in
    run)     _arguments '--strategy[Strategy]:strategy:('"${strategies[@]}"')' '--plan[Plan before executing]' '--plan-only[Plan only]' '--auto-approve[Skip HITL gates]' '--model[Model]:model:' '--agui[AG-UI mode]' '1:task:' ;;
    workflow) _values 'subcommand' review refactor document test list ;;
    skill)    _values 'subcommand' list show install remove validate run verify ;;
    memory)   _values 'subcommand' list add forget clear search ;;
    *)        _describe 'gamelan commands' commands ;;
  esac
}
_gamelan
ZSH_COMP
        echo "   ✓ Zsh completions  → ${ZSH_COMP_DIR}/_gamelan"
    fi

    # Fish completion
    FISH_COMP_DIR="${HOME}/.config/fish/completions"
    if command -v fish >/dev/null 2>&1; then
        mkdir -p "${FISH_COMP_DIR}"
        cat > "${FISH_COMP_DIR}/gamelan.fish" << 'FISH_COMP'
complete -c gamelan -f
complete -c gamelan -n __fish_use_subcommand -a chat       -d 'Interactive REPL'
complete -c gamelan -n __fish_use_subcommand -a run        -d 'One-shot task'
complete -c gamelan -n __fish_use_subcommand -a watch      -d 'File-watch mode'
complete -c gamelan -n __fish_use_subcommand -a workflow   -d 'Multi-step workflows'
complete -c gamelan -n __fish_use_subcommand -a skill      -d 'Skill management'
complete -c gamelan -n __fish_use_subcommand -a models     -d 'Model management'
complete -c gamelan -n __fish_use_subcommand -a memory     -d 'Memory management'
complete -c gamelan -n __fish_use_subcommand -a checkpoint -d 'Checkpoint management'
complete -c gamelan -n __fish_use_subcommand -a plan       -d 'Plan management'
complete -c gamelan -n __fish_use_subcommand -a eval       -d 'Benchmarking and audit'
complete -c gamelan -n __fish_use_subcommand -a config     -d 'Configuration'

# run --strategy
complete -c gamelan -n '__fish_seen_subcommand_from run' -l strategy -a 'direct react reflexion multi pipeline auto' -d 'Orchestration strategy'
complete -c gamelan -n '__fish_seen_subcommand_from run' -l plan -d 'Plan before executing'
complete -c gamelan -n '__fish_seen_subcommand_from run' -l auto-approve -d 'Skip HITL gates'
FISH_COMP
        echo "   ✓ Fish completions → ${FISH_COMP_DIR}/gamelan.fish"
    fi
fi

# ── Done ───────────────────────────────────────────────────────────────────
echo
echo "✅ Gamelan CLI installed successfully!"
echo
echo "Add to your PATH if needed:"
echo "   export PATH=\"${BIN_DIR}:\$PATH\""
echo
echo "Quick start:"
echo "   gamelan                    # start interactive REPL"
echo "   gamelan run 'fix the NPE'  # one-shot task"
echo "   gamelan --help             # see all commands"
echo
echo "🎶 Happy coding!"
