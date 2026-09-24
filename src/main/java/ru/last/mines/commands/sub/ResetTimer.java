package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

public final class ResetTimer {
    private ResetTimer() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("resettimer")
                .argument(new SuggestingArgument("id", () -> plugin.getMineManager().getMines().keySet()))
                .executor((sender, args) -> {
                    String id = (String) args.get("id");
                    if (id == null) {
                        sender.sendMessage(ColorUtils.colorString("<red>Использование: /lastmines resettimer <id>"));
                        return;
                    }
                    Mine mine = plugin.getMineManager().getMine(id);
                    if (mine != null) {
                        mine.setTimeLeft(mine.getResetTime());
                        plugin.getConfigManager().getMessages().getMineReset().send(sender, "%mine%", id);
                    } else {
                        plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
                    }
                });
    }
}
