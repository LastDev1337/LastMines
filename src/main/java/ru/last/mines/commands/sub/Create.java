package ru.last.mines.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.api.events.*;
import ru.last.mines.commands.*;
import ru.last.mines.config.models.*;
import ru.last.mines.hooks.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;

import java.io.File;

@SubCommand(name = "create", aliases = {"c", "crt"})
public class Create extends AbstractSubCommand {

    public Create(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cКоманда только для игроков!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /lastmines create <id>");
            return;
        }

        Location[] sel = WEHook.getSelection(player);
        if (sel == null) {
            plugin.getConfigManager().getMessages().getSelectRegion().send(sender);
            return;
        }

        String id = args[1];
        if (plugin.getMineManager().getMine(id) != null) {
            plugin.getConfigManager().getMessages().getMineExists().send(sender);
            return;
        }

        try {
            File file = new File(plugin.getDataFolder() + "/mines", id + ".yml");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            dev.by1337.yaml.YamlMap config = DefaultMine.generate(id, sel[0], sel[1]);
            try {
                java.nio.file.Files.writeString(file.toPath(), config.saveToString(), java.nio.charset.StandardCharsets.UTF_8);
                Mine mine = new Mine(plugin, id, config);
                plugin.getMineManager().getMines().put(id, mine);
                mine.createHologram();
            } catch (Exception e) {
                plugin.getDebugLogger().error("Не удалось создать автошахту " + id, e);
                player.sendMessage("§cПроизошла ошибка при создании автошахты. Проверьте консоль.");
                return;
            }
            
            MineCreateEvent apiEvent = new MineCreateEvent(plugin.getMineManager().getMine(id));
            Bukkit.getPluginManager().callEvent(apiEvent);
            
            plugin.getConfigManager().getMessages().getCreateSuccess().send(sender, "%mine%", id);
            
            if (Bukkit.getPluginManager().getPlugin("BMenu") != null) {
                plugin.getGuiManager().openMenu(player, id);
            } else {
                sender.sendMessage("§cBMenu не установлен, GUI недоступно!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sender.sendMessage("§cПроизошла ошибка при создании шахты.");
        }
    }
}
