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
import ru.last.mines.gui.PagedView;
import dev.by1337.bmenu.animation.util.AnimationUtil;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

import java.util.List;
import java.util.Arrays;

public class ActionsProvider extends DefaultMenu {

    private static final PagedView PAGES = new PagedView();

    public ActionsProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    public static void changePage(Player player, int delta) {
        PAGES.changePage(player, delta);
    }

    @Override
    protected void generate() {
        YamlMap map = config.yaml();
        if (!map.has("actions_list")) return;
        YamlMap listMap = map.get("actions_list").asYamlMap().orDefault(new YamlMap());

        String slotsStr = listMap.get("slots").asString("");
        List<Integer> slots = slotsStr.isBlank() ? List.of() : Arrays.stream(AnimationUtil.readSlots(slotsStr)).boxed().toList();
        String name = listMap.get("name").asString("&f{INDEX}. &7{ACTION}");
        int pageSize = Math.max(1, slots.size());

        String finalMineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (finalMineId == null) return;
        Mine mine = LastMinesProvider.getApi().getMine(finalMineId);
        if (mine == null) return;

        List<String> actions = mine.getRawActions();

        int page = PAGES.resolvePage(viewer.getUniqueId(), actions.size(), pageSize);
        int maxPage = PAGES.maxPage(viewer.getUniqueId());
        addArgument("PAGE", String.valueOf(page + 1));
        addArgument("MAX_PAGE", String.valueOf(maxPage + 1));

        int from = page * pageSize;
        int to = Math.min(actions.size(), from + pageSize);

        for (int i = from; i < to; i++) {
            String action = actions.get(i);
            int slot = slots.get(i - from);
            if (slot < 0 || slot >= layers.getBaseLayer().length) continue;
            int index = i;

            Material icon = Material.PAPER;
            String lower = action.toLowerCase();
            if (lower.contains("[title]")) icon = Material.OAK_SIGN;
            else if (lower.contains("[actionbar]")) icon = Material.NAME_TAG;
            else if (lower.contains("[sound]")) icon = Material.NOTE_BLOCK;

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtils.color(name.replace("{INDEX}", String.valueOf(index + 1)).replace("{ACTION}", action)));
                meta.lore(List.of(ColorUtils.color("<red>ЛКМ: <gray>удалить")));
                item.setItemMeta(meta);
            }

            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    LastMines.get().getActionManager().removeAction(finalMineId, index);
                    menu.refresh();
                }
            };
        }
    }
}
