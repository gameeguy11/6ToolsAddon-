package gamerguy11.sixtoolsaddon.mixin.swing;

import gamerguy11.sixtoolsaddon.modules.visual.SwingSpeed;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntitySwingSpeedMixin {

    @Inject(method = "getHandSwingProgress", at = @At("RETURN"), cancellable = true)
    private void sixtoolsaddon$swingProgress(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        SwingSpeed module = Modules.get().get(SwingSpeed.class);
        if (module == null || !module.isActive()) return;

        cir.setReturnValue(module.getRenderProgress(tickDelta));
    }
}
