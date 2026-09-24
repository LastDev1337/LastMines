package ru.last.mines.old;

import dev.by1337.yaml.YamlMap;
import ru.last.mines.old.impl.OldPositions;

public final class OldManager {
    private OldManager(){}

    public static boolean migrate(YamlMap mineMap) {
        return OldPositions.migrate(mineMap);
    }
}
