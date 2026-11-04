package ru.last.mines.actions;

import dev.laststudio.lib.api.LLibAPI;
import dev.laststudio.lib.api.action.ParsedAction;
import org.bukkit.Material;
import ru.last.mines.utils.time.TimeUtils;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MineActionParser {

    public enum When { NONE, UPDATE_RESET, UPDATE_AT, BREAK, EVERY }

    public record Parsed(When when, int seconds, MineAction action) {}

    private static final Pattern WHEN = Pattern.compile("(?i)\\[(update|break|every)(?::([^\\]]*))?]\\s*");
    private static final Pattern RADIUS = Pattern.compile("(?i)\\[radius:(-?\\d+)]\\s*");
    private static final Pattern MINE = Pattern.compile("(?i)\\[mine]\\s*");
    private static final Pattern SELF = Pattern.compile("(?i)\\[self]\\s*");
    private static final Pattern ALL = Pattern.compile("(?i)\\[all]\\s*");
    private static final Pattern LEGACY_ACTIONBAR = Pattern.compile(
            "(?i)^(\\[actionbar]\\s*(?:\\[chance:[^\\]]*]\\s*)?)\\d+\\s+(?:true|false)\\s+");
    private static final Pattern HEAD = Pattern.compile("(?i)^(\\[(title|message)]\\s*(?:\\[chance:[^\\]]*]\\s*)?)(.*)$", Pattern.DOTALL);

    private static final Pattern COMMAND_TYPE = Pattern.compile("(?i)^\\s*\\[(console|player)]");

    private static final Pattern TYPE_ALIAS = Pattern.compile("(?i)^\\[action[_-]bar]");

    private MineActionParser() {}

    public static boolean runsCommands(String raw) {
        return raw != null && COMMAND_TYPE.matcher(raw).find();
    }

    public static Parsed parse(String raw, Logger log) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();

        When when = When.NONE;
        int seconds = -1;
        Material material = null;
        Matcher m = WHEN.matcher(s);
        if (m.find()) {
            String kind = m.group(1).toLowerCase(Locale.ROOT);
            String arg = m.group(2) == null ? "" : m.group(2).trim();
            s = s.substring(0, m.start()) + s.substring(m.end());
            if (kind.equals("update")) {
                if (arg.isEmpty()) {
                    when = When.UPDATE_RESET;
                } else {
                    try {
                        seconds = TimeUtils.parseToSeconds(arg);
                        when = When.UPDATE_AT;
                    } catch (Exception e) {
                        log.warning("[LastMines] Неверное время в действии: " + raw);
                        return null;
                    }
                }
            } else if (kind.equals("every")) {
                try {
                    seconds = TimeUtils.parseToSeconds(arg);
                    if (seconds < 1) throw new IllegalArgumentException();
                    when = When.EVERY;
                } catch (Exception e) {
                    log.warning("[LastMines] Неверный интервал в [every:N]: " + raw);
                    return null;
                }
            } else {
                when = When.BREAK;
                if (!arg.isEmpty()) {
                    material = Material.matchMaterial(arg);
                    if (material == null) {
                        log.warning("[LastMines] Неизвестный материал '" + arg + "' в действии: " + raw);
                        return null;
                    }
                }
            }
        }

        MineAction.Target target = when == When.BREAK ? MineAction.Target.SELF : MineAction.Target.ALL;
        int radius = 0;
        Matcher r = RADIUS.matcher(s);
        if (r.find()) {
            radius = Integer.parseInt(r.group(1));
            target = radius < 0 ? MineAction.Target.MINE : MineAction.Target.RADIUS;
            s = r.replaceFirst("");
        }
        if (MINE.matcher(s).find()) { target = MineAction.Target.MINE; s = MINE.matcher(s).replaceFirst(""); }
        if (SELF.matcher(s).find()) { target = MineAction.Target.SELF; s = SELF.matcher(s).replaceFirst(""); }
        if (ALL.matcher(s).find()) { target = MineAction.Target.ALL; s = ALL.matcher(s).replaceFirst(""); }

        s = TYPE_ALIAS.matcher(s.trim()).replaceFirst("[actionbar]");
        s = normalize(s);
        List<ParsedAction> parsed = LLibAPI.get().actions().parse(List.of(s));
        if (parsed.isEmpty()) return null;
        return new Parsed(when, seconds, new MineAction(target, radius, material, parsed));
    }

    static String normalize(String s) {
        s = LEGACY_ACTIONBAR.matcher(s).replaceFirst("$1");
        Matcher h = HEAD.matcher(s);
        if (!h.matches()) return s;
        String head = h.group(1);
        String body = h.group(3);
        if (h.group(2).equalsIgnoreCase("message")) {
            return head + body.replace("\\n", "\n");
        }
        if (!body.startsWith("\"")) {
            return head + "\"" + body.replace("\n", "\\n") + "\" 20;40;20";
        }
        int end = body.lastIndexOf('"');
        if (end <= 0) return s;
        String text = body.substring(1, end).replace("\n", "\\n");
        String times = body.substring(end + 1).trim();
        return head + "\"" + text + "\" " + (times.isEmpty() ? "20;40;20" : times);
    }
}
