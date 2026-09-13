package ru.last.mines.commands.sub;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.mines.api.events.*;
import ru.last.mines.commands.*;
import ru.last.mines.config.models.*;
import ru.last.mines.hooks.*;
import ru.last.mines.managers.*;
import ru.last.mines.models.*;
import ru.last.mines.LastMines;
import ru.last.mines.utils.ColorUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@SubCommand(name = "create", aliases = {"c", "crt"})
public class Create extends AbstractSubCommand {

    public Create(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getConfigManager().getMessages().getOnlyPlayers().send(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorString("<red>Использование: /lastmines create <id>"));
            return;
        }

        Location[] sel = WEHook.getSelection(player);
        if (sel == null) {
            plugin.getConfigManager().getMessages().getSelectRegion().send(sender);
            return;
        }

        String id = args[1];
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
            YamlMap config = DefaultMine.generate(id, sel[0], sel[1]);
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
