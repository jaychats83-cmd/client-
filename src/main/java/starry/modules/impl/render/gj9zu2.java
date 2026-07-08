package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Hand;
import starry.events.api.EventHandler;
import starry.events.impl.HandOffsetEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class gj9zu2 extends ModuleStructure {

    SliderSettings mainHandXSetting = new SliderSettings(StringHelper.decrypt(new byte[]{7, (byte)-94, 54, (byte)-122, 50, (byte)-45, 86, (byte)-72, 46, (byte)-29, 7}), StringHelper.decrypt(new byte[]{18, (byte)-29, 41, (byte)-119, 126, (byte)-18, 82, (byte)-10, 57, (byte)-90, 43, (byte)-100, 123, (byte)-11, 80, (byte)-10, 44, (byte)-84, 45, (byte)-56, 127, (byte)-6, 94, (byte)-72, 106, (byte)-85, 62, (byte)-122, 118}))
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings mainHandYSetting = new SliderSettings(StringHelper.decrypt(new byte[]{7, (byte)-94, 54, (byte)-122, 50, (byte)-45, 86, (byte)-72, 46, (byte)-29, 6}), StringHelper.decrypt(new byte[]{19, (byte)-29, 41, (byte)-119, 126, (byte)-18, 82, (byte)-10, 57, (byte)-90, 43, (byte)-100, 123, (byte)-11, 80, (byte)-10, 44, (byte)-84, 45, (byte)-56, 127, (byte)-6, 94, (byte)-72, 106, (byte)-85, 62, (byte)-122, 118}))
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings mainHandZSetting = new SliderSettings(StringHelper.decrypt(new byte[]{7, (byte)-94, 54, (byte)-122, 50, (byte)-45, 86, (byte)-72, 46, (byte)-29, 5}), StringHelper.decrypt(new byte[]{16, (byte)-29, 41, (byte)-119, 126, (byte)-18, 82, (byte)-10, 57, (byte)-90, 43, (byte)-100, 123, (byte)-11, 80, (byte)-10, 44, (byte)-84, 45, (byte)-56, 127, (byte)-6, 94, (byte)-72, 106, (byte)-85, 62, (byte)-122, 118}))
            .setValue(0.0F).range(-2.5F, 2.5F);

    SliderSettings offHandXSetting = new SliderSettings(StringHelper.decrypt(new byte[]{5, (byte)-91, 57, (byte)-56, 90, (byte)-6, 89, (byte)-78, 106, (byte)-101}), StringHelper.decrypt(new byte[]{18, (byte)-29, 41, (byte)-119, 126, (byte)-18, 82, (byte)-10, 57, (byte)-90, 43, (byte)-100, 123, (byte)-11, 80, (byte)-10, 44, (byte)-84, 45, (byte)-56, 125, (byte)-3, 81, (byte)-10, 34, (byte)-94, 49, (byte)-116}))
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings offHandYSetting = new SliderSettings(StringHelper.decrypt(new byte[]{5, (byte)-91, 57, (byte)-56, 90, (byte)-6, 89, (byte)-78, 106, (byte)-102}), StringHelper.decrypt(new byte[]{19, (byte)-29, 41, (byte)-119, 126, (byte)-18, 82, (byte)-10, 57, (byte)-90, 43, (byte)-100, 123, (byte)-11, 80, (byte)-10, 44, (byte)-84, 45, (byte)-56, 125, (byte)-3, 81, (byte)-10, 34, (byte)-94, 49, (byte)-116}))
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings offHandZSetting = new SliderSettings(StringHelper.decrypt(new byte[]{5, (byte)-91, 57, (byte)-56, 90, (byte)-6, 89, (byte)-78, 106, (byte)-103}), StringHelper.decrypt(new byte[]{16, (byte)-29, 41, (byte)-119, 126, (byte)-18, 82, (byte)-10, 57, (byte)-90, 43, (byte)-100, 123, (byte)-11, 80, (byte)-10, 44, (byte)-84, 45, (byte)-56, 125, (byte)-3, 81, (byte)-10, 34, (byte)-94, 49, (byte)-116}))
            .setValue(0.0F).range(-2.5F, 2.5F);

    public gj9zu2() {
        super(StringHelper.decrypt(new byte[]{28, (byte)-86, 58, (byte)-97, 95, (byte)-12, 83, (byte)-77, 38}), StringHelper.decrypt(new byte[]{28, (byte)-86, 58, (byte)-97, 50, (byte)-42, 88, (byte)-78, 47, (byte)-81}), ModuleCategory.VISUALS);
        settings(mainHandXSetting, mainHandYSetting, mainHandZSetting,
                offHandXSetting, offHandYSetting, offHandZSetting);
    }

    @EventHandler
    public void onHandOffset(HandOffsetEvent e) {
        Hand hand = e.getHand();
        if (hand.equals(Hand.MAIN_HAND) && e.getStack().getItem() instanceof CrossbowItem) return;

        MatrixStack matrix = e.getMatrices();

        if (hand.equals(Hand.MAIN_HAND)) {
            matrix.translate(mainHandXSetting.getValue(), mainHandYSetting.getValue(), mainHandZSetting.getValue());
        } else {
            matrix.translate(offHandXSetting.getValue(), offHandYSetting.getValue(), offHandZSetting.getValue());
        }
    }
}
