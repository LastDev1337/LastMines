package ru.last.mines.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.last.mines.LastMines;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.cmd.argument.ArgumentStrings;
import ru.last.mines.managers.ActionManager;

public class ActionsRegister {
    public static void register(LastMines plugin, ActionManager actionManager) {
        try {
            Command<ExecuteContext> root = MenuCommands.getCommands();

            root.sub(new Command<ExecuteContext>("[tp_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("player"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [tp_mine] <mine> <player>");
                        String playerName = (String) args.getOrThrow("player", "Use: [tp_mine] <mine> <player>");
                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null) actionManager.teleportToMine(player, mine);
                    })
            );

            root.sub(new Command<ExecuteContext>("[reset_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [reset_mine] <mine>");
                        actionManager.resetMine(mine);
                    })
            );

            root.sub(new Command<ExecuteContext>("[delete_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [delete_mine] <mine>");
                        actionManager.deleteMine(mine);
                    })
            );

            root.sub(new Command<ExecuteContext>("[update_mine]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentStrings<>("opts"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [update_mine] <mine> [rarity] [next]");
                        String opts = args.containsKey("opts") ? (String) args.get("opts") : "";
                        actionManager.updateMine(mine, opts.split(" "));
                    })
            );

            root.sub(new Command<ExecuteContext>("[block_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("block"))
                    .argument(new ArgumentString<>("action"))
                    .argument(new ArgumentString<>("value"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [block_edit] <mine> <block> <action> <value>");
                        String blockStr = (String) args.getOrThrow("block", "");
                        String action = (String) args.getOrThrow("action", "");
                        String value = args.containsKey("value") ? (String) args.get("value") : "";
                        actionManager.editBlock(mine, blockStr, action, value);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[rarity_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("rarity"))
                    .argument(new ArgumentString<>("action"))
                    .argument(new ArgumentString<>("value"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [rarity_edit] <mine> <rarity> <action> <value>");
                        String rarityStr = (String) args.getOrThrow("rarity", "");
                        String action = (String) args.getOrThrow("action", "");
                        String value = args.containsKey("value") ? (String) args.get("value") : "";
                        actionManager.editRarity(mine, rarityStr, action, value);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[block_add]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mineId = (String) args.getOrThrow("mine", "Use: [block_add] <mine>");
                        Player player = ctx.getPlayer();
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item.getType() == Material.AIR) {
                            player.sendMessage("§cВозьмите блок в руку!");
                            return;
                        }
                        if (!item.getType().isBlock()) {
                            player.sendMessage("§cЭтот предмет не является блоком!");
                            return;
                        }
                        actionManager.editBlock(mineId, item.getType().name(), "chance", "+10");
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[holo_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("index"))
                    .argument(new ArgumentString<>("action"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [holo_edit] <mine> <index> <action>");
                        String indexStr = (String) args.getOrThrow("index", "");
                        String action = (String) args.getOrThrow("action", "");
                        try {
                            int idx = Integer.parseInt(indexStr);
                            actionManager.editHolo(mine, idx, action, "");
                            ctx.menu.refresh();
                        } catch (Exception ignored) {}
                    })
            );

            root.sub(new Command<ExecuteContext>("[chance_input]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("material"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [chance_input] <mine> <material>");
                        String mat = (String) args.getOrThrow("material", "");
                        ctx.menu.close();
                        LastMines.get().getChanceInputListener().startSession(ctx.getPlayer(), mine, mat);
                    })
            );

            root.sub(new Command<ExecuteContext>("[limit_input]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("material"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [limit_input] <mine> <material>");
                        String mat = (String) args.getOrThrow("material", "");
                        ctx.menu.close();
                        LastMines.get().getLimitInputListener().startSession(ctx.getPlayer(), mine, mat);
                    })
            );

            root.sub(new Command<ExecuteContext>("[open_block_settings]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("material"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "");
                        String mat = (String) args.getOrThrow("material", "");
                        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "lastmines gui " + mine + " block_settings " + ctx.getPlayer().getName() + " " + mat);
                    })
            );

            root.sub(new Command<ExecuteContext>("[open_drop_items]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("material"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "");
                        String mat = (String) args.getOrThrow("material", "");
                        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "lastmines gui " + mine + " block_drop_items " + ctx.getPlayer().getName() + " " + mat);
                    })
            );

            root.sub(new Command<ExecuteContext>("[drop_item_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("block"))
                    .argument(new ArgumentString<>("drop"))
                    .argument(new ArgumentString<>("action"))
                    .argument(new ArgumentString<>("value"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [drop_item_edit] <mine> <block> <drop> <action> <value>");
                        String block = (String) args.getOrThrow("block", "");
                        String drop = (String) args.getOrThrow("drop", "");
                        String action = (String) args.getOrThrow("action", "");
                        String value = args.containsKey("value") ? (String) args.get("value") : "";
                        actionManager.editDropItem(mine, block, drop, action, value);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[enchant_toggle]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [enchant_toggle] <mine>");
                        actionManager.toggleEnchantRequirement(mine);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[enchant_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("enchant"))
                    .argument(new ArgumentString<>("delta"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [enchant_edit] <mine> <enchant> <delta>");
                        String enchant = (String) args.getOrThrow("enchant", "");
                        String delta = (String) args.getOrThrow("delta", "");
                        actionManager.editEnchant(mine, enchant, delta);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[perm]")
                    .argument(new ArgumentString<>("action"))
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentStrings<>("args"))
                    .executor((ctx, args) -> {
                        String action = (String) args.getOrThrow("action", "");
                        String mineId = (String) args.getOrThrow("mine", "");
                        String value = args.containsKey("args") ? (String) args.get("args") : "";

                        if ("enable".equalsIgnoreCase(action)) {
                            actionManager.editPerm(mineId, action, value);
                            ctx.menu.refresh();
                        } else if ("value".equalsIgnoreCase(action)) {
                            ctx.menu.close();
                            ctx.getPlayer().sendMessage("§e▶ §fВведите новое право доступа в чат (например: lastmines.mine.default).\n§8(У вас есть 1 минута на ввод)");
                            LastMines.get().getPermInputListener().startSession(ctx.getPlayer(), mineId, "value");
                        }
                    })
            );

            root.sub(new Command<ExecuteContext>("[perm_message_add_input]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [perm_message_add_input] <mine>");
                        Player player = ctx.getPlayer();
                        ctx.menu.close();
                        LastMines.get().getTextInputListener().startSession(player,
                                "&e▶ &fВведите строку действия в чат, например:\n&f[message] &7Текст сообщения",
                                msg -> {
                                    if (msg.isBlank()) return;
                                    actionManager.addPermMessage(mine, msg);
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + mine + " permission_messages " + player.getName());
                                });
                    })
            );

            root.sub(new Command<ExecuteContext>("[perm_messages_page]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("delta"))
                    .executor((ctx, args) -> {
                        String deltaStr = (String) args.getOrThrow("delta", "0");
                        try {
                            ru.last.mines.gui.providers.PermMessagesProvider.changePage(ctx.getPlayer(), Integer.parseInt(deltaStr));
                        } catch (Exception ignored) {}
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[mine_toggle_stop]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [mine_toggle_stop] <mine>");
                        actionManager.toggleStop(mine);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[reset_time_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("delta"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [reset_time_edit] <mine> <delta>");
                        String delta = (String) args.getOrThrow("delta", "Use: [reset_time_edit] <mine> <delta>");
                        actionManager.editResetTime(mine, delta);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[block_drops]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("material"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [block_drops] <mine> <material>");
                        String mat = (String) args.getOrThrow("material", "");
                        ctx.menu.close();
                        LastMines.get().getBlockDropsListener().open(ctx.getPlayer(), mine, mat);
                    })
            );

            root.sub(new Command<ExecuteContext>("[block_add_input]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [block_add_input] <mine>");
                        Player player = ctx.getPlayer();
                        ctx.menu.close();
                        LastMines.get().getTextInputListener().startSession(player,
                                "&e▶ &fВведите название блока в чат (например: STONE).",
                                msg -> {
                                    Material mat = Material.matchMaterial(msg.trim());
                                    if (mat == null || !mat.isBlock()) {
                                        player.sendMessage("§cНеизвестный блок: " + msg);
                                        return;
                                    }
                                    actionManager.editBlock(mine, mat.name(), "chance", "10");
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + mine + " blocks " + player.getName());
                                });
                    })
            );

            root.sub(new Command<ExecuteContext>("[rarity_add_input]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [rarity_add_input] <mine>");
                        Player player = ctx.getPlayer();
                        ctx.menu.close();
                        LastMines.get().getTextInputListener().startSession(player,
                                "&e▶ &fВведите ID новой редкости в чат (например: epic).",
                                msg -> {
                                    String rarityId = msg.trim().replaceAll("\\s+", "_");
                                    if (rarityId.isEmpty()) return;
                                    actionManager.addRarity(mine, rarityId);
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + mine + " rarities " + player.getName());
                                });
                    })
            );

            root.sub(new Command<ExecuteContext>("[holo_line_add_input]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [holo_line_add_input] <mine>");
                        Player player = ctx.getPlayer();
                        ctx.menu.close();
                        LastMines.get().getTextInputListener().startSession(player,
                                "&e▶ &fВведите новую строку голограммы в чат.",
                                msg -> {
                                    if (msg.isBlank()) return;
                                    actionManager.addHoloLine(mine, msg);
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + mine + " holograms " + player.getName());
                                });
                    })
            );

            root.sub(new Command<ExecuteContext>("[action_add_input]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [action_add_input] <mine>");
                        Player player = ctx.getPlayer();
                        ctx.menu.close();
                        LastMines.get().getTextInputListener().startSession(player,
                                "&e▶ &fВведите строку действия в чат, например:\n&f[message] [update:15] &7Текст сообщения",
                                msg -> {
                                    if (msg.isBlank()) return;
                                    actionManager.addAction(mine, msg);
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lastmines gui " + mine + " actions " + player.getName());
                                });
                    })
            );

            root.sub(new Command<ExecuteContext>("[action_remove]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("index"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [action_remove] <mine> <index>");
                        String indexStr = (String) args.getOrThrow("index", "");
                        try {
                            actionManager.removeAction(mine, Integer.parseInt(indexStr));
                            ctx.menu.refresh();
                        } catch (Exception ignored) {}
                    })
            );

            root.sub(new Command<ExecuteContext>("[online_toggle]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [online_toggle] <mine>");
                        actionManager.toggleOnlineEnable(mine);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[online_min_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("delta"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [online_min_edit] <mine> <delta>");
                        String delta = (String) args.getOrThrow("delta", "");
                        actionManager.editOnlineMin(mine, delta);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[online_max_edit]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("delta"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [online_max_edit] <mine> <delta>");
                        String delta = (String) args.getOrThrow("delta", "");
                        actionManager.editOnlineMax(mine, delta);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[tp_toggle]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [tp_toggle] <mine>");
                        actionManager.toggleTeleportEnable(mine);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[tp_set]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [tp_set] <mine>");
                        actionManager.setTeleportToPlayer(mine, ctx.getPlayer());
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[holo_toggle]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [holo_toggle] <mine>");
                        actionManager.toggleHoloEnable(mine);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[holo_cycle_provider]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [holo_cycle_provider] <mine>");
                        actionManager.cycleHoloProvider(mine);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[holo_set_offset]")
                    .argument(new ArgumentString<>("mine"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [holo_set_offset] <mine>");
                        actionManager.setHoloOffsetToPlayer(mine, ctx.getPlayer());
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[mode_switch]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("mode"))
                    .executor((ctx, args) -> {
                        String mine = (String) args.getOrThrow("mine", "Use: [mode_switch] <mine> <mode>");
                        String mode = (String) args.getOrThrow("mode", "");
                        actionManager.switchMineMode(mine, mode);
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[all_blocks_page]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("delta"))
                    .executor((ctx, args) -> {
                        String deltaStr = (String) args.getOrThrow("delta", "0");
                        try {
                            ru.last.mines.gui.providers.AllBlocksProvider.changePage(ctx.getPlayer(), Integer.parseInt(deltaStr));
                        } catch (Exception ignored) {}
                        ctx.menu.refresh();
                    })
            );

            root.sub(new Command<ExecuteContext>("[actions_page]")
                    .argument(new ArgumentString<>("mine"))
                    .argument(new ArgumentString<>("delta"))
                    .executor((ctx, args) -> {
                        String deltaStr = (String) args.getOrThrow("delta", "0");
                        try {
                            ru.last.mines.gui.providers.ActionsProvider.changePage(ctx.getPlayer(), Integer.parseInt(deltaStr));
                        } catch (Exception ignored) {}
                        ctx.menu.refresh();
                    })
            );

        } catch (Exception ex) {
            plugin.getDebugger().error("Failed to register custom BMenu actions", ex);
        }
    }
}
