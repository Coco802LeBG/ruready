package net.krypton.utility.mixin;

import net.krypton.utility.module.FullbrightModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feeds the lightmap an out-of-range gamma without ever touching the stored option value. */
@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin<T> {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void krypton$forceGamma(CallbackInfoReturnable<T> cir) {
        if (!FullbrightModule.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Options options = mc == null ? null : mc.options;
        if (options != null && (Object) this == options.gamma()) {
            cir.setReturnValue((T) (Object) Double.valueOf(FullbrightModule.gamma()));
        }
    }
}
