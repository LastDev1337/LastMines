package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

public final class Delete {
    private Delete() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("delete")
                .argument(new SuggestingArgument("id", () -> plugin.getMineManager().getMines().keySet()))
                .executor((sender, args) -> {
                    String id = (String) args.get("id");
                    if (id == null) {
                        sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines delete <id>"));
                        return;
                    }
                    Mine mine = plugin.getMineManager().getMine(id);
                    if (mine == null) {
                        plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
                        return;
                    }
                    plugin.getActionManager().deleteMine(id);
                    sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Шахта " + id + " успешно удалена."));
                });
    }
}
