package ru.last.mines.config.models;

import dev.by1337.yaml.YamlMap;

public class DebugSettings {
    private final boolean enable;
    private final String prefix;

    public DebugSettings(YamlMap map) {
        this.enable = map.get("enable").asBool(true);
        this.prefix = map.get("prefix").asString("[Debug] ");
    }
    
    public DebugSettings(boolean enable, String prefix) {
        this.enable = enable;
        this.prefix = prefix;
    }

    public boolean isEnable() { return enable; }
    public String getPrefix() { return prefix; }
}
