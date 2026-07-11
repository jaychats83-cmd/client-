package starry.modules.module.setting.implement;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import starry.modules.module.setting.Setting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class BlockSelectSetting extends Setting {
    private final List<Block> blocks = new ArrayList<>();
    private final List<String> selected = new ArrayList<>();
    private Consumer<List<String>> changeListener = ignored -> {};

    public BlockSelectSetting(String name, String description) {
        super(name, description);
        Registries.BLOCK.stream()
                .filter(block -> block.asItem() != net.minecraft.item.Items.AIR)
                .sorted(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()))
                .forEach(blocks::add);
    }

    public List<Block> getBlocks() { return blocks; }
    public List<String> getSelected() { return selected; }
    public boolean isSelected(Block block) { return selected.contains(Registries.BLOCK.getId(block).toString()); }
    public BlockSelectSetting selected(Iterable<String> ids) { selected.clear(); ids.forEach(selected::add); return this; }
    public BlockSelectSetting onChange(Consumer<List<String>> listener) { changeListener = listener; return this; }
    public void toggle(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        if (!selected.remove(id)) selected.add(id);
        changeListener.accept(List.copyOf(selected));
    }
}
