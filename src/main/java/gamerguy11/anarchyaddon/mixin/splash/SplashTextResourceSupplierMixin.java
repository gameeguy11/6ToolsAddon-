package gamerguy11.anarchyaddon.mixin.splash;

import gamerguy11.anarchyaddon.splash.CustomSplashes;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {

    // In 1.21.11 the old getSplashText():String was replaced by get():SplashTextRenderer.
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void anarchyaddon$overrideSplash(CallbackInfoReturnable<SplashTextRenderer> cir) {
        String custom = CustomSplashes.pick();
        if (custom != null) {
            cir.setReturnValue(new SplashTextRenderer(Text.literal(custom)));
        }
    }
}
