package ru.last.mines.gui.providers;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.last.mines.LastMines;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.Mine;
import ru.last.mines.models.MineBlock;

public class BlockSettingsProvider extends DefaultMenu {

    public BlockSettingsProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
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

        String material = args.get("MATERIAL");
        if (material == null) return;

        double chance = 0;
        int limit = 0;
        for (MineBlock mb : mine.getCurrentBlocks()) {
            if (mb.material().name().equalsIgnoreCase(material)) {
                chance = mb.chance();
                limit = mb.max();
                break;
            }
        }
        addArgument("CHANCE", String.valueOf(chance));
        addArgument("LIMIT", String.valueOf(limit));
    }
}
