package ru.last.mines.config.models;

import dev.by1337.yaml.YamlMap;
import ru.last.mines.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Messages {
    private final String prefix;
    private final Message noPermission;
    private final Message reloaded;
    private final Message mineReset;
    private final Message mineNotFound;
    private final Message guiNotInstalled;
    private final Message createSuccess;
    private final Message selectRegion;
    private final Message mineExists;
    private final Message onlyPlayers;
    private final Message teleported;
    private final Message updateNextRarity;
    private final Message updateRarityNotFound;
    private final Message listEmpty;
    private final Message listHeader;
    private final Message listFormat;
    private final Message updateSuccess;
    
    private final Message chanceExceeded;
    private final Message inputChance;
    private final Message inputLimit;
    private final Message inputTimeout;
    private final Message invalidNumber;

    private final Message languageUsage;
    private final Message languageInvalid;
    private final Message languageSet;

    private final List<String> help;

    public Messages(YamlMap map) {
        this.prefix = map.get("prefix").asString("<gradient:#ffcc00:#ff9900>[LastMines]</gradient> ");
        this.noPermission = new Message(prefix + map.get("no-permission").asString("&cУ вас нет прав!"));
        this.reloaded = new Message(prefix + map.get("reloaded").asString("&aПлагин успешно перезагружен!"));
        this.mineReset = new Message(prefix + map.get("mine-reset").asString("&aШахта %mine% успешно обновлена!"));
        this.mineNotFound = new Message(prefix + map.get("mine-not-found").asString("&cШахта не найдена!"));
        this.guiNotInstalled = new Message(prefix + map.get("gui-not-installed").asString("&cBMenu не установлен!"));
        this.createSuccess = new Message(prefix + map.get("create-success").asString("&aШахта %mine% успешно создана!"));
        this.selectRegion = new Message(prefix + map.get("select-region").asString("&cСначала выделите регион!"));
        this.mineExists = new Message(prefix + map.get("mine-exists").asString("&cШахта с таким ID уже существует!"));
        this.onlyPlayers = new Message(prefix + map.get("only-players").asString("&cТолько для игроков."));
        this.teleported = new Message(prefix + map.get("teleported").asString("&aВы телепортированы к шахте &e%mine%&a."));
        this.updateNextRarity = new Message(prefix + map.get("update-next-rarity").asString("&aСледующая редкость для шахты &e%mine% &aустановлена на: &e%rarity%"));
        this.updateRarityNotFound = new Message(prefix + map.get("update-rarity-not-found").asString("&cРедкость '&e%rarity%&c' не найдена (или шахта не использует RARITY)!"));
        this.listEmpty = new Message(prefix + map.get("list-empty").asString("&cНет загруженных шахт."));
        this.listHeader = new Message(prefix + map.get("list-header").asString("&aЗагруженные шахты:"));
        this.listFormat = new Message(map.get("list-format").asString("&8- &e%mine% &7(Осталось: %time% сек)"));
        this.updateSuccess = new Message(prefix + map.get("update-success").asString("&aШахта &e%mine% &aпринудительно обновлена!"));
        
        this.chanceExceeded = new Message(prefix + map.get("chance-exceeded").asString("&cОбщий шанс не может превышать 100%!"));
        this.inputChance = new Message(prefix + map.get("input-chance").asString("&eНапишите шанс в чат (0.0001 - 100). &7У вас есть 1 минута."));
        this.inputLimit = new Message(prefix + map.get("input-limit").asString("&eНапишите лимит в чат (1 - 64). &7У вас есть 1 минута."));
        this.inputTimeout = new Message(prefix + map.get("input-timeout").asString("&cВремя на ввод вышло!"));
        this.invalidNumber = new Message(prefix + map.get("invalid-number").asString("&cНеверный формат числа!"));

        this.languageUsage = new Message(prefix + map.get("language-usage").asString("&eИспользование: &f/lastmines language <lang>&e. Доступные: &f%languages%&e. Текущий: &f%current%"));
        this.languageInvalid = new Message(prefix + map.get("language-invalid").asString("&cОшибка: язык &e%lang% &cне поддерживается! Доступные: &f%languages%"));
        this.languageSet = new Message(prefix + map.get("language-set").asString("&aЯзык плагина изменён на &e%lang%&a."));

        List<String> defHelp = Arrays.asList(
            "&8&m--------------------------------------",
            "  &e&lLastMines &7- Помощь",
            " ",
            "&8- &a/lastmines help &7- Помощь",
            "&8- &a/lastmines create <id> &7- Создать шахту",
            "&8- &a/lastmines reset <id> &7- Сбросить шахту",
            "&8- &a/lastmines gui <id> &7- Настроить шахту",
            "&8- &a/lastmines list &7- Список шахт",
            "&8- &a/lastmines tp <id> &7- Телепортироваться к шахте",
            "&8- &a/lastmines migrate <plugin> &7- Мигрировать шахты из других плагинов",
            "&8- &a/lastmines update <id> [rarity] [next] &7- Принудительно обновить шахту или сменить редкость",
            "&8- &a/lastmines reload &7- Перезагрузить конфигурацию",
            "&8- &a/lastmines language <lang> &7- Сменить язык плагина",
            "&8&m--------------------------------------"
        );
        this.help = new ArrayList<>();
        if (map.get("help").getRaw() instanceof List<?> list) {
            for (Object obj : list) {
                this.help.add(String.valueOf(obj));
            }
        } else {
            this.help.addAll(defHelp);
        }
    }

    public String getPrefix() { return prefix; }
    public Message getNoPermission() { return noPermission; }
    public Message getReloaded() { return reloaded; }
    public Message getMineReset() { return mineReset; }
    public Message getMineNotFound() { return mineNotFound; }
    public Message getGuiNotInstalled() { return guiNotInstalled; }
    public Message getCreateSuccess() { return createSuccess; }
    public Message getSelectRegion() { return selectRegion; }
    public Message getMineExists() { return mineExists; }
    public Message getOnlyPlayers() { return onlyPlayers; }
    public Message getTeleported() { return teleported; }
    public Message getUpdateNextRarity() { return updateNextRarity; }
    public Message getUpdateRarityNotFound() { return updateRarityNotFound; }
    public Message getListEmpty() { return listEmpty; }
    public Message getListHeader() { return listHeader; }
    public Message getListFormat() { return listFormat; }
    public Message getUpdateSuccess() { return updateSuccess; }
    
    public Message getChanceExceeded() { return chanceExceeded; }
    public Message getInputChance() { return inputChance; }
    public Message getInputLimit() { return inputLimit; }
    public Message getInputTimeout() { return inputTimeout; }
    public Message getInvalidNumber() { return invalidNumber; }

    public Message getLanguageUsage() { return languageUsage; }
    public Message getLanguageInvalid() { return languageInvalid; }
    public Message getLanguageSet() { return languageSet; }

    public List<String> getHelp() { return help; }
}
