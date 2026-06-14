package ru.last.mines.utils;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;

public class TimeFormatter {
    private static YamlMap config;
    private static YamlMap defaultsViews;

    public static void init(YamlMap timeFormatConfig) {
        config = timeFormatConfig;
        if (config != null) {
            defaultsViews = config.get("defaults_views").asYamlMap().orDefault(new YamlMap());
        }
    }

    public static String format(long millis, String formatType) { return format(millis, formatType, null); }

    public static String format(long millis, String formatType, String contextKey) {
        if (config == null) return formatDefault(millis, formatType);

        if (formatType == null || formatType.isEmpty() || formatType.equals("default")) {
            if (contextKey != null) {
                formatType = getDefaultView(contextKey);
            } else {
                formatType = "default";
            }
        }

        long totalSeconds = (long) Math.ceil(millis / 1000.0);
        long years = totalSeconds / 31536000;
        totalSeconds %= 31536000;
        long months = totalSeconds / 2592000;
        totalSeconds %= 2592000;
        long weeks = totalSeconds / 604800;
        totalSeconds %= 604800;
        long days = totalSeconds / 86400;
        totalSeconds %= 86400;
        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        YamlMap formatsSec = config.get("formats").asYamlMap().hasResult() ? config.get("formats").asYamlMap().getOrThrow() : new YamlMap();
        YamlMap formatMap = formatsSec.get(formatType).asYamlMap().hasResult() ? formatsSec.get(formatType).asYamlMap().getOrThrow() : null;
        
        if (formatMap == null) return formatDefault(millis, formatType);

        YamlMap f = formatMap.get("formats").asYamlMap().hasResult() ? formatMap.get("formats").asYamlMap().getOrThrow() : new YamlMap();
        YamlMap t = formatMap.get("types").asYamlMap().hasResult() ? formatMap.get("types").asYamlMap().getOrThrow() : new YamlMap();

        String formatStr;
        if (years > 0) formatStr = f.get("years").asString("%years% г.");
        else if (months > 0) formatStr = f.get("months").asString("%months% мес.");
        else if (weeks > 0) formatStr = f.get("weeks").asString("%weeks% нед.");
        else if (days > 0) formatStr = f.get("days").asString("%days% д.");
        else if (hours > 0) formatStr = f.get("hours").asString("%hours% ч.");
        else if (minutes > 0) formatStr = f.get("minutes").asString("%minutes% мин.");
        else formatStr = f.get("seconds").asString("%seconds% сек.");

        formatStr = formatStr.replace("%years%", years > 0 ? years + (t.has("years") ? " " + getType(t.get("years"), years) : "") : "");
        formatStr = formatStr.replace("%months%", months > 0 || years > 0 ? (formatType.equals("clock") ? String.format("%02d", months) : months) + (t.has("months") ? " " + getType(t.get("months"), months) : "") : "");
        formatStr = formatStr.replace("%weeks%", weeks > 0 || months > 0 || years > 0 ? (formatType.equals("clock") ? String.format("%02d", weeks) : weeks) + (t.has("weeks") ? " " + getType(t.get("weeks"), weeks) : "") : "");
        formatStr = formatStr.replace("%days%", days > 0 || weeks > 0 || months > 0 || years > 0 ? (formatType.equals("clock") ? String.format("%02d", days) : days) + (t.has("days") ? " " + getType(t.get("days"), days) : "") : "");
        formatStr = formatStr.replace("%hours%", hours > 0 || days > 0 || weeks > 0 || months > 0 || years > 0 ? (formatType.equals("clock") ? String.format("%02d", hours) : hours) + (t.has("hours") ? " " + getType(t.get("hours"), hours) : "") : "");
        formatStr = formatStr.replace("%minutes%", minutes > 0 || hours > 0 || days > 0 || weeks > 0 || months > 0 || years > 0 ? (formatType.equals("clock") ? String.format("%02d", minutes) : minutes) + (t.has("minutes") ? " " + getType(t.get("minutes"), minutes) : "") : "");
        formatStr = formatStr.replace("%seconds%", (formatType.equals("clock") ? String.format("%02d", seconds) : seconds) + (t.has("seconds") ? " " + getType(t.get("seconds"), seconds) : ""));

        return formatStr.trim().replaceAll(" +", " ");
    }

    public static String getDefaultView(String path) {
        if (defaultsViews == null) return "default";
        
        if (defaultsViews.has("all")) return defaultsViews.get("all").asString("default");

        String[] segments = path.split("\\.");
        YamlMap current = defaultsViews;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (!current.has(segment)) return "default";
            
            dev.by1337.yaml.YamlValue node = current.get(segment);
            if (node.asYamlMap().hasResult()) {
                current = node.asYamlMap().getOrThrow();
                if (current.has("all")) return current.get("all").asString("default");
            } else {
                if (i == segments.length - 1) {
                    return node.asString("default");
                }
                return "default";
            }
        }
        return "default";
    }
    
    private static String getType(YamlValue node, long num) {
        if (node.getRaw() instanceof String) return node.asString("");
        if (node.asYamlMap().hasResult()) {
            YamlMap map = node.asYamlMap().getOrThrow();
            return plural(num, map.get("form_1").asString(""), map.get("form_2").asString(""), map.get("form_5").asString(""), map.get("form_3").asString(""));
        }
        return "";
    }

    public static String formatDefault(long millis, String formatType) {
        long totalSeconds = (long) Math.ceil(millis / 1000.0);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if ("clock".equalsIgnoreCase(formatType) || "simple".equalsIgnoreCase(formatType)) {
            if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            return String.format("%02d:%02d", minutes, seconds);
        } else if ("detail".equalsIgnoreCase(formatType)) {
            StringBuilder sb = new StringBuilder();
            if (hours > 0) sb.append(hours).append(" ").append(plural(hours, "час", "часа", "часов", "часа")).append(" ");
            if (minutes > 0) sb.append(minutes).append(" ").append(plural(minutes, "минута", "минуты", "минут", "минуты")).append(" ");
            if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append(" ").append(plural(seconds, "секунда", "секунды", "секунд", "секунды"));
            return sb.toString().trim();
        } else {
            StringBuilder sb = new StringBuilder();
            if (hours > 0) sb.append(hours).append(" ч. ");
            if (minutes > 0) sb.append(minutes).append(" мин. ");
            if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append(" сек.");
            return sb.toString().trim();
        }
    }

    private static String plural(long n, String form1, String form2, String form5, String form3) {
        n = Math.abs(n) % 100;
        long n1 = n % 10;
        if (n > 10 && n < 20) return form5;
        if (n1 > 1 && n1 < 5) return form3 != null && !form3.isEmpty() && n1 == 3 ? form3 : form2;
        if (n1 == 1) return form1;
        return form5;
    }
}
