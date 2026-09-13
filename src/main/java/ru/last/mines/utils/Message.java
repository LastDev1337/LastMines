package ru.last.mines.utils;

import org.bukkit.command.CommandSender;
import dev.laststudio.lib.api.LLibAPI;

public class Message {
    private final String text;

    public Message(String text) { this.text = text; }

    public void send(CommandSender sender, String... replacements) {
        if (text == null || text.isEmpty()) return;
        String result = text;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                result = result.replace(replacements[i], LLibAPI.get().messages().escape(replacements[i + 1]));
            }
        }
        sender.sendMessage(ColorUtils.colorString(result));
    }
}
