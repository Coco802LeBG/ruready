package net.krypton.utility.core.setting;

import com.google.gson.JsonElement;

/** A typed, serialisable module option. Settings drive both the module logic and the ClickGUI widgets. */
public abstract class Setting<T> {
    public final String key;
    public final String name;
    protected T value;
    protected boolean hidden;

    protected Setting(String key, String name, T def) {
        this.key = key;
        this.name = name;
        this.value = def;
    }

    public T get() {
        return value;
    }

    /** Hidden settings are persisted but never shown in the ClickGUI (e.g. HUD widget positions). */
    public boolean hidden() {
        return hidden;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}
