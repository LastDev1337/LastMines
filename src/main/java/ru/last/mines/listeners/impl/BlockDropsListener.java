package ru.last.mines.listeners.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.mine.models.DropItem;
import ru.last.mines.mine.Mine;
import ru.last.mines.mine.models.MineBlock;
import ru.last.mines.utils.ColorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockDropsListener implements Listener {

    private static final int SIZE = 54;
    private static final int EDIT_SLOTS = 45;
    private static final int BACK_SLOT = 45;
    private static final int SAVE_SLOT = 53;

    private record Session(String mineId, String material) {}

    private final Map<Inventory, Session> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public void open(Player player, String mineId, String material) {
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

        Inventory inv = Bukkit.createInventory(null, SIZE, ColorUtils.color("&8Дроп блока &f" + material));
        if (target != null) {
            int slot = 0;
            for (DropItem drop : target.drops()) {
                if (slot >= EDIT_SLOTS) break;
                inv.setItem(slot++, new ItemStack(drop.material()));
            }
        }
        inv.setItem(BACK_SLOT, backButton());
        inv.setItem(SAVE_SLOT, saveButton());

        sessions.put(inv, new Session(mineId, material));
        player.openInventory(inv);
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        var meta = item.getItemMeta();
        meta.displayName(ColorUtils.color("&cНазад (без сохранения)"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack saveButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS);
        var meta = item.getItemMeta();
        meta.displayName(ColorUtils.color("&aСохранить дроп"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Редактор дропа: слоты 0..44 - "призрачные". Клик предметом из курсора ставит в слот ПРОСТО иконку материала
     * (предмет игрока не тратится), клик пустой рукой по слоту очищает его. Всё остальное запрещено, чтобы нельзя было
     * вынести иконки себе в инвентарь (создание предметов) или потерять свои предметы внутри виртуального окна.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Session session = sessions.get(event.getInventory());
        if (session == null) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        boolean top = event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory());
        Inventory inv = event.getInventory();

        if (!top) {
            if (event.isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                putGhost(inv, event.getCurrentItem().getType());
            }
            return;
        }

        int slot = event.getSlot();
        if (slot < EDIT_SLOTS) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                inv.setItem(slot, new ItemStack(cursor.getType()));
            } else {
                inv.setItem(slot, null);
            }
            return;
        }

        if (slot == SAVE_SLOT) {
            List<DropItem> drops = collectDrops(inv, session);
            LastMines.get().getActionManager().setBlockDrops(session.mineId(), session.material(), drops);
            player.sendMessage(ColorUtils.colorString("&aДроп блока " + session.material() + " сохранён (" + drops.size() + " предметов)."));
            player.closeInventory();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + session.mineId() + " block_drop_items " + player.getName() + " " + session.material());
        } else if (slot == BACK_SLOT) {
            player.closeInventory();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + session.mineId() + " block_settings " + player.getName() + " " + session.material());
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (sessions.containsKey(event.getInventory())) event.setCancelled(true);
    }

    private void putGhost(Inventory inv, Material material) {
        for (int i = 0; i < EDIT_SLOTS; i++) {
            ItemStack existing = inv.getItem(i);
            if (existing != null && existing.getType() == material) return;
        }
        for (int i = 0; i < EDIT_SLOTS; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, new ItemStack(material));
                return;
            }
        }
    }

    private List<DropItem> collectDrops(Inventory inv, Session session) {
        Map<Material, DropItem> existing = new HashMap<>();
        Mine mine = LastMinesProvider.getApi().getMine(session.mineId());
        if (mine != null) {
            Material target = Material.matchMaterial(session.material());
            for (MineBlock mb : mine.getCurrentBlocks()) {
                if (mb.material() == target) {
                    for (DropItem drop : mb.drops()) existing.put(drop.material(), drop);
                    break;
                }
            }
        }

        List<DropItem> drops = new ArrayList<>();
        List<Material> seen = new ArrayList<>();
        for (int i = 0; i < EDIT_SLOTS; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR || seen.contains(item.getType())) continue;
            seen.add(item.getType());
            drops.add(existing.getOrDefault(item.getType(), new DropItem(item.getType(), 100.0, false)));
        }
        return drops;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        sessions.remove(event.getInventory());
    }
}
