package net.krypton.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.krypton.utility.core.ConfigManager;
import net.krypton.utility.core.ModuleManager;
import net.krypton.utility.hud.HudRenderer;
import net.krypton.utility.inventory.InventoryActions;
import net.krypton.utility.inventory.InventoryUi;
import net.krypton.utility.module.FreecamModule;
import net.krypton.utility.render.EspRenderer;
import net.krypton.utility.render.Projector;
import net.krypton.utility.ui.ClickGuiScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KryptonUtility implements ClientModInitializer {
    public static final String MOD_ID = "krypton_utility";
    public static final Logger LOGGER = LoggerFactory.getLogger("Krypton Utility");

    private static KeyMapping openGui;
    private static KeyMapping toggleFreecam;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ConfigManager.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.krypton_utility.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        toggleFreecam = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.krypton_utility.toggle_freecam", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        ClientTickEvents.END_CLIENT_TICK.register(KryptonUtility::tick);

        // HUD + screen-space ESP, drawn after the vanilla subtitles element
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), HudRenderer::render);

        // camera matrices for the projected ESP + depth-tested "World 3D" boxes
        WorldRenderEvents.END_EXTRACTION.register(Projector::capture);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(EspRenderer::render3D);

        InventoryUi.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ModuleManager.disconnect();
            Projector.invalidate();
            InventoryActions.cancel();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        LOGGER.info("Krypton Utility loaded");
    }

    private static void tick(Minecraft mc) {
        while (openGui.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new ClickGuiScreen());
            }
        }
        while (toggleFreecam.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                ModuleManager.get(FreecamModule.class).toggle();
            }
        }
        ModuleManager.tick(mc);
        InventoryActions.tick(mc);
    }
}
