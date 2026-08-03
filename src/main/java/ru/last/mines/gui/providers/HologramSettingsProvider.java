package ru.last.mines.gui.providers;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;

public class HologramSettingsProvider extends DefaultMenu {

    public HologramSettingsProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
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

        addArgument("HOLO_ENABLE", String.valueOf(mine.isHoloEnable()));
        addArgument("HOLO_PROVIDER", mine.getHoloProvider());
        Location pos = mine.getHoloOffset();
        addArgument("HOLO_POS", pos == null ? "не установлена" : String.format("%.0f;%.0f;%.0f", pos.getX(), pos.getY(), pos.getZ()));
    }
}
