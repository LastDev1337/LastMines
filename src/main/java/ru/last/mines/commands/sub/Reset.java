package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

public final class Reset {
    private Reset() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("reset")
                .argument(new SuggestingArgument("id", () -> plugin.getMineManager().getMines().keySet()))
                .executor((sender, args) -> {
                    String id = (String) args.get("id");
                    if (id == null) {
                        sender.sendMessage(ColorUtils.colorString("<red>Использование: /lastmines reset <id>"));
                        return;
                    }
                    Mine mine = plugin.getMineManager().getMine(id);
                    if (mine != null) {
                        if (mine.isResetting()) {
                            sender.sendMessage(ColorUtils.colorString("<red>Автошахта " + id + " уже сбрасывается."));
                            return;
                        }
                        plugin.getActionManager().resetMine(id);
                        plugin.getConfigManager().getMessages().getMineReset().send(sender, "%mine%", id);
                    } else {
                        plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
                    }
                });
    }
}
