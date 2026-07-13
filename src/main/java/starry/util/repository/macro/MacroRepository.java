package starry.util.repository.macro;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import starry.events.api.EventHandler;
import starry.events.api.EventManager;
import starry.events.impl.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class MacroRepository {
    private static MacroRepository instance;
    private final List<Macro> macroList = new ArrayList<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public MacroRepository() {
        instance = this;
    }

    public static MacroRepository getInstance() {
        if (instance == null) {
            instance = new MacroRepository();
        }
        return instance;
    }

    public void init() {
        EventManager.register(this);
    }

    public void addMacro(String name, String message, int key) {
        macroList.add(new Macro(name, message, key));
    }

    public void addMacroAndSave(String name, String message, int key) {
        addMacro(name, message, key);
    }

    public boolean hasMacro(String name) {
        return macroList.stream().anyMatch(macro -> macro.name().equalsIgnoreCase(name));
    }

    public Optional<Macro> getMacro(String name) {
        return macroList.stream()
                .filter(macro -> macro.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public void deleteMacro(String name) {
        macroList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
    }

    public void deleteMacroAndSave(String name) {
        deleteMacro(name);
    }

    public void clearList() {
        macroList.clear();
    }

    public void clearListAndSave() {
        clearList();
    }

    public int size() {
        return macroList.size();
    }

    public List<String> getMacroNames() {
        return macroList.stream().map(Macro::name).collect(Collectors.toList());
    }

    public void setMacros(List<Macro> macros) {
        macroList.clear();
        macroList.addAll(macros);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (event.action() != 1) return;

        macroList.stream()
                .filter(macro -> macro.key() == event.key())
                .findFirst()
                .ifPresent(macro -> {
                    String message = macro.message();
                    if (message.startsWith("/")) {
                        mc.player.networkHandler.sendChatCommand(message.substring(1));
                    } else {
                        mc.player.networkHandler.sendChatMessage(message);
                    }
                });
    }
}