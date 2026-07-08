package starry.util.theme;

import java.awt.*;

public class Theme {
    public final String name;
    public final int mainBg;
    public final int panelBg;
    public final int panelBg2;
    public final int outline;
    public final int text;
    public final int text2;
    public final int accent;
    public final int accentText;

    public Theme(String name, int mainBg, int panelBg, int panelBg2, int outline, int text, int text2, int accent, int accentText) {
        this.name = name;
        this.mainBg = mainBg;
        this.panelBg = panelBg;
        this.panelBg2 = panelBg2;
        this.outline = outline;
        this.text = text;
        this.text2 = text2;
        this.accent = accent;
        this.accentText = accentText;
    }

    public static final Theme DARK = new Theme(
            "Dark",
            0xFF141418, 0xFF1A1A1F, 0xFF1E1E26,
            0xFF373746, 0xFFFFFFFF, 0xFFAFAFAF,
            0xFF5B3FD4, 0xFF8264D2
    );

    public static final Theme LIGHT = new Theme(
            "Light",
            0xFFF0F0F0, 0xFFFFFFFF, 0xFFE8E8E8,
            0xFFCCCCCC, 0xFF000000, 0xFF555555,
            0xFF4A90D9, 0xFFFFFFFF
    );

    public static final Theme BLUE = new Theme(
            "Blue",
            0xFF0D1117, 0xFF161B22, 0xFF21262D,
            0xFF30363D, 0xFFC9D1D9, 0xFF8B949E,
            0xFF58A6FF, 0xFFFFFFFF
    );

    public static final Theme PURPLE = new Theme(
            "Purple",
            0xFF14141E, 0xFF1A1A28, 0xFF222233,
            0xFF3A3A55, 0xFFE0D0FF, 0xFFA090C0,
            0xFF7C4DFF, 0xFFFFFFFF
    );

    public static final Theme[] PRESETS = {DARK, LIGHT, BLUE, PURPLE};

    public Color color(int argb) {
        return new Color(argb, true);
    }
}
