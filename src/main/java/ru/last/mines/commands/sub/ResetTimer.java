package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;
import ru.last.mines.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

@SubCommand(name = "resettimer", aliases = {"rt"})
public class ResetTimer extends AbstractSubCommand {

    public ResetTimer(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorString("<red>Использование: /lastmines resettimer <id>"));
            return;
        }
        String id = args[1];
        Mine mine = plugin.getMineManager().getMine(id);
        if (mine != null) {
            mine.setTimeLeft(mine.getResetTime());
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
