package ru.last.mines.gui.providers;

import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MainProvider extends DefaultMenu {
    public MainProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
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

        addArgument("STOPPED", String.valueOf(mine.isStopped()));
        addArgument("ENABLE", String.valueOf(mine.isEnable()));
        addArgument("MODE", mine.getMode().name());
    }
}
