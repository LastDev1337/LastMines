package ru.last.mines.gui.providers;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.DefaultMenu;
import dev.by1337.bmenu.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.models.DropItem;
import ru.last.mines.models.Mine;
import ru.last.mines.models.MineBlock;

public class BlockDropItemProvider extends DefaultMenu {

    public BlockDropItemProvider(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
    }

    @Override
    protected void generate() {
        String mineId = args.get("MINE_ID");
        String material = args.get("MATERIAL");
        String dropMaterial = args.get("DROP_MATERIAL");
        if (mineId == null || material == null || dropMaterial == null) return;

        Mine mine = LastMinesProvider.getApi().getMine(mineId);
        if (mine == null) return;

        Material mat = Material.matchMaterial(material);
        Material dropMat = Material.matchMaterial(dropMaterial);

        for (MineBlock mb : mine.getCurrentBlocks()) {
            if (mb.material() != mat) continue;
            for (DropItem drop : mb.drops()) {
                if (drop.material() == dropMat) {
                    addArgument("CHANCE", String.valueOf(drop.chance()));
                    addArgument("FORTUNE", String.valueOf(drop.fortune()));
                    return;
                }
            }
        }
    }
}
