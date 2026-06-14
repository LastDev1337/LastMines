package ru.last.mines.commands.sub;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;
import ru.last.mines.commands.*;
import ru.last.mines.models.*;

import java.util.List;
import java.util.stream.Collectors;

@SubCommand(name = "update")
public class Update extends AbstractSubCommand {

    public Update(LastMines plugin) { super(plugin); }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "Использование: /lastmines update <id> [rarity]");
            return;
        }

        String id = args[1];
        Mine mine = plugin.getMineManager().getMine(id);
        if (mine == null) {
            plugin.getConfigManager().getMessages().getMineNotFound().send(sender);
            return;
        }

        if (args.length >= 3) {
            String rarityId = args[2];
            boolean isNextOnly = args.length >= 4 && args[3].equalsIgnoreCase("next");

            if (!mine.setNextRarity(rarityId)) {
                plugin.getConfigManager().getMessages().getUpdateRarityNotFound().send(sender, "%rarity%", rarityId);
                return;
            }
            
            plugin.getConfigManager().getMessages().getUpdateNextRarity().send(sender, "%mine%", id, "%rarity%", rarityId);
            
            if (!isNextOnly) {
                mine.forceUpdate();
                plugin.getConfigManager().getMessages().getUpdateSuccess().send(sender, "%mine%", id);
            }
        } else {
            mine.forceUpdate();
            plugin.getConfigManager().getMessages().getUpdateSuccess().send(sender, "%mine%", id);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return plugin.getMineManager().getMines().keySet().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            Mine mine = plugin.getMineManager().getMine(args[1]);
            if (mine != null && mine.getRarities() != null) {
                return mine.getRarities().stream()
                        .map(MineRarity::id)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 4) {
            if ("next".startsWith(args[3].toLowerCase())) {
                return List.of("next");
            }
        }
        return super.tabComplete(sender, args);
    }
}
