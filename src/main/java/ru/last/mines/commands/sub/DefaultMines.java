package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;
import ru.last.mines.utils.ColorUtils;

@SubCommand(name = "defaultmine", aliases = {"dm", "dmine", "dfmine", "dfm"})
public class DefaultMines extends AbstractSubCommand {

    public DefaultMines(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getMineManager().extractDefaultMines();
        sender.sendMessage(ColorUtils.colorString(plugin.getConfigManager().getMessages().getPrefix()
                + "<green>Стандартные шахты (blocks, rarity) восстановлены."));

        AbstractSubCommand reload = plugin.getMainCommand().getSubCommand("reload");
        if (reload != null) {
            reload.execute(sender, args);
        }
    }
}
