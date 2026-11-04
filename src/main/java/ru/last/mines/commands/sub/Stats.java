package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.stats.PlayerStats;
import ru.last.mines.stats.StatsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Stats {
    private Stats() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("stats")
                .requires(cs.permission("lastmines.stats"))
                .argument(new SuggestingArgument("player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()))
                .argument(new SuggestingArgument("material", () -> Arrays.stream(Material.values()).filter(Material::isBlock)
                        .map(m -> m.name().toLowerCase(Locale.ROOT)).toList()))
                .executor((sender, args) -> execute(plugin, sender, (String) args.get("player"), (String) args.get("material")));
    }

    private static void execute(LastMines plugin, CommandSender sender, String playerArg, String materialArg) {
        var messages = plugin.getConfigManager().getMessages();
        StatsService stats = plugin.getStatsService();
        if (stats == null || !stats.enabled()) {
            messages.custom("stats-disabled", "&cСтатистика недоступна.").send(sender);
            return;
        }
        String target = playerArg;
        if (target == null) {
            if (!(sender instanceof Player self)) {
                messages.custom("stats-console-usage", "&cУкажите игрока.").send(sender);
                return;
            }
            target = self.getName();
        } else if (!target.equalsIgnoreCase(sender.getName()) && !sender.hasPermission("lastmines.stats.others")) {
            messages.custom("stats-no-permission-others", "&cНет права.").send(sender);
            return;
        }
        Material material = null;
        if (materialArg != null) {
            material = Material.matchMaterial(materialArg.toUpperCase(Locale.ROOT));
            if (material == null) {
                messages.custom("stats-unknown-material", "&cНеизвестный материал: &e%material%").send(sender, "%material%", materialArg);
                return;
            }
        }
        final String name = target;
        final Material mat = material;
        stats.async(() -> {
            PlayerStats found = stats.lookup(name);
            stats.reply(sender, () -> show(plugin, sender, name, found, mat));
        });
    }

    private static void show(LastMines plugin, CommandSender sender, String name, PlayerStats found, Material material) {
        var messages = plugin.getConfigManager().getMessages();
        if (found == null) {
            messages.custom("stats-player-not-found", "&cИгрок %player% не найден.").send(sender, "%player%", name);
            return;
        }
        String shown = found.name();
        if (material != null) {
            messages.custom("stats-material", "&6%player%: %material% — %value%")
                    .send(sender, "%player%", shown, "%material%", material.name(), "%value%", String.valueOf(found.amount(material)));
            return;
        }
        messages.custom("stats-total", "&6%player%: %value%").send(sender, "%player%", shown, "%value%", String.valueOf(found.total()));
        List<Map.Entry<Material, Long>> top = new ArrayList<>(found.blocksCopy().entrySet());
        top.sort(Map.Entry.<Material, Long>comparingByValue().reversed());
        if (top.isEmpty()) return;
        messages.custom("stats-top-materials", "&7Топ материалов:").send(sender);
        for (Map.Entry<Material, Long> e : top.subList(0, Math.min(5, top.size()))) {
            messages.custom("stats-top-material-line", "&8 • &f%material% — %value%")
                    .send(sender, "%material%", e.getKey().name(), "%value%", String.valueOf(e.getValue()));
        }
    }
}
