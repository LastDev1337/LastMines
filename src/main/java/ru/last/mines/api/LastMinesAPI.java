package ru.last.mines.api;

import org.bukkit.entity.Player;
import ru.last.mines.models.Mine;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.last.mines.models.Mine;

import java.util.Collection;

public interface LastMinesAPI {

    /**
     * Получить шахту по ID.
     */
    Mine getMine(String id);

    /**
     * Получить все загруженные шахты.
     */
    Collection<Mine> getMines();

    /**
     * Открыть меню (BMenu) для настройки шахты.
     */
    void openGui(Player player, String id);

    /**
     * Проверить, находится ли локация в указанном радиусе от шахты.
     */
    boolean isWithinRadius(Mine mine, Location location, double radius);

    /**
     * Получить все шахты, в радиусе которых находится игрок.
     */
    Collection<Mine> getMinesInRadius(Location location, double radius);

    /**
     * Принудительно обновить (заполнить блоками) шахту.
     */
    void resetMine(String id);

    /**
     * Проверить, находится ли локация внутри какой-либо шахты.
     */
    Mine getMineAt(Location location);

    /**
     * Получить шахту, в которой сейчас находится игрок.
     */
    Mine getMineAt(Player player);
}
