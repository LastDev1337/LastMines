package ru.last.mines.commands.sub.migrate;

import org.bukkit.command.CommandSender;
import dev.by1337.yaml.YamlMap;
import ru.last.mines.LastMines;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.LinkedHashMap;

public class CataMinesMigration {

    public static void migrate(LastMines plugin, CommandSender sender) {
        File cataFolder = new File(plugin.getDataFolder().getParentFile(), "CataMines/mines");
        if (!cataFolder.exists() || !cataFolder.isDirectory()) {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "§cПапка plugins/CataMines/mines не найдена!");
            return;
        }

        File[] files = cataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "§cНет файлов для миграции в plugins/CataMines/mines/");
            return;
        }

        int migrated = 0;
        File lastMinesFolder = new File(plugin.getDataFolder(), "mines");
        if (!lastMinesFolder.exists()) lastMinesFolder.mkdirs();

        for (File file : files) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                lines.removeIf(line -> line.contains("==:"));
                String cleanYaml = String.join("\n", lines);
                
                YamlMap cataConfig = YamlMap.loadFromString(cleanYaml);
                
                YamlMap mineSec = null;
                try { mineSec = cataConfig.get("Mine").asYamlMap().orDefault(null); } catch (Exception ignored) {}
                
                if (mineSec == null) continue;
                
                String name = mineSec.get("name").asString(file.getName().replace(".yml", ""));
                
                YamlMap regionMap = mineSec.get("region").asYamlMap().orDefault(new YamlMap());
                String world = regionMap.get("world").asString("world");
                
                double p1x = regionMap.get("p1").asYamlMap().orDefault(new YamlMap()).get("x").asDouble(0.0);
                double p1y = regionMap.get("p1").asYamlMap().orDefault(new YamlMap()).get("y").asDouble(0.0);
                double p1z = regionMap.get("p1").asYamlMap().orDefault(new YamlMap()).get("z").asDouble(0.0);
                
                double p2x = regionMap.get("p2").asYamlMap().orDefault(new YamlMap()).get("x").asDouble(0.0);
                double p2y = regionMap.get("p2").asYamlMap().orDefault(new YamlMap()).get("y").asDouble(0.0);
                double p2z = regionMap.get("p2").asYamlMap().orDefault(new YamlMap()).get("z").asDouble(0.0);
                
                int resetDelay = mineSec.get("resetDelay").asInt(300);
                
                YamlMap newConfig = new YamlMap();
                newConfig.set("name", name);
                newConfig.set("world", world);
                newConfig.set("reset_time", resetDelay);
                newConfig.set("mode", "BLOCKS");
                
                YamlMap positions = new YamlMap();
                positions.set("one", p1x + ";" + p1y + ";" + p1z);
                positions.set("two", p2x + ";" + p2y + ";" + p2z);
                newConfig.set("positions", positions.getRaw());
                
                List<Map<String, Object>> newBlocks = new ArrayList<>();
                Object compRaw = mineSec.get("composition").getRaw();
                if (compRaw instanceof List<?> list) {
                    for (Object obj : list) {
                        if (obj instanceof Map<?, ?> blockMap) {
                            String blockId = String.valueOf(blockMap.get("block"));
                            Object chanceObj = blockMap.get("chance");
                            double chance = 100.0;
                            if (chanceObj instanceof Number n) chance = n.doubleValue();
                            else if (chanceObj instanceof String s) {
                                try { chance = Double.parseDouble(s); } catch (Exception ignored) {}
                            }
                            
                            Map<String, Object> bm = new LinkedHashMap<>();
                            bm.put("id", blockId);
                            bm.put("chance", chance);
                            bm.put("drop", new ArrayList<String>());
                            newBlocks.add(bm);
                        }
                    }
                }
                newConfig.set("blocks", newBlocks);
                
                YamlMap warnMap = mineSec.get("warn").asYamlMap().orDefault(new YamlMap());
                Object warnRaw = warnMap.get("warnSeconds").getRaw();
                List<String> warnSeconds = new ArrayList<>();
                if (warnRaw instanceof List<?> list) {
                    for (Object o : list) warnSeconds.add(String.valueOf(o));
                }
                
                List<String> newActions = new ArrayList<>();
                for (String secStr : warnSeconds) {
                    try {
                        int sec = Integer.parseInt(secStr.trim());
                        newActions.add("[message] [update:" + sec + "] &fАвтоШахта &e" + name + " &fобновится через " + sec + " сек.");
                    } catch (Exception ignored) {}
                }
                newActions.add("[message] [update] &fАвтоШахта &e" + name + " &fобновилась!");
                newConfig.set("actions", newActions);
                
                boolean tpEnable = mineSec.get("teleportPlayers").asBool(false);
                if (tpEnable) {
                    YamlMap tpMap = new YamlMap();
                    tpMap.set("enable", true);
                    YamlMap tpLoc = mineSec.get("teleportLocation").asYamlMap().orDefault(new YamlMap());
                    double tpX = tpLoc.get("x").asDouble(0.0);
                    double tpY = tpLoc.get("y").asDouble(100.0);
                    double tpZ = tpLoc.get("z").asDouble(0.0);
                    tpMap.set("pos", tpX + ";" + tpY + ";" + tpZ + ";0;0");
                    newConfig.set("teleport", tpMap.getRaw());
                }
                
                File outFile = new File(lastMinesFolder, file.getName());
                java.nio.file.Files.writeString(outFile.toPath(), newConfig.saveToString(), java.nio.charset.StandardCharsets.UTF_8);
                migrated++;
            } catch (Exception e) { plugin.getLogger().warning("Ошибка при миграции шахты " + file.getName() + ": " + e.getMessage()); }
        }
        
        if (migrated > 0) {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "§aУспешно мигрировано " + migrated + " шахт из CataMines!");
            plugin.getMineManager().loadAll();
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "§aШахты загружены!");
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessages().getPrefix() + "§cНе удалось мигрировать ни одной шахты.");
        }
    }
}
