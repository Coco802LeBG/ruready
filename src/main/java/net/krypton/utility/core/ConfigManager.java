package net.krypton.utility.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.krypton.utility.KryptonUtility;
import net.krypton.utility.core.setting.Setting;
import net.krypton.utility.ui.Theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON persistence: config/krypton-utility.json */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCHEMA = 1;

    private ConfigManager() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("krypton-utility.json");
    }

    public static void load() {
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (root.has("accent")) {
                Theme.setAccent(root.get("accent").getAsInt());
            }
            JsonObject modules = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : ModuleManager.all()) {
                if (!modules.has(m.id())) {
                    continue;
                }
                JsonObject o = modules.getAsJsonObject(m.id());
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> setting : m.settings()) {
                        JsonElement e = s.get(setting.key);
                        if (e != null) {
                            setting.fromJson(e);
                        }
                    }
                }
                if (m.persistEnabled() && o.has("enabled")) {
                    m.setEnabled(o.get("enabled").getAsBoolean());
                    m.toggleAnim = m.isEnabled() ? 1f : 0f;
                }
            }
        } catch (Exception e) {
            KryptonUtility.LOGGER.warn("Could not read config, using defaults", e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA);
        root.addProperty("accent", Theme.accentIndex());
        JsonObject modules = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            if (m.persistEnabled()) {
                o.addProperty("enabled", m.isEnabled());
            }
            JsonObject s = new JsonObject();
            for (Setting<?> setting : m.settings()) {
                s.add(setting.key, setting.toJson());
            }
            o.add("settings", s);
            modules.add(m.id(), o);
        }
        root.add("modules", modules);
        try {
            Files.writeString(file(), GSON.toJson(root));
        } catch (IOException e) {
            KryptonUtility.LOGGER.warn("Could not write config", e);
        }
    }
}
