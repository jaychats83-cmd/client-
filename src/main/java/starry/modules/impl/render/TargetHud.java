package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.events.impl.PacketEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetHud extends ModuleStructure {
    SliderSettings xCoord = new SliderSettings("X", "").setValue(500f).range(0f, 1920f);
    SliderSettings yCoord = new SliderSettings("Y", "").setValue(500f).range(0f, 1080f);
    BooleanSetting hudTimeout = new BooleanSetting("Timeout", "").setValue(true);

    private long lastAttackTime;
    private static final long TIMEOUT = 10000;

    public TargetHud() {
        super("Target HUD", ModuleCategory.RENDER);
        settings(xCoord, yCoord, hudTimeout);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        int x = xCoord.getInt();
        int y = yCoord.getInt();
        var ctx = event.getDrawContext();

        if ((!hudTimeout.isValue() || (System.currentTimeMillis() - lastAttackTime <= TIMEOUT))
                && mc.player.getAttacking() instanceof PlayerEntity player && player.isAlive()) {

            var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            ctx.fill(x, y, x + 340, y + 200, new Color(0, 0, 0, 175).getRGB());
            ctx.drawText(mc.textRenderer, player.getName().getString() + " - " + String.format("%.1f", player.distanceTo(mc.player)) + " blocks",
                    x + 23, y + 5, Color.WHITE.getRGB(), true);

            String type = entry == null ? "Bot" : "Player";
            ctx.drawText(mc.textRenderer, "Type: " + type, x + 5, y + 35, entry == null ? new Color(255, 80, 80).getRGB() : Color.WHITE.getRGB(), true);
            ctx.drawText(mc.textRenderer, "Health: " + Math.round(player.getHealth() + player.getAbsorptionAmount()),
                    x + 5, y + 65, Color.GREEN.getRGB(), true);
            ctx.drawText(mc.textRenderer, "Invisible: " + (player.isInvisible() ? "Yes" : "No"),
                    x + 5, y + 95, Color.WHITE.getRGB(), true);
            if (entry != null)
                ctx.drawText(mc.textRenderer, "Ping: " + entry.getLatency(), x + 5, y + 125, Color.WHITE.getRGB(), true);
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent event) {
        if (!event.isSend()) return;
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override public void interact(Hand hand) {}
                @Override public void interactAt(Hand hand, Vec3d pos) {}
                @Override public void attack() {
                    if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) lastAttackTime = System.currentTimeMillis();
                }
            });
        }
    }
}
