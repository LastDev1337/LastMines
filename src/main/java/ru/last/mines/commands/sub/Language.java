package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;

import java.util.List;
import java.util.stream.Collectors;

@SubCommand(name = "language", aliases = {"lang"})
public class Language extends AbstractSubCommand {

    public Language(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<String> available = plugin.getConfigManager().getAvailableLanguages();
        String languages = String.join(", ", available);

        if (args.length < 2) {
            plugin.getConfigManager().getMessages().getLanguageUsage().send(sender,
                    "%languages%", languages,
                    "%current%", plugin.getConfigManager().getMainConfig().getLanguage());
            return;
        }

        String lang = args[1].toLowerCase();
        if (!available.contains(lang)) {
            plugin.getConfigManager().getMessages().getLanguageInvalid().send(sender,
                    "%lang%", lang, "%languages%", languages);
            return;
        }

        plugin.getConfigManager().setLanguage(lang);

        plugin.getConfigManager().loadAll();
        plugin.getMineManager().unloadAll();
        plugin.getMineManager().loadAll();
        if (plugin.getGuiManager() != null) plugin.getGuiManager().reload();
        if (plugin.getMainCommand() != null) plugin.getMainCommand().loadSubCommands();

        plugin.getConfigManager().getMessages().getLanguageSet().send(sender, "%lang%", lang);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return plugin.getConfigManager().getAvailableLanguages().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}
