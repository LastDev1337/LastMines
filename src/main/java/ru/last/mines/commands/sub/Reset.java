package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;
import ru.last.mines.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

@SubCommand(name = "reset", aliases = {"r"})
public class Reset extends AbstractSubCommand {

    public Reset(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorString("<red>Использование: /lastmines reset <id>"));
            return;
        }
        String id = args[1];
        Mine mine = plugin.getMineManager().getMine(id);
        if (mine != null) {
            if (mine.isResetting()) {
                sender.sendMessage(ColorUtils.colorString("<red>Автошахта " + id + " уже сбрасывается."));
                return;
            }
            plugin.getActionManager().resetMine(id);
            plugin.getConfigManager().getMessages().getMineReset().send(sender, "%mine%", id);
        } else {
            plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(plugin.getMineManager().getMines().keySet());
        }
        return super.tabComplete(sender, args);
    }
}
