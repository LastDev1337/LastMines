package ru.last.mines.utils;

import net.kyori.adventure.text.Component;
import dev.laststudio.lib.api.LLibAPI;

public class ColorUtils {

    public static Component color(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return LLibAPI.get().messages().render(text.replace("\\n", "\n"));
    }

    public static String colorString(String text) {
        if (text == null || text.isEmpty()) return text;
        return LLibAPI.get().messages().renderLegacy(text.replace("\\n", "\n"));
    }
}
