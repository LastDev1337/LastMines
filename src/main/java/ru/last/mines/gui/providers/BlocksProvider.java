package ru.last.mines.gui.providers;

import dev.by1337.bmenu.slot.component.MenuClickType;
import dev.by1337.item.ItemModel;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.yaml.YamlMap;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import ru.last.mines.LastMines;
import ru.last.mines.api.*;
import ru.last.mines.gui.ClickCommandResolver;
import ru.last.mines.models.*;
import ru.last.mines.utils.*;

import java.util.*;

public class BlocksProvider extends DefaultMenu {
    
    private final Map<Integer, MineBlock> slotToBlock = new HashMap<>();

    public BlocksProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    @Override
    protected void generate() {
        slotToBlock.clear();
        YamlMap map = config.yaml();
        if (!map.has("blocks_list")) return;
        YamlMap listMap = map.get("blocks_list").asYamlMap().orDefault(new YamlMap());
        
        String slotsStr = listMap.get("slots").asString("");
        List<Integer> slots = parseSlots(slotsStr);
        
        String name = listMap.get("name").asString("&eБлок &f{MATERIAL}");
        List<String> lore = new ArrayList<>();
        if (listMap.has("lore")) {
            Object rawLore = listMap.get("lore").getRaw();
            if (rawLore instanceof List<?> l) {
                for (Object o : l) lore.add(o.toString());
            }
        }
        
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId == null) return;
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        
        List<MineBlock> blocks = mine.getCurrentBlocks();
        
        for (int i = 0; i < Math.min(slots.size(), blocks.size()); i++) {
            MineBlock mb = blocks.get(i);
            int slot = slots.get(i);
            
            ItemStack item = new ItemStack(mb.material() == null ? Material.STONE : mb.material());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String materialTag = "<lang:" + item.getType().translationKey() + ">";

                String dName = name.replace("{MATERIAL}", materialTag);
                meta.displayName(ColorUtils.color(dName));

                List<Component> dLore = new ArrayList<>();
                for (String l : lore) {
                    String line = l.replace("{MATERIAL}", materialTag)
                                   .replace("{CHANCE}", String.valueOf(mb.chance()));
                    dLore.add(ColorUtils.color(line));
                }
                meta.lore(dLore);
                item.setItemMeta(meta);
            }
            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    YamlMap listMap = config.yaml().get("blocks_list").asYamlMap().orDefault(new YamlMap());
                    ClickCommandResolver.resolveAndRun(menu, listMap, type, Map.of(
                            "{MATERIAL}", item.getType().name(),
                            "{MINE_ID}", mine.getId()
                    ));
                }
            };
            slotToBlock.put(slot, mb);
        }
    }

    private List<Integer> parseSlots(String slotsStr) {
        List<Integer> slots = new ArrayList<>();
        String[] parts = slotsStr.replace(" ", "").split(",");
        for (String p : parts) {
            if (p.contains("-")) {
                String[] range = p.split("-");
                if (range.length == 2) {
                    try {
                        int min = Integer.parseInt(range[0]);
                        int max = Integer.parseInt(range[1]);
                        for (int i = min; i <= max; i++) slots.add(i);
                    } catch (Exception ignored) {}
                }
            } else {
                try { slots.add(Integer.parseInt(p)); } catch (Exception ignored) {}
            }
        }
        return slots;
    }
}
