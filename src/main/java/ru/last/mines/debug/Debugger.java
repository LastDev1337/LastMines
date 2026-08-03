package ru.last.mines.debug;

import org.bukkit.Bukkit;
import ru.last.mines.LastMines;
import ru.last.mines.config.models.*;
import ru.last.mines.utils.*;

public class Debugger {

    public void info(String message) { log(DebugType.INFO, message); }
    public void warn(String message) { log(DebugType.WARN, message); }
    public void error(String message) { log(DebugType.ERROR, message); }
    public void error(String message, Throwable t) { logError(message, t); }

    public void log(DebugType type, String message) {
        Main config = LastMines.get().getConfigManager().getMainConfig();
        if (!config.isDebugEnable()) return;

        boolean enabled = false;
        String prefix = "";
        
        switch (type) {
            case INFO -> {
                enabled = config.getInfo().isEnable();
                prefix = config.getInfo().getPrefix();
            }
            case WARN -> {
                enabled = config.getWarn().isEnable();
                prefix = config.getWarn().getPrefix();
            }
            case ERROR -> {
                enabled = config.getError().isEnable();
                prefix = config.getError().getPrefix();
            }
        }
        
        if (!enabled) return;
        Bukkit.getConsoleSender().sendMessage(ColorUtils.colorString("<white>[LastMines] <reset>" + prefix + message));
    }

    public void logError(String message, Throwable t) {
        Main config = LastMines.get().getConfigManager().getMainConfig();
        if (!config.isDebugEnable() || !config.getError().isEnable()) return;
        
        String prefix = config.getError().getPrefix();
        Bukkit.getConsoleSender().sendMessage(ColorUtils.colorString("<red>============================================================"));
        Bukkit.getConsoleSender().sendMessage(ColorUtils.colorString("<aqua>[LastMines] <reset>" + prefix));
        Bukkit.getConsoleSender().sendMessage(ColorUtils.colorString("<red>КРИТИЧЕСКАЯ ОШИБКА: " + message));
        Bukkit.getConsoleSender().sendMessage(ColorUtils.colorString("<red>============================================================"));
        t.printStackTrace();
    }
}
