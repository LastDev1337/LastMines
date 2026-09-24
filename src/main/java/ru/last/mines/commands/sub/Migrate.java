package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.commands.sub.migrate.AutoMineMigration;
import ru.last.mines.commands.sub.migrate.CataMinesMigration;
import ru.last.mines.commands.sub.migrate.RealMinesMigration;
import ru.last.mines.utils.ColorUtils;

import java.util.List;

public final class Migrate {
    private Migrate() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("migrate")
                .argument(new SuggestingArgument("plugin", () -> List.of("CataMines", "AutoMine", "RealMines")))
                .executor((sender, args) -> {
                    String targetPlugin = (String) args.get("plugin");
                    if (targetPlugin == null) {
                        sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines migrate <plugin>"));
                        return;
                    }

                    if (targetPlugin.equalsIgnoreCase("CataMines")) {
                        CataMinesMigration.migrate(plugin, sender);
                    } else if (targetPlugin.equalsIgnoreCase("AutoMine")) {
                        AutoMineMigration.migrate(plugin, sender);
                    } else if (targetPlugin.equalsIgnoreCase("RealMines")) {
                        RealMinesMigration.migrate(plugin, sender);
                    } else {
                        sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Плагин " + targetPlugin + " не поддерживается! Доступно: CataMines, AutoMine, RealMines."));
                    }
                });
    }
}
