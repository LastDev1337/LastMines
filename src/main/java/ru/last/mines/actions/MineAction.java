package ru.last.mines.actions;

import dev.laststudio.lib.api.action.ParsedAction;
import org.bukkit.Material;

import java.util.List;

public record MineAction(Target target, int radius, Material material, List<ParsedAction> parsed) {

    public enum Target { ALL, MINE, RADIUS, SELF }

    public boolean global() {
        for (ParsedAction action : parsed) {
            String name = action.name().toLowerCase(java.util.Locale.ROOT);
            if (name.equals("console") || name.equals("broadcast") || name.endsWith("_all")) return true;
        }
        return false;
    }
}
