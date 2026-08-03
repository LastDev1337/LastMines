package ru.last.mines.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

public class Sounds {
    public static Sound parseSound(String soundName) {
        NamespacedKey key = NamespacedKey.fromString(soundName.toLowerCase());
        Sound sound = key != null ? Registry.SOUNDS.get(key) : null;
        if (sound != null) return sound;
        try {
            return Sound.valueOf(soundName.toUpperCase().replace('.', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
