package ru.last.mines.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SubCommand(name = "gui", aliases = {"menu"})
public class Gui extends AbstractSubCommand {

    public Gui(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cКоманда только для игроков!");
            return;
        }
        if (args.length < 2) {
            if (Bukkit.getPluginManager().getPlugin("BMenu") != null) {
                try {
                    dev.by1337.bmenu.menu.Menu menu = dev.by1337.bmenu.BMenu.menuLoader().create("lastmines:all_mines", (Player) sender, null);
                    menu.open();
                } catch (Exception e) {
                    sender.sendMessage("§cНе удалось открыть меню всех шахт. Проверьте консоль.");
                    e.printStackTrace();
                }
            } else {
                sender.sendMessage("§cBMenu не установлен, GUI недоступно!");
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
            plugin.getActionManager().openGui((Player) sender, id);
        } else {
            sender.sendMessage("§cBMenu не установлен, GUI недоступно!");
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
