package com.frank1o3.franklylib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Wraps a plain {@code @ConfigEntry}-annotated object (mutable POJO or
 * record — see {@link ConfigEntry}) with dirty-tracked autosave and safe
 * hot-reload, backed by a single JSON file on disk.
 *
 * <p>
 * <b>Usage</b>
 * <pre>{@code
 * FranklyConfigHolder<MyConfig> config = FranklyConfigHolder.builder(MyConfig.class, MyConfig::new)
 *         .path(FabricLoader.getInstance().getConfigDir().resolve("mymod.json"))
 *         .autosaveTicks(6000)   // ~5 minutes at 20 tps
 *         .build();
 *
 * // read
 * double max = config.get().maxScaleLimit;
 *
 * // write (mutable POJO or record — same call either way)
 * config.update(cfg -> { cfg.maxScaleLimit = 8.0; return cfg; });
 *
 * // once per tick from your own tick event:
 * config.tick();
 *
 * // on shutdown:
 * config.flushAndClose();
 * }</pre>
 *
 * <p>
 * <b>Dirty vs. stale.</b> "Dirty" means the in-memory instance has changed
 * since the last save (triggers a write). "Stale" means the file on disk has
 * changed since we last read it — by another process, a manual edit, or a
 * {@code /reload}-style command (triggers a re-read). If both are true at
 * once, the in-memory dirty state wins: the external change is not applied,
 * and the next autosave overwrites it. This avoids silently discarding a
 * change the mod itself just made.
 *
 * <p>
 * <b>Load-time and reload-time validation is per-field, not all-or-nothing.</b>
 * A field with a missing or wrong-typed JSON value falls back to its
 * previous/default value with a logged warning; only a completely unparsable
 * file aborts the load and leaves the in-memory state untouched.
 */
public final class FranklyConfigHolder<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger("franklylib/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Class<T> type;
    private final ConfigSchema schema;
    private final Path path;
    private final Supplier<T> defaultFactory;
    private final int autosaveIntervalTicks;
    private final int staleCheckIntervalTicks;

    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveInProgress = new AtomicBoolean(false);
    private final ExecutorService ioExecutor;

    private final List<Consumer<T>> reloadListeners = new ArrayList<>();
    private final List<Consumer<T>> saveListeners = new ArrayList<>();

    private volatile T current;
    private volatile long lastKnownModifiedMillis = -1;
    private int ticksUntilAutosave;
    private int ticksUntilStaleCheck;

    private FranklyConfigHolder(Class<T> type, ConfigSchema schema, Path path, Supplier<T> defaultFactory,
            int autosaveIntervalTicks, int staleCheckIntervalTicks) {
        this.type = type;
        this.schema = schema;
        this.path = path;
        this.defaultFactory = defaultFactory;
        this.autosaveIntervalTicks = autosaveIntervalTicks;
        this.staleCheckIntervalTicks = staleCheckIntervalTicks;
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "frankly-config-" + schema.type().getSimpleName());
            t.setDaemon(true);
            return t;
        });
        this.ticksUntilAutosave = autosaveIntervalTicks;
        this.ticksUntilStaleCheck = staleCheckIntervalTicks;
        loadInitial();
    }

    // -------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------

    /** The current in-memory config instance. Do not cache this across a {@link #update} call. */
    public T get() {
        return current;
    }

    /**
     * Applies {@code updater} to the current instance and marks the holder
     * dirty. For a mutable POJO, mutate the fields on the argument and
     * return it (same reference). For a record, build and return a new
     * instance — either way this is the one call site that needs to know
     * which kind of object it's holding.
     */
    public synchronized T update(UnaryOperator<T> updater) {
        current = updater.apply(current);
        dirty.set(true);
        return current;
    }

    /** True if there are in-memory changes not yet written to disk. */
    public boolean isDirty() {
        return dirty.get();
    }

    /** Call once per game tick (client or server) to drive autosave and stale-file checks. */
    public void tick() {
        if (dirty.get() && --ticksUntilAutosave <= 0) {
            ticksUntilAutosave = autosaveIntervalTicks;
            saveIfDirty();
        }
        if (staleCheckIntervalTicks > 0 && --ticksUntilStaleCheck <= 0) {
            ticksUntilStaleCheck = staleCheckIntervalTicks;
            checkForExternalChanges();
        }
    }

    /** Writes to disk asynchronously if dirty; no-op otherwise. */
    public void saveIfDirty() {
        if (!dirty.get() || !saveInProgress.compareAndSet(false, true)) {
            return;
        }
        T snapshot = current;
        ioExecutor.execute(() -> {
            try {
                if (!dirty.compareAndSet(true, false)) {
                    return;
                }
                writeToDisk(snapshot);
                saveListeners.forEach(listener -> listener.accept(snapshot));
            } finally {
                saveInProgress.set(false);
                if (dirty.get()) {
                    saveIfDirty();
                }
            }
        });
    }

    /** Writes to disk unconditionally, on the calling thread. Intended for shutdown hooks. */
    public synchronized void save() {
        writeToDisk(current);
        dirty.set(false);
        T snapshot = current;
        saveListeners.forEach(listener -> listener.accept(snapshot));
    }

    /**
     * If the file's on-disk modification time has moved since we last
     * read/wrote it, re-parses and merges it in (see class docs for the
     * dirty-wins-over-stale policy). Safe to call off the tick loop too,
     * e.g. from a manual "/mymod reload" command.
     */
    public synchronized void checkForExternalChanges() {
        if (dirty.get()) {
            return; // don't clobber unsaved in-memory changes
        }
        if (!Files.exists(path)) {
            return;
        }
        long onDisk;
        try {
            onDisk = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return;
        }
        if (onDisk <= lastKnownModifiedMillis) {
            return;
        }
        reloadFromDisk();
    }

    /** Forces a reload from disk right now, ignoring the dirty-wins policy. Use with care. */
    public synchronized void forceReload() {
        reloadFromDisk();
    }

    public void onReload(Consumer<T> listener) {
        reloadListeners.add(listener);
    }

    public void onSave(Consumer<T> listener) {
        saveListeners.add(listener);
    }

    /** Flushes any pending changes and shuts down the background save thread. Call on mod/server shutdown. */
    public void flushAndClose() {
        save();
        ioExecutor.shutdown();
    }

    public ConfigSchema schema() {
        return schema;
    }

    public Path path() {
        return path;
    }

    // -------------------------------------------------------------------
    // Load / save / reload internals
    // -------------------------------------------------------------------

    private void loadInitial() {
        if (!Files.exists(path)) {
            current = defaultFactory.get();
            writeToDisk(current);
            return;
        }
        T loaded = tryReadFromDisk(defaultFactory.get());
        current = loaded != null ? loaded : defaultFactory.get();
        try {
            lastKnownModifiedMillis = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            lastKnownModifiedMillis = System.currentTimeMillis();
        }
    }

    private void reloadFromDisk() {
        T reloaded = tryReadFromDisk(current);
        if (reloaded == null) {
            return; // unparsable file — in-memory state is untouched, error already logged
        }
        current = reloaded;
        try {
            lastKnownModifiedMillis = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            lastKnownModifiedMillis = System.currentTimeMillis();
        }
        reloadListeners.forEach(listener -> listener.accept(reloaded));
    }

    /**
     * @param fallback the instance to fall back to per-field; for a reload
     *                 this is the current instance, for first load it's a
     *                 fresh default one.
     * @return the merged instance, or {@code null} if the file couldn't be
     *         parsed as JSON at all.
     */
    private @Nullable T tryReadFromDisk(T fallback) {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | JsonParseException | IllegalStateException e) {
            LOGGER.error("Config file {} is not valid JSON; keeping current in-memory config", path, e);
            return null;
        }
        return type.cast(schema.mergeFromJson(root, fallback, message -> LOGGER.warn("[{}] {}", path, message)));
    }

    private void writeToDisk(T instance) {
        JsonObject root = schema.serialize(instance);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            lastKnownModifiedMillis = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            LOGGER.error("Failed to write config to {}", path, e);
        }
    }

    // -------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------

    public static <T> Builder<T> builder(Class<T> type, Supplier<T> defaultFactory) {
        return new Builder<>(type, defaultFactory);
    }

    public static final class Builder<T> {
        private final Class<T> type;
        private final Supplier<T> defaultFactory;
        private Path path;
        private int autosaveIntervalTicks = 20 * 60 * 5; // ~5 minutes
        private int staleCheckIntervalTicks = 20 * 10; // ~10 seconds

        private Builder(Class<T> type, Supplier<T> defaultFactory) {
            this.type = type;
            this.defaultFactory = defaultFactory;
        }

        public Builder<T> path(Path path) {
            this.path = path;
            return this;
        }

        /** How often {@link #tick()} checks for unsaved changes to flush. 0 disables autosave (manual {@link #save()} only). */
        public Builder<T> autosaveTicks(int ticks) {
            this.autosaveIntervalTicks = ticks;
            return this;
        }

        /** How often {@link #tick()} checks whether the file changed externally. 0 disables the check. */
        public Builder<T> staleCheckTicks(int ticks) {
            this.staleCheckIntervalTicks = ticks;
            return this;
        }

        public FranklyConfigHolder<T> build() {
            if (path == null) {
                throw new IllegalStateException("FranklyConfigHolder.Builder requires .path(...)");
            }
            ConfigSchema schema = ConfigSchema.of(type);
            return new FranklyConfigHolder<>(type, schema, path, defaultFactory, autosaveIntervalTicks,
                    staleCheckIntervalTicks);
        }
    }
}
