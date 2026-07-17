package tech.kayys.wayang.skills.store;

import tech.kayys.wayang.skill.spi.SkillDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Unified Wayang Skill Store — the single runtime facade for all skill sources.
 *
 * <h3>Sources (loaded in order, later sources can override earlier ones):</h3>
 * <ol>
 *   <li><b>Builtin</b> — classpath {@code default-skills/} (read-only)</li>
 *   <li><b>User</b>    — {@code ~/.wayang/skills/} (SKILL.md, JSON, YAML)</li>
 *   <li><b>Custom</b>  — additional load paths (via system property or API)</li>
 *   <li><b>DB</b>      — enterprise backend (plugged in via {@link SkillStoreBackend})</li>
 * </ol>
 *
 * <h3>Configuration system properties:</h3>
 * <ul>
 *   <li>{@code wayang.skills.dir}        — override base dir (default: ~/.wayang/skills)</li>
 *   <li>{@code wayang.skills.load_paths} — colon-separated extra load paths</li>
 *   <li>{@code wayang.skills.backend}    — {@code file} (default) | {@code db}</li>
 * </ul>
 *
 * <p>This class is pure Java — no CDI, no Mutiny, no Quarkus — and works
 * correctly in the plain CLI agent context. The {@link SkillStoreBackend} SPI
 * allows the enterprise DB backend to plug in transparently at runtime via
 * {@link ServiceLoader}.
 */
public final class WayangSkillStore {

    private static final Logger LOG = Logger.getLogger(WayangSkillStore.class.getName());

    /** Lazy singleton. */
    private static volatile WayangSkillStore INSTANCE;

    private final Path baseDir;
    private final List<Path> customLoadPaths = new ArrayList<>();
    private final ConcurrentHashMap<String, SkillEntry> index = new ConcurrentHashMap<>();

    // Optional enterprise DB backend (plugged in via ServiceLoader)
    private final SkillStoreBackend backend;

    private WayangSkillStore(Path baseDir, SkillStoreBackend backend) {
        this.baseDir = baseDir;
        this.backend = backend;
    }

    // ─── singleton access ────────────────────────────────────────────────────

    public static WayangSkillStore getInstance() {
        if (INSTANCE == null) {
            synchronized (WayangSkillStore.class) {
                if (INSTANCE == null) {
                    INSTANCE = create();
                    INSTANCE.load();
                }
            }
        }
        return INSTANCE;
    }

    private static WayangSkillStore create() {
        String dirProp = System.getProperty("wayang.skills.dir",
                System.getProperty("user.home") + "/.wayang/skills");
        Path baseDir = Paths.get(dirProp);

        // Try to discover a DB backend via ServiceLoader (enterprise mode)
        SkillStoreBackend backend = null;
        try {
            ServiceLoader<SkillStoreBackend> loader = ServiceLoader.load(SkillStoreBackend.class);
            Iterator<SkillStoreBackend> it = loader.iterator();
            if (it.hasNext()) {
                backend = it.next();
                LOG.info("Skills: enterprise DB backend discovered — " + backend.name());
            }
        } catch (Exception e) {
            LOG.fine("No enterprise skill backend available: " + e.getMessage());
        }

        WayangSkillStore store = new WayangSkillStore(baseDir, backend);

        // Register custom load paths from system property
        String loadPaths = System.getProperty("wayang.skills.load_paths", "");
        if (!loadPaths.isBlank()) {
            for (String p : loadPaths.split(":")) {
                if (!p.isBlank()) store.customLoadPaths.add(Paths.get(p.strip()));
            }
        }

        return store;
    }

    // ─── loading ─────────────────────────────────────────────────────────────

    /**
     * Load / reload all skills from all sources. Safe to call multiple times.
     */
    public void load() {
        index.clear();

        // 1. Classpath builtins
        for (SkillEntry e : BuiltinSkillLoader.load()) {
            index.put(e.id(), e);
        }
        LOG.fine("Skills: loaded " + index.size() + " builtins");

        // 2. User skills from ~/.wayang/skills/
        loadDirectory(baseDir, "user");

        // 3. Custom load paths
        for (Path p : customLoadPaths) {
            loadDirectory(p, "custom");
        }

        // 4. Enterprise DB backend (merges on top, db entries win for same id)
        if (backend != null) {
            try {
                List<SkillEntry> dbEntries = backend.loadAll().toCompletableFuture().join();
                for (SkillEntry e : dbEntries) {
                    index.put(e.id(), e);
                }
                LOG.fine("Skills: merged " + dbEntries.size() + " entries from DB backend");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to load skills from DB backend: " + e.getMessage(), e);
            }
        }

        LOG.info("Skills: total " + index.size() + " skills loaded");
    }

    /** Scan a directory for SKILL.md, *.json, *.yaml, *.yml files. */
    private void loadDirectory(Path dir, String source) {
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    // Subdirectory: look for SKILL.md inside
                    loadSkillDirectory(entry, source);
                } else {
                    // Top-level file: JSON or YAML
                    loadSkillFile(entry, null, source);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to scan skill directory: " + dir, e);
        }
    }

    /** Load a named skill directory (expects SKILL.md inside). */
    private void loadSkillDirectory(Path dir, String source) {
        String id = dir.getFileName().toString();
        Path skillMd = dir.resolve("SKILL.md");
        if (Files.isRegularFile(skillMd)) {
            loadSkillFile(skillMd, id, source);
            return;
        }
        // Fallback: look for skill.json or skill.yaml
        for (String name : List.of("skill.json", "skill.yaml", "skill.yml")) {
            Path f = dir.resolve(name);
            if (Files.isRegularFile(f)) {
                loadSkillFile(f, id, source);
                return;
            }
        }
    }

    private void loadSkillFile(Path file, String idHint, String source) {
        String fileName = file.getFileName().toString();
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String id = idHint != null ? idHint : fileNameWithoutExtension(fileName);
            SkillDefinition def;
            SkillEntry.SkillFormat fmt;

            if (fileName.endsWith("SKILL.md") || fileName.endsWith(".md")) {
                def = SkillFileParser.parseSkillMd(id, content);
                fmt = SkillEntry.SkillFormat.SKILL_MD;
            } else if (fileName.endsWith(".json")) {
                def = SkillFileParser.parseJson(id, content);
                fmt = SkillEntry.SkillFormat.JSON;
            } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                def = SkillFileParser.parseYaml(id, content);
                fmt = SkillEntry.SkillFormat.YAML;
            } else {
                return; // unsupported extension
            }

            SkillEntry entry = SkillEntry.builder()
                    .id(def.id())
                    .name(def.name())
                    .description(def.description())
                    .category(def.category())
                    .source(source)
                    .format(fmt)
                    .path(file.toAbsolutePath().toString())
                    .readOnly(false)
                    .enabled(true)
                    .definition(def)
                    .build();
            index.put(def.id(), entry);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load skill from " + file + ": " + e.getMessage());
        }
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    /** List all enabled skills. */
    public List<SkillEntry> list() {
        return index.values().stream()
                .filter(SkillEntry::enabled)
                .sorted(Comparator.comparing(SkillEntry::id))
                .collect(Collectors.toList());
    }

    /** List all skills including disabled ones. */
    public List<SkillEntry> listAll() {
        return index.values().stream()
                .sorted(Comparator.comparing(SkillEntry::id))
                .collect(Collectors.toList());
    }

    /** Find by exact id. */
    public Optional<SkillEntry> find(String id) {
        return Optional.ofNullable(index.get(id));
    }

    /** Search by keyword (id, name, description, category). */
    public List<SkillEntry> search(String query) {
        if (query == null || query.isBlank()) return list();
        String q = query.toLowerCase();
        return index.values().stream()
                .filter(e -> contains(e.id(), q) || contains(e.name(), q)
                        || contains(e.description(), q) || contains(e.category(), q))
                .sorted(Comparator.comparing(SkillEntry::id))
                .collect(Collectors.toList());
    }

    /** Filter by category. */
    public List<SkillEntry> listByCategory(String category) {
        return index.values().stream()
                .filter(e -> category.equalsIgnoreCase(e.category()))
                .sorted(Comparator.comparing(SkillEntry::id))
                .collect(Collectors.toList());
    }

    /** Filter by source (builtin, user, custom, db). */
    public List<SkillEntry> listBySource(String source) {
        return index.values().stream()
                .filter(e -> source.equalsIgnoreCase(e.source()))
                .sorted(Comparator.comparing(SkillEntry::id))
                .collect(Collectors.toList());
    }

    /**
     * Create and persist a new user skill.
     *
     * @throws IllegalArgumentException if a skill with that id already exists
     * @throws IllegalStateException    if the skills directory cannot be created
     */
    public SkillEntry create(SkillDefinition def) throws IOException {
        if (index.containsKey(def.id())) {
            throw new IllegalArgumentException("Skill '" + def.id() + "' already exists. Use update() to modify it.");
        }
        return persist(def, "user");
    }

    /**
     * Update an existing user skill. Read-only (builtin) skills require
     * {@code force=true}.
     */
    public SkillEntry update(SkillDefinition def, boolean force) throws IOException {
        SkillEntry existing = index.get(def.id());
        if (existing != null && existing.readOnly() && !force) {
            throw new IllegalStateException("Skill '" + def.id() + "' is a built-in skill and cannot be modified. Use --force to override.");
        }
        String source = (existing != null) ? existing.source() : "user";
        return persist(def, source);
    }

    /**
     * Delete a skill by id.
     *
     * @param force if true, allows deleting built-in skills (creates a local override)
     * @throws IllegalArgumentException if the skill does not exist
     * @throws IllegalStateException    if attempting to delete a builtin without force
     */
    public void delete(String id, boolean force) throws IOException {
        SkillEntry entry = index.get(id);
        if (entry == null) throw new IllegalArgumentException("Skill '" + id + "' not found.");
        if (entry.readOnly() && !force) {
            throw new IllegalStateException("Skill '" + id + "' is a built-in skill and cannot be deleted. Use --force to override.");
        }

        // Delete the file
        if (entry.path() != null && !entry.path().startsWith("default-skills/")) {
            Path file = Paths.get(entry.path());
            Files.deleteIfExists(file);
            // If directory is now empty, remove it
            Path parent = file.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent)) {
                    if (!ds.iterator().hasNext()) Files.delete(parent);
                }
            }
        }

        // Also remove from DB backend if available
        if (backend != null) {
            try { backend.delete(id).toCompletableFuture().join(); } catch (Exception e) {
                LOG.warning("Failed to delete skill from DB backend: " + e.getMessage());
            }
        }

        index.remove(id);
    }

    /** Enable a skill by id. */
    public void enable(String id) throws IOException {
        setEnabled(id, true);
    }

    /** Disable a skill by id. */
    public void disable(String id) throws IOException {
        setEnabled(id, false);
    }

    // ─── Custom load paths ───────────────────────────────────────────────────

    public void addLoadPath(Path path) {
        if (!customLoadPaths.contains(path)) {
            customLoadPaths.add(path);
            loadDirectory(path, "custom");
            saveLoadPaths();
        }
    }

    public void removeLoadPath(Path path) {
        customLoadPaths.remove(path);
        // Remove entries that came from this path
        index.entrySet().removeIf(e ->
                e.getValue().path() != null && e.getValue().path().startsWith(path.toAbsolutePath().toString()));
        saveLoadPaths();
    }

    public List<Path> getLoadPaths() {
        return Collections.unmodifiableList(customLoadPaths);
    }

    /** Export a skill to a file (SKILL.md, JSON, or YAML based on file extension). */
    public void export(String id, Path outputFile) throws IOException {
        SkillEntry entry = find(id).orElseThrow(() -> new IllegalArgumentException("Skill '" + id + "' not found."));
        String content;
        String name = outputFile.getFileName().toString();
        if (name.endsWith(".json")) {
            content = SkillFileWriter.toJson(entry.definition());
        } else if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            content = SkillFileWriter.toYaml(entry.definition());
        } else {
            content = SkillFileWriter.toSkillMd(entry.definition());
        }
        Files.writeString(outputFile, content, StandardCharsets.UTF_8);
    }

    /** Import a skill from a file. Returns the created SkillEntry. */
    public SkillEntry importFrom(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        String content  = Files.readString(file, StandardCharsets.UTF_8);
        String id = fileNameWithoutExtension(fileName);
        SkillDefinition def;
        if (fileName.endsWith(".json")) {
            def = SkillFileParser.parseJson(id, content);
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            def = SkillFileParser.parseYaml(id, content);
        } else {
            def = SkillFileParser.parseSkillMd(id, content);
        }
        return create(def);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private SkillEntry persist(SkillDefinition def, String source) throws IOException {
        // Write to ~/.wayang/skills/<id>/SKILL.md
        Path skillDir = baseDir.resolve(def.id());
        Files.createDirectories(skillDir);
        Path skillFile = skillDir.resolve("SKILL.md");
        Files.writeString(skillFile, SkillFileWriter.toSkillMd(def), StandardCharsets.UTF_8);

        // Also write JSON sidecar for DB/enterprise alignment
        Files.writeString(skillDir.resolve("skill.json"), SkillFileWriter.toJson(def), StandardCharsets.UTF_8);

        // Sync to DB backend if available
        if (backend != null) {
            try { backend.save(def).toCompletableFuture().join(); } catch (Exception e) {
                LOG.warning("Failed to sync skill to DB backend: " + e.getMessage());
            }
        }

        SkillEntry entry = SkillEntry.builder()
                .id(def.id())
                .name(def.name())
                .description(def.description())
                .category(def.category())
                .source(source)
                .format(SkillEntry.SkillFormat.SKILL_MD)
                .path(skillFile.toAbsolutePath().toString())
                .readOnly(false)
                .enabled(true)
                .definition(def)
                .build();
        index.put(def.id(), entry);
        return entry;
    }

    private void setEnabled(String id, boolean enabled) throws IOException {
        SkillEntry existing = index.get(id);
        if (existing == null) throw new IllegalArgumentException("Skill '" + id + "' not found.");
        SkillEntry updated = SkillEntry.builder()
                .id(existing.id()).name(existing.name()).description(existing.description())
                .category(existing.category()).source(existing.source()).format(existing.format())
                .path(existing.path()).readOnly(existing.readOnly()).enabled(enabled)
                .createdAt(existing.createdAt()).updatedAt(Instant.now())
                .definition(existing.definition())
                .build();
        index.put(id, updated);
        // Write a .disabled marker file if disabling
        if (existing.path() != null && !existing.path().startsWith("default-skills/")) {
            Path markerFile = Paths.get(existing.path()).getParent().resolve(".disabled");
            if (enabled) { Files.deleteIfExists(markerFile); }
            else { Files.writeString(markerFile, "", StandardCharsets.UTF_8); }
        }
    }

    private void saveLoadPaths() {
        // Persist load paths to ~/.wayang/skills/.load_paths
        try {
            Files.createDirectories(baseDir);
            Path cfg = baseDir.resolve(".load_paths");
            String content = customLoadPaths.stream()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));
            Files.writeString(cfg, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warning("Failed to save custom load paths: " + e.getMessage());
        }
    }

    private void loadSavedLoadPaths() {
        Path cfg = baseDir.resolve(".load_paths");
        if (!Files.isRegularFile(cfg)) return;
        try {
            Files.readAllLines(cfg).stream()
                    .map(String::strip).filter(s -> !s.isBlank())
                    .map(Paths::get).forEach(customLoadPaths::add);
        } catch (IOException e) {
            LOG.warning("Failed to read custom load paths: " + e.getMessage());
        }
    }

    private static boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    private static String fileNameWithoutExtension(String name) {
        int dot = name.lastIndexOf('.');
        // Handle SKILL.md specifically
        if (name.equals("SKILL.md")) return "unknown";
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Returns the base directory for user skills. */
    public Path getBaseDir() { return baseDir; }

    /** Returns a summary of the store state. */
    public String summary() {
        long builtins = index.values().stream().filter(e -> "builtin".equals(e.source())).count();
        long user     = index.values().stream().filter(e -> "user".equals(e.source())).count();
        long custom   = index.values().stream().filter(e -> "custom".equals(e.source())).count();
        long db       = index.values().stream().filter(e -> "db".equals(e.source())).count();
        return String.format("  builtin=%d  user=%d  custom=%d  db=%d  total=%d",
                builtins, user, custom, db, index.size());
    }
}
