package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.MainCommand;
import ru.last.mines.commands.SuggestingArgument;

import java.util.List;

public final class Language {
    private Language() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("language")
                .argument(new SuggestingArgument("lang", () -> plugin.getConfigManager().getAvailableLanguages()))
                .executor((sender, args) -> execute(plugin, sender, (String) args.get("lang")));
    }

    private static void execute(LastMines plugin, CommandSender sender, String lang) {
        List<String> available = plugin.getConfigManager().getAvailableLanguages();
        String languages = String.join(", ", available);

        if (lang == null) {
            plugin.getConfigManager().getMessages().getLanguageUsage().send(sender,
                    "%languages%", languages, "%current%", plugin.getConfigManager().getMainConfig().getLanguage());
            return;
        }

        lang = lang.toLowerCase();
        if (!available.contains(lang)) {
            plugin.getConfigManager().getMessages().getLanguageInvalid().send(sender, "%lang%", lang, "%languages%", languages);
            return;
        }

        plugin.getConfigManager().setLanguage(lang);

        plugin.getConfigManager().loadAll();
        plugin.getMineManager().unloadAll();
        plugin.getMineManager().loadAll();
        if (plugin.getGuiManager() != null) plugin.getGuiManager().reload();
        MainCommand.register(plugin);

        plugin.getConfigManager().getMessages().getLanguageSet().send(sender, "%lang%", lang);
    }
}
