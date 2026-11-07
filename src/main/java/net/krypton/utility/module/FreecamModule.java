package net.krypton.utility.module;

import net.krypton.utility.core.Category;
import net.krypton.utility.core.Module;
import net.krypton.utility.core.ModuleManager;
import net.krypton.utility.core.setting.BoolSetting;
import net.krypton.utility.core.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Detached camera. A client-only RemotePlayer (never added to the level) becomes the camera entity, so vanilla
 * interpolates, culls and renders from its pose while the real player stays where it is. Mouse look and movement
 * keys are redirected by two small mixins.
 */
public final class FreecamModule extends Module {
    public final NumberSetting speed = add(new NumberSetting("speed", "Speed", 1.0, 0.1, 5.0, 0.1));
    public final NumberSetting smoothing = add(new NumberSetting("smoothing", "Smoothing", 0.55, 0.0, 0.95, 0.05));
    public final BoolSetting blockInteraction = add(new BoolSetting("block_interaction", "Block interactions", true));

    private static boolean active;
    private static boolean bypass;

    private RemotePlayer camera;
    private Vec3 pos = Vec3.ZERO;
    private Vec3 velocity = Vec3.ZERO;
    private float yaw;
    private float pitch;

    public FreecamModule() {
        super("freecam", "Freecam", "Detach the camera from your body and fly around.", Category.VISUALS, false);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected boolean canEnable() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        camera = new RemotePlayer(mc.level, mc.player.getGameProfile());
        Vec3 eye = mc.gameRenderer.getMainCamera().position();
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        pos = new Vec3(eye.x, eye.y - camera.getEyeHeight(), eye.z);
        velocity = Vec3.ZERO;
        apply(false);
        active = true;
        mc.setCameraEntity(camera);
    }

    @Override
    protected void onDisable() {
        active = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        camera = null;
    }

    @Override
    public void onDisconnect() {
        if (isEnabled()) {
            active = false;
            camera = null;
            setEnabled(false);
        }
    }

    @Override
    public void onTick(Minecraft mc) {
        if (camera == null || mc.player == null || mc.level == null) {
            return;
        }
        // read the real key state through our own bypass flag
        bypass = true;
        Options o = mc.options;
        double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double strafe = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        double vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
        boolean fast = o.keySprint.isDown();
        bypass = false;

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        Vec3 look = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
        Vec3 left = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        Vec3 wish = look.scale(forward).add(left.scale(strafe)).add(0, vertical, 0);
        if (wish.lengthSqr() > 1.0E-6) {
            wish = wish.normalize();
        }
        double v = speed.asDouble() * (fast ? 2.5 : 1.0);
        Vec3 target = wish.scale(v);
        double k = 1.0 - smoothing.asDouble();
        velocity = velocity.add(target.subtract(velocity).scale(k));
        if (velocity.lengthSqr() < 1.0E-6 && wish.lengthSqr() < 1.0E-6) {
            velocity = Vec3.ZERO;
        }
        pos = pos.add(velocity);
        apply(true);
    }

    private void apply(boolean interpolate) {
        if (interpolate) {
            camera.setOldPosAndRot();
        }
        camera.setPos(pos.x, pos.y, pos.z);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        if (!interpolate) {
            camera.setOldPosAndRot();
        }
    }

    private void look(double yawDelta, double pitchDelta) {
        yaw += (float) yawDelta * 0.15f;
        pitch = Mth.clamp(pitch + (float) pitchDelta * 0.15f, -90f, 90f);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        // keep the previous rotation in sync so mouse look is not delayed by tick interpolation
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    public Vec3 cameraPosition() {
        return pos.add(0, camera == null ? 1.62 : camera.getEyeHeight(), 0);
    }

    // ------------------------------------------------------------------ static hooks used by the mixins

    public static boolean isActive() {
        return active;
    }

    public static FreecamModule instance() {
        return ModuleManager.get(FreecamModule.class);
    }

    public static boolean handleTurn(Entity self, double yawDelta, double pitchDelta) {
        if (!active) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (self != mc.player) {
            return false;
        }
        FreecamModule m = instance();
        if (m.camera == null) {
            return false;
        }
        m.look(yawDelta, pitchDelta);
        return true;
    }

    public static boolean shouldBlock(KeyMapping key) {
        if (!active || bypass) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        Options o = mc.options;
        if (o == null) {
            return false;
        }
        if (key == o.keyUp || key == o.keyDown || key == o.keyLeft || key == o.keyRight
                || key == o.keyJump || key == o.keyShift || key == o.keySprint) {
            return true;
        }
        return instance().blockInteraction.get() && (key == o.keyAttack || key == o.keyUse || key == o.keyPickItem);
    }
}
