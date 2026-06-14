package ru.last.mines.models;

import java.util.List;
import org.bukkit.Material;

public record MineBlock(
        Material material,
        double chance,
        int min,
        int max,
        List<String> drops
) { }