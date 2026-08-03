package ru.last.mines.commands.sub;

import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.BMenu;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SubCommand(name = "gui", aliases = {"menu"})
public class Gui extends AbstractSubCommand {

    private static final Set<String> SIMPLE_REOPEN_MENUS = Set.of(
            "blocks", "rarities", "actions", "permissions", "holograms", "online", "enchants", "reset_time",
            "permission_messages"
    );

    public Gui(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                plugin.getConfigManager().getMessages().getOnlyPlayers().send(sender);
                return;
            }
            if (Bukkit.getPluginManager().getPlugin("BMenu") != null) {
                try {
                    Menu menu = BMenu.menuLoader().create("lastmines:all_mines", (Player) sender, null);
                    menu.open();
                } catch (Exception e) {
                    sender.sendMessage("§cНе удалось открыть меню всех шахт. Проверьте консоль.");
                    e.printStackTrace();
                }
            } else {
                plugin.getConfigManager().getMessages().getGuiNotInstalled().send(sender);
            }
            return;
        }
        String id = args[1];
        Mine mine = plugin.getMineManager().getMine(id);
        if (mine == null) {
            plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("BMenu") != null) {
            if (args.length >= 5 && "block_settings".equalsIgnoreCase(args[2])) {
                Player target = Bukkit.getPlayer(args[3]);
                String mat = args[4];
                if (target != null) {
                    try {
                        plugin.getGuiManager().getViewingMines().put(target.getUniqueId(), id);
                        Menu menu = BMenu.menuLoader().create("lastmines:block_settings", target, null);
                        menu.addArgument("MINE_ID", id);
                        menu.addArgument("MATERIAL", mat);
                        
                        double chance = 0;
                        int limit = 0;
                        for (MineBlock mb : mine.getCurrentBlocks()) {
                            if (mb.material().name().equalsIgnoreCase(mat)) {
                                chance = mb.chance();
                                limit = mb.max();
                                break;
                            }
                        }
                        menu.addArgument("CHANCE", String.valueOf(chance));
                        menu.addArgument("LIMIT", String.valueOf(limit));

                        menu.open();
                    } catch (Exception e) {
                        sender.sendMessage("§cНе удалось открыть меню. Проверьте консоль.");
                        e.printStackTrace();
                    }
                }
            } else if (args.length >= 5 && "block_drop_items".equalsIgnoreCase(args[2])) {
                Player target = Bukkit.getPlayer(args[3]);
                String mat = args[4];
                if (target != null) {
                    try {
                        plugin.getGuiManager().getViewingMines().put(target.getUniqueId(), id);
                        Menu menu = BMenu.menuLoader().create("lastmines:block_drop_items", target, null);
                        menu.addArgument("MINE_ID", id);
                        menu.addArgument("MATERIAL", mat);
                        menu.open();
                    } catch (Exception e) {
                        sender.sendMessage("§cНе удалось открыть меню. Проверьте консоль.");
                        e.printStackTrace();
                    }
                }
            } else if (args.length >= 6 && "block_drop_item".equalsIgnoreCase(args[2])) {
                Player target = Bukkit.getPlayer(args[3]);
                String mat = args[4];
                String dropMat = args[5];
                if (target != null) {
                    try {
                        plugin.getGuiManager().getViewingMines().put(target.getUniqueId(), id);
                        Menu menu = BMenu.menuLoader().create("lastmines:block_drop_item", target, null);
                        menu.addArgument("MINE_ID", id);
                        menu.addArgument("MATERIAL", mat);
                        menu.addArgument("DROP_MATERIAL", dropMat);
                        menu.open();
                    } catch (Exception e) {
                        sender.sendMessage("§cНе удалось открыть меню. Проверьте консоль.");
                        e.printStackTrace();
                    }
                }
            } else if (args.length >= 4 && SIMPLE_REOPEN_MENUS.contains(args[2].toLowerCase())) {
                Player target = Bukkit.getPlayer(args[3]);
                if (target != null) {
                    try {
                        plugin.getGuiManager().getViewingMines().put(target.getUniqueId(), id);
                        Menu menu = BMenu.menuLoader().create("lastmines:" + args[2].toLowerCase(), target, null);
                        menu.addArgument("MINE_ID", id);
                        menu.open();
                    } catch (Exception e) {
                        sender.sendMessage("§cНе удалось открыть меню. Проверьте консоль.");
                        e.printStackTrace();
                    }
                }
            } else {
                if (!(sender instanceof Player)) {
                    plugin.getConfigManager().getMessages().getOnlyPlayers().send(sender);
                    return;
                }
                plugin.getActionManager().openGui((Player) sender, id);
            }
        } else {
            plugin.getConfigManager().getMessages().getGuiNotInstalled().send(sender);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(plugin.getMineManager().getMines().keySet());
        }
        return Collections.emptyList();
    }
}
