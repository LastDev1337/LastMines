package ru.last.mines.commands.sub.migrate;

import org.bukkit.command.CommandSender;
import dev.by1337.yaml.YamlMap;
import ru.last.mines.LastMines;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.Map;
import java.util.LinkedHashMap;

public class RealMinesMigration {

    public static void migrate(LastMines plugin, CommandSender sender) {
        String prefix = plugin.getConfigManager().getMessages().getPrefix();

        File rmFolder = new File(plugin.getDataFolder().getParentFile(), "RealMines/mines");
        if (!rmFolder.exists() || !rmFolder.isDirectory()) {
            sender.sendMessage(prefix + "§cПапка plugins/RealMines/mines не найдена!");
            return;
        }

        File[] files = rmFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            sender.sendMessage(prefix + "§cНет файлов для миграции в plugins/RealMines/mines/");
            return;
        }

        int migrated = 0;
        File lastMinesFolder = new File(plugin.getDataFolder(), "mines");
        if (!lastMinesFolder.exists()) lastMinesFolder.mkdirs();

        for (File file : files) {
            try {
                YamlMap rmConfig = YamlMap.load(file);
                String name = rmConfig.get("name").asString(file.getName().replace(".yml", ""));
                
                String world = "world";
                double p1x = 0, p1y = 0, p1z = 0;
                double p2x = 0, p2y = 0, p2z = 0;
                
                YamlMap p1Sec = null;
                try { p1Sec = rmConfig.get("POS1").asYamlMap().orDefault(null); } catch (Exception ignored) {}
                
                if (p1Sec != null) {
                    p1x = p1Sec.get("x").asDouble(0.0); p1y = p1Sec.get("y").asDouble(0.0); p1z = p1Sec.get("z").asDouble(0.0);
                    world = p1Sec.get("world").asString("world");
                } else {
                    String p1Str = rmConfig.get("POS1").asString(null);
                    if (p1Str != null) {
                        String[] sp1 = p1Str.split(";");
                        if (sp1.length >= 3) {
                            try {
                                p1x = Double.parseDouble(sp1[0]); p1y = Double.parseDouble(sp1[1]); p1z = Double.parseDouble(sp1[2]);
                            } catch (Exception ignored) {}
                        }
                    }
                }

                YamlMap p2Sec = null;
                try { p2Sec = rmConfig.get("POS2").asYamlMap().orDefault(null); } catch (Exception ignored) {}
                if (p2Sec != null) {
                    p2x = p2Sec.get("x").asDouble(0.0); p2y = p2Sec.get("y").asDouble(0.0); p2z = p2Sec.get("z").asDouble(0.0);
                } else {
                    String p2Str = rmConfig.get("POS2").asString(null);
                    if (p2Str != null) {
                        String[] sp2 = p2Str.split(";");
                        if (sp2.length >= 3) {
                            try {
                                p2x = Double.parseDouble(sp2[0]); p2y = Double.parseDouble(sp2[1]); p2z = Double.parseDouble(sp2[2]);
                            } catch (Exception ignored) {}
                        }
                    }
                }

                int resetTime = rmConfig.get("reset").asYamlMap().orDefault(new YamlMap())
                                        .get("time").asYamlMap().orDefault(new YamlMap())
                                        .get("value").asInt(300);

                YamlMap newConfig = new YamlMap();
                newConfig.set("name", name);
                newConfig.set("world", world);
                newConfig.set("reset_time", resetTime);
                newConfig.set("mode", "BLOCKS");
                
                YamlMap positions = new YamlMap();
                positions.set("one", p1x + ";" + p1y + ";" + p1z);
                positions.set("two", p2x + ";" + p2y + ";" + p2z);
                newConfig.set("positions", positions.getRaw());

                List<Map<String, Object>> newBlocks = new ArrayList<>();
                YamlMap blocksSec = null;
                try { blocksSec = rmConfig.get("Blocks").asYamlMap().orDefault(null); } catch (Exception ignored) {}
                
                if (blocksSec != null) {
                    for (String b : blocksSec.getRaw().keySet()) {
                        double chance = blocksSec.get(b).asDouble(0.0);
                        if (chance <= 1.0) chance *= 100.0;
                        Map<String, Object> bm = new LinkedHashMap<>();
                        bm.put("id", b);
                        bm.put("chance", chance);
                        bm.put("drop", new ArrayList<>());
                        newBlocks.add(bm);
                    }
                }
                newConfig.set("blocks", newBlocks);
                newConfig.set("actions", List.of("[message] [update] &fАвтоШахта &e" + name + " &fобновилась!"));

                File outFile = new File(lastMinesFolder, file.getName());
                java.nio.file.Files.writeString(outFile.toPath(), newConfig.saveToString(), java.nio.charset.StandardCharsets.UTF_8);
                migrated++;
            } catch (Exception e) { plugin.getLogger().warning("Ошибка при миграции шахты " + file.getName() + " из RealMines: " + e.getMessage()); }
        }

        if (migrated > 0) {
            sender.sendMessage(prefix + "§aУспешно мигрировано " + migrated + " шахт из RealMines!");
            plugin.getMineManager().loadAll();
            sender.sendMessage(prefix + "§aШахты загружены!");
        } else {
            sender.sendMessage(prefix + "§cНе удалось мигрировать ни одной шахты.");
        }
    }
}
