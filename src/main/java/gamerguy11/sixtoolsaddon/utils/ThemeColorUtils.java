package gamerguy11.sixtoolsaddon.utils;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.lang.reflect.Field;

public final class ThemeColorUtils {
    private ThemeColorUtils() {}

    public static SettingColor resolve(SettingColor color, boolean useTheme) {
        if (!useTheme) return color;

        SettingColor themeColor = getThemeColor();
        if (themeColor == null) return color;

        return new SettingColor(themeColor.r, themeColor.g, themeColor.b, color.a);
    }

    private static SettingColor getThemeColor() {
        GuiTheme theme = GuiThemes.get();
        if (theme == null) return null;

        Class<?> type = theme.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("accentColor");
                field.setAccessible(true);
                Object value = field.get(theme);
                if (value instanceof Setting<?> setting && setting.get() instanceof SettingColor color) return color;
            } catch (ReflectiveOperationException ignored) {
            }
            type = type.getSuperclass();
        }

        return null;
    }
}
