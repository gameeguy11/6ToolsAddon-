package gamerguy11.sixtoolsaddon.shulkerview;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.util.math.ColorHelper;

public class ColorUtils {
    private ColorUtils() {
        throw new AssertionError();
    }

    public static int getColor(ShulkerBoxBlock block) {
        if (block.getColor() != null) {
            return ColorHelper.withAlpha(255, block.getColor().getMapColor().color);
        }
        return 0xff9953b0;
    }
}
