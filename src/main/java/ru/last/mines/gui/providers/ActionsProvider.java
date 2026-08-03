package ru.last.mines.gui.providers;

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
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActionsProvider extends DefaultMenu {

    private static final Map<UUID, Integer> pageByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> maxPageByPlayer = new HashMap<>();

    public ActionsProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    public static void changePage(Player player, int delta) {
        int current = pageByPlayer.getOrDefault(player.getUniqueId(), 0);
        int max = maxPageByPlayer.getOrDefault(player.getUniqueId(), 0);
        pageByPlayer.put(player.getUniqueId(), Math.clamp(current + delta, 0, max));
    }

    @Override
    protected void generate() {
        YamlMap map = config.yaml();
        if (!map.has("actions_list")) return;
        YamlMap listMap = map.get("actions_list").asYamlMap().orDefault(new YamlMap());

        List<Integer> slots = parseSlots(listMap.get("slots").asString(""));
        String name = listMap.get("name").asString("&f{INDEX}. &7{ACTION}");
        int pageSize = Math.max(1, slots.size());

        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId == null) return;
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        List<String> actions = mine.getRawActions();

        int maxPage = Math.max(0, (actions.size() - 1) / pageSize);
        maxPageByPlayer.put(viewer.getUniqueId(), maxPage);
        int page = Math.min(pageByPlayer.getOrDefault(viewer.getUniqueId(), 0), maxPage);
        pageByPlayer.put(viewer.getUniqueId(), page);
        addArgument("PAGE", String.valueOf(page + 1));
        addArgument("MAX_PAGE", String.valueOf(maxPage + 1));

        int from = page * pageSize;
        int to = Math.min(actions.size(), from + pageSize);

        for (int i = from; i < to; i++) {
            String action = actions.get(i);
            int slot = slots.get(i - from);
            int index = i;

            Material icon = Material.PAPER;
            String lower = action.toLowerCase();
            if (lower.contains("[title]")) icon = Material.OAK_SIGN;
            else if (lower.contains("[actionbar]")) icon = Material.NAME_TAG;
            else if (lower.contains("[sound]")) icon = Material.NOTE_BLOCK;

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorString(name.replace("{INDEX}", String.valueOf(index + 1)).replace("{ACTION}", action)));
                meta.setLore(List.of(ColorUtils.colorString("&cЛКМ: &7удалить")));
                item.setItemMeta(meta);
            }

            String finalMineId = mineId;
            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    LastMines.get().getActionManager().removeAction(finalMineId, index);
                    menu.refresh();
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
