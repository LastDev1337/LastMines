package ru.last.mines.gui.providers;

import ru.last.mines.LastMines;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class OnlineProvider extends DefaultMenu {
    public OnlineProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId != null) {
            this.addArgument("MINE_ID", mineId);
        }
    }

    @Override
    protected void generate() {
        String mineId = LastMines.get().getGuiManager().getViewingMines().get(viewer.getUniqueId());
        if (mineId == null) return;
        ru.last.mines.models.Mine mine = ru.last.mines.api.LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        dev.by1337.yaml.YamlMap map = config.yaml();
        if (!map.has("online_list")) return;
        dev.by1337.yaml.YamlMap listMap = map.get("online_list").asYamlMap().orDefault(new dev.by1337.yaml.YamlMap());

        String slotsStr = listMap.get("slots").asString("0-17");
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        String[] parts = slotsStr.replace(" ", "").split(",");
        for (String p : parts) {
            if (p.contains("-")) {
                String[] range = p.split("-");
                if (range.length == 2) {
                    try {
                        int min = Integer.parseInt(range[0]);
                        int max = Integer.parseInt(range[1]);
                        for (int i = min; i <= max; i++) slots.add(i);
                    } catch (Exception ignored) {}
                }
            } else {
                try { slots.add(Integer.parseInt(p)); } catch (Exception ignored) {}
            }
        }

        String name = listMap.get("name").asString("&aИгрок &f{PLAYER}");
        java.util.List<String> lore = new java.util.ArrayList<>();
        if (listMap.has("lore")) {
            Object rawLore = listMap.get("lore").getRaw();
            if (rawLore instanceof java.util.List<?> l) {
                for (Object o : l) lore.add(o.toString());
            }
        }

        java.util.List<Player> onlineInMine = new java.util.ArrayList<>();
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (mine.equals(ru.last.mines.api.LastMinesProvider.getApi().getMineAt(p.getLocation()))) {
                onlineInMine.add(p);
            }
        }

        for (int i = 0; i < Math.min(slots.size(), onlineInMine.size()); i++) {
            Player p = onlineInMine.get(i);
            int slot = slots.get(i);

            org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);
                meta.setDisplayName(ru.last.mines.utils.ColorUtils.colorString(name.replace("{PLAYER}", p.getName())));
                java.util.List<String> dLore = new java.util.ArrayList<>();
                for (String l : lore) {
                    dLore.add(ru.last.mines.utils.ColorUtils.colorString(l.replace("{PLAYER}", p.getName())));
                }
                meta.setLore(dLore);
                head.setItemMeta(meta);
            }

            layers.getBaseLayer()[slot] = new dev.by1337.bmenu.slot.impl.SimpleSlotContent(dev.by1337.item.ItemModel.fromItemStack(head)) {
                @Override
                public void doClick(Menu menu, Player player, dev.by1337.bmenu.slot.component.MenuClickType type) {
                    String key = "on_" + type.name().toLowerCase() + "_click";
                    if (!listMap.has(key)) key = "on_click";
                    if (listMap.has(key)) {
                        Object cmdRaw = listMap.get(key).getRaw();
                        java.util.List<String> commands = new java.util.ArrayList<>();
                        if (cmdRaw instanceof java.util.List<?> l) {
                            for (Object o : l) commands.add(o.toString());
                        } else if (cmdRaw instanceof String s) {
                            commands.add(s);
                        }
                        for (String cmd : commands) {
                            String parsed = cmd.replace("{PLAYER}", p.getName()).replace("{MINE_ID}", mine.getId());
                            menu.runCommands(dev.by1337.bmenu.command.ExecuteContext.of(menu), java.util.Collections.singletonList(parsed));
                        }
                    }
                }
            };
        }
    }
}
