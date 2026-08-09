package com.frank1o3.franklylib.client.gui.animation;

import com.frank1o3.franklylib.FranklyLib;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Optional, resource-pack-driven widget animation service. Definitions live at
 * {@code assets/<namespace>/franklylib/ui_animations/<name>.json}; a widget
 * opts
 * in by passing {@code <namespace>:<name>} to its builder.
 */
@Environment(EnvType.CLIENT)
public final class FranklyUiAnimations {
    private static final String DIRECTORY = "franklylib/ui_animations";
    private static final Map<Object, Instance> INSTANCES = new WeakHashMap<>();
    private static volatile Map<Identifier, Definition> definitions = Map.of();

    private FranklyUiAnimations() {
    }

    public static void registerReloadListener() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(FranklyLib.id("ui_animations"), (ResourceManagerReloadListener) manager -> {
                    Map<Identifier, Definition> loaded = new HashMap<>();
                    manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"))
                            .forEach((fileId, resource) -> {
                                Definition definition = parse(fileId, resource);
                                if (definition != null) {
                                    String path = fileId.getPath().substring(DIRECTORY.length() + 1,
                                            fileId.getPath().length() - 5);
                                    loaded.put(Identifier.fromNamespaceAndPath(fileId.getNamespace(), path),
                                            definition);
                                }
                            });
                    definitions = Map.copyOf(loaded);
                    synchronized (INSTANCES) {
                        INSTANCES.clear();
                    }
                });
    }

    public static FranklyUiAnimation frame(Object owner, @Nullable Identifier animationId, boolean hovered,
            boolean active) {
        Definition definition = animationId == null ? null : definitions.get(animationId);
        if (definition == null) {
            return FranklyUiAnimation.IDENTITY;
        }
        FranklyUiAnimation target = !active ? definition.disabled : hovered ? definition.hover : definition.idle;
        long now = System.nanoTime() / 1_000_000L;
        synchronized (INSTANCES) {
            Instance instance = INSTANCES.get(owner);
            if (instance == null || !instance.animationId.equals(animationId)) {
                instance = new Instance(animationId, target, target, now);
                INSTANCES.put(owner, instance);
                return target;
            }
            float elapsed = Math.min(1f, (now - instance.startedAt) / (float) definition.durationMs);
            FranklyUiAnimation current = FranklyUiAnimation.lerp(instance.from, instance.to,
                    ease(elapsed, definition.easing));
            if (!target.equals(instance.to)) {
                instance = new Instance(animationId, current, target, now);
                INSTANCES.put(owner, instance);
                return current;
            }
            return current;
        }
    }

    private static @Nullable Definition parse(Identifier id, Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            int duration = root.has("duration_ms") ? Math.max(1, root.get("duration_ms").getAsInt()) : 120;
            String easing = root.has("easing") ? root.get("easing").getAsString() : "linear";
            JsonObject states = root.has("states") ? root.getAsJsonObject("states") : new JsonObject();
            return new Definition(duration, easing, state(states, "idle"), state(states, "hover"),
                    state(states, "disabled"));
        } catch (Exception exception) {
            FranklyLib.LOGGER.warn("Ignoring invalid UI animation {}", id, exception);
            return null;
        }
    }

    private static FranklyUiAnimation state(JsonObject states, String name) {
        JsonObject value = states.has(name) ? states.getAsJsonObject(name) : new JsonObject();
        return new FranklyUiAnimation(number(value, "translate_x", 0f), number(value, "translate_y", 0f),
                number(value, "scale", 1f), number(value, "alpha", 1f));
    }

    private static float number(JsonObject object, String key, float fallback) {
        JsonElement value = object.get(key);
        return value == null ? fallback : value.getAsFloat();
    }

    private static float ease(float value, String easing) {
        return switch (easing) {
            case "in_quad" -> value * value;
            case "out_quad" -> 1f - (1f - value) * (1f - value);
            case "in_out_quad" -> value < .5f ? 2f * value * value : 1f - (float) Math.pow(-2f * value + 2f, 2) / 2f;
            default -> value;
        };
    }

    private record Definition(int durationMs, String easing, FranklyUiAnimation idle, FranklyUiAnimation hover,
            FranklyUiAnimation disabled) {
    }

    private record Instance(Identifier animationId, FranklyUiAnimation from, FranklyUiAnimation to, long startedAt) {
    }

    public static FranklyUiAnimation beginTransform(GuiGraphicsExtractor graphics, Object owner,
            @Nullable Identifier animationId, boolean hovered, boolean active, float centerX, float centerY) {
        FranklyUiAnimation transform = frame(owner, animationId, hovered, active);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX + transform.translateX(), centerY + transform.translateY());
        graphics.pose().scale(transform.scale());
        graphics.pose().translate(-centerX, -centerY);
        return transform;
    }

    public static void endTransform(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public static int applyAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24;
        return (Math.round(baseAlpha * Math.clamp(alpha, 0f, 1f)) << 24) | (color & 0x00FFFFFF);
    }
}
