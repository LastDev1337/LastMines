package ru.last.mines.models;

import org.bukkit.Material;

public record DropItem(
        Material material,
        double chance,
        boolean fortune
) { }
