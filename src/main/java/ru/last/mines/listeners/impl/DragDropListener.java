package ru.last.mines.listeners.impl;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import ru.last.mines.LastMines;
import ru.last.mines.api.*;
import ru.last.mines.models.*;

import java.util.Collections;
import java.util.List;

public class DragDropListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryView view = event.getView();
        String title = PlainTextComponentSerializer.plainText().serialize(view.title());
        Inventory topInv = view.getTopInventory();
        Inventory bottomInv = view.getBottomInventory();

        if (!title.contains("Блоки шахты") && !title.contains("Блоки")) return;

        String mineId = LastMines.get().getGuiManager().getViewingMines().get(player.getUniqueId());
        if (mineId == null) return;

        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        ItemStack item = null;
        if (event.getClickedInventory() == bottomInv && event.isShiftClick()) {
            item = event.getCurrentItem();
        } else if (event.getClickedInventory() == topInv) {
            item = event.getCursor();
        }

        if (item == null || item.getType() == Material.AIR || !item.getType().isBlock()) return;

        Material mat = item.getType();
        List<MineBlock> blocks = mine.getCurrentBlocks();
        boolean exists = false;
        for (MineBlock mb : blocks) {
            if (mb.material() == mat) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            blocks.add(new MineBlock(mat, 1.0, 0, 0, Collections.emptyList()));
            mine.save();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + mineId + " blocks " + player.getName());
        }
    }
}
