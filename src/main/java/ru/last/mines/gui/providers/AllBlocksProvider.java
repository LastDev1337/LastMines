package ru.last.mines.gui.providers;

import dev.by1337.bmenu.slot.component.MenuClickType;
import dev.by1337.item.ItemModel;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.last.mines.LastMines;
import dev.by1337.bmenu.animation.util.AnimationUtil;
import ru.last.mines.utils.ColorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

public class AllBlocksProvider extends DefaultMenu {

    private static final Map<UUID, Integer> pageByPlayer = new HashMap<>();

    private static volatile int lastPageSize = 45;

    private static List<Material> allBlocks() {
        return LastMines.get().getBlockCatalogManager().getBlocks();
    }

    public AllBlocksProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    public static int getMaxPage() {
        return Math.max(0, (allBlocks().size() - 1) / Math.max(1, lastPageSize));
    }

    public static void changePage(Player player, int delta) {
        int current = pageByPlayer.getOrDefault(player.getUniqueId(), 0);
        pageByPlayer.put(player.getUniqueId(), Math.clamp(current + delta, 0, getMaxPage()));
    }

    @Override
    protected void generate() {
        String finalMineId = args.get("MINE_ID");
        if (finalMineId == null) return;

        String slotsStr = config.yaml().get("slots").asString("0-44");
        List<Integer> slots = slotsStr.isBlank() ? List.of() : Arrays.stream(AnimationUtil.readSlots(slotsStr)).boxed().toList();
        lastPageSize = Math.max(1, slots.size());

        int page = Math.min(pageByPlayer.getOrDefault(viewer.getUniqueId(), 0), getMaxPage());
        pageByPlayer.put(viewer.getUniqueId(), page);
        addArgument("PAGE", String.valueOf(page + 1));
        addArgument("MAX_PAGE", String.valueOf(getMaxPage() + 1));

        List<Material> allBlocks = allBlocks();
        int from = page * lastPageSize;
        int to = Math.min(allBlocks.size(), from + lastPageSize);

        for (int i = from; i < to; i++) {
            Material mat = allBlocks.get(i);
            int slot = slots.get(i - from);
            if (slot < 0 || slot >= layers.getBaseLayer().length) continue;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.translatable(mat.translationKey()).color(NamedTextColor.WHITE));
                meta.lore(List.of(ColorUtils.color("&7Нажмите, чтобы добавить в шахту")));
                item.setItemMeta(meta);
            }

            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(item)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
                    LastMines.get().getActionManager().editBlock(finalMineId, mat.name(), "chance", "10");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + finalMineId + " blocks " + player.getName());
                }
            };
        }
    }
}
