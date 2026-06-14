package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;
import ru.last.mines.commands.sub.migrate.*;

import java.util.List;
import java.util.stream.Collectors;

@SubCommand(name = "migrate", aliases = {"mgrt"})
public class Migrate extends AbstractSubCommand {

    public Migrate(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines migrate <plugin>");
            return;
        }

        String targetPlugin = args[1];
        if (targetPlugin.equalsIgnoreCase("CataMines")) {
            CataMinesMigration.migrate(plugin, sender);
        } else if (targetPlugin.equalsIgnoreCase("AutoMine")) {
            AutoMineMigration.migrate(plugin, sender);
        } else if (targetPlugin.equalsIgnoreCase("RealMines")) {
            RealMinesMigration.migrate(plugin, sender);
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "§cПлагин " + targetPlugin + " не поддерживается! Доступно: CataMines, AutoMine, RealMines.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return List.of("CataMines", "AutoMine", "RealMines").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
