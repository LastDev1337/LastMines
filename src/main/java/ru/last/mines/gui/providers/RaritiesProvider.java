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
import ru.last.mines.models.MineRarity;
import ru.last.mines.utils.ColorUtils;
import java.util.*;

public class RaritiesProvider extends DefaultMenu {
    
    private final Map<Integer, MineRarity> slotToRarity = new HashMap<>();

    public RaritiesProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
    }
    
    @Override
    protected void syncItems() {
        super.syncItems();
        
        slotToRarity.clear();
        YamlMap map = config.yaml();
        if (!map.has("rarities_list")) return;
        YamlMap listMap = map.get("rarities_list").asYamlMap().orDefault(new YamlMap());
        
        String slotsStr = listMap.get("slots").asString("");
        List<Integer> slots = parseSlots(slotsStr);
        
        String materialName = listMap.get("material").asString("NETHER_STAR");
        String name = listMap.get("name").asString("&eРедкость &f{RARITY_NAME}");
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
        
        List<MineRarity> rarities = mine.getRarities();
        
        for (int i = 0; i < Math.min(slots.size(), rarities.size()); i++) {
            MineRarity mr = rarities.get(i);
            int slot = slots.get(i);
            
            Material mat = Material.matchMaterial(materialName);
            if (mat == null) mat = Material.NETHER_STAR;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String dName = name.replace("{RARITY_NAME}", mr.name())
                                   .replace("{RARITY_ID}", mr.id());
                meta.setDisplayName(ColorUtils.colorString(dName));
                
                List<String> dLore = new ArrayList<>();
                for (String l : lore) {
                    String line = l.replace("{RARITY_NAME}", mr.name())
                                   .replace("{RARITY_ID}", mr.id())
                                   .replace("{CHANCE}", String.valueOf(mr.chance()));
                    dLore.add(ColorUtils.colorString(line));
                }
                meta.setLore(dLore);
                item.setItemMeta(meta);
            }
            getInventory().setItem(slot, item);
            slotToRarity.put(slot, mr);
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
