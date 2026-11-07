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
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Incremental chunk scanner. Loaded chunks around the player are visited a few per tick (bounded work), results are
 * cached per chunk and re-validated periodically, then handed to the shared highlight renderer.
 */
public final class BlockScannerModule extends Module implements HighlightSource {
    private enum Kind {
        DIAMOND("Diamond", 0xFF22D3EE, BlockTags.DIAMOND_ORES, null),
        ANCIENT_DEBRIS("Ancient debris", 0xFFB45309, null, Blocks.ANCIENT_DEBRIS),
        EMERALD("Emerald", 0xFF22C55E, BlockTags.EMERALD_ORES, null),
        GOLD("Gold", 0xFFFACC15, BlockTags.GOLD_ORES, null),
        IRON("Iron", 0xFFE5C9B0, BlockTags.IRON_ORES, null),
        COPPER("Copper", 0xFFFB923C, BlockTags.COPPER_ORES, null),
        REDSTONE("Redstone", 0xFFEF4444, BlockTags.REDSTONE_ORES, null),
        LAPIS("Lapis", 0xFF3B82F6, BlockTags.LAPIS_ORES, null),
        COAL("Coal", 0xFF6B7280, BlockTags.COAL_ORES, null),
        SPAWNER("Spawner", 0xFFA855F7, null, Blocks.SPAWNER);

        final String label;
        final int color;
        final TagKey<Block> tag;
        final Block block;

        Kind(String label, int color, TagKey<Block> tag, Block block) {
            this.label = label;
            this.color = color;
            this.tag = tag;
            this.block = block;
        }

        boolean matches(BlockState state) {
            return tag != null ? state.is(tag) : state.is(block);
        }
    }

    private record Hit(long pos, Kind kind) {
    }

    private record CachedChunk(List<Hit> hits, long scannedAt) {
    }

    public final ModeSetting style = add(new ModeSetting("style", "Style", "Filled", EspRenderer.STYLES));
    public final NumberSetting radius = add(new NumberSetting("radius", "Radius (chunks)", 4, 1, 8, 1));
    public final NumberSetting vertical = add(new NumberSetting("vertical", "Vertical range", 48, 8, 128, 8));
    public final NumberSetting maxBoxes = add(new NumberSetting("max_boxes", "Max boxes", 192, 32, 1024, 32));
    public final NumberSetting opacity = add(new NumberSetting("opacity", "Fill opacity", 70, 10, 200, 10));
    public final BoolSetting exposedOnly = add(new BoolSetting("exposed_only", "Exposed only", false));
    public final BoolSetting labels = add(new BoolSetting("labels", "Labels", false));
    private final Map<Kind, BoolSetting> kinds = new HashMap<>();

    private final Map<Long, CachedChunk> cache = new HashMap<>();
    private int tickCounter;

    public BlockScannerModule() {
        super("block_scanner", "Block Scanner", "Highlights selected blocks in loaded chunks.", Category.WORLD, false);
        for (Kind k : Kind.values()) {
            boolean def = k == Kind.DIAMOND || k == Kind.ANCIENT_DEBRIS || k == Kind.EMERALD || k == Kind.SPAWNER;
            kinds.put(k, add(new BoolSetting("kind_" + k.name().toLowerCase(), k.label, def)));
        }
    }

    @Override
    protected void onEnable() {
        cache.clear();
    }

    @Override
    protected void onDisable() {
        cache.clear();
    }

    @Override
    public void onDisconnect() {
        cache.clear();
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private List<Kind> activeKinds() {
        List<Kind> list = new ArrayList<>();
        for (Kind k : Kind.values()) {
            if (kinds.get(k).get()) {
                list.add(k);
            }
        }
        return list;
    }

    @Override
    public void onTick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        tickCounter++;
        List<Kind> active = activeKinds();
        if (active.isEmpty()) {
            cache.clear();
            return;
        }
        int r = radius.asInt();
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;

        // drop chunks that left the radius
        cache.keySet().removeIf(k -> {
            int cx = (int) (k >> 32);
            int cz = (int) (long) k;
            return Math.abs(cx - pcx) > r + 1 || Math.abs(cz - pcz) > r + 1;
        });

        // pick the stalest chunks first, nearest ring first for new ones
        int budget = 2;
        for (int ring = 0; ring <= r && budget > 0; ring++) {
            for (int dx = -ring; dx <= ring && budget > 0; dx++) {
                for (int dz = -ring; dz <= ring && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    CachedChunk cached = cache.get(key(cx, cz));
                    if (cached != null && tickCounter - cached.scannedAt() < 100) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    cache.put(key(cx, cz), scan(level, chunk, cx, cz, mc.player.blockPosition().getY(), active));
                    budget--;
                }
            }
        }
    }

    private CachedChunk scan(ClientLevel level, LevelChunk chunk, int cx, int cz, int py, List<Kind> active) {
        int minY = level.dimensionType().minY();
        int maxY = minY + level.dimensionType().height() - 1;
        int y0 = Math.max(minY, py - vertical.asInt());
        int y1 = Math.min(maxY, py + vertical.asInt());
        boolean exposed = exposedOnly.get();
        List<Hit> hits = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = y0; y <= y1; y++) {
                    pos.set((cx << 4) + x, y, (cz << 4) + z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    for (Kind k : active) {
                        if (k.matches(state)) {
                            if (!exposed || isExposed(level, pos, neighbour)) {
                                hits.add(new Hit(pos.asLong(), k));
                            }
                            break;
                        }
                    }
                }
            }
        }
        return new CachedChunk(hits, tickCounter);
    }

    private static boolean isExposed(ClientLevel level, BlockPos pos, BlockPos.MutableBlockPos tmp) {
        for (Direction d : Direction.values()) {
            tmp.set(pos).move(d);
            if (!level.getBlockState(tmp).canOcclude()) {
                return true;
            }
        }
        return false;
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
        if (mc.player == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        double maxSq = (radius.asInt() * 16.0 + 8) * (radius.asInt() * 16.0 + 8);
        List<Target> all = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (CachedChunk c : cache.values()) {
            for (Hit h : c.hits()) {
                p.set(h.pos());
                double d = Vec3.atCenterOf(p).distanceToSqr(cam);
                if (d > maxSq) {
                    continue;
                }
                all.add(new Target(new AABB(p).deflate(0.02), h.kind().color, h.kind().label, d));
            }
        }
        all.sort((a, b) -> Double.compare(a.distSq(), b.distSq()));
        int limit = Math.min(all.size(), maxBoxes.asInt());
        for (int i = 0; i < limit; i++) {
            out.add(all.get(i));
        }
    }
}
