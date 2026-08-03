package ru.last.mines.gui.providers;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;

public class EnchantsProvider extends DefaultMenu {

    public EnchantsProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
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

        addArgument("ENCHANT_ENABLE", String.valueOf(mine.isEnchantEnable()));
        addArgument("EFFICIENCY_LVL", String.valueOf(mine.getRequiredEnchants().getOrDefault("efficiency", 0)));
        addArgument("FORTUNE_LVL", String.valueOf(mine.getRequiredEnchants().getOrDefault("fortune", 0)));
        addArgument("SILK_TOUCH_LVL", String.valueOf(mine.getRequiredEnchants().getOrDefault("silk_touch", 0)));
        addArgument("UNBREAKING_LVL", String.valueOf(mine.getRequiredEnchants().getOrDefault("unbreaking", 0)));
    }
}
