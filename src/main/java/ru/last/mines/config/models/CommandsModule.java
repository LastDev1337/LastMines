package ru.last.mines.config.models;

import dev.by1337.yaml.YamlMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandsModule {
    private final Map<String, CommandConfig> commands = new HashMap<>();

    public CommandsModule(YamlMap root) {
        YamlMap cmds = root.get("commands").asYamlMap().orDefault(new YamlMap());
        for (String key : cmds.getRaw().keySet()) {
            commands.put(key.toLowerCase(), new CommandConfig(cmds.get(key).asYamlMap().getOrThrow()));
        }
    }

    public CommandConfig getCommand(String name) {
        return commands.getOrDefault(name.toLowerCase(), new CommandConfig(new YamlMap()));
    }

    public static class CommandConfig {
        private final boolean enable;
        private final boolean tabCompleter;
        private final List<String> aliases;

        public CommandConfig(YamlMap map) {
            this.enable = map.get("enable").asBool(true);
            this.tabCompleter = map.get("tab-completer").asBool(true);
            
            this.aliases = new ArrayList<>();
            Object rawAliases = map.get("aliases").getRaw();
            if (rawAliases instanceof List<?> list) {
                for (Object o : list) {
                    this.aliases.add(String.valueOf(o).toLowerCase());
                }
            }
        }

        public boolean isEnable() { return enable; }
        public boolean isTabCompleter() { return tabCompleter; }
        public List<String> getAliases() { return aliases; }
    }
}
