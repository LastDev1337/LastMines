package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;

@SubCommand(name = "reload", aliases = {"rl"})
public class Reload extends AbstractSubCommand {

    public Reload(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getConfigManager().loadAll();
        plugin.getMineManager().unloadAll();
        plugin.getMineManager().loadAll();
        if (plugin.getGuiManager() != null) plugin.getGuiManager().reload();
        if (plugin.getMainCommand() != null) plugin.getMainCommand().loadSubCommands();
        plugin.getConfigManager().getMessages().getReloaded().send(sender);
    }
}
