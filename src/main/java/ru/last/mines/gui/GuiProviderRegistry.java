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
        BMenu.menuLoader().codecRegistry().register("lastmines:block_settings", BlockSettingsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:reset_time", ResetTimeProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:delete_confirm", DeleteConfirmProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:block_drop_items", BlockDropItemsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:block_drop_item", BlockDropItemProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:enchants", EnchantsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:online_requirements", OnlineRequirementsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:teleport_settings", TeleportSettingsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:hologram_settings", HologramSettingsProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:all_blocks", AllBlocksProvider::new);
        BMenu.menuLoader().codecRegistry().register("lastmines:permission_messages", PermMessagesProvider::new);
    }
}
