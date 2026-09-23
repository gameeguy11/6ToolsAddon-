package gamerguy11.sixtoolsaddon.gui;

import gamerguy11.sixtoolsaddon.deathlogger.DeathLogStore;
import gamerguy11.sixtoolsaddon.deathlogger.DeathRecord;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;

import java.util.List;

public class DeathLogScreen extends WindowScreen {
    public DeathLogScreen(GuiTheme theme) {
        super(theme, "Death Log");
    }

    @Override
    public void initWidgets() {
        add(theme.label("Showing the last " + DeathLogStore.GUI_LIMIT + " deaths. Full history is saved to sixtoolsaddon-deaths.txt")).expandX();
        add(theme.horizontalSeparator()).expandX();

        List<DeathRecord> entries = DeathLogStore.lastEntries(DeathLogStore.GUI_LIMIT);

        WTable table = add(theme.table()).expandX().minWidth(400).widget();

        if (entries.isEmpty()) {
            table.add(theme.label("No deaths logged yet")).expandX().pad(10);
        } else {
            for (DeathRecord entry : entries) {
                table.add(theme.label(entry.timestamp));
                table.add(theme.label(entry.coordsString()));
                table.add(theme.label(entry.dimension)).expandCellX().right();

                table.row();
            }
        }
    }
}
