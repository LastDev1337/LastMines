package ru.last.mines.gui.providers;

import ru.last.mines.LastMines;

import dev.by1337.item.ItemModel;
import org.bukkit.Material;
import dev.by1337.bmenu.slot.component.MenuClickType;
import org.bukkit.inventory.ItemStack;
import dev.by1337.yaml.YamlMap;
import java.util.ArrayList;
import java.util.Arrays;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.meta.ItemMeta;

import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.gui.ClickCommandResolver;
import dev.by1337.bmenu.animation.util.AnimationUtil;
import ru.last.mines.utils.ColorUtils;
import ru.last.mines.models.Mine;
import net.kyori.adventure.text.Component;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class HologramsProvider extends DefaultMenu {
    public HologramsProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    @Override
    protected void generate() {
        YamlMap map = config.yaml();
        if (!map.has("lines_list")) return;
        YamlMap listMap = map.get("lines_list").asYamlMap().orDefault(new YamlMap());

        String slotsStr = listMap.get("slots").asString("");
        List<Integer> slots = slotsStr.isBlank() ? List.of() : Arrays.stream(AnimationUtil.readSlots(slotsStr)).boxed().toList();

        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId == null) return;
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;
        
        List<String> lines = mine.getHoloTexts();
        String name = listMap.get("name").asString("&eСтрока #{INDEX}");
        List<String> loreTemplate = new ArrayList<>();
        if (listMap.has("lore")) {
            Object rawLore = listMap.get("lore").getRaw();
            if (rawLore instanceof List<?> l) {
                for (Object o : l) loreTemplate.add(o.toString());
            }
        }

        for (int i = 0; i < Math.min(slots.size(), lines.size()); i++) {
            String line = lines.get(i);
            int slot = slots.get(i);
            if (slot < 0 || slot >= layers.getBaseLayer().length) continue;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtils.color(name.replace("{INDEX}", String.valueOf(i + 1))));
                List<Component> dLore = new ArrayList<>();
                for (String l : loreTemplate) {
                    dLore.add(ColorUtils.color(l.replace("{LINE}", line)));
                }
                meta.lore(dLore);
                item.setItemMeta(meta);
            }
            final int index = i;
            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    YamlMap listMap = config.yaml().get("lines_list").asYamlMap().orDefault(new YamlMap());
                    ClickCommandResolver.resolveAndRun(menu, listMap, type, Map.of(
                            "{INDEX}", String.valueOf(index),
                            "{MINE_ID}", mine.getId()
                    ));
                }
            };
        }
    }
}
