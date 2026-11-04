package ru.last.mines.holograms;

import ru.last.mines.mine.Mine;

public interface HologramProvider {
    void create(Mine mine);
    void update(Mine mine);
    void delete(Mine mine);
    void removeAll();
}
