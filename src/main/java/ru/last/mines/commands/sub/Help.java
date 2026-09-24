package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.utils.ColorUtils;

public final class Help {
    private Help() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("help").executor(sender -> execute(plugin, sender));
    }

    public static void execute(LastMines plugin, CommandSender sender) {
        for (String line : plugin.getConfigManager().getMessages().getHelp()) {
            sender.sendMessage(ColorUtils.colorString(line));
        }
    }
}
