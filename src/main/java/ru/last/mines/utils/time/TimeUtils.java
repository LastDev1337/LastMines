package ru.last.mines.utils.time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*(milliseconds|minutes|seconds|months|hours|weeks|years|ticks|month|hour|week|year|days|sec|min|day|ms|mo|t|s|m|h|d|w|y)?", Pattern.CASE_INSENSITIVE);

    public static long parseToMillis(String input) {
        if (input == null || input.isEmpty()) return 0;
        
        Matcher matcher = TIME_PATTERN.matcher(input);
        long totalMillis = 0;
        boolean found = false;

        while (matcher.find()) {
            String valStr = matcher.group(1);
            if (valStr == null || valStr.isEmpty()) continue;
            found = true;
            long value = Long.parseLong(valStr);
            String unit = matcher.group(2);

            if (unit == null) {
                totalMillis += value * 1000L;
                continue;
            }

            unit = unit.toLowerCase();
            switch (unit) {
                case "ms": case "milliseconds":
                    totalMillis += value;
                    break;
                case "t": case "ticks":
                    totalMillis += value * 50L;
                    break;
                case "s": case "sec": case "seconds":
                    totalMillis += value * 1000L;
                    break;
                case "m": case "min": case "minutes":
                    totalMillis += value * 60_000L;
                    break;
                case "h": case "hour": case "hours":
                    totalMillis += value * 3_600_000L;
                    break;
                case "d": case "day": case "days":
                    totalMillis += value * 86_400_000L;
                    break;
                case "w": case "week": case "weeks":
                    totalMillis += value * 604_800_000L;
                    break;
                case "mo": case "month": case "months":
                    totalMillis += value * 2_592_000_000L;
                    break;
                case "y": case "year": case "years":
                    totalMillis += value * 31_536_000_000L;
                    break;
            }
        }

        if (!found) {
            try {
                return Long.parseLong(input) * 1000L;
            } catch (Exception ignored) {}
        }
        return totalMillis;
    }

    public static int parseToSeconds(String input) { return (int) (parseToMillis(input) / 1000L); }
    public static int parseToTicks(String input) { return (int) (parseToMillis(input) / 50L); }
}
