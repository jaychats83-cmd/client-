package starry.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import starry.events.api.EventHandler;
import starry.events.impl.TickEvent;
import starry.modules.module.ModuleStructure;
import starry.modules.module.category.ModuleCategory;
import starry.modules.module.setting.implement.BindSetting;
import starry.modules.module.setting.implement.BooleanSetting;
import starry.modules.module.setting.implement.MinMaxSetting;
import starry.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoubleAnchorV3 extends ModuleStructure {
    BindSetting activateKey = new BindSetting("Activate Key", "Key that starts double anchoring").setKey(GLFW.GLFW_KEY_G);
    SliderSettings switchDelay = new SliderSettings("Switch Delay", "Delay between steps in ticks").setValue(0).range(0, 20);
    SliderSettings totemSlot = new SliderSettings("Totem Slot", "Slot held after charging").setValue(1).range(1, 9);
    MinMaxSetting randomDelay = new MinMaxSetting("Random Delay", "Extra delay in ticks").defaultValue(0, 0).range(0, 100);
    BooleanSetting randomInteractions = new BooleanSetting("Random Interactions", "Randomize charge interactions").setValue(false);
    MinMaxSetting randomHits = new MinMaxSetting("Random Hits", "Interactions per charge step").defaultValue(1, 4).range(1, 4);
    int delayCounter, sampledDelay, step;
    boolean anchoring;

    public DoubleAnchorV3() {
        super("Double Anchor V3", "Donor double-anchor sequence using only a real targeted block face", ModuleCategory.CPVP);
        settings(activateKey, switchDelay, totemSlot, randomDelay, randomInteractions, randomHits);
    }
    @Override public void activate() { resetAll(); }
    @Override public void deactivate() { resetAll(); }

    @EventHandler public void onTick(TickEvent event) {
        if (mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null || !hasItems()) return;
        if (!anchoring && !activationPressed()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) { anchoring = false; resetDelay(); return; }
        if (step == 0 && mc.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) { step = 2; return; }
        if (delayCounter++ < switchDelay.getInt() + randomDelay()) return;
        resetDelay();
        switch (step) {
            case 0 -> select(Items.RESPAWN_ANCHOR);
            case 1 -> use(hit);
            case 2 -> select(Items.GLOWSTONE);
            case 3 -> useRandom(hit);
            case 4 -> select(Items.RESPAWN_ANCHOR);
            case 5 -> { use(hit); use(hit); }
            case 6 -> select(Items.GLOWSTONE);
            case 7 -> useRandom(hit);
            case 8 -> selectSlot(totemSlot.getInt() - 1);
            case 9 -> use(hit);
            case 10 -> { resetAll(); return; }
        }
        step++;
    }
    private boolean hasItems() {
        boolean anchor = false, glow = false;
        for (int i=0;i<9;i++) { ItemStack s=mc.player.getInventory().getStack(i); anchor |= s.isOf(Items.RESPAWN_ANCHOR); glow |= s.isOf(Items.GLOWSTONE); }
        return anchor && glow;
    }
    private boolean activationPressed() {
        int key=activateKey.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN || !pressed(key)) { resetAll(); return false; }
        anchoring=true; return true;
    }
    private boolean pressed(int key) { return key < 0 ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key+100)==GLFW.GLFW_PRESS : GLFW.glfwGetKey(mc.getWindow().getHandle(), key)==GLFW.GLFW_PRESS; }
    private int randomDelay() { if (sampledDelay==0 && randomDelay.getIntMax()>randomDelay.getIntMin()) sampledDelay=Math.max(0,randomDelay.getRandomValueInt()); return sampledDelay; }
    private void useRandom(BlockHitResult hit) { int count=randomInteractions.isValue()?Math.max(1,Math.min(4,randomHits.getRandomValueInt())):1; for(int i=0;i<count;i++) use(hit); }
    private void use(BlockHitResult hit) { mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit); mc.player.swingHand(Hand.MAIN_HAND); }
    private void select(net.minecraft.item.Item item) { for(int i=0;i<9;i++) if(mc.player.getInventory().getStack(i).isOf(item)){selectSlot(i);return;} }
    private void selectSlot(int slot) { if(slot<0||slot>8)return; mc.player.getInventory().setSelectedSlot(slot); if(mc.getNetworkHandler()!=null)mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot)); }
    private void resetDelay(){delayCounter=0;sampledDelay=0;}
    private void resetAll(){step=0;anchoring=false;resetDelay();}
}
