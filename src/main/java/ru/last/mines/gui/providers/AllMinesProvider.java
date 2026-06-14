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
import dev.by1337.yaml.YamlValue;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import java.util.*;

public class AllMinesProvider extends DefaultMenu {
    
    private final Map<Integer, String> slotToMine = new HashMap<>();

    public AllMinesProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
    }
    
    @Override
    protected void syncItems() {
        super.syncItems();
        
        slotToMine.clear();
        YamlMap map = config.yaml();
        if (!map.has("mines_list")) return;
        YamlMap listMap = map.get("mines_list").asYamlMap().orDefault(new YamlMap());
        
        String slotsStr = listMap.get("slots").asString("");
        List<Integer> slots = parseSlots(slotsStr);
        
        String materialName = listMap.get("material").asString("DIAMOND_ORE");
        String name = listMap.get("name").asString("&aШахта {MINE_ID}");
        List<String> lore = new ArrayList<>();
        if (listMap.has("lore")) {
            Object rawLore = listMap.get("lore").getRaw();
            if (rawLore instanceof List<?> l) {
                for (Object o : l) lore.add(o.toString());
            }
        }
        
        List<Mine> mines = new ArrayList<>(LastMinesProvider.getApi().getMines());
        
        for (int i = 0; i < Math.min(slots.size(), mines.size()); i++) {
            Mine mine = mines.get(i);
            int slot = slots.get(i);
            
            Material mat = Material.matchMaterial(materialName);
            if (mat == null) mat = Material.STONE;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String dName = name.replace("{MINE_ID}", mine.getId());
                dName = PlaceholderAPI.setPlaceholders(viewer, dName);
                meta.setDisplayName(ColorUtils.colorString(dName));
                
                List<String> dLore = new ArrayList<>();
                for (String l : lore) {
                    String line = l.replace("{MINE_ID}", mine.getId());
                    line = PlaceholderAPI.setPlaceholders(viewer, line);
                    dLore.add(ColorUtils.colorString(line));
                }
                meta.setLore(dLore);
                item.setItemMeta(meta);
            }
            getInventory().setItem(slot, item);
            slotToMine.put(slot, mine.getId());
        }
    }
    
    @Override
    public void onClick(InventoryClickEvent e) {
        if (slotToMine.containsKey(e.getRawSlot())) {
            e.setCancelled(true);
            String mineId = slotToMine.get(e.getRawSlot());
            YamlMap map = config.yaml();
            if (map.has("mines_list")) {
                YamlMap listMap = map.get("mines_list").asYamlMap().orDefault(new YamlMap());
                String onClick = listMap.get("on_click").asString("");
                if (onClick.contains("[open]")) {
                    ru.last.mines.LastMines.getInstance().getGuiManager().openMenu(viewer, mineId);
                }
            }
            return;
        }
        super.onClick(e);
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
