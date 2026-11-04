package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.stats.StatsService;
import ru.last.mines.stats.TopEntry;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Top {
    private static final int PAGE_SIZE = 10;

    private Top() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("top")
                .requires(cs.permission("lastmines.top"))
                .argument(new SuggestingArgument("material", () -> Arrays.stream(Material.values()).filter(Material::isBlock)
                        .map(m -> m.name().toLowerCase(Locale.ROOT)).toList()))
                .argument(new SuggestingArgument("page", () -> List.of("1", "2", "3")))
                .executor((sender, args) -> execute(plugin, sender, (String) args.get("material"), (String) args.get("page")));
    }

    private static void execute(LastMines plugin, CommandSender sender, String first, String second) {
        var messages = plugin.getConfigManager().getMessages();
        StatsService stats = plugin.getStatsService();
        if (stats == null || !stats.enabled()) {
            messages.custom("stats-disabled", "&cСтатистика недоступна.").send(sender);
            return;
        }
        String materialArg = first;
        String pageArg = second;
        if (first != null && first.matches("\\d+")) {
            materialArg = null;
            pageArg = first;
        }
        Material material = null;
        if (materialArg != null) {
            material = Material.matchMaterial(materialArg.toUpperCase(Locale.ROOT));
            if (material == null) {
                messages.custom("stats-unknown-material", "&cНеизвестный материал: &e%material%").send(sender, "%material%", materialArg);
                return;
            }
        }
        int page = 1;
        if (pageArg != null) {
            try { page = Math.max(1, Integer.parseInt(pageArg)); } catch (NumberFormatException ignored) { }
        }
        final Material mat = material;
        final int wantedPage = page;
        stats.async(() -> {
            List<TopEntry> top = stats.topBlocking(mat);
            stats.reply(sender, () -> show(plugin, sender, top, mat, wantedPage));
        });
    }

    private static void show(LastMines plugin, CommandSender sender, List<TopEntry> top, Material material, int page) {
        var messages = plugin.getConfigManager().getMessages();
        if (top.isEmpty()) {
            messages.custom("top-empty", "&cВ топе пока никого нет.").send(sender);
            return;
        }
        int pages = (top.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = Math.min(page, pages);
        messages.custom("top-header", "&6Топ (%what%) %page%/%pages%")
                .send(sender, "%what%", material == null ? "all" : material.name(), "%page%", String.valueOf(current), "%pages%", String.valueOf(pages));
        int from = (current - 1) * PAGE_SIZE;
        for (int i = from; i < Math.min(top.size(), from + PAGE_SIZE); i++) {
            TopEntry e = top.get(i);
            messages.custom("top-line", "&e%place%. %player% — %value%")
                    .send(sender, "%place%", String.valueOf(i + 1), "%player%", e.name(), "%value%", String.valueOf(e.value()));
        }
    }
}
