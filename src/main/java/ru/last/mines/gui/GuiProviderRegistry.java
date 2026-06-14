package ru.last.mines.gui;

import dev.by1337.bmenu.BMenu;
import ru.last.mines.gui.providers.*;

public class GuiProviderRegistry {
    
    public static void register() {
        BMenu.menuLoader().codecRegistry().register("lastmines:main", MainProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:blocks", BlocksProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:rarities", RaritiesProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:holograms", HologramsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:actions", ActionsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:permissions", PermissionsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:online", OnlineProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:all_mines", AllMinesProvider::new);
    }
}
