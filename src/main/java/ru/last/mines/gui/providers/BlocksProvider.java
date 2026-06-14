package ru.last.mines.gui.providers;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import dev.by1337.yaml.YamlMap;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;
import ru.last.mines.models.MineBlock;
import ru.last.mines.utils.ColorUtils;
import java.util.*;

public class BlocksProvider extends DefaultMenu {
    
    private final Map<Integer, MineBlock> slotToBlock = new HashMap<>();

    public BlocksProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
    }
    
    @Override
    protected void syncItems() {
        super.syncItems();
        
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
        
        String mineId = ru.last.mines.LastMines.getInstance().getGuiManager().getViewingMines().get(viewer.getUniqueId());
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
                String dName = name.replace("{MATERIAL}", item.getType().name());
                meta.setDisplayName(ColorUtils.colorString(dName));
                
                List<String> dLore = new ArrayList<>();
                for (String l : lore) {
                    String line = l.replace("{MATERIAL}", item.getType().name())
                                   .replace("{CHANCE}", String.valueOf(mb.chance()));
                    dLore.add(ColorUtils.colorString(line));
                }
                meta.setLore(dLore);
                item.setItemMeta(meta);
            }
            getInventory().setItem(slot, item);
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
