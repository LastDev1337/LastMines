package ru.last.mines.utils;

import dev.by1337.core.util.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {

    public static Component color(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MiniMessage.deserialize(text.replace("\\n", "\n"));
    }

    public static String colorString(String text) {
        if (text == null || text.isEmpty()) return text;
        Component component = color(text);
        return LegacyComponentSerializer.builder()
                .character('§')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build()
                .serialize(component);
    }
}
