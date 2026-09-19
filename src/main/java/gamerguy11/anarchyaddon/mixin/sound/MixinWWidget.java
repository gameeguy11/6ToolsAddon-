package gamerguy11.anarchyaddon.mixin.sound;

import gamerguy11.anarchyaddon.modules.utility.SoundEditor;
import gamerguy11.anarchyaddon.sound.SoundType;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorModule;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.Click;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WWidget.class, remap = false)
public class MixinWWidget {
    @Shadow public boolean mouseOver;
    @Unique private boolean anarchyaddon$wasOver;

    @Inject(method = "mouseMoved", at = @At("TAIL"))
    private void anarchyaddon$onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY, CallbackInfo ci) {
        if (!(((Object) this) instanceof WMeteorModule)) return;

        if (mouseOver && !anarchyaddon$wasOver) {
            SoundEditor editor = Modules.get().get(SoundEditor.class);
            if (editor != null) editor.play(SoundType.GUI_HOVER);
        }

        anarchyaddon$wasOver = mouseOver;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void anarchyaddon$onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!(((Object) this) instanceof WMeteorModule)) return;
        if (!mouseOver) return;

        SoundEditor editor = Modules.get().get(SoundEditor.class);
        if (editor == null) return;

        if (click.button() == 0) editor.play(SoundType.GUI_CLICK_LEFT);
        else if (click.button() == 1) editor.play(SoundType.GUI_CLICK_RIGHT);
    }
}
