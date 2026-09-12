package gamerguy11.anarchyaddon.modules.utility;

import gamerguy11.anarchyaddon.AnarchyAddon;
import gamerguy11.anarchyaddon.shulkerview.RenderHandler;
import gamerguy11.anarchyaddon.shulkerview.UpdateHandler;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class ShulkerView extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBackground = settings.createGroup("Background");
    private final SettingGroup sgPosition = settings.createGroup("Position");

    private final Setting<Boolean> compact = sgGeneral.add(new BoolSetting.Builder()
        .name("compact")
        .description("Merges stacks of the same item and hides empty slots in the preview.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> bothSides = sgGeneral.add(new BoolSetting.Builder()
        .name("both-sides")
        .description("Once previews fill up the left side of the screen, continues them on the right.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tooltips = sgGeneral.add(new BoolSetting.Builder()
        .name("tooltips")
        .description("Shows the normal item tooltip when hovering an item inside a preview.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> scale = sgGeneral.add(new IntSetting.Builder()
        .name("scale")
        .description("Size of the previews, in tenths (10 = normal size).")
        .defaultValue(10)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the preview background, including its opacity (alpha). Set alpha to 0 for no background at all.")
        .defaultValue(new SettingColor(0, 0, 0, 76))
        .build()
    );

    private final Setting<Boolean> anchorRight = sgPosition.add(new BoolSetting.Builder()
        .name("anchor-right")
        .description("Starts drawing previews from the right edge of the screen instead of the left. With both-sides on, overflow spills to the opposite side.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> offsetX = sgPosition.add(new IntSetting.Builder()
        .name("offset-x")
        .description("Extra horizontal offset in pixels, measured inward from whichever edge (left/right) previews are anchored to.")
        .defaultValue(0)
        .range(-1000, 1000)
        .sliderRange(-200, 200)
        .build()
    );

    private final Setting<Integer> offsetY = sgPosition.add(new IntSetting.Builder()
        .name("offset-y")
        .description("Extra vertical offset in pixels, measured down from the top of the screen.")
        .defaultValue(0)
        .range(-1000, 1000)
        .sliderRange(-200, 200)
        .build()
    );

    private final RenderHandler renderHandler = new RenderHandler(this);
    private final UpdateHandler updateHandler = new UpdateHandler(this);

    public ShulkerView() {
        super(AnarchyAddon.CATEGORY, "shulker-view", "Shows shulker box contents in a preview, right in your inventory.");
    }

    public boolean isCompact() {
        return compact.get();
    }

    public boolean isBothSides() {
        return bothSides.get();
    }

    public boolean isTooltips() {
        return tooltips.get();
    }

    public int getBackground() {
        return backgroundColor.get().getPacked();
    }

    public float getScale() {
        return scale.get() / 10f;
    }

    public boolean isAnchorRight() {
        return anchorRight.get();
    }

    public int getOffsetX() {
        return offsetX.get();
    }

    public int getOffsetY() {
        return offsetY.get();
    }

    public RenderHandler getRenderHandler() {
        return renderHandler;
    }

    public UpdateHandler getUpdateHandler() {
        return updateHandler;
    }
}
