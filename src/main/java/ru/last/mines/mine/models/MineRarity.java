package ru.last.mines.mine.models;

import java.util.List;

public record MineRarity(
        String id,
        double chance,
        String name,
        List<MineBlock> blocks,
        String icon
) { }
