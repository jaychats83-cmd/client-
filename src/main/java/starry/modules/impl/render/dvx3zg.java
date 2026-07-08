package starry.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import starry.events.api.EventHandler;
import starry.events.impl.HandAnimationEvent;
import starry.events.impl.SwingDurationEvent;

import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.SelectSetting;
import starry.modules.module.setting.implement.SliderSettings;
import starry.util.string.StringHelper;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class dvx3zg extends ModuleStructure {
    SelectSetting swingType = new SelectSetting(StringHelper.decrypt(new byte[]{25, (byte)-76, 54, 56, (byte)-81, (byte)-4, 23, (byte)-126, 51, (byte)-77, 58}), StringHelper.decrypt(new byte[]{25, (byte)-90, 51, (byte)-115, 113, (byte)-17, 23, (byte)-91, 61, (byte)-86, 49, (byte)-113, 50, (byte)-17, 78, (byte)-90, 47}))
            .value(StringHelper.decrypt(new byte[]{9, (byte)-85, 48, (byte)-104}), StringHelper.decrypt(new byte[]{25, (byte)-76, 54, (byte)-104, 119}), StringHelper.decrypt(new byte[]{14, (byte)-84, 40, (byte)-122}), StringHelper.decrypt(new byte[]{25, (byte)-82, 48, (byte)-121, 102, (byte)-13}), StringHelper.decrypt(new byte[]{25, (byte)-82, 48, (byte)-121, 102, (byte)-13, 23, (byte)-28}), StringHelper.decrypt(new byte[]{26, (byte)-84, 40, (byte)-115, 96}), StringHelper.decrypt(new byte[]{12, (byte)-90, 62, (byte)-101, 102}), StringHelper.decrypt(new byte[]{30, (byte)-76, 54, (byte)-101, 102}), StringHelper.decrypt(new byte[]{14, (byte)-90, 57, (byte)-119, 103, (byte)-9, 67}));
    SliderSettings hitStrengthSetting = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-76, 54, 56, (byte)-81, (byte)-4, 23, (byte)-123, 62, (byte)-79, 58, (byte)-122, 117, (byte)-17, 95}), StringHelper.decrypt(new byte[]{25, (byte)-76, 54, 56, (byte)-81, (byte)-4, 23, (byte)-73, 36, (byte)-86, 50, (byte)-119, 102, (byte)-14, 88, (byte)-72, 106, (byte)-80, 43, (byte)-102, 119, (byte)-11, 80, (byte)-94, 34}))
            .range(0.5F, 3.0F).setValue(1.0F);
    SliderSettings swingSpeedSetting = new SliderSettings(StringHelper.decrypt(new byte[]{25, (byte)-76, 54, 56, (byte)-81, (byte)-4, 23, (byte)-110, 63, (byte)-79, 62, (byte)-100, 123, (byte)-12, 89}), StringHelper.decrypt(new byte[]{2, (byte)-86, 43, (byte)-56, 115, (byte)-11, 94, (byte)-69, 43, (byte)-73, 54, (byte)-121, 124, (byte)-69, 83, (byte)-93, 56, (byte)-94, 43, (byte)-127, 125, (byte)-11}))
            .range(0.5F, 4.0F).setValue(1.0F);

    BooleanSetting onlySwing = new BooleanSetting(StringHelper.decrypt(new byte[]{5, 19, (byte)-30, (byte)-124, 107, (byte)-69, 96, (byte)-66, 47, (byte)-83, 127, (byte)-69, 101, (byte)-14, 89, (byte)-79, 35, (byte)-83, 56}), StringHelper.decrypt(new byte[]{25, (byte)-85, 48, (byte)-97, 97, (byte)-69, 86, (byte)-72, 35, (byte)-82, 62, (byte)-100, 123, (byte)-12, 89, (byte)-10, 37, (byte)-83, 51, (byte)-111, 50, (byte)-20, 95, (byte)-77, 36, (byte)-29, 44, (byte)-97, 123, (byte)-11, 80, (byte)-65, 36, (byte)-92}))
            .setValue(false);

    @NonFinal
    private float spinAngle = 0.0F;
    @NonFinal
    private float spinBackTimer = 0.0F;
    @NonFinal
    private boolean wasSwinging = false;

    public dvx3zg() {
        super(StringHelper.decrypt(new byte[]{(byte)-102, 70, 40, (byte)-127, 124, (byte)-4, 118, (byte)-72, 35, (byte)-82, 62, (byte)-100, 123, (byte)-12, 89}), StringHelper.decrypt(new byte[]{25, (byte)-76, 54, 56, (byte)-81, (byte)-4, 23, (byte)-105, 36, (byte)-86, 50, (byte)-119, 102, (byte)-14, 88, (byte)-72}), ModuleCategory.VISUALS);
        settings(swingType, hitStrengthSetting, swingSpeedSetting, onlySwing);
    }

    @EventHandler
    public void onSwingDuration(SwingDurationEvent e) {
        e.setAnimation(swingSpeedSetting.getValue());
    }

    @EventHandler
    public void onHandAnimation(HandAnimationEvent e) {
        boolean isMainHand = e.getHand().equals(Hand.MAIN_HAND);
        if (isMainHand) {
            MatrixStack matrix = e.getMatrices();
            float swingProgress = e.getSwingProgress();
            int i = mc.player.getMainArm().equals(Arm.RIGHT) ? 1 : -1;
            float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float sinSmooth = (float) (Math.sin(swingProgress * Math.PI) * 0.5F);
            float strength = hitStrengthSetting.getValue();

            if (onlySwing.isValue() ? mc.player.handSwingTicks != 0 : true) {
                switch (swingType.getSelected()) {
                    case "Chop" -> {
                        matrix.translate(0.56F * i, -0.44F, -0.72F);
                        matrix.translate(0.0F, 0.33F * -0.6F, 0.0F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * i));
                        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                        float f2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f2 * -20.0F * i * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f2 * -80.0F * strength));
                        matrix.translate(0.4F, 0.2F, 0.2F);
                        matrix.translate(-0.5F, 0.08F, 0.0F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                    }
                    case "Twist" -> {
                        matrix.translate(i * 0.56F, -0.36F, -0.72F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80 * i));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60 * i * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30));
                        matrix.translate(0, -0.1F, 0.05F);
                    }
                    case "Swipe" -> {
                        matrix.translate(0.56F * i, -0.32F, -0.72F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70 * i));
                        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20 * i));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70));
                    }
                    case "Default" -> {
                        matrix.translate(i * 0.56F, -0.52F - (sin2 * 0.5F * strength), -0.72F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * i));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 * i));
                    }
                    case "Down" -> {
                        matrix.translate(i * 0.56F, -0.32F, -0.72F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5 * strength));
                        matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
                    }
                    case "Smooth" -> {
                        matrix.translate(i * 0.56F, -0.42F, -0.72F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + sin1 * -20.0F * strength)));
                        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * sin2 * -20.0F * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
                        matrix.translate(0, -0.1, 0);
                    }
                    case "Smooth 2" -> {
                        matrix.translate(i * 0.56F, -0.42F, -0.72F);
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                        matrix.translate(0, -0.1, 0);
                    }
                    case "Power" -> {
                        matrix.translate(i * 0.56F, -0.32F, -0.72F);
                        matrix.translate((-sinSmooth * sinSmooth * sin1) * i * strength, 0, 0);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60 * strength));
                    }
                    case "Feast" -> {
                        matrix.translate(i * 0.56F, -0.32F, -0.72F);
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75 * i * strength));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45 * strength));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                    }
                }
            } else {
                matrix.translate(i * 0.56F, -0.52F, -0.72F);
            }
            e.cancel();
        }
    }
}
