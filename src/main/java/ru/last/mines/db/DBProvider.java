package ru.last.mines.db;

import org.bukkit.Material;
import ru.last.mines.stats.Delta;
import ru.last.mines.stats.PlayerStats;
import ru.last.mines.stats.TopEntry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Единый интерфейс хранилища статистики. Все методы блокирующие - вызывать только из async-потока. */
public interface DBProvider {

    /** Создать таблицы и применить миграции схемы. */
    void init() throws Exception;

    /** Прибавить накопленные приращения одним пакетом (всё или ничего). */
    void flush(Map<UUID, Delta> batch) throws Exception;

    /** Статистика игрока; если записи нет - пустая. */
    PlayerStats load(UUID uuid, String fallbackName) throws Exception;

    /** Топ по всем блокам (material == null) или по одному материалу. */
    List<TopEntry> top(Material material, int limit) throws Exception;

    /** UUID по нику (без учёта регистра) или null. */
    UUID findUuid(String name) throws Exception;

    void close();
}
