package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;
import ru.last.mines.utils.*;

@SubCommand(name = "help")
public class Help extends AbstractSubCommand {

    public Help(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        for (String line : plugin.getConfigManager().getMessages().getHelp()) {
            sender.sendMessage(ColorUtils.colorString(line));
        }
    }
}
