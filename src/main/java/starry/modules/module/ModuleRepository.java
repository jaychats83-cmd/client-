package starry.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import starry.modules.impl.combat.*;
import starry.modules.impl.movement.*;
import starry.modules.impl.player.*;
import starry.modules.impl.render.*;
import starry.modules.impl.util.*;
import starry.modules.impl.basefind.*;
import starry.modules.impl.extras.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<ModuleStructure> moduleStructures = new ArrayList<>();
    List<ModuleStructure> hiddenModules = new ArrayList<>();
    Set<Class<? extends ModuleStructure>> registeredClasses = new HashSet<>();

    public void setup() {
        builder()
                // Combat
                .add(new AimAssist())
                .add(new AirPlace())
                .add(new AnchorMacro())
                .add(new AntiTrap())
                .add(new ArmorSaver())
                .add(new AttributeSwap())
                .add(new AutoCrystal())
                .add(new AutoDoubleHand())
                .add(new AutoDTAP())
                .add(new AutoHitCrystal())
                .add(new AutoInventoryTotem())
                .add(new AutoJumpReset())
                .add(new LungeMacro())
                .add(new AutoMace())
                .add(new AutoPot())
                .add(new AutoPotRefill())
                .add(new AutoWTap())
                .add(new CartPvPModule())
                .add(new Criticals())
                .add(new CrystalOptimizer())
                .add(new DoubleAnchor())
                .add(new DoubleAnchorV3())
                .add(new HoverTotem())
                .add(new Killaura())
                .add(new MaceSwap())
                .add(new NoMissDelay())
                .add(new OneNineEightMacro())
                .add(new SafeAnchor())
                .add(new ShieldDisabler())
                .add(new StunSlam())
                .add(new TotemKB())
                .add(new TotemOffhand())
                .add(new TriggerBot())
                .add(new Velocity())
                // ESP finders
                .add(new RotatedDeepslateESP())
                .add(new PistonESP())
                .add(new SpawnerESP())
                .add(new VillagerESP())
                .add(new StorageESP())
                // Extras
                .add(new AntiPacketKick())
                .add(new Nuker())
                .add(new FreecamMine())
                .add(new PacketMine())
                .add(new GuiMove())
                .add(new SpeedMine())
                .add(new NoRotation())
                // Movement
                .add(new Sprint())
                .add(new Flight())
                // Render
                .add(new Freecam())
                .add(new ArmorStatus())
                .add(new Coordinates())
                .add(new FakeScoreboard())
                .add(new Keystrokes())
                .add(new NoBounce())
                .add(new BaseBlockFinder())
                .add(new BaseEntityFinder())
                .add(new SusChunkESP())
                .add(new TargetHud())
                .add(new x7m4j9())
                .add(new al8gug())
                .add(new dl3shr())
                .add(new dvx3zg())
                .add(new dvz3a8())
                .add(new Fog())
                .add(new gj9zu2())
                .add(new h1v5ow())
                .add(new Hitbox())
                .add(new hr71oe())
                .add(new j2aj68())
                .add(new jvwsoq())
                .add(new k44sr0())
                .add(new k5g3vf())
                .add(new k9rp40())
                .add(new KillEffect())
                .add(new l44eio())
                .add(new lkaj12())
                .add(new m1hj8e())
                .add(new o8zfl9())
                .add(new qxbn5c())
                .add(new svyaih())
                .add(new TargetESP())
                .add(new xnpteo())
                .add(new zos00u())
                .add(new zznwpi())
                // Player
                .add(new Friends())
                .add(new AntiWeb())
                .add(new CartTrap())
                .add(new FakeLag())
                // Misc / Util
                .add(new AntiAFK())
                .add(new AutoClicker())
                .add(new AutoEat())
                .add(new AutoFirework())
                .add(new AutoTool())
                .add(new AutoXP())
                .add(new KeyPearl())
                .add(new NoBreakDelay())
                .add(new NoJumpDelay())
                .add(new PackSpoof())
                .add(new PingSpoof())
                .add(new Prevent())
                .add(new CommandModule())
                .add(new FastPlace())
                .add(new Hitboxes())
                .add(new HoldUseItem())
                .add(new ItemAction())
                .add(new MeleeAssist())
                .add(new RepeatingCommand())
                .add(new StateToggle())
                .add(new TargetPlace())
                .add(new UICommand());
    }

    public ModuleBuilder builder() {
        return new ModuleBuilder(this);
    }

    void registerModule(ModuleStructure module, boolean hidden) {
        Class<? extends ModuleStructure> clazz = module.getClass();
        if (registeredClasses.contains(clazz)) {
            throw new DuplicateModuleException(clazz.getSimpleName());
        }
        registeredClasses.add(clazz);
        if (hidden) {
            hiddenModules.add(module);
            module.setState(true);
        } else {
            moduleStructures.add(module);
        }
    }

    public List<ModuleStructure> modules() {
        return moduleStructures;
    }

    public List<ModuleStructure> hiddenModules() {
        return hiddenModules;
    }

    public List<ModuleStructure> allModules() {
        List<ModuleStructure> all = new ArrayList<>(moduleStructures);
        all.addAll(hiddenModules);
        return all;
    }
}
