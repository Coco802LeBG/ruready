package net.krypton.utility.module;

import net.krypton.utility.core.Category;
import net.krypton.utility.core.Module;
import net.krypton.utility.core.setting.BoolSetting;
import net.krypton.utility.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Customisable HUD: info card + active-module list. Widget positions are stored as screen fractions. */
public final class HudModule extends Module {
    public final BoolSetting watermark = add(new BoolSetting("watermark", "Title row", true));
    public final BoolSetting coords = add(new BoolSetting("coords", "Coordinates", true));
    public final BoolSetting converted = add(new BoolSetting("converted", "Nether / Overworld coords", true));
    public final BoolSetting facing = add(new BoolSetting("facing", "Facing", true));
    public final BoolSetting biome = add(new BoolSetting("biome", "Biome", true));
    public final BoolSetting fps = add(new BoolSetting("fps", "FPS", true));
    public final BoolSetting speed = add(new BoolSetting("speed", "Speed", true));
    public final BoolSetting moduleList = add(new BoolSetting("module_list", "Module list", true));

    public final NumberSetting infoX = add(NumberSetting.hiddenFraction("info_x", 0.0));
    public final NumberSetting infoY = add(NumberSetting.hiddenFraction("info_y", 0.0));
    public final NumberSetting listX = add(NumberSetting.hiddenFraction("list_x", 1.0));
    public final NumberSetting listY = add(NumberSetting.hiddenFraction("list_y", 0.0));

    private double prevX;
    private double prevZ;
    private double blocksPerSecond;

    public HudModule() {
        super("hud", "HUD", "Coordinates and status widgets. Drag them in the HUD editor.", Category.INTERFACE, true);
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        double dx = mc.player.getX() - prevX;
        double dz = mc.player.getZ() - prevZ;
        blocksPerSecond = Math.sqrt(dx * dx + dz * dz) * 20.0;
        prevX = mc.player.getX();
        prevZ = mc.player.getZ();
    }

    public double blocksPerSecond() {
        return blocksPerSecond;
    }
}
