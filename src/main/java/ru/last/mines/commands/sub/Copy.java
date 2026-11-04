package ru.last.mines.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.yaml.YamlMap;
import dev.laststudio.lib.api.command.CommandService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.api.events.MineCreateEvent;
import ru.last.mines.commands.SuggestingArgument;
import ru.last.mines.mine.Mine;
import ru.last.mines.mine.MineManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class Copy {
    private Copy() {}

    public static Command<CommandSender> build(LastMines plugin, CommandService cs) {
        return cs.command("copy")
                .requires(cs.permission("lastmines.copy"))
                .argument(new SuggestingArgument("id", () -> plugin.getMineManager().getMines().keySet()))
                .argument(new ArgumentString<CommandSender>("new"))
                .executor((sender, args) -> execute(plugin, sender, (String) args.get("id"), (String) args.get("new")));
    }

    private static void execute(LastMines plugin, CommandSender sender, String sourceId, String newId) {
        var messages = plugin.getConfigManager().getMessages();
        if (sourceId == null || newId == null) {
            messages.custom("copy-usage", "&cИспользование: /lastmines copy <шахта> <новая_шахта>").send(sender);
            return;
        }
        Mine source = plugin.getMineManager().getMine(sourceId);
        if (source == null) {
            messages.getMineNotFound().send(sender);
            return;
        }
        if (!MineManager.isValidId(newId)) {
            messages.custom("copy-invalid-id", "&cНедопустимый id: разрешены только буквы, цифры, '_' и '-'.").send(sender);
            return;
        }
        File file = new File(plugin.getDataFolder() + File.separator + "mines", newId + ".yml");
        if (plugin.getMineManager().getMine(newId) != null || file.exists()) {
            messages.getMineExists().send(sender, "%mine%", newId);
            return;
        }

        try {
            source.save();
            File sourceFile = new File(plugin.getDataFolder() + File.separator + "mines", sourceId + ".yml");
            YamlMap map = YamlMap.load(sourceFile);

            map.set("positions", null);
            map.set("hologram", null);
            map.set("teleport.pos", null);
            map.set("teleport.enable", false);
            map.set("stopped", true);
            map.set("name", newId);

            Files.writeString(file.toPath(), map.saveToString(), StandardCharsets.UTF_8);
            Mine copy = new Mine(plugin, newId, map);
            plugin.getMineManager().getMines().put(newId, copy);
            Bukkit.getPluginManager().callEvent(new MineCreateEvent(copy));
            messages.custom("copy-success", "&aШахта &e%source% &aскопирована в &e%mine%&a. Копия остановлена и без области: задайте область и снимите остановку.")
                    .send(sender, "%source%", sourceId, "%mine%", newId);
        } catch (Exception e) {
            plugin.getDebugger().error("Не удалось скопировать шахту " + sourceId + " -> " + newId, e);
            messages.custom("copy-failed", "&cНе удалось скопировать шахту, подробности в консоли.").send(sender);
            file.delete();
        }
    }
}
