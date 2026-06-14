package ru.last.mines.commands;

import org.bukkit.command.CommandSender;
import ru.last.mines.LastMines;

import java.util.Collections;
import java.util.List;

public abstract class AbstractSubCommand {
    protected final LastMines plugin;

    public AbstractSubCommand(LastMines plugin) { this.plugin = plugin; }
    public abstract void execute(CommandSender sender, String[] args);
    public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
