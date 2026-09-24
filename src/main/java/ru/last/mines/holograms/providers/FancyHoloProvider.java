package ru.last.mines.holograms.providers;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import dev.by1337.yaml.YamlMap;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector3f;
import ru.last.mines.holograms.*;
import ru.last.mines.models.*;
import ru.last.mines.utils.*;
import ru.last.mines.utils.time.*;

import java.util.ArrayList;
import java.util.List;

public class FancyHoloProvider implements HologramProvider {

    @Override
    public void create(Mine mine) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        String holoName = "lastmines_" + mine.getId();

        manager.getHologram(holoName).ifPresent(hologram -> {
            hologram.deleteHologram();
            manager.removeHologram(hologram);
        });

        Location loc = mine.getHoloOffset();
        if (loc == null || loc.getWorld() == null) return;

        TextHologramData data = new TextHologramData(holoName, loc);
        data.setText(applyPlaceholders(mine, mine.getHoloTexts()));

        YamlMap holoMap = mine.getHoloMap();
        if (holoMap != null) {
            if (holoMap.has("visibility_distance")) {
                data.setVisibilityDistance(holoMap.get("visibility_distance").asInt(-1));
            }
            if (holoMap.has("persistent")) {
                data.setPersistent(holoMap.get("persistent").asBool(false));
            } else {
                data.setPersistent(false);
            }
            if (holoMap.has("scale_x") || holoMap.has("scale_y") || holoMap.has("scale_z")) {
                Vector3f scale = new Vector3f(
                        (float) (double) holoMap.get("scale_x").asDouble(1.0),
                        (float) (double) holoMap.get("scale_y").asDouble(1.0),
                        (float) (double) holoMap.get("scale_z").asDouble(1.0)
                );
                data.setScale(scale);
            }
            if (holoMap.has("shadow_radius")) {
                data.setShadowRadius((float) (double) holoMap.get("shadow_radius").asDouble(0.0));
            }
            if (holoMap.has("shadow_strength")) {
                data.setShadowStrength((float) (double) holoMap.get("shadow_strength").asDouble(1.0));
            }
            if (holoMap.has("text_shadow")) {
                data.setTextShadow(holoMap.get("text_shadow").asBool(false));
            }
            if (holoMap.has("see_through")) {
                data.setSeeThrough(holoMap.get("see_through").asBool(false));
            }
            if (holoMap.has("text_alignment")) {
                String align = holoMap.get("text_alignment").asString("center").toUpperCase();
                TextDisplay.TextAlignment textAlignment;
                try {
                    textAlignment = TextDisplay.TextAlignment.valueOf(align);
                } catch (IllegalArgumentException e) { textAlignment = TextDisplay.TextAlignment.CENTER; }
                data.setTextAlignment(textAlignment);
            }
            if (holoMap.has("update_interval")) {
                data.setTextUpdateInterval(TimeUtils.parseToTicks(holoMap.get("update_interval").asString("-1")));
            }
        }

        Hologram hologram = manager.create(data);
        manager.addHologram(hologram);
    }

    @Override
    public void update(Mine mine) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        String holoName = "lastmines_" + mine.getId();

        manager.getHologram(holoName).ifPresent(hologram -> {
            if (hologram.getData() instanceof TextHologramData data) {
                data.setText(applyPlaceholders(mine, mine.getHoloTexts()));
                hologram.queueUpdate();
            }
        });
    }

    @Override
    public void delete(Mine mine) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        String holoName = "lastmines_" + mine.getId();

        manager.getHologram(holoName).ifPresent(hologram -> {
            hologram.deleteHologram();
            manager.removeHologram(hologram);
        });
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
