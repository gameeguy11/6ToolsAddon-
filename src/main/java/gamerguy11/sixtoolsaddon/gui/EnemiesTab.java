package gamerguy11.sixtoolsaddon.gui;

import gamerguy11.sixtoolsaddon.systems.enemies.Enemy;
import gamerguy11.sixtoolsaddon.systems.enemies.Enemies;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import net.minecraft.client.gui.screen.Screen;

public class EnemiesTab extends Tab {
    public EnemiesTab() {
        super("Enemies");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new EnemiesScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof EnemiesScreen;
    }

    private static class EnemiesScreen extends WindowTabScreen {
        public EnemiesScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList list = add(theme.horizontalList()).expandX().widget();

            WTextBox nameW = list.add(theme.textBox("", (c1, c2) -> c2 != ' ')).expandX().widget();
            nameW.setFocused(true);

            WPlus addButton = list.add(new WRedPlus()).widget();
            addButton.action = () -> {
                String name = nameW.get().trim();
                if (name.isEmpty()) return;

                if (Enemies.get().add(new Enemy(name))) {
                    nameW.set("");
                    initTable(table);
                    nameW.setFocused(true);
                }
            };

            enterAction = addButton.action;
        }

        private void initTable(WTable table) {
            table.clear();
            if (Enemies.get().isEmpty()) return;

            for (Enemy enemy : Enemies.get()) {
                table.add(theme.label(enemy.getName()));

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    Enemies.get().remove(enemy);
                    initTable(table);
                };

                table.row();
            }
        }
    }

    private static class WRedPlus extends WPlus implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            MeteorGuiTheme theme = theme();
            double pad = pad();
            double s = theme.scale(3);

            renderBackground(renderer, this, pressed, mouseOver);
            renderer.quad(x + pad, y + height / 2 - s / 2, width - pad * 2, s, theme.minusColor.get());
            renderer.quad(x + width / 2 - s / 2, y + pad, s, height - pad * 2, theme.minusColor.get());
        }
    }
}
