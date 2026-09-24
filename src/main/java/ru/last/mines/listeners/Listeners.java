package ru.last.mines.listeners;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import ru.last.mines.LastMines;
import ru.last.mines.listeners.impl.BlockDropsListener;
import ru.last.mines.listeners.impl.BlockListener;
import ru.last.mines.listeners.impl.DragDropListener;
import ru.last.mines.listeners.impl.MineEventListener;
import ru.last.mines.listeners.impl.input.ChanceInputListener;
import ru.last.mines.listeners.impl.input.LimitInputListener;
import ru.last.mines.listeners.impl.input.PermInputListener;
import ru.last.mines.listeners.impl.input.TextInputListener;

public class Listeners {

    private static BlockDropsListener blockDropsListener;
    private static ChanceInputListener chanceInputListener;
    private static LimitInputListener limitInputListener;
    private static PermInputListener permInputListener;
    private static TextInputListener textInputListener;

    public static void register() {
        LastMines main = LastMines.get();
        PluginManager plm = Bukkit.getServer().getPluginManager();

        blockDropsListener = new BlockDropsListener();
        chanceInputListener = new ChanceInputListener();
        limitInputListener = new LimitInputListener();
        permInputListener = new PermInputListener();
        textInputListener = new TextInputListener();

        plm.registerEvents(blockDropsListener, main);
        plm.registerEvents(new BlockListener(), main);
        plm.registerEvents(new DragDropListener(), main);
        plm.registerEvents(new MineEventListener(main), main);
        plm.registerEvents(chanceInputListener, main);
        plm.registerEvents(limitInputListener, main);
        plm.registerEvents(permInputListener, main);
        plm.registerEvents(textInputListener, main);
    }

    public static BlockDropsListener getBlockDropsListener() { return blockDropsListener; }
    public static ChanceInputListener getChanceInputListener() { return chanceInputListener; }
    public static LimitInputListener getLimitInputListener() { return limitInputListener; }
    public static PermInputListener getPermInputListener() { return permInputListener; }
    public static TextInputListener getTextInputListener() { return textInputListener; }
}
