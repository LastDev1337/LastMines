package ru.last.mines.config.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.bukkit.Location;
import dev.by1337.yaml.YamlMap;

public class DefaultMine {

    public static YamlMap generate(String id, Location pos1, Location pos2) {
        YamlMap config = new YamlMap();
        
        config.set("name", id);
        config.set("world", pos1.getWorld().getName());
        config.set("reset_time", "5m");
        config.set("mode", "BLOCKS");
        
        YamlMap positions = new YamlMap();
        positions.set("one", pos1.getBlockX() + ";" + pos1.getBlockY() + ";" + pos1.getBlockZ());
        positions.set("two", pos2.getBlockX() + ";" + pos2.getBlockY() + ";" + pos2.getBlockZ());
        config.set("positions", positions.getRaw());
        
        List<Map<String, Object>> defaultBlocks = new ArrayList<>();
        
        Map<String, Object> stoneMap = new LinkedHashMap<>();
        stoneMap.put("id", "STONE");
        stoneMap.put("chance", 80.0);
        stoneMap.put("drop", new ArrayList<String>());
        defaultBlocks.add(stoneMap);
        
        Map<String, Object> coalMap = new LinkedHashMap<>();
        coalMap.put("id", "COAL_ORE");
        coalMap.put("chance", 20.0);
        coalMap.put("drop", new ArrayList<String>());
        defaultBlocks.add(coalMap);
        
        config.set("blocks", defaultBlocks);
        
        List<String> actions = new ArrayList<>();
        actions.add("[message] [update:60] &fАвтоШахта &e" + id + " &fобновится через 1 минуту!");
        actions.add("[message] [update:10] &fАвтоШахта &e" + id + " &fобновится через 10 секунд!");
        actions.add("[message] [update] &fАвтоШахта &e" + id + " &fобновилась!");
        config.set("actions", actions);
        
        return config;
    }
}
