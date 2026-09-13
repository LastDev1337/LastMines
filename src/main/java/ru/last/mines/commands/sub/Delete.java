package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;
import ru.last.mines.utils.ColorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SubCommand(name = "delete", aliases = {"del", "remove"})
public class Delete extends AbstractSubCommand {

    public Delete(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines delete <id>"));
            return;
        }

        String id = args[1];
        Mine mine = plugin.getMineManager().getMine(id);
        if (mine == null) {
            plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
            return;
        }

        plugin.getActionManager().deleteMine(id);

        sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Шахта " + id + " успешно удалена."));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(plugin.getMineManager().getMines().keySet());
        }
        return Collections.emptyList();
    }
}
