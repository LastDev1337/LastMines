package ru.last.mines.gui.providers;

import dev.by1337.bmenu.slot.component.MenuClickType;
import dev.by1337.item.ItemModel;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import dev.by1337.yaml.YamlMap;
import ru.last.mines.LastMines;
import ru.last.mines.api.*;
import dev.by1337.bmenu.animation.util.AnimationUtil;
import ru.last.mines.models.*;
import ru.last.mines.utils.*;
import net.kyori.adventure.text.Component;
import me.clip.placeholderapi.PlaceholderAPI;
import java.util.*;

public class AllMinesProvider extends DefaultMenu {
    
    private final Map<Integer, String> slotToMine = new HashMap<>();

    public AllMinesProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
    }

    @Override
    protected void generate() {
        slotToMine.clear();
        YamlMap map = config.yaml();
        if (!map.has("mines_list")) return;
        YamlMap listMap = map.get("mines_list").asYamlMap().orDefault(new YamlMap());

        String slotsStr = listMap.get("slots").asString("");
        List<Integer> slots = slotsStr.isBlank() ? List.of() : Arrays.stream(AnimationUtil.readSlots(slotsStr)).boxed().toList();
        
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
            if (slot < 0 || slot >= layers.getBaseLayer().length) continue;

            Material mat = Material.matchMaterial(materialName);
            if (mat == null) mat = Material.STONE;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            boolean papiEnabled = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
            if (meta != null) {
                String dName = name.replace("{MINE_ID}", mine.getId());
                if (papiEnabled) dName = PlaceholderAPI.setPlaceholders(viewer, dName);
                meta.displayName(ColorUtils.color(dName));

                List<Component> dLore = new ArrayList<>();
                for (String l : lore) {
                    String line = l.replace("{MINE_ID}", mine.getId());
                    if (papiEnabled) line = PlaceholderAPI.setPlaceholders(viewer, line);
                    dLore.add(ColorUtils.color(line));
                }
                meta.lore(dLore);
                item.setItemMeta(meta);
            }
            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    YamlMap listMap = config.yaml().get("mines_list").asYamlMap().orDefault(new YamlMap());
                    String key = "on_" + type.name().toLowerCase() + "_click";
                    if (!listMap.has(key)) key = "on_click";
                    if (listMap.has(key)) {
                        Object cmdRaw = listMap.get(key).getRaw();
                        List<String> commands = new ArrayList<>();
                        if (cmdRaw instanceof List<?> l) {
                            for (Object o : l) commands.add(o.toString());
                        } else if (cmdRaw instanceof String s) {
                            commands.add(s);
                        }
                        for (String cmd : commands) {
                            if (cmd.contains("[open]")) {
                                LastMines.get().getGuiManager().openMenu(viewer, mine.getId());
                            }
                        }
                    }
                }
            };
            slotToMine.put(slot, mine.getId());
        }
    }
}
