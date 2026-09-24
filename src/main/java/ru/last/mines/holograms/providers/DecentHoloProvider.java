package ru.last.mines.holograms.providers;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import ru.last.mines.holograms.*;
import ru.last.mines.models.*;
import ru.last.mines.utils.*;
import ru.last.mines.utils.time.*;

import java.util.ArrayList;
import java.util.List;

public class DecentHoloProvider implements HologramProvider {

    @Override
    public void create(Mine mine) {
        String holoName = "lastmines_" + mine.getId();
        Hologram holo = DHAPI.getHologram(holoName);
        if (holo != null) {
            holo.delete();
        }
        Location loc = mine.getHoloOffset();
        if (loc == null) return;
        holo = DHAPI.createHologram(holoName, loc, applyPlaceholders(mine, mine.getHoloTexts()));
        holo.setSaveToFile(false);
    }

    @Override
    public void update(Mine mine) {
        String holoName = "lastmines_" + mine.getId();
        Hologram holo = DHAPI.getHologram(holoName);
        if (holo != null) {
            DHAPI.setHologramLines(holo, applyPlaceholders(mine, mine.getHoloTexts()));
        }
    }

    @Override
    public void delete(Mine mine) {
        String holoName = "lastmines_" + mine.getId();
        Hologram holo = DHAPI.getHologram(holoName);
        if (holo != null) {
            holo.delete();
        }
    }

    @Override
    public void removeAll() {
        /*
         DecentHolograms автоматически выгружает все голограммы на сервере
        */
    }

    private List<String> applyPlaceholders(Mine mine, List<String> lines) {
        List<String> res = new ArrayList<>();
        for (String s : lines) {
            if (s.contains("%time_reset:detail%")) {
                s = s.replace("%time_reset:detail%", TimeFormatter.format(mine.getTimeLeft() * 1000L, "detail"));
            } else if (s.contains("%time_reset:clock%")) {
                s = s.replace("%time_reset:clock%", TimeFormatter.format(mine.getTimeLeft() * 1000L, "clock"));
            } else if (s.contains("%time_reset:default%")) { s = s.replace("%time_reset:default%", TimeFormatter.format(mine.getTimeLeft() * 1000L, "default")); }
            res.add(ColorUtils.colorString(s));
        }
        return res;
    }
}
