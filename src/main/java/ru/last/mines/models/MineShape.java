package ru.last.mines.models;

import java.util.Locale;

public enum MineShape {
    CUBOID, CYLINDER, POLYGON, ELLIPSOID;

    public static MineShape fromString(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "cuboid", "cube", "box" -> CUBOID;
            case "cylinder", "cyl" -> CYLINDER;
            case "polygon", "poly" -> POLYGON;
            case "ellipsoid", "ellipse", "ellip" -> ELLIPSOID;
            default -> valueOf(s.toUpperCase(Locale.ROOT));
        };
    }
}
