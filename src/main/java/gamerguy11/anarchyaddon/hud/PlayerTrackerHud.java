package gamerguy11.anarchyaddon.hud;

import gamerguy11.anarchyaddon.AnarchyAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerTrackerHud extends HudElement {
    public static final HudElementInfo<PlayerTrackerHud> INFO = new HudElementInfo<>(
        AnarchyAddon.HUD_GROUP,
        "player-tracker",
        "Lists nearby players, color-coded as friend/enemy/other, with distance.",
        PlayerTrackerHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgGrid = settings.createGroup("Grid Snapping");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("limit")
        .description("Max number of players to list.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 50)
        .build()
    );

    private final Setting<Boolean> showDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("show-distance")
        .description("Shows how far away each player is, in meters (blocks).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders a shadow behind the text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal text alignment.")
        .defaultValue(Alignment.Left)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("Padding around the element.")
        .defaultValue(2)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<SettingColor> friendColor = sgColors.add(new ColorSetting.Builder()
        .name("friend-color")
        .description("Color for players on your Meteor friends list.")
        .defaultValue(new SettingColor(75, 225, 75))
        .build()
    );

    private final Setting<SettingColor> enemyColor = sgColors.add(new ColorSetting.Builder()
        .name("enemy-color")
        .description("Color for players on the enemy-names list below.")
        .defaultValue(new SettingColor(225, 75, 75))
        .build()
    );

    private final Setting<SettingColor> otherColor = sgColors.add(new ColorSetting.Builder()
        .name("other-color")
        .description("Color for everyone else.")
        .defaultValue(new SettingColor(225, 225, 225))
        .build()
    );

    private final Setting<SettingColor> distanceColor = sgColors.add(new ColorSetting.Builder()
        .name("distance-color")
        .description("Color used for the distance text.")
        .defaultValue(new SettingColor(175, 175, 175))
        .build()
    );

    private final Setting<List<String>> enemyNames = sgColors.add(new StringListSetting.Builder()
        .name("enemy-names")
        .description("Player names to mark with the enemy color. Not case sensitive. Meteor Client has no built-in enemy list, so this addon keeps its own.")
        .build()
    );

    private final Setting<Boolean> snapToGrid = sgGrid.add(new BoolSetting.Builder()
        .name("snap-to-grid")
        .description("While dragging this element in the HUD editor, snaps it to a pixel grid instead of free placement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> gridSize = sgGrid.add(new IntSetting.Builder()
        .name("grid-size")
        .description("Size, in pixels, of one grid cell.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 50)
        .visible(snapToGrid::get)
        .build()
    );

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this HUD element instead of the global HUD text scale.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .visible(customScale::get)
        .build()
    );

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a background behind the list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .defaultValue(new SettingColor(25, 25, 25, 100))
        .visible(background::get)
        .build()
    );

    private final List<AbstractClientPlayerEntity> players = new ArrayList<>();

    public PlayerTrackerHud() {
        super(INFO);
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    @Override
    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth() - border.get() * 2, width, alignment);
    }

    @Override
    public void move(int deltaX, int deltaY) {
        super.move(deltaX, deltaY);

        if (snapToGrid.get()) {
            int size = Math.max(1, gridSize.get());

            box.x = Math.round((float) box.x / size) * size;
            box.y = Math.round((float) box.y / size) * size;

            updatePos();
        }
    }

    @Override
    public void tick(HudRenderer renderer) {
        double width = renderer.textWidth("Players:", shadow.get(), getScale());
        double height = renderer.textHeight(shadow.get(), getScale());

        if (mc.world == null || mc.getCameraEntity() == null) {
            setSize(width, height);
            return;
        }

        for (AbstractClientPlayerEntity player : getPlayers()) {
            String text = player.getName().getString();
            if (showDistance.get()) text += String.format(" (%sm)", Math.round(mc.getCameraEntity().distanceTo(player)));

            width = Math.max(width, renderer.textWidth(text, shadow.get(), getScale()));
            height += renderer.textHeight(shadow.get(), getScale()) + 2;
        }

        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double y = this.y + border.get();

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        renderer.text("Players:", x + border.get() + alignX(renderer.textWidth("Players:", shadow.get(), getScale()), alignment.get()), y, otherColor.get(), shadow.get(), getScale());

        if (mc.world == null || mc.getCameraEntity() == null) return;
        double spaceWidth = renderer.textWidth(" ", shadow.get(), getScale());

        for (AbstractClientPlayerEntity player : getPlayers()) {
            String name = player.getName().getString();
            Color color = colorFor(player);

            double width = renderer.textWidth(name, shadow.get(), getScale());

            String distanceText = null;
            if (showDistance.get()) {
                distanceText = String.format("(%sm)", Math.round(mc.getCameraEntity().distanceTo(player)));
                width += spaceWidth + renderer.textWidth(distanceText, shadow.get(), getScale());
            }

            double x = this.x + border.get() + alignX(width, alignment.get());
            y += renderer.textHeight(shadow.get(), getScale()) + 2;

            x = renderer.text(name, x, y, color, shadow.get(), getScale());
            if (showDistance.get()) renderer.text(distanceText, x + spaceWidth, y, distanceColor.get(), shadow.get(), getScale());
        }
    }

    private Color colorFor(AbstractClientPlayerEntity player) {
        if (Friends.get().isFriend(player)) return friendColor.get();
        if (isEnemy(player)) return enemyColor.get();
        return otherColor.get();
    }

    private boolean isEnemy(AbstractClientPlayerEntity player) {
        String name = player.getName().getString();

        for (String enemy : enemyNames.get()) {
            if (enemy.equalsIgnoreCase(name)) return true;
        }

        return false;
    }

    private List<AbstractClientPlayerEntity> getPlayers() {
        players.clear();
        players.addAll(mc.world.getPlayers());
        players.removeIf(Objects::isNull);
        players.removeIf(player -> player.equals(mc.player));
        players.sort(Comparator.comparingDouble(player -> player.squaredDistanceTo(mc.getCameraEntity())));

        if (players.size() > limit.get()) {
            players.subList(limit.get(), players.size()).clear();
        }

        return players;
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }
}
