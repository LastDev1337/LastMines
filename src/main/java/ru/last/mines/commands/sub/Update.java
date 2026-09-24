package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.models.Mine;
import ru.last.mines.models.MineRarity;
import ru.last.mines.utils.ColorUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Update {
    private Update() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("update")
                .argument(new SuggestingArgument("id", () -> plugin.getMineManager().getMines().keySet()))
                .argument(new SuggestingArgument("rarity", () -> allRarityIds(plugin)))
                .argument(new SuggestingArgument("next", () -> List.of("next")))
                .executor((sender, args) -> execute(plugin, sender,
                        (String) args.get("id"), (String) args.get("rarity"), (String) args.get("next")));
    }

    // Suggests rarity ids across all mines, not just the one being updated - BCmd doesn't
    // expose an earlier argument's value while suggesting a later one on the same command.
    private static Collection<String> allRarityIds(LastMines plugin) {
        Set<String> ids = new LinkedHashSet<>();
        for (Mine mine : plugin.getMineManager().getMines().values()) {
            for (MineRarity r : mine.getRarities()) ids.add(r.id());
        }
        return ids;
    }

    private static void execute(LastMines plugin, CommandSender sender, String id, String rarityId, String nextFlag) {
        if (id == null) {
            sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines update <id> [rarity]"));
            return;
        }
        Mine mine = plugin.getMineManager().getMine(id);
        if (mine == null) {
            plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
            return;
        }

        if (rarityId != null) {
            boolean isNextOnly = "next".equalsIgnoreCase(nextFlag);

            if (!mine.setNextRarity(rarityId)) {
                plugin.getConfigManager().getMessages().getUpdateRarityNotFound().send(sender, "%rarity%", rarityId);
                return;
            }

            plugin.getConfigManager().getMessages().getUpdateNextRarity().send(sender, "%mine%", id, "%rarity%", rarityId);

            if (!isNextOnly) {
                reportUpdate(plugin, sender, mine, id);
            }
        } else {
            reportUpdate(plugin, sender, mine, id);
        }
    }

    private static void reportUpdate(LastMines plugin, CommandSender sender, Mine mine, String id) {
        if (mine.forceUpdate()) {
            plugin.getConfigManager().getMessages().getUpdateSuccess().send(sender, "%mine%", id);
        } else {
            sender.sendMessage(ColorUtils.colorString("<red>Автошахта " + id + " уже сбрасывается."));
        }
    }
}
