package ru.last.mines.gui;

import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.slot.component.MenuClickType;
import dev.by1337.yaml.YamlMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ClickCommandResolver {
    private ClickCommandResolver() {}

    public static List<String> resolve(YamlMap listMap, MenuClickType type) {
        Object cmdRaw = listMap.has(type.getConfigKeyClick()) ? listMap.get(type.getConfigKeyClick()).getRaw() : null;

        if (cmdRaw == null && listMap.has("on_click")) {
            Object onClickRaw = listMap.get("on_click").getRaw();
            if (onClickRaw instanceof Map<?, ?> m) {
                cmdRaw = m.get(type.name().toLowerCase());
            } else {
                cmdRaw = onClickRaw;
            }
        }

        List<String> commands = new ArrayList<>();
        if (cmdRaw instanceof List<?> l) {
            for (Object o : l) commands.add(o.toString());
        } else if (cmdRaw instanceof String s) {
            commands.add(s);
        }
        return commands;
    }

    public static void resolveAndRun(Menu menu, YamlMap listMap, MenuClickType type, Map<String, String> replacements) {
        for (String cmd : resolve(listMap, type)) {
            for (Map.Entry<String, String> e : replacements.entrySet()) {
                cmd = cmd.replace(e.getKey(), e.getValue());
            }
            menu.runCommands(ExecuteContext.of(menu), Collections.singletonList(cmd));
        }
    }
}
