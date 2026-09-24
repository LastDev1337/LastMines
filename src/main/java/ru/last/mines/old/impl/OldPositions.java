package ru.last.mines.old.impl;

import dev.by1337.yaml.YamlMap;

import java.util.List;

public final class OldPositions {
    private OldPositions() {}

    public static boolean migrate(YamlMap mineMap) {
        if (mineMap.has("positions.points")) return false;
        if (!mineMap.has("positions.one") && !mineMap.has("positions.two")) return false;

        String one = mineMap.get("positions.one").asString("0;100;0");
        String two = mineMap.get("positions.two").asString("5;105;5");

        mineMap.set("positions.points", List.of(one, two));
        mineMap.set("positions.one", null);
        mineMap.set("positions.two", null);
        return true;
    }
}
