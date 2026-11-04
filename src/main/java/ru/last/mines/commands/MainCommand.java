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

        Command<CommandSender> root = plugin.getRootCommand();
        if (root == null) {
            root = buildRoot(plugin, cs);
            cs.register(plugin, root);
            plugin.setRootCommand(root);
        } else {
            root.getSubcommands().clear();
        }

        applySubcommands(plugin, cs, root);
    }

    public static void unregister(LastMines plugin) {
        Command<CommandSender> root = plugin.getRootCommand();
        if (root != null) {
            LLibAPI.get().commands().unregister(root);
            plugin.setRootCommand(null);
        }
    }

    private static final java.util.Set<String> PUBLIC_SUBS = java.util.Set.of("stats", "top");

    private static Command<CommandSender> buildRoot(LastMines plugin, CommandService cs) {
        return cs.command("lastmines")
                .aliases("mines", "lm", "lmine", "lmines")
                .requires(sender -> {
                    if (plugin.getConfigManager().getMainConfig().getModules().isCommandsEnabled()) return true;
                    sender.sendMessage(ColorUtils.colorString("&cВсе команды плагина отключены в конфигурации."));
                    return false;
                })
                .requires(sender -> sender.hasPermission("lastmines.admin") || sender.hasPermission("lastmines.stats") || sender.hasPermission("lastmines.top"))
                .executor(sender -> Help.execute(plugin, sender));
    }

    private static void applySubcommands(LastMines plugin, CommandService cs, Command<CommandSender> root) {
        CommandsModule cmdModule = plugin.getConfigManager().getCommandsModule();

        add(root, cmdModule, cs, Help.build(plugin, cs));
        add(root, cmdModule, cs, Create.build(plugin, cs), "c", "crt");
        add(root, cmdModule, cs, Delete.build(plugin, cs), "del", "remove");
        add(root, cmdModule, cs, ru.last.mines.commands.sub.List.build(plugin, cs));
        add(root, cmdModule, cs, Gui.build(plugin, cs), "menu");
        add(root, cmdModule, cs, Update.build(plugin, cs));
        add(root, cmdModule, cs, Reset.build(plugin, cs), "r");
        add(root, cmdModule, cs, ResetTimer.build(plugin, cs), "rt");
        add(root, cmdModule, cs, Tp.build(plugin, cs), "tp");
        add(root, cmdModule, cs, Migrate.build(plugin, cs), "mgrt");
        add(root, cmdModule, cs, Reload.build(plugin, cs), "rl");
        add(root, cmdModule, cs, DefaultMines.build(plugin, cs), "dm", "dmine", "dfmine", "dfm");
        add(root, cmdModule, cs, Language.build(plugin, cs), "lang");
        add(root, cmdModule, cs, Near.build(plugin, cs), "n", "radius");
        add(root, cmdModule, cs, Copy.build(plugin, cs), "cp", "clone");
        add(root, cmdModule, cs, Stats.build(plugin, cs), "st");
        add(root, cmdModule, cs, Top.build(plugin, cs));
    }

    private static void add(Command<CommandSender> root, CommandsModule cmdModule, CommandService cs, Command<CommandSender> sub, String... defaultAliases) {
        CommandsModule.CommandConfig cfg = cmdModule.getCommand(sub.name());
        if (!cfg.isEnable()) return;
        if (!PUBLIC_SUBS.contains(sub.name()) && !sub.name().equals("copy")) sub.requires(cs.permission("lastmines.admin"));
        for (String alias : defaultAliases) sub.alias(alias);
        for (String alias : cfg.getAliases()) sub.alias(alias);
        root.sub(sub);
    }
}
