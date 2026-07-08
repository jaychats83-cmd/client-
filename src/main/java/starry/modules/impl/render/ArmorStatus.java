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
        int width = 172;
        int height = 26 + armor.size() * 18;

        var ctx = event.getDrawContext();
        ctx.fill(left, top, left + width, top + height, new Color(7, 11, 17, 178).getRGB());
        ctx.drawText(mc.textRenderer, "Armor", left + 12, top + 9, Color.WHITE.getRGB(), true);

        int row = top + 28;
        for (ItemStack stack : armor) {
            String name = stack.isEmpty() ? "Empty" : stack.getName().getString();
            ctx.drawText(mc.textRenderer, shorten(name, 18), left + 12, row, stack.isEmpty() ? new Color(162, 176, 194).getRGB() : Color.WHITE.getRGB(), true);
            if (durability.isValue() && !stack.isEmpty() && stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                ctx.drawText(mc.textRenderer, String.valueOf(remaining), left + width - 42, row, new Color(77, 166, 255).getRGB(), true);
            }
            row += 18;
        }
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
