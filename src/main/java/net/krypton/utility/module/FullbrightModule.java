package net.krypton.utility.module;

import net.krypton.utility.core.Category;
import net.krypton.utility.core.Module;
import net.krypton.utility.core.ModuleManager;
import net.krypton.utility.core.setting.NumberSetting;

/** Forces a very high gamma for the lightmap. The saved option value is never modified. */
public final class FullbrightModule extends Module {
    public final NumberSetting gamma = add(new NumberSetting("gamma", "Gamma", 12.0, 2.0, 16.0, 0.5));

    public FullbrightModule() {
        super("fullbright", "Fullbright", "Maximum brightness everywhere, caves included.", Category.VISUALS, false);
    }

    public static boolean isActive() {
        try {
            return ModuleManager.get(FullbrightModule.class).isEnabled();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static double gamma() {
        return ModuleManager.get(FullbrightModule.class).gamma.asDouble();
    }
}
