package net.krypton.utility.module;

import net.krypton.utility.core.Category;
import net.krypton.utility.core.Module;
import net.krypton.utility.core.setting.BoolSetting;
import net.krypton.utility.core.setting.ModeSetting;
import net.krypton.utility.core.setting.NumberSetting;
import net.krypton.utility.render.EspRenderer;
import net.krypton.utility.render.HighlightSource;
import net.krypton.utility.render.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Collision-box overlay for entities and storage blocks. */
public final class EspModule extends Module implements HighlightSource {
    public final ModeSetting style = add(new ModeSetting("style", "Style", "Corners", EspRenderer.STYLES));
    public final NumberSetting range = add(new NumberSetting("range", "Range", 96, 16, 256, 8));
    public final NumberSetting opacity = add(new NumberSetting("opacity", "Fill opacity", 60, 10, 200, 10));
    public final BoolSetting players = add(new BoolSetting("players", "Players", true));
    public final BoolSetting hostile = add(new BoolSetting("hostile", "Hostile mobs", true));
    public final BoolSetting passive = add(new BoolSetting("passive", "Passive mobs", false));
    public final BoolSetting items = add(new BoolSetting("items", "Dropped items", false));
    public final BoolSetting containers = add(new BoolSetting("containers", "Containers", true));
    public final BoolSetting labels = add(new BoolSetting("labels", "Labels", true));

    private static final int PLAYER = 0xFF60A5FA;
    private static final int HOSTILE = 0xFFF43F5E;
    private static final int PASSIVE = 0xFF34D399;
    private static final int ITEM = 0xFFFACC15;

    private final List<Target> containerCache = new ArrayList<>();
    private int refreshTimer;

    public EspModule() {
        super("esp", "ESP Boxes", "Bounding boxes around entities and storage blocks.", Category.VISUALS, false);
    }

    @Override
    protected void onEnable() {
        refreshTimer = 0;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (!containers.get() || mc.level == null || mc.player == null) {
            containerCache.clear();
            return;
        }
        if (refreshTimer-- > 0) {
            return;
        }
        refreshTimer = 20;
        scanContainers(mc);
    }

    @Override
    public void onDisconnect() {
        containerCache.clear();
    }

    private void scanContainers(Minecraft mc) {
        ClientLevel level = mc.level;
        List<Target> found = new ArrayList<>();
        int radius = range.asInt() / 16 + 1;
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
            for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    Integer color = containerColor(be);
                    if (color == null) {
                        continue;
                    }
                    BlockPos p = be.getBlockPos();
                    AABB box = new AABB(p);
                    if (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity) {
                        box = box.deflate(0.04);
                    }
                    found.add(new Target(box, color, containerName(be), 0));
                }
            }
        }
        containerCache.clear();
        containerCache.addAll(found);
    }

    private static Integer containerColor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) {
            return 0xFFF43F5E;
        }
        if (be instanceof ChestBlockEntity) {
            return 0xFFF59E0B;
        }
        if (be instanceof EnderChestBlockEntity) {
            return 0xFFA855F7;
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return 0xFFEC4899;
        }
        if (be instanceof BarrelBlockEntity) {
            return 0xFFD97706;
        }
        if (be instanceof HopperBlockEntity) {
            return 0xFF94A3B8;
        }
        if (be instanceof DispenserBlockEntity) {
            return 0xFF64748B;
        }
        return null;
    }

    private static String containerName(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) {
            return "Trapped chest";
        }
        if (be instanceof ChestBlockEntity) {
            return "Chest";
        }
        if (be instanceof EnderChestBlockEntity) {
            return "Ender chest";
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return "Shulker";
        }
        if (be instanceof BarrelBlockEntity) {
            return "Barrel";
        }
        if (be instanceof HopperBlockEntity) {
            return "Hopper";
        }
        return "Dispenser";
    }

    // ------------------------------------------------------------------ HighlightSource

    @Override
    public String style() {
        return style.get();
    }

    @Override
    public int fillAlpha() {
        return opacity.asInt();
    }

    @Override
    public boolean labels() {
        return labels.get();
    }

    @Override
    public void collect(float pt, List<Target> out) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        double maxSq = range.asDouble() * range.asDouble();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player && !FreecamModule.isActive()) {
                continue;
            }
            int color;
            if (e instanceof Player) {
                if (!players.get()) {
                    continue;
                }
                color = PLAYER;
            } else if (e instanceof Enemy) {
                if (!hostile.get()) {
                    continue;
                }
                color = HOSTILE;
            } else if (e instanceof ItemEntity) {
                if (!items.get()) {
                    continue;
                }
                color = ITEM;
            } else if (e instanceof LivingEntity) {
                if (!passive.get()) {
                    continue;
                }
                color = PASSIVE;
            } else {
                continue;
            }
            Vec3 lp = e.getPosition(pt);
            double d = lp.distanceToSqr(cam);
            if (d > maxSq) {
                continue;
            }
            AABB box = e.getBoundingBox().move(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ());
            String label = e instanceof ItemEntity ie ? ie.getItem().getHoverName().getString() : e.getName().getString();
            out.add(new Target(box, color, label, d));
        }

        if (containers.get()) {
            for (Target t : containerCache) {
                Vec3 c = t.box().getCenter();
                double d = c.distanceToSqr(cam);
                if (d <= maxSq) {
                    out.add(new Target(t.box(), t.color(), t.label(), d));
                }
            }
        }
    }
}
