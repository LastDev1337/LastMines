package ru.last.mines.mine.models;

import org.bukkit.Material;

public record DropItem(
        Material material,
        double chance,
        boolean fortune
) { }
