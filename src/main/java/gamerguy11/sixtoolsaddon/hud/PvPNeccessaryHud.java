package gamerguy11.sixtoolsaddon.hud;

import gamerguy11.sixtoolsaddon.utils.ThemeColorUtils;
import gamerguy11.sixtoolsaddon.SixToolsAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class PvPNeccessaryHud extends HudElement {
    public static final HudElementInfo<PvPNeccessaryHud> INFO = new HudElementInfo<>(SixToolsAddon.HUD_GROUP, "pvp-neccessary", "Displays selected PvP items and their inventory counts.", PvPNeccessaryHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to display.")
            .defaultValue(Items.TOTEM_OF_UNDYING, Items.ENDER_PEARL, Items.END_CRYSTAL, Items.OBSIDIAN)
            .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
            .name("text-color")
            .description("Color of the item count text.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );

    private final Setting<Boolean> textColorUseTheme = sgGeneral.add(new BoolSetting.Builder()
            .name("text-use-theme")
            .description("Use the current Meteor theme accent color.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Integer> margin = sgScale.add(new IntSetting.Builder()
            .name("margin")
            .description("Space between items.")
            .defaultValue(0)
            .onChanged(aInt -> calculateSize())
            .min(0)
            .sliderRange(0, 10)
            .build()
    );

    public final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
            .name("custom-scale")
            .description("Applies a custom scale to this HUD element.")
            .defaultValue(false)
            .onChanged(aBoolean -> calculateSize())
            .build()
    );

    public final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(customScale::get)
            .defaultValue(2.0)
            .onChanged(aDouble -> calculateSize())
            .min(0.5)
            .sliderRange(0.5, 3)
            .build()
    );

    public final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
            .name("background")
            .description("Displays background.")
            .defaultValue(false)
            .build()
    );

    public final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
    );

    public final Setting<Boolean> backgroundColorUseTheme = sgBackground.add(new BoolSetting.Builder()
            .name("background-color-use-theme")
            .description("Uses Meteor's current GUI theme accent color instead of the color above.")
            .visible(background::get)
            .defaultValue(false)
            .build()
    );

    private PvPNeccessaryHud() {
        super(INFO);
        calculateSize();
    }

    private void calculateSize() {
        float currentScale = getScale();
        int count = items.get().size();
        setSize(23 * currentScale * count, 17 * currentScale + 20);
    }

    @Override
    public void render(HudRenderer renderer) {
        calculateSize();
        int itemsLength = items.get().size();
        int scaleOffset = (int) (getScale() * 10);
        int intScale = (int) getScale();

        for (int i = 0; i < itemsLength; i++) {
            Item item = items.get().get(i);
            ItemStack itemStack = new ItemStack(item, InvUtils.find(item).count());
            int offset = i == 0 ? 0 : i * 50 * scaleOffset / (20 - margin.get());
            int textXOffset = 6 * intScale;
            int textYOffset = 17 * intScale;

            if (itemStack.getCount() > 100) textXOffset -= 6 * intScale;
            else if (itemStack.getCount() > 10) textXOffset -= 2 * intScale;

            int finalTextXOffset = textXOffset;
            renderer.post(() -> {
                renderItem(renderer, itemStack, x + offset, y);
                renderText(renderer, itemStack, x + offset + finalTextXOffset, y + textYOffset);
            });
        }

        if (background.get()) renderer.quad(x, y, getWidth(), getHeight(), ThemeColorUtils.resolve(backgroundColor.get(), backgroundColorUseTheme.get()));
    }

    private void renderItem(HudRenderer renderer, ItemStack itemStack, int x, int y) {
        boolean resetToZero = false;
        if (itemStack.isEmpty()) {
            itemStack.setCount(1);
            resetToZero = true;
        }
        renderer.item(itemStack, x, y, getScale(), false);
        if (resetToZero) itemStack.setCount(0);
    }

    private void renderText(HudRenderer renderer, ItemStack itemStack, double x, double y) {
        boolean resetToZero = false;
        if (itemStack.isEmpty()) {
            itemStack.setCount(1);
            resetToZero = true;
        }
        renderer.text(Integer.toString(itemStack.getCount()), x, y, ThemeColorUtils.resolve(textColor.get(), textColorUseTheme.get()), true, getScale() / 2);
        if (resetToZero) itemStack.setCount(0);
    }

    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
    }
}
