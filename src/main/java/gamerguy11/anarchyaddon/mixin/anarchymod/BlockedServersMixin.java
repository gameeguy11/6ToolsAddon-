package gamerguy11.anarchyaddon.mixin.anarchymod;

import com.mojang.patchy.BlockedServers;
import gamerguy11.anarchyaddon.anarchymod.Domains;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockedServers.class)
public class BlockedServersMixin {

    @Inject(method = "isBlockedServerHostName", at = @At("RETURN"), cancellable = true, remap = false)
    public void isBlockedServerHostName(String server, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && Domains.contains(server)) {
            cir.setReturnValue(false);
        }
    }
}
