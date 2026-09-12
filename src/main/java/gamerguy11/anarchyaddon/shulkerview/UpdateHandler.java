package gamerguy11.anarchyaddon.shulkerview;

import gamerguy11.anarchyaddon.modules.utility.ShulkerView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class UpdateHandler {
    private final ObjectList<ShulkerInfo> shulkerList = ObjectLists.synchronize(new ObjectArrayList<>());
    private final ShulkerView config;

    public UpdateHandler(ShulkerView config) {
        this.config = config;
    }

    public void tick() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) return;
        HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;

        shulkerList.clear();
        for (Slot slot : screen.getScreenHandler().slots) {
            ShulkerInfo shulkerInfo = ShulkerInfo.create(config, slot.getStack(), slot.id);
            if (shulkerInfo == null) continue;
            shulkerList.add(shulkerInfo);
        }
    }

    public ObjectList<ShulkerInfo> getShulkerList() {
        return shulkerList;
    }
}
