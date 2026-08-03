package ru.last.mines.gui.providers;

import dev.by1337.bmenu.slot.component.MenuClickType;
import dev.by1337.item.ItemModel;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.yaml.YamlMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.DropItem;
import ru.last.mines.models.Mine;
import ru.last.mines.models.MineBlock;
import ru.last.mines.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class BlockDropItemsProvider extends DefaultMenu {

    public BlockDropItemsProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    @Override
    protected void generate() {
        YamlMap map = config.yaml();
        if (!map.has("drop_items_list")) return;
        YamlMap listMap = map.get("drop_items_list").asYamlMap().orDefault(new YamlMap());

        List<Integer> slots = parseSlots(listMap.get("slots").asString(""));
        String name = listMap.get("name").asString("&e{DROP_MATERIAL}");
        List<String> lore = new ArrayList<>();
        if (listMap.has("lore")) {
            Object rawLore = listMap.get("lore").getRaw();
            if (rawLore instanceof List<?> l) {
                for (Object o : l) lore.add(o.toString());
            }
        }

        String mineId = args.get("MINE_ID");
        String material = args.get("MATERIAL");
        if (mineId == null || material == null) return;
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        Material mat = Material.matchMaterial(material);
        MineBlock target = null;
        for (MineBlock mb : mine.getCurrentBlocks()) {
            if (mb.material() == mat) {
                target = mb;
                break;
            }
        }
        if (target == null) return;

        List<DropItem> drops = target.drops();
        for (int i = 0; i < Math.min(slots.size(), drops.size()); i++) {
            DropItem drop = drops.get(i);
            int slot = slots.get(i);

            ItemStack item = new ItemStack(drop.material());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String materialTag = "<lang:" + drop.material().translationKey() + ">";

                meta.displayName(ColorUtils.color(name.replace("{DROP_MATERIAL}", materialTag)));
                List<Component> dLore = new ArrayList<>();
                for (String l : lore) {
                    String line = l.replace("{DROP_MATERIAL}", materialTag)
                            .replace("{CHANCE}", String.valueOf(drop.chance()))
                            .replace("{FORTUNE}", String.valueOf(drop.fortune()));
                    dLore.add(ColorUtils.color(line));
                }
                meta.lore(dLore);
                item.setItemMeta(meta);
            }

            String finalMineId = mineId;
            String finalMaterial = material;
            String dropMaterialName = drop.material().name();
            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    if (type == MenuClickType.RIGHT) {
                        LastMines.get().getActionManager().editDropItem(finalMineId, finalMaterial, dropMaterialName, "remove", "");
                        menu.refresh();
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                "lastmines gui " + finalMineId + " block_drop_item " + player.getName() + " " + finalMaterial + " " + dropMaterialName);
                    }
                }
            };
        }
    }

    private List<Integer> parseSlots(String slotsStr) {
        List<Integer> slots = new ArrayList<>();
        for (String p : slotsStr.replace(" ", "").split(",")) {
            if (p.contains("-")) {
                String[] range = p.split("-");
                if (range.length == 2) {
                    try {
                        for (int i = Integer.parseInt(range[0]); i <= Integer.parseInt(range[1]); i++) slots.add(i);
                    } catch (Exception ignored) {}
                }
            } else {
                try { slots.add(Integer.parseInt(p)); } catch (Exception ignored) {}
            }
        }
        return slots;
    }
}
