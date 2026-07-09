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

        ScoreLine[] lines = scoreboardLines();
        var ctx = event.getDrawContext();
        int width = mc.textRenderer.getWidth(title.getText());
        for (ScoreLine line : lines) width = Math.max(width, mc.textRenderer.getWidth(line.label + line.value));
        if (showFooter.isValue()) width = Math.max(width, mc.textRenderer.getWidth(footer.getText()));
        width = Math.max(112, width + 18);

        int lineHeight = 10;
        int headerHeight = 16;
        int footerHeight = showFooter.isValue() ? 16 : 3;
        int height = headerHeight + lines.length * lineHeight + footerHeight + 4;
        int x = mc.getWindow().getScaledWidth() - width - 10;
        int y = yOffset.getInt();
        int a = alpha.getInt();

        ctx.fill(x, y, x + width, y + height, new Color(22, 24, 27, Math.min(170, a)).getRGB());
        ctx.fill(x, y, x + width, y + headerHeight, new Color(18, 20, 22, Math.min(210, a + 20)).getRGB());
        drawCentered(ctx, title.getText(), x, y + 4, width, new Color(120, 235, 84).getRGB());

        int rowY = y + headerHeight + 3;
        for (ScoreLine line : lines) {
            if (line.label.isEmpty() && line.value.isEmpty()) {
                rowY += lineHeight;
                continue;
            }
            ctx.drawText(mc.textRenderer, line.label, x + 7, rowY, line.labelColor, true);
            ctx.drawText(mc.textRenderer, line.value, x + 7 + mc.textRenderer.getWidth(line.label), rowY, line.valueColor, true);
            rowY += lineHeight;
        }

        if (showFooter.isValue()) {
            ctx.fill(x, y + height - footerHeight, x + width, y + height - footerHeight + 1, new Color(80, 80, 80, 120).getRGB());
            drawCentered(ctx, footer.getText(), x, y + height - footerHeight + 5, width, new Color(185, 185, 185).getRGB());
        }
    }

    private ScoreLine[] scoreboardLines() {
        ScoreLine nameLine = line("Name: ", mc.player == null ? "Player" : mc.player.getName().getString(), 0xFFBFC7D5, 0xFF79D46A);
        ScoreLine rankLine = line("Rank: ", "BOOSTER", 0xFFBFC7D5, 0xFFD449FF);
        ScoreLine moneyLine = line("$ Money: ", "200M", 0xFFFFD84A, 0xFF7CFF5F);
        ScoreLine shardsLine = line("Shards: ", "761", 0xFFFF4D6D, 0xFFFF5AAE);
        ScoreLine killsLine = line("Kills: ", String.valueOf(mc.player == null ? 0 : mc.player.getScore()), 0xFFFF4D4D, 0xFFFFFFFF);
        ScoreLine deathsLine = line("Deaths: ", "7", 0xFFFF8C2E, 0xFFFFA747);
        ScoreLine teamLine = line("Team: ", "VOID", 0xFF58A6FF, 0xFF20D0FF);
        ScoreLine timeLine = line("Playtime: ", LocalTime.now().format(DateTimeFormatter.ofPattern("H'h'")), 0xFFEBCB5C, 0xFFFFD463);
        if (!showSession.isValue()) return new ScoreLine[]{nameLine, rankLine, teamLine, timeLine};
        return new ScoreLine[]{nameLine, rankLine, blank(), moneyLine, shardsLine, killsLine, deathsLine, teamLine, timeLine};
    }

    private void drawCentered(net.minecraft.client.gui.DrawContext ctx, String text, int x, int y, int width, int color) {
        ctx.drawText(mc.textRenderer, text, x + (width / 2) - (mc.textRenderer.getWidth(text) / 2), y, color, false);
    }

    private ScoreLine line(String label, String value, int labelColor, int valueColor) {
        return new ScoreLine(label, value, labelColor, valueColor);
    }

    private ScoreLine blank() {
        return new ScoreLine("", "", 0, 0);
    }

    private record ScoreLine(String label, String value, int labelColor, int valueColor) {
    }
}
