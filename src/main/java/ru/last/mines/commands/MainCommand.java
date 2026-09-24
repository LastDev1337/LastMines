package ru.last.mines.commands;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.LLibAPI;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.sub.*;
import ru.last.mines.config.models.*;
import ru.last.mines.utils.ColorUtils;

public final class MainCommand {
    private MainCommand() {}

    public static void register(LastMines plugin) {
        CommandService cs = LLibAPI.get().commands();

        Command<CommandSender> old = plugin.getRootCommand();
        if (old != null) {
            cs.unregister(old);
        }

        Command<CommandSender> root = build(plugin, cs);
        cs.register(plugin, root);
        plugin.setRootCommand(root);
    }

    public static void unregister(LastMines plugin) {
        Command<CommandSender> root = plugin.getRootCommand();
        if (root != null) {
            LLibAPI.get().commands().unregister(root);
            plugin.setRootCommand(null);
        }
    }

    private static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        CommandsModule cmdModule = plugin.getConfigManager().getCommandsModule();

        Command<CommandSender> root = cs.command("lastmines")
                .alias("mines")
                .requires(sender -> {
                    if (plugin.getConfigManager().getMainConfig().getModules().isCommandsEnabled()) return true;
                    sender.sendMessage(ColorUtils.colorString("&cВсе команды плагина отключены в конфигурации."));
                    return false;
                })
                .requires(cs.permission("lastmines.admin"))
                .executor(sender -> Help.execute(plugin, sender));

        add(root, cmdModule, Help.build(plugin, cs));
        add(root, cmdModule, Create.build(plugin, cs), "c", "crt");
        add(root, cmdModule, Delete.build(plugin, cs), "del", "remove");
        add(root, cmdModule, ru.last.mines.commands.sub.List.build(plugin, cs));
        add(root, cmdModule, Gui.build(plugin, cs), "menu");
        add(root, cmdModule, Update.build(plugin, cs));
        add(root, cmdModule, Reset.build(plugin, cs), "r");
        add(root, cmdModule, ResetTimer.build(plugin, cs), "rt");
        add(root, cmdModule, Tp.build(plugin, cs), "tp");
        add(root, cmdModule, Migrate.build(plugin, cs), "mgrt");
        add(root, cmdModule, Reload.build(plugin, cs), "rl");
        add(root, cmdModule, DefaultMines.build(plugin, cs), "dm", "dmine", "dfmine", "dfm");
        add(root, cmdModule, Language.build(plugin, cs), "lang");
        add(root, cmdModule, Near.build(plugin, cs), "n", "radius");

        return root;
    }

    private static void add(Command<CommandSender> root, CommandsModule cmdModule, Command<CommandSender> sub, String... defaultAliases) {
        CommandsModule.CommandConfig cfg = cmdModule.getCommand(sub.name());
        if (!cfg.isEnable()) return;
        for (String alias : defaultAliases) sub.alias(alias);
        for (String alias : cfg.getAliases()) sub.alias(alias);
        root.sub(sub);
    }
}
