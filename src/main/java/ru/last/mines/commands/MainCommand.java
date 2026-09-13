package ru.last.mines.commands;

import com.google.common.reflect.ClassPath;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import ru.last.mines.LastMines;
import ru.last.mines.config.models.*;
import ru.last.mines.utils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.reflect.ClassPath.ClassInfo;

import java.util.*;

public class MainCommand implements CommandExecutor, TabCompleter {
    private final LastMines plugin;
    private final Map<String, AbstractSubCommand> subCommands = new HashMap<>();

    public MainCommand(LastMines plugin) {
        this.plugin = plugin;
        loadSubCommands();
    }

    public AbstractSubCommand getSubCommand(String name) {
        return subCommands.get(name.toLowerCase());
    }

    public void loadSubCommands() {
        subCommands.clear();
        try {
            boolean commandsEnabled = plugin.getConfigManager().getMainConfig().getModules().isCommandsEnabled();
            if (!commandsEnabled) return;
            
            CommandsModule cmdModule = plugin.getConfigManager().getCommandsModule();
            
            ClassPath cp = ClassPath.from(plugin.getClass().getClassLoader());
            for (ClassInfo info : cp.getTopLevelClasses("ru.last.mines.commands.sub")) {
                Class<?> clazz = info.load();
                if (AbstractSubCommand.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(SubCommand.class)) {
                    SubCommand meta = clazz.getAnnotation(SubCommand.class);
                    String baseName = meta.name().toLowerCase();
                    
                    CommandsModule.CommandConfig config = cmdModule.getCommand(baseName);
                    if (!config.isEnable()) continue;
                    
                    AbstractSubCommand cmd = (AbstractSubCommand) clazz.getConstructor(LastMines.class).newInstance(plugin);
                    subCommands.put(baseName, cmd);
                    
                    for (String alias : meta.aliases()) {
                        subCommands.put(alias.toLowerCase(), cmd);
                    }
                    
                    for (String customAlias : config.getAliases()) {
                        subCommands.put(customAlias.toLowerCase(), cmd);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!plugin.getConfigManager().getMainConfig().getModules().isCommandsEnabled()) {
            sender.sendMessage(ColorUtils.colorString("&cВсе команды плагина отключены в конфигурации."));
            return true;
        }
        
        if (!sender.hasPermission("lastmines.admin")) {
            plugin.getConfigManager().getMessages().getNoPermission().send(sender);
            return true;
        }

        if (args.length == 0) {
            AbstractSubCommand help = subCommands.get("help");
            if (help != null) help.execute(sender, args);
            return true;
        }

        String subName = args[0].toLowerCase();
        if (subCommands.containsKey(subName)) {
            subCommands.get(subName).execute(sender, args);
            return true;
        }

        AbstractSubCommand help = subCommands.get("help");
        if (help != null) help.execute(sender, args);
        else sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "&cUnknown subcommand!"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!plugin.getConfigManager().getMainConfig().getModules().isCommandsEnabled()) return Collections.emptyList();
        if (!sender.hasPermission("lastmines.admin")) return Collections.emptyList();
        
        CommandsModule cmdModule = plugin.getConfigManager().getCommandsModule();
        
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (AbstractSubCommand cmd : new HashSet<>(subCommands.values())) {
                SubCommand info = cmd.getClass().getAnnotation(SubCommand.class);
                if (info != null && info.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                    CommandsModule.CommandConfig config = cmdModule.getCommand(info.name());
                    if (config.isTabCompleter()) {
                        list.add(info.name());
                    }
                }
            }
            return list;
        }
        
        String subName = args[0].toLowerCase();
        if (subCommands.containsKey(subName)) {
            AbstractSubCommand cmd = subCommands.get(subName);
            SubCommand info = cmd.getClass().getAnnotation(SubCommand.class);
            if (info != null) {
                CommandsModule.CommandConfig config = cmdModule.getCommand(info.name());
                if (!config.isTabCompleter()) return Collections.emptyList();
            }
            return cmd.tabComplete(sender, args);
        }
        
        return Collections.emptyList();
    }
}
