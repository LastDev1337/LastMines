package ru.last.mines.gui.providers;

import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.slot.component.MenuClickType;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;
import dev.by1337.item.ItemModel;
import dev.by1337.yaml.YamlMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import ru.last.mines.LastMines;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.last.mines.api.LastMinesProvider;
import dev.by1337.bmenu.animation.util.AnimationUtil;
import ru.last.mines.models.Mine;
import ru.last.mines.utils.ColorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

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
        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        YamlMap map = config.yaml();
        if (!map.has("online_list")) return;
        YamlMap listMap = map.get("online_list").asYamlMap().orDefault(new YamlMap());

        String slotsStr = listMap.get("slots").asString("0-17");
        List<Integer> slots = slotsStr.isBlank() ? List.of() : Arrays.stream(AnimationUtil.readSlots(slotsStr)).boxed().toList();

        String name = listMap.get("name").asString("&aИгрок &f{PLAYER}");
        List<String> lore = new ArrayList<>();
        if (listMap.has("lore")) {
            Object rawLore = listMap.get("lore").getRaw();
            if (rawLore instanceof List<?> l) {
                for (Object o : l) lore.add(o.toString());
            }
        }

        List<Player> onlineInMine = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (mine.equals(LastMinesProvider.getApi().getMineAt(p.getLocation()))) {
                onlineInMine.add(p);
            }
        }

        for (int i = 0; i < Math.min(slots.size(), onlineInMine.size()); i++) {
            Player p = onlineInMine.get(i);
            int slot = slots.get(i);
            if (slot < 0 || slot >= layers.getBaseLayer().length) continue;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);
                meta.displayName(ColorUtils.color(name.replace("{PLAYER}", p.getName())));
                List<Component> dLore = new ArrayList<>();
                for (String l : lore) {
                    dLore.add(ColorUtils.color(l.replace("{PLAYER}", p.getName())));
                }
                meta.lore(dLore);
                head.setItemMeta(meta);
            }

            layers.getBaseLayer()[slot] = new SimpleSlotContent(ItemModel.fromItemStack(head)) {
                @Override
                public void doClick(Menu menu, Player player, MenuClickType type) {
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
                            menu.runCommands(ExecuteContext.of(menu), Collections.singletonList(parsed));
                        }
                    }
                }
            };
        }
    }
}
