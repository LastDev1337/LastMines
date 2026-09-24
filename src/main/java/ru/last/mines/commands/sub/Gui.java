package ru.last.mines.commands.sub;

import dev.by1337.bmenu.BMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.models.Mine;
import ru.last.mines.models.MineBlock;
import ru.last.mines.utils.ColorUtils;

import java.util.Set;

public final class Gui {
    private Gui() {}

    private static final Set<String> SIMPLE_REOPEN_MENUS = Set.of(
            "blocks", "rarities", "actions", "permissions", "holograms", "online", "enchants", "reset_time",
            "permission_messages"
    );

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("gui")
                .argument(new SuggestingArgument("id", () -> plugin.getMineManager().getMines().keySet()))
                .argument(new ArgumentString<CommandSender>("sub"))
                .argument(new ArgumentString<CommandSender>("a"))
                .argument(new ArgumentString<CommandSender>("b"))
                .argument(new ArgumentString<CommandSender>("c"))
                .executor((sender, args) -> execute(plugin, sender,
                        (String) args.get("id"), (String) args.get("sub"),
                        (String) args.get("a"), (String) args.get("b"), (String) args.get("c")));
    }

    private static void execute(LastMines plugin, CommandSender sender, String id, String sub, String a, String b, String c) {
        if (id == null) {
            if (!(sender instanceof Player)) {
                plugin.getConfigManager().getMessages().getOnlyPlayers().send(sender);
                return;
            }
            if (plugin.getGuiManager() != null) {
                try {
                    Menu menu = BMenu.menuLoader().create("lastmines:all_mines", (Player) sender, null);
                    menu.open();
                } catch (Throwable e) {
                    sender.sendMessage(ColorUtils.colorString("<red>Не удалось открыть меню всех шахт. Проверьте консоль."));
                    e.printStackTrace();
                }
            } else {
                plugin.getConfigManager().getMessages().getGuiNotInstalled().send(sender);
            }
            return;
        }

        Mine mine = plugin.getMineManager().getMine(id);
        if (mine == null) {
            plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
            return;
        }

        if (plugin.getGuiManager() == null) {
            plugin.getConfigManager().getMessages().getGuiNotInstalled().send(sender);
            return;
        }

        if ("block_settings".equalsIgnoreCase(sub) && a != null && b != null) {
            Player target = Bukkit.getPlayer(a);
            String mat = b;
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
                } catch (Throwable e) {
                    sender.sendMessage(ColorUtils.colorString("<red>Не удалось открыть меню. Проверьте консоль."));
                    e.printStackTrace();
                }
            }
        } else if ("block_drop_items".equalsIgnoreCase(sub) && a != null && b != null) {
            Player target = Bukkit.getPlayer(a);
            String mat = b;
            if (target != null) {
                try {
                    plugin.getGuiManager().getViewingMines().put(target.getUniqueId(), id);
                    Menu menu = BMenu.menuLoader().create("lastmines:block_drop_items", target, null);
                    menu.addArgument("MINE_ID", id);
                    menu.addArgument("MATERIAL", mat);
                    menu.open();
                } catch (Throwable e) {
                    sender.sendMessage(ColorUtils.colorString("<red>Не удалось открыть меню. Проверьте консоль."));
                    e.printStackTrace();
                }
            }
        } else if ("block_drop_item".equalsIgnoreCase(sub) && a != null && b != null && c != null) {
            Player target = Bukkit.getPlayer(a);
            String mat = b;
            String dropMat = c;
            if (target != null) {
                try {
                    plugin.getGuiManager().getViewingMines().put(target.getUniqueId(), id);
                    Menu menu = BMenu.menuLoader().create("lastmines:block_drop_item", target, null);
                    menu.addArgument("MINE_ID", id);
                    menu.addArgument("MATERIAL", mat);
                    menu.addArgument("DROP_MATERIAL", dropMat);
                    menu.open();
                } catch (Throwable e) {
                    sender.sendMessage(ColorUtils.colorString("<red>Не удалось открыть меню. Проверьте консоль."));
                    e.printStackTrace();
                }
            }
        } else if (sub != null && a != null && SIMPLE_REOPEN_MENUS.contains(sub.toLowerCase())) {
            Player target = Bukkit.getPlayer(a);
            if (target != null) {
                try {
                    plugin.getGuiManager().getViewingMines().put(target.getUniqueId(), id);
                    Menu menu = BMenu.menuLoader().create("lastmines:" + sub.toLowerCase(), target, null);
                    menu.addArgument("MINE_ID", id);
                    menu.open();
                } catch (Throwable e) {
                    sender.sendMessage(ColorUtils.colorString("<red>Не удалось открыть меню. Проверьте консоль."));
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
    }
}
