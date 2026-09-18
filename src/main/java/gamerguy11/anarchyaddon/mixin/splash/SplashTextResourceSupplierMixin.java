package gamerguy11.anarchyaddon.mixin.splash;

import gamerguy11.anarchyaddon.splash.CustomSplashes;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {

    @Inject(method = "getSplashText", at = @At("RETURN"), cancellable = true)
    private void anarchyaddon$overrideSplash(CallbackInfoReturnable<String> cir) {
        String custom = CustomSplashes.pick();
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
