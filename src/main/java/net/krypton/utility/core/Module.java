package net.krypton.utility.core;

import net.krypton.utility.core.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class of every feature. A module owns its settings and reacts to enable/disable/tick. */
public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final boolean defaultEnabled;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    // ClickGUI animation state
    public float toggleAnim;
    public float expandAnim;
    public boolean expanded;

    protected Module(String id, String name, String description, Category category, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    public List<Setting<?>> visibleSettings() {
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : settings) {
            if (!s.hidden()) {
                out.add(s);
            }
        }
        return out;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Whether the enabled state is written to the config file. */
    public boolean persistEnabled() {
        return true;
    }

    protected boolean canEnable() {
        return true;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean value) {
        if (value == enabled) {
            return;
        }
        if (value && !canEnable()) {
            return;
        }
        enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick(Minecraft mc) {
    }

    /** Called when the player leaves a world / server. */
    public void onDisconnect() {
    }
}
