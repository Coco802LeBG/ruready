package net.krypton.utility.module;

import net.krypton.utility.core.Category;
import net.krypton.utility.core.Module;
import net.krypton.utility.core.setting.BoolSetting;
import net.krypton.utility.core.setting.ModeSetting;
import net.krypton.utility.core.setting.NumberSetting;

/** Settings holder for the container tools (buttons, sorting and quick transfer). Logic lives in the inventory package. */
public final class InventoryModule extends Module {
    public final BoolSetting showButtons = add(new BoolSetting("show_buttons", "Show buttons", true));
    public final ModeSetting sortBy = add(new ModeSetting("sort_by", "Sort by", "Name", "Name", "Count"));
    public final BoolSetting includeHotbar = add(new BoolSetting("include_hotbar", "Include hotbar", false));
    public final NumberSetting clicksPerTick = add(new NumberSetting("clicks_per_tick", "Clicks per tick", 2, 1, 8, 1));
    public final NumberSetting delay = add(new NumberSetting("delay", "Delay (ticks)", 0, 0, 5, 1));

    public InventoryModule() {
        super("inventory_tools", "Inventory Tools", "Sort containers and move items in bulk.", Category.INVENTORY, true);
    }

    @Override
    public void onDisable() {
        net.krypton.utility.inventory.InventoryActions.cancel();
    }
}
