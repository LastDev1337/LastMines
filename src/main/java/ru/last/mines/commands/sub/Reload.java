package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.MainCommand;

public final class Reload {
    private Reload() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("reload").executor(sender -> execute(plugin, sender));
    }

    public static void execute(LastMines plugin, CommandSender sender) {
        plugin.getConfigManager().loadAll();
        plugin.getMineManager().unloadAll();
        plugin.getMineManager().loadAll();
        if (plugin.getGuiManager() != null) plugin.getGuiManager().reload();
        MainCommand.register(plugin);
        plugin.getConfigManager().getMessages().getReloaded().send(sender);
    }
}
