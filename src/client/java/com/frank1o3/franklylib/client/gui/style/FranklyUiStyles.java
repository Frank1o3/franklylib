package com.frank1o3.franklylib.client.gui.style;

import com.frank1o3.franklylib.FranklyLib;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/** Resource-pack UI styles loaded from {@code assets/<namespace>/ui_styles/*.json}. */
@Environment(EnvType.CLIENT)
public final class FranklyUiStyles {
    private static volatile String resourceDirectory = "ui_styles";
    private static volatile Map<Identifier, FranklyUiStyle> definitions = Map.of();

    private FranklyUiStyles() {}

    /**
     * Changes the path below {@code assets/<namespace>} scanned on the next resource reload.
     * For example, {@code setResourceDirectory("my_mod/ui_styles")} lets a mod keep these
     * files beside its own assets. Call during client initialisation.
     */
    public static void setResourceDirectory(String directory) {
        if (directory == null || directory.isBlank() || directory.startsWith("/") || directory.endsWith("/")) {
            throw new IllegalArgumentException("UI style directory must be a relative, non-empty path");
        }
        resourceDirectory = directory;
    }

    public static String getResourceDirectory() { return resourceDirectory; }

    public static @Nullable FranklyUiStyle get(@Nullable Identifier styleId) {
        return styleId == null ? null : definitions.get(styleId);
    }

    /** Returns the requested style, or the supplied in-code defaults when it does not exist. */
    public static FranklyUiStyle resolve(@Nullable Identifier styleId, FranklyUiStyle fallback) {
        FranklyUiStyle style = get(styleId);
        return style != null ? style : fallback;
    }

    public static void registerReloadListener() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(FranklyLib.id("ui_styles"),
                (ResourceManagerReloadListener) manager -> {
                    String directory = resourceDirectory;
                    Map<Identifier, FranklyUiStyle> loaded = new HashMap<>();
                    manager.listResources(directory, id -> id.getPath().endsWith(".json")).forEach((fileId, resource) -> {
                        FranklyUiStyle style = parse(fileId, resource);
                        if (style != null) {
                            String path = fileId.getPath().substring(directory.length() + 1, fileId.getPath().length() - 5);
                            loaded.put(Identifier.fromNamespaceAndPath(fileId.getNamespace(), path), style);
                        }
                    });
                    definitions = Map.copyOf(loaded);
                });
    }

    private static @Nullable FranklyUiStyle parse(Identifier id, Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject colors = object(root, "colors");
            JsonObject border = object(root, "border");
            FranklyUiStyle base = FranklyUiStyle.DEFAULT;
            String type = string(border, "type", string(root, "border_type", base.borderType().name().toLowerCase()));
            FranklyUiStyle.BorderType borderType = switch (type.toLowerCase()) {
                case "none" -> FranklyUiStyle.BorderType.NONE;
                case "rounded" -> FranklyUiStyle.BorderType.ROUNDED;
                default -> FranklyUiStyle.BorderType.SQUARE;
            };
            int zIndex = integer(root, "z_index", integer(root, "zIndex", base.zIndex()));
            return new FranklyUiStyle(
                    color(colors, "background", base.background()), color(colors, "hover_background", base.hoverBackground()),
                    color(colors, "disabled_background", base.disabledBackground()), color(colors, "border", base.borderColor()),
                    color(colors, "text", base.textColor()), color(colors, "disabled_text", base.disabledTextColor()),
                    color(colors, "accent", base.accentColor()), integer(root, "padding", base.padding()), borderType,
                    integer(border, "radius", integer(root, "border_radius", base.borderRadius())),
                    Math.max(0, integer(border, "width", integer(root, "border_width", base.borderWidth()))),
                    zIndex);
        } catch (Exception exception) {
            FranklyLib.LOGGER.warn("Ignoring invalid UI style {}", id, exception);
            return null;
        }
    }

    private static JsonObject object(JsonObject root, String key) {
        JsonElement value = root.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }
    private static String string(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value == null ? fallback : value.getAsString();
    }
    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value == null ? fallback : value.getAsInt();
    }
    private static int color(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        if (value == null) return fallback;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) return value.getAsInt();
        String hex = value.getAsString().replace("#", "").replace("0x", "");
        long parsed = Long.parseLong(hex, 16);
        return (int) (hex.length() <= 6 ? parsed | 0xFF000000L : parsed);
    }
}
