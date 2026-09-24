package ru.last.mines.config;

import dev.by1337.yaml.YamlMap;
import org.bukkit.plugin.java.JavaPlugin;
import ru.last.mines.config.models.*;
import ru.last.mines.gui.GuiManager;
import ru.last.mines.utils.time.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private static final String DEFAULT_LANGUAGE = "ru";
    private static final Pattern LANG_JAR_PATTERN = Pattern.compile("^([a-z]{2,3})/messages\\.yml$");

    private final JavaPlugin plugin;
    private final Map<ConfigType, Object> configs = new EnumMap<>(ConfigType.class);

    public ConfigManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void loadAll() {
        configs.clear();
        for (ConfigType type : ConfigType.values()) {
            loadConfig(type);
        }

        if (getMainConfig() == null) {
            plugin.getLogger().severe("config.yml не загружен (см. ошибку выше). Плагин будет отключён.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        if (getMainConfig().getModules().isTimeFormatEnabled() && configs.containsKey(ConfigType.TIME_FORMAT)) {
            TimeFormatter.init((YamlMap) configs.get(ConfigType.TIME_FORMAT));
        }
    }

    private void loadConfig(ConfigType type) {
        File file = new File(plugin.getDataFolder(), type.getFileName());

        if (!file.exists()) {
            if (type == ConfigType.MAIN) {
                saveOrCreateEmpty(type.getFileName());
            } else {
                extractLangResource(type.getFileName());
            }
        }

        try {
            YamlMap rootMap = YamlMap.load(file);

            switch (type) {
                case MAIN -> configs.put(type, new Main(rootMap));
                case MESSAGES -> configs.put(type, new Messages(rootMap));
                case TIME_FORMAT -> configs.put(type, rootMap);
                case COMMANDS -> configs.put(type, new CommandsModule(rootMap));
            }

        } catch (Exception e) { plugin.getLogger().log(Level.SEVERE, "Не удалось загрузить конфигурацию: " + type.getFileName(), e); }
    }

    public void extractLangResource(String relativePath) {
        File target = new File(plugin.getDataFolder(), relativePath);
        if (target.exists()) return;

        String lang = getMainConfig() != null ? getMainConfig().getLanguage() : DEFAULT_LANGUAGE;
        String jarPath = lang + "/" + relativePath;
        if (plugin.getResource(jarPath) == null) {
            if (!lang.equals(DEFAULT_LANGUAGE)) {
                plugin.getLogger().warning("Язык '" + lang + "' не содержит " + relativePath + ", использую " + DEFAULT_LANGUAGE);
            }
            jarPath = DEFAULT_LANGUAGE + "/" + relativePath;
        }

        try (InputStream in = plugin.getResource(jarPath)) {
            if (in == null) {
                plugin.getLogger().warning("Ресурс не найден в jar-е: " + jarPath);
                return;
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Не удалось создать директории для файла: " + relativePath);
                return;
            }
            Files.copy(in, target.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось извлечь ресурс " + relativePath, e);
        }
    }

    private void saveOrCreateEmpty(String fileName) {
        try {
            plugin.saveResource(fileName, false);
        } catch (IllegalArgumentException e) {
            File file = new File(plugin.getDataFolder(), fileName);
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    plugin.getLogger().warning("Не удалось создать директории для файла: " + fileName);
                }
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Не удалось создать пустой файл: " + fileName);
                }
            } catch (Exception ex) { plugin.getLogger().log(Level.SEVERE, "Ошибка при попытке создать файл " + fileName, ex); }
        }
    }

    public List<String> getAvailableLanguages() {
        List<String> result = new ArrayList<>();
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            try (JarFile jar = new JarFile(jarFile)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    Matcher m = LANG_JAR_PATTERN.matcher(entry.getName());
                    if (m.matches()) result.add(m.group(1));
                }
            }
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось определить список языков из jar-а", e);
        }
        if (result.isEmpty()) result.add(DEFAULT_LANGUAGE);
        Collections.sort(result);
        return result;
    }

    private List<String> langTextResources() {
        List<String> result = new ArrayList<>(List.of(
                ConfigType.MESSAGES.getFileName(),
                ConfigType.TIME_FORMAT.getFileName(),
                ConfigType.COMMANDS.getFileName()
        ));
        for (String menu : GuiManager.DEFAULT_MENUS) {
            result.add("menus/" + menu);
        }
        return result;
    }

    public void setLanguage(String lang) {
        File configFile = new File(plugin.getDataFolder(), ConfigType.MAIN.getFileName());
        try {
            String content = configFile.exists() ? Files.readString(configFile.toPath()) : "";
            Matcher m = Pattern.compile("(?m)^language:.*").matcher(content);
            String updated = m.find() ? m.replaceFirst("language: " + lang) : "language: " + lang + "\n" + content;
            Files.writeString(configFile.toPath(), updated);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить язык в config.yml", e);
            return;
        }

        loadConfig(ConfigType.MAIN);

        for (String path : langTextResources()) {
            File target = new File(plugin.getDataFolder(), path);
            target.delete();
            extractLangResource(path);
        }
    }

    public Main getMainConfig() { return (Main) configs.get(ConfigType.MAIN); }
    public Messages getMessages() { return (Messages) configs.get(ConfigType.MESSAGES); }
    public CommandsModule getCommandsModule() { return (CommandsModule) configs.get(ConfigType.COMMANDS); }
}
