package ru.last.mines.managers;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import ru.last.mines.LastMines;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class BlockCatalogManager {
    private final LastMines plugin;
    private final File blocksDir;
    private final File indexFile;

    private String version;
    private List<Material> blocks = List.of();

    public BlockCatalogManager(LastMines plugin) {
        this.plugin = plugin;
        this.blocksDir = new File(plugin.getDataFolder(), "blocks");
        this.indexFile = new File(blocksDir, "index.json");
    }

    public void load() {
        if (!blocksDir.exists()) blocksDir.mkdirs();

        this.version = detectVersion();
        List<String> ids = computeBlockIds();

        YamlMap index = indexFile.exists() ? YamlMap.load(indexFile) : new YamlMap();
        String canonicalVersion = findIdenticalVersion(index, ids);

        if (canonicalVersion == null) {
            canonicalVersion = version;
            writeBlocksFile(new File(blocksDir, version + "/blocks.json"), ids);
        }

        putVersionMapping(index, version, canonicalVersion);
        saveIndex(index);

        this.blocks = ids.stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .toList();

        plugin.getDebugger().info("Каталог блоков (" + getVersion() + "): " + blocks.size()
                + " блоков, файл: " + canonicalVersion + "/blocks.json");
    }

    public List<Material> getBlocks() { return blocks; }
    public String getVersion() { return version; }

    private String detectVersion() {
        String raw = Bukkit.getBukkitVersion();
        int dash = raw.indexOf('-');
        return dash > 0 ? raw.substring(0, dash) : raw;
    }

    private List<String> computeBlockIds() {
        return Arrays.stream(Material.values())
                .filter(Material::isBlock)
                .filter(Material::isItem)
                .filter(m -> !m.isLegacy())
                .filter(m -> m != Material.AIR && m != Material.CAVE_AIR && m != Material.VOID_AIR)
                .map(m -> m.name().toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
    }

    private String findIdenticalVersion(YamlMap index, List<String> ids) {
        Object rawVersions = index.get("versions").getRaw();
        if (!(rawVersions instanceof java.util.Map<?, ?> versionsMap)) return null;

        for (Object canonical : new java.util.LinkedHashSet<>(versionsMap.values())) {
            File file = new File(blocksDir, canonical + "/blocks.json");
            if (!file.exists()) continue;
            List<String> existing = readBlocksFile(file);
            if (existing.equals(ids)) return canonical.toString();
        }
        return null;
    }

    private List<String> readBlocksFile(File file) {
        try {
            YamlMap map = YamlMap.load(file);
            Object raw = map.get("blocks").getRaw();
            List<String> result = new ArrayList<>();
            if (raw instanceof List<?> list) {
                for (Object o : list) result.add(String.valueOf(o));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void writeBlocksFile(File file, List<String> ids) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        StringBuilder json = new StringBuilder("{\n  \"blocks\": [\n");
        for (int i = 0; i < ids.size(); i++) {
            json.append("    \"").append(ids.get(i)).append('"');
            if (i != ids.size() - 1) json.append(',');
            json.append('\n');
        }
        json.append("  ]\n}\n");

        try {
            Files.writeString(file.toPath(), json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить " + file, e);
        }
    }

    private void saveIndex(YamlMap index) {
        Object rawVersions = index.get("versions").getRaw();
        StringBuilder json = new StringBuilder("{\n  \"versions\": {\n");
        if (rawVersions instanceof java.util.Map<?, ?> map) {
            int i = 0;
            for (var entry : map.entrySet()) {
                json.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append('"');
                if (++i != map.size()) json.append(',');
                json.append('\n');
            }
        }
        json.append("  }\n}\n");

        try {
            Files.writeString(indexFile.toPath(), json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить " + indexFile, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void putVersionMapping(YamlMap index, String version, String canonical) {
        Object rawVersions = index.get("versions").getRaw();
        Map<String, Object> versionsMap;
        if (rawVersions instanceof Map<?, ?> m) {
            versionsMap = (Map<String, Object>) m;
        } else {
            versionsMap = new LinkedHashMap<>();
            index.getRaw().put("versions", versionsMap);
        }
        versionsMap.put(version, canonical);
    }
}
