package ru.last.mines.config;

public enum ConfigType {
    MAIN("config.yml"),
    MESSAGES("messages.yml"),
    TIME_FORMAT("modules/time-format.yml"),
    COMMANDS("modules/commands.yml");

    private final String fileName;
    ConfigType(String fileName) { this.fileName = fileName; }
    public String getFileName() { return fileName; }
}
