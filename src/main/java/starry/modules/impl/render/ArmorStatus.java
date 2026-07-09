package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.events.api.EventHandler;
import starry.events.impl.DrawEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SliderSettings;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArmorStatus extends ModuleStructure {
    SliderSettings x = new SliderSettings("X", "").setValue(8f).range(0f, 1000f);
    SliderSettings y = new SliderSettings("Y", "").setValue(214f).range(0f, 1000f);
    BooleanSetting durability = new BooleanSetting("Durability", "").setValue(true);

    public ArmorStatus() {
        super("Armor Status", ModuleCategory.RENDER);
        settings(x, y, durability);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null) return;

        List<ItemStack> armor = new ArrayList<>();
        armor.add(mc.player.getEquippedStack(EquipmentSlot.HEAD));
        armor.add(mc.player.getEquippedStack(EquipmentSlot.CHEST));
        armor.add(mc.player.getEquippedStack(EquipmentSlot.LEGS));
        armor.add(mc.player.getEquippedStack(EquipmentSlot.FEET));
        int left = x.getInt();
        int top = y.getInt();
        int cell = 24;
        int width = armor.size() * cell + 6;
        int height = 27;

        var ctx = event.getDrawContext();
        ctx.fill(left, top, left + width, top + height, new Color(8, 10, 14, 150).getRGB());

        int itemX = left + 4;
        for (ItemStack stack : armor) {
            ctx.fill(itemX - 2, top + 3, itemX + 20, top + 24, new Color(18, 22, 28, 180).getRGB());
            if (!stack.isEmpty()) {
                ctx.drawItem(stack, itemX, top + 5);
                ctx.drawStackOverlay(mc.textRenderer, stack, itemX, top + 5);
            }
            if (durability.isValue() && !stack.isEmpty() && stack.isDamageable()) {
                int percent = Math.round((stack.getMaxDamage() - stack.getDamage()) * 100f / stack.getMaxDamage());
                int color = durabilityColor(percent);
                String text = String.valueOf(percent);
                ctx.drawText(mc.textRenderer, text, itemX + 9 - mc.textRenderer.getWidth(text) / 2, top + 17, color, true);
            }
            itemX += cell;
        }
    }

    private int durabilityColor(int percent) {
        if (percent > 65) return new Color(85, 230, 120).getRGB();
        if (percent > 30) return new Color(255, 210, 80).getRGB();
        return new Color(255, 80, 80).getRGB();
    }
}
