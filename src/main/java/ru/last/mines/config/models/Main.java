package ru.last.mines.config.models;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.mines.utils.time.TimeUtils;

public class Main {
    private final boolean debugEnable;
    private final DebugSettings info;
    private final DebugSettings warn;
    private final DebugSettings error;
    private final int timeLoadAllMines;
    private final String language;

    private final Modules modules;

    public Main(YamlMap root) {
        this.timeLoadAllMines = TimeUtils.parseToSeconds(root.get("time_load_all_mines").asString("5s"));
        this.language = root.get("language").asString("ru").toLowerCase().trim();
        
        YamlValue debugNode = root.get("debug");
        if (debugNode.asYamlMap().hasResult()) {
            YamlMap debugMap = debugNode.asYamlMap().getOrThrow();
            this.debugEnable = debugMap.get("enable").asBool(false);
            YamlValue levelNode = debugMap.get("level");
            if (levelNode.asYamlMap().hasResult()) {
                YamlMap levelMap = levelNode.asYamlMap().getOrThrow();
                this.info = new DebugSettings(levelMap.get("info").asYamlMap().getOrThrow());
                this.warn = new DebugSettings(levelMap.get("warn").asYamlMap().getOrThrow());
                this.error = new DebugSettings(levelMap.get("error").asYamlMap().getOrThrow());
            } else {
                this.info = new DebugSettings(true, "[Debug] ");
                this.warn = new DebugSettings(true, "[Debug] ");
                this.error = new DebugSettings(true, "[Debug] ");
            }
        } else {
            this.debugEnable = false;
            this.info = new DebugSettings(true, "[Debug] ");
            this.warn = new DebugSettings(true, "[Debug] ");
            this.error = new DebugSettings(true, "[Debug] ");
        }

        YamlMap modulesMap = root.get("modules").asYamlMap().orDefault(new YamlMap());
        this.modules = new Modules(modulesMap);
    }

    public boolean isDebugEnable() { return debugEnable; }
    public DebugSettings getInfo() { return info; }
    public DebugSettings getWarn() { return warn; }
    public DebugSettings getError() { return error; }
    public int getTimeLoadAllMines() { return timeLoadAllMines; }
    public String getLanguage() { return language; }
    public Modules getModules() { return modules; }
    
    public static class Modules {
        private final boolean all;
        private final boolean commands;
        private final boolean timeFormat;
        
        public Modules(YamlMap map) {
            this.all = map.get("all").asBool(true);
            this.commands = map.get("commands").asBool(true);
            this.timeFormat = map.get("time-format").asBool(true);
        }
        
        public boolean isCommandsEnabled() { return all && commands; }
        public boolean isTimeFormatEnabled() { return all && timeFormat; }
    }
}
