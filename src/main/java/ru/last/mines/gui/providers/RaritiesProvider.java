package ru.last.mines.gui.providers;

import ru.last.mines.LastMines;

import dev.by1337.bmenu.slot.component.MenuClickType;
import dev.by1337.item.ItemModel;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import ru.last.mines.api.*;
import ru.last.mines.gui.ClickCommandResolver;
import ru.last.mines.models.*;
import ru.last.mines.utils.*;
import java.util.*;

public class RaritiesProvider extends DefaultMenu {
    
    private final Map<Integer, MineRarity> slotToRarity = new HashMap<>();

    public RaritiesProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void generate() {
        
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
        
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId == null) return;
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        
        List<MineRarity> rarities = mine.getRarities();
        
        for (int i = 0; i < Math.min(slots.size(), rarities.size()); i++) {
            MineRarity mr = rarities.get(i);
            int slot = slots.get(i);
            if (slot < 0 || slot >= layers.getBaseLayer().length) continue;

            Material mat = mr.icon() != null ? Material.matchMaterial(mr.icon()) : null;
            if (mat == null) mat = Material.matchMaterial(materialName);
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
            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    YamlMap listMap = config.yaml().get("rarities_list").asYamlMap().orDefault(new YamlMap());
                    ClickCommandResolver.resolveAndRun(menu, listMap, type, Map.of(
                            "{RARITY_ID}", mr.id(),
                            "{MINE_ID}", mine.getId()
                    ));
                }
            };
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
