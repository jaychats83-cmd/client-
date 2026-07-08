package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.modules.module.setting.implement.TextSetting;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FakeScoreboard extends ModuleStructure {
    TextSetting title = new TextSetting("Title", "").setText("QCLOUD SMP");
    TextSetting footer = new TextSetting("Footer", "").setText("QCLOUDCLIENT.NET");
    SliderSettings yOffset = new SliderSettings("Y Offset", "").setValue(72f).range(20f, 220f);
    SliderSettings alpha = new SliderSettings("Alpha", "").setValue(185f).range(80f, 255f);
    BooleanSetting showSession = new BooleanSetting("Session Stats", "").setValue(true);
    BooleanSetting showFooter = new BooleanSetting("Footer", "").setValue(true);

    public FakeScoreboard() {
        super("Fake Scoreboard", ModuleCategory.RENDER);
        settings(title, footer, yOffset, alpha, showSession, showFooter);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.getWindow() == null) return;

        String[] lines = scoreboardLines();
        var ctx = event.getDrawContext();
        int width = mc.textRenderer.getWidth(title.getText());
        for (String line : lines) width = Math.max(width, mc.textRenderer.getWidth(line));
        if (showFooter.isValue()) width = Math.max(width, mc.textRenderer.getWidth(footer.getText()));
        width = Math.max(142, width + 24);

        int lineHeight = 11;
        int headerHeight = 22;
        int footerHeight = showFooter.isValue() ? 18 : 4;
        int height = headerHeight + lines.length * lineHeight + footerHeight;
        int x = mc.getWindow().getScaledWidth() - width - 10;
        int y = yOffset.getInt();
        int a = alpha.getInt();

        ctx.fill(x, y, x + width, y + height, new Color(5, 9, 20, a).getRGB());
        ctx.fill(x, y + headerHeight - 1, x + width, y + headerHeight, new Color(65, 190, 255, 115).getRGB());
        ctx.fill(x, y + headerHeight, x + width, y + height - footerHeight, new Color(20, 55, 82, Math.min(210, a + 10)).getRGB());
        drawCentered(ctx, title.getText(), x, y + 7, width, new Color(116, 226, 255).getRGB());

        int rowY = y + headerHeight + 4;
        for (int i = 0; i < lines.length; i++) {
            int c = i == 0 ? new Color(158, 242, 255).getRGB() : new Color(218, 230, 255).getRGB();
            ctx.drawText(mc.textRenderer, lines[i], x + 9, rowY, c, false);
            rowY += lineHeight;
        }

        if (showFooter.isValue()) {
            ctx.fill(x + 8, y + height - footerHeight + 2, x + width - 8, y + height - footerHeight + 3, new Color(90, 130, 175, 95).getRGB());
            drawCentered(ctx, footer.getText(), x, y + height - footerHeight + 7, width, new Color(151, 178, 205).getRGB());
        }
    }

    private String[] scoreboardLines() {
        String nameLine = "Name: Protected by qcloud";
        String rankLine = "Rank: QCLOUD";
        String moneyLine = "Money: $0";
        String shardsLine = "Shards: 0";
        String killsLine = "Kills: " + (mc.player == null ? 0 : mc.player.getScore());
        String deathsLine = "Deaths: 0";
        String teamLine = "Team: N/A";
        String timeLine = "Playtime: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (!showSession.isValue()) return new String[]{nameLine, rankLine, teamLine, timeLine};
        return new String[]{nameLine, rankLine, "", moneyLine, shardsLine, killsLine, deathsLine, teamLine, timeLine};
    }

    private void drawCentered(net.minecraft.client.gui.DrawContext ctx, String text, int x, int y, int width, int color) {
        ctx.drawText(mc.textRenderer, text, x + (width / 2) - (mc.textRenderer.getWidth(text) / 2), y, color, false);
    }
}
