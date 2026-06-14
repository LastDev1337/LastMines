package ru.last.mines.holograms.providers;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.Color;
import ru.last.mines.holograms.*;
import ru.last.mines.models.*;
import ru.last.mines.utils.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanillaHoloProvider implements HologramProvider {
    private final Map<String, TextDisplay> displays = new HashMap<>();

    public VanillaHoloProvider() { }

    @SuppressWarnings("deprecation")
    @Override
    public void create(Mine mine) {
        if (displays.containsKey(mine.getId())) return;
        
        Location loc = mine.getHoloOffset();
        if (loc == null || loc.getWorld() == null) return;
        
        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        display.setBillboard(TextDisplay.Billboard.CENTER);
        display.setText(buildText(mine));
        display.setDefaultBackground(false);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        
        dev.by1337.yaml.YamlMap holoMap = mine.getHoloMap();
        if (holoMap != null) {
            if (holoMap.has("view_range")) {
                display.setViewRange((float) (double) holoMap.get("view_range").asDouble(1.0));
            }
            if (holoMap.has("shadowed")) {
                display.setShadowed(holoMap.get("shadowed").asBool(false));
            }
            if (holoMap.has("shadow_radius")) {
                display.setShadowRadius((float) (double) holoMap.get("shadow_radius").asDouble(0.0));
            }
            if (holoMap.has("shadow_strength")) {
                display.setShadowStrength((float) (double) holoMap.get("shadow_strength").asDouble(1.0));
            }
            if (holoMap.has("see_through")) {
                display.setSeeThrough(holoMap.get("see_through").asBool(false));
            }
            if (holoMap.has("alignment")) {
                String align = holoMap.get("alignment").asString("center").toUpperCase();
                try {
                    display.setAlignment(TextDisplay.TextAlignment.valueOf(align));
                } catch (IllegalArgumentException e) {
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                }
            }
            if (holoMap.has("size_x") || holoMap.has("size_y") || holoMap.has("size_z")) {
                org.bukkit.util.Transformation transformation = display.getTransformation();
                transformation.getScale().set(
                        (float) (double) holoMap.get("size_x").asDouble(1.0),
                        (float) (double) holoMap.get("size_y").asDouble(1.0),
                        (float) (double) holoMap.get("size_z").asDouble(1.0)
                );
                display.setTransformation(transformation);
            }
        }
        
        displays.put(mine.getId(), display);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void update(Mine mine) {
        TextDisplay display = displays.get(mine.getId());
        if (display != null && display.isValid()) {
            display.setText(buildText(mine));
        } else {
            displays.remove(mine.getId());
            create(mine);
        }
    }

    @Override
    public void delete(Mine mine) {
        TextDisplay display = displays.remove(mine.getId());
        if (display != null) {
            display.remove();
        }
    }

    public void removeAll() {
        for (TextDisplay display : displays.values()) {
            if (display != null) {
                display.remove();
            }
        }
        displays.clear();
    }

    private String buildText(Mine mine) {
        List<String> lines = mine.getHoloTexts();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.contains("%time_reset:detail%")) {
                s = s.replace("%time_reset:detail%", TimeFormatter.format(mine.getTimeLeft() * 1000L, "detail", "placeholders.local"));
            } else if (s.contains("%time_reset:clock%")) {
                s = s.replace("%time_reset:clock%", TimeFormatter.format(mine.getTimeLeft() * 1000L, "clock", "placeholders.local"));
            } else if (s.contains("%time_reset:default%")) {
                s = s.replace("%time_reset:default%", TimeFormatter.format(mine.getTimeLeft() * 1000L, "default", "placeholders.local"));
            }
            s = ColorUtils.colorString(s);
            sb.append(s);
            if (i < lines.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
