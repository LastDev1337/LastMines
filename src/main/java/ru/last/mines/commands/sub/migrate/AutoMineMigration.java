package ru.last.mines.commands.sub.migrate;

import org.bukkit.command.CommandSender;
import dev.by1337.yaml.YamlMap;
import ru.last.mines.LastMines;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.Map;
import java.util.LinkedHashMap;

public class AutoMineMigration {

    public static void migrate(LastMines plugin, CommandSender sender) {
        String prefix = plugin.getConfigManager().getMessages().getPrefix();

        File autoMinesFile = new File(plugin.getDataFolder().getParentFile(), "AutoMine/AutoMines.yml");
        if (!autoMinesFile.exists()) {
            sender.sendMessage(prefix + "§cФайл plugins/AutoMine/AutoMines.yml не найден!");
            return;
        }

        YamlMap config;
        try {
            config = YamlMap.load(autoMinesFile);
        } catch (Exception e) {
            sender.sendMessage(prefix + "§cОшибка чтения AutoMines.yml!");
            return;
        }

        YamlMap autoMinesSec = null;
        try { autoMinesSec = config.get("AutoMines").asYamlMap().orDefault(null); } catch (Exception ignored) {}
        
        if (autoMinesSec == null) {
            sender.sendMessage(prefix + "§cВ AutoMines.yml нет секции AutoMines!");
            return;
        }

        int migrated = 0;
        File lastMinesFolder = new File(plugin.getDataFolder(), "mines");
        if (!lastMinesFolder.exists()) lastMinesFolder.mkdirs();

        for (String key : autoMinesSec.getRaw().keySet()) {
            try {
                if (!ru.last.mines.managers.MineManager.isValidId(key)) {
                    plugin.getLogger().warning("Пропущена шахта с недопустимым id из AutoMine: " + key);
                    continue;
                }
                YamlMap mineSec = autoMinesSec.get(key).asYamlMap().orDefault(null);
                if (mineSec == null) continue;

                String world = mineSec.get("world").asString("world");
                
                double p1x = mineSec.get("firstPos").asYamlMap().orDefault(new YamlMap()).get("x").asDouble(0.0);
                double p1y = mineSec.get("firstPos").asYamlMap().orDefault(new YamlMap()).get("y").asDouble(0.0);
                double p1z = mineSec.get("firstPos").asYamlMap().orDefault(new YamlMap()).get("z").asDouble(0.0);

                double p2x = mineSec.get("secondPos").asYamlMap().orDefault(new YamlMap()).get("x").asDouble(0.0);
                double p2y = mineSec.get("secondPos").asYamlMap().orDefault(new YamlMap()).get("y").asDouble(0.0);
                double p2z = mineSec.get("secondPos").asYamlMap().orDefault(new YamlMap()).get("z").asDouble(0.0);

                int timeUpdate = mineSec.get("timeUpdate").asInt(300);

                YamlMap newConfig = new YamlMap();
                newConfig.set("name", key);
                newConfig.set("world", world);
                newConfig.set("reset_time", timeUpdate);
                
                YamlMap posMap = new YamlMap();
                posMap.set("one", p1x + ";" + p1y + ";" + p1z);
                posMap.set("two", p2x + ";" + p2y + ";" + p2z);
                newConfig.set("positions", posMap.getRaw());

                YamlMap types = null;
                try { types = mineSec.get("typeMine").asYamlMap().orDefault(null); } catch (Exception ignored) {}
                
                if (types == null || types.getRaw().isEmpty()) {
                    newConfig.set("mode", "BLOCKS");
                    newConfig.set("blocks", new ArrayList<>());
                } else {
                    if (types.getRaw().size() == 1) {
                        newConfig.set("mode", "BLOCKS");
                        String firstType = types.getRaw().keySet().iterator().next();
                        
                        List<String> blockList = new ArrayList<>();
                        Object rawList = types.get(firstType).asYamlMap().orDefault(new YamlMap()).get("blockList").getRaw();
                        if (rawList instanceof List<?> list) {
                            for (Object o : list) blockList.add(String.valueOf(o));
                        }
                        
                        List<Map<String, Object>> newBlocks = new ArrayList<>();
                        for (String b : blockList) {
                            String[] split = b.split(":");
                            if (split.length >= 2) {
                                try {
                                    double chance = Double.parseDouble(split[0]);
                                    String id = split[1];
                                    Map<String, Object> bm = new LinkedHashMap<>();
                                    bm.put("id", id);
                                    bm.put("chance", chance);
                                    bm.put("drop", new ArrayList<>());
                                    newBlocks.add(bm);
                                } catch (Exception ignored) {}
                            }
                        }
                        newConfig.set("blocks", newBlocks);
                    } else {
                        newConfig.set("mode", "RARITY");
                        List<Map<String, Object>> raritiesList = new ArrayList<>();
                        for (String typeKey : types.getRaw().keySet()) {
                            YamlMap tSec = types.get(typeKey).asYamlMap().orDefault(null);
                            if (tSec == null) continue;

                            double chance = tSec.get("chance").asDouble(10.0);
                            String name = tSec.get("name").asString(typeKey);

                            Map<String, Object> rSec = new LinkedHashMap<>();
                            rSec.put("id", typeKey);
                            rSec.put("chance", chance);
                            rSec.put("name", name);

                            List<String> blockList = new ArrayList<>();
                            Object rawList = tSec.get("blockList").getRaw();
                            if (rawList instanceof List<?> list) {
                                for (Object o : list) blockList.add(String.valueOf(o));
                            }

                            List<Map<String, Object>> newBlocks = new ArrayList<>();
                            for (String b : blockList) {
                                String[] split = b.split(":");
                                if (split.length >= 2) {
                                    try {
                                        double bChance = Double.parseDouble(split[0]);
                                        String id = split[1];
                                        Map<String, Object> bm = new LinkedHashMap<>();
                                        bm.put("id", id);
                                        bm.put("chance", bChance);
                                        bm.put("drop", new ArrayList<>());
                                        newBlocks.add(bm);
                                    } catch (Exception ignored) {}
                                }
                            }
                            rSec.put("blocks", newBlocks);
                            raritiesList.add(rSec);
                        }
                        newConfig.set("rarity", raritiesList);
                    }
                }

                newConfig.set("actions", List.of("[message] [update] &fАвтоШахта &e" + key + " &fобновилась!"));

                File outFile = new File(lastMinesFolder, key + ".yml");
                java.nio.file.Files.writeString(outFile.toPath(), newConfig.saveToString(), java.nio.charset.StandardCharsets.UTF_8);
                migrated++;
            } catch (Exception e) { plugin.getLogger().warning("Ошибка при миграции шахты " + key + " из AutoMine: " + e.getMessage()); }
        }

        if (migrated > 0) {
            sender.sendMessage(prefix + "§aУспешно мигрировано " + migrated + " шахт из AutoMine!");
            plugin.getMineManager().loadAll();
            sender.sendMessage(prefix + "§aШахты загружены!");
        } else {
            sender.sendMessage(prefix + "§cНе удалось мигрировать ни одной шахты.");
        }
    }
}
