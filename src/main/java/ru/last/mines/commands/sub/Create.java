package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.yaml.YamlMap;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import ru.last.mines.api.events.MineCreateEvent;
import ru.last.mines.config.models.DefaultMine;
import ru.last.mines.hooks.WEHook;
import ru.last.mines.managers.MineManager;
import ru.last.mines.models.Mine;
import ru.last.mines.models.MineShape;
import ru.last.mines.utils.ColorUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class Create {
    private Create() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("create")
                .argument(new ArgumentString<CommandSender>("id"))
                .executor((sender, args) -> execute(plugin, sender, (String) args.get("id")));
    }

    private static void execute(LastMines plugin, CommandSender sender, String id) {
        if (!(sender instanceof Player player)) {
            plugin.getConfigManager().getMessages().getOnlyPlayers().send(sender);
            return;
        }
        if (id == null) {
            sender.sendMessage(ColorUtils.colorString("<red>Использование: /lastmines create <id>"));
            return;
        }

        Object[] captured = WEHook.captureSelection(player);
        if (captured == null) {
            plugin.getConfigManager().getMessages().getSelectRegion().send(sender);
            return;
        }
        MineShape shape = (MineShape) captured[0];
        @SuppressWarnings("unchecked")
        List<String> points = (List<String>) captured[1];

        if (!MineManager.isValidId(id)) {
            sender.sendMessage(ColorUtils.colorString("<red>Недопустимый id автошахты: разрешены только буквы, цифры, '_' и '-'."));
            return;
        }
        if (plugin.getMineManager().getMine(id) != null) {
            plugin.getConfigManager().getMessages().getMineExists().send(sender);
            return;
        }

        try {
            File file = new File(plugin.getDataFolder() + "/mines", id + ".yml");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            YamlMap config = DefaultMine.generate(id, player.getWorld(), shape, points);
            try {
                Files.writeString(file.toPath(), config.saveToString(), StandardCharsets.UTF_8);
                Mine mine = new Mine(plugin, id, config);
                plugin.getMineManager().getMines().put(id, mine);
                mine.createHologram();
            } catch (Exception e) {
                plugin.getDebugger().error("Не удалось создать автошахту " + id, e);
                player.sendMessage(ColorUtils.colorString("<red>Произошла ошибка при создании автошахты. Проверьте консоль."));
                return;
            }

            MineCreateEvent apiEvent = new MineCreateEvent(plugin.getMineManager().getMine(id));
            Bukkit.getPluginManager().callEvent(apiEvent);

            plugin.getConfigManager().getMessages().getCreateSuccess().send(sender, "%mine%", id);

            if (plugin.getGuiManager() != null) {
                plugin.getGuiManager().openMenu(player, id);
            } else {
                plugin.getConfigManager().getMessages().getGuiNotInstalled().send(sender);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sender.sendMessage(ColorUtils.colorString("<red>Произошла ошибка при создании шахты."));
        }
    }
}
