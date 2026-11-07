package net.krypton.utility.core;

import net.krypton.utility.module.BlockScannerModule;
import net.krypton.utility.module.EspModule;
import net.krypton.utility.module.FreecamModule;
import net.krypton.utility.module.FullbrightModule;
import net.krypton.utility.module.HudModule;
import net.krypton.utility.module.InventoryModule;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    public static void init() {
        MODULES.clear();
        MODULES.add(new FreecamModule());
        MODULES.add(new FullbrightModule());
        MODULES.add(new EspModule());
        MODULES.add(new BlockScannerModule());
        MODULES.add(new InventoryModule());
        MODULES.add(new HudModule());
        for (Module m : MODULES) {
            if (m.defaultEnabled()) {
                m.setEnabled(true);
            }
            m.toggleAnim = m.isEnabled() ? 1f : 0f;
        }
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> in(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.category() == category) {
                out.add(m);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) {
            if (m.getClass() == type) {
                return (T) m;
            }
        }
        throw new IllegalStateException("Module not registered: " + type.getName());
    }

    public static void tick(Minecraft mc) {
        for (Module m : MODULES) {
            if (m.isEnabled()) {
                m.onTick(mc);
            }
        }
    }

    public static void disconnect() {
        for (Module m : MODULES) {
            m.onDisconnect();
        }
    }
}
