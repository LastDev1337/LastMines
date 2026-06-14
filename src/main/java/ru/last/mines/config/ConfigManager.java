package ru.last.mines.config;

import dev.by1337.yaml.YamlMap;
import org.bukkit.plugin.java.JavaPlugin;
import ru.last.mines.config.models.*;
import ru.last.mines.utils.*;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<ConfigType, Object> configs = new EnumMap<>(ConfigType.class);

    public ConfigManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void loadAll() {
        configs.clear();
        for (ConfigType type : ConfigType.values()) {
            loadConfig(type);
        }
        
        if (getMainConfig().getModules().isTimeFormatEnabled() && configs.containsKey(ConfigType.TIME_FORMAT)) {
            TimeFormatter.init((YamlMap) configs.get(ConfigType.TIME_FORMAT));
        }
    }

    private void loadConfig(ConfigType type) {
        File file = new File(plugin.getDataFolder(), type.getFileName());

        if (!file.exists()) {
            try {
                plugin.saveResource(type.getFileName(), false);
            } catch (IllegalArgumentException e) {
                try {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        plugin.getLogger().warning("Не удалось создать директории для файла: " + type.getFileName());
                    }
                    if (!file.createNewFile()) {
                        plugin.getLogger().warning("Не удалось создать пустой файл: " + type.getFileName());
                    }
                } catch (Exception ex) { plugin.getLogger().log(Level.SEVERE, "Ошибка при попытке создать файл " + type.getFileName(), ex); }
            }
        }

        try {
            YamlMap rootMap = YamlMap.load(file);

            switch (type) {
                case MAIN -> configs.put(type, new Main(rootMap));
                case MESSAGES -> configs.put(type, new Messages(rootMap));
                case TIME_FORMAT -> configs.put(type, rootMap);
                case COMMANDS -> configs.put(type, new ru.last.mines.config.models.CommandsModule(rootMap));
            }

        } catch (Exception e) { plugin.getLogger().log(Level.SEVERE, "Не удалось загрузить конфигурацию: " + type.getFileName(), e); }
    }

    public Main getMainConfig() { return (Main) configs.get(ConfigType.MAIN); }
    public Messages getMessages() { return (Messages) configs.get(ConfigType.MESSAGES); }
    public ru.last.mines.config.models.CommandsModule getCommandsModule() { return (ru.last.mines.config.models.CommandsModule) configs.get(ConfigType.COMMANDS); }
}
