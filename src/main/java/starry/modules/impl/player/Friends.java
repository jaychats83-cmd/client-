package starry.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.AttackEvent;
import starry.events.impl.DrawEvent;
import starry.events.impl.InteractEntityEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Friends extends ModuleStructure {
    BooleanSetting antiAttack = new BooleanSetting("Anti-Attack", "").setValue(false);
    BooleanSetting disableAimAssist = new BooleanSetting("Anti-Aim", "").setValue(false);
    BooleanSetting friendStatus = new BooleanSetting("Friend Status", "").setValue(false);

    public Friends() {
        super("Friends", ModuleCategory.PLAYER);
        settings(antiAttack, disableAimAssist, friendStatus);
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (antiAttack.isValue() && isFriendTarget()) event.getTarget();
    }

    @EventHandler
    public void onInteractEntity(InteractEntityEvent event) {
        if (antiAttack.isValue() && event.getEntity() instanceof PlayerEntity) event.setCancelled(false);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (!friendStatus.isValue()) return;
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity) {
            event.getDrawContext().drawText(mc.textRenderer, "Player is friend",
                    mc.getWindow().getWidth() / 2, mc.getWindow().getHeight() / 2 + 25, Color.GREEN.getRGB(), true);
        }
    }

    private boolean isFriendTarget() {
        return mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity;
    }
}
