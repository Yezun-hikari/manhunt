package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.Arrays;
import java.util.List;

public class GUIManager implements Listener {
    private final Main plugin;
    private final String MAIN_TITLE = "Manhunt Admin";
    private final String ROLES_TITLE = "Assign Roles";
    private final String OBJ_TYPE_TITLE = "Select Objective Type";
    private final String OBJ_ITEM_TITLE = "Select Item Objective";
    private final String OBJ_MOB_TITLE = "Select Mob Objective";
    private final String CONFIG_TITLE = "Game Config";
    private final String PLAYER_ROLE_TITLE = "Role: ";

    public GUIManager(Main plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        GUIHolder holder = new GUIHolder(MAIN_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 27, MAIN_TITLE);
        holder.setInventory(inv);

        inv.setItem(10, createGuiItem(Material.PLAYER_HEAD, "§eAssign Roles", "Click to assign Hunter/Runner"));
        inv.setItem(12, createGuiItem(Material.TARGET, "§eSet Objective", "Click to set Item or Mob objective"));
        inv.setItem(14, createGuiItem(Material.REPEATER, "§eConfiguration", "Click to set Head-start or Seed"));
        inv.setItem(16, createGuiItem(Material.EMERALD_BLOCK, "§aStart Game", "Click to generate world and start"));

        player.openInventory(inv);
    }

    public void openRolesMenu(Player player) {
        GUIHolder holder = new GUIHolder(ROLES_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 54, ROLES_TITLE);
        holder.setInventory(inv);

        // Back button
        inv.setItem(45, createGuiItem(Material.OAK_DOOR, "§cBack"));

        // Mass assign hunters
        inv.setItem(49, createGuiItem(Material.IRON_SWORD, "§cMass Assign Hunters", "Makes all remaining default/spectator", "players into Hunters."));

        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break; // limit to prevent overflow in simple GUI

            Role r = plugin.getGameManager().getRole(p);

            // Player head
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);
                String roleName = r == Role.RUNNER ? "§aRunner" : (r == Role.HUNTER ? "§cHunter" : "§7Spectator");
                meta.setDisplayName("§e" + p.getName());
                meta.setLore(Arrays.asList("Current Role: " + roleName, "Click to change"));

                if (r == Role.RUNNER || r == Role.HUNTER) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);

            // Colored glass pane below head for immediate visual recognition
            Material paneMat = Material.GRAY_STAINED_GLASS_PANE;
            if (r == Role.RUNNER) paneMat = Material.LIME_STAINED_GLASS_PANE;
            else if (r == Role.HUNTER) paneMat = Material.RED_STAINED_GLASS_PANE;

            // We place the pane in the slot right below the head (slot + 9)
            // But we must be careful with grid layout. So we just fill alternating rows or let's just make the item itself glowing.
            // Wait, the prompt said: "Löse das z.B., indem du farbige Glasscheiben hinter/unter die Köpfe packst, den Köpfen einen Glow/Enchantment-Effekt gibst".
            // We added the glow effect above. Let's place the glass pane immediately after the head in the inventory.
            // Or better yet, heads on even slots, panes on odd slots? No, heads on row 0, panes on row 1.

            // Let's restructure the loop to place heads on rows 0, 2 and panes on rows 1, 3.
            int row = (slot / 9);
            int col = slot % 9;

            // Place head
            inv.setItem(row * 18 + col, head);
            // Place pane below
            inv.setItem(row * 18 + col + 9, createGuiItem(paneMat, "§7Status Indicator"));

            slot++;
            if (slot >= 18) break; // Can only fit 18 players with this layout in 54-size GUI (rows 0,1 and 2,3)
        }
        player.openInventory(inv);
    }

    public void openPlayerRoleMenu(Player player, String targetName) {
        String title = PLAYER_ROLE_TITLE + targetName;
        // Keep it under 32 chars
        if (title.length() > 32) title = title.substring(0, 32);

        GUIHolder holder = new GUIHolder(title);
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);

        inv.setItem(11, createGuiItem(Material.DIAMOND_BOOTS, "§aSet as Runner"));
        inv.setItem(13, createGuiItem(Material.IRON_SWORD, "§cSet as Hunter"));
        inv.setItem(15, createGuiItem(Material.ENDER_EYE, "§7Set as Spectator"));

        inv.setItem(18, createGuiItem(Material.OAK_DOOR, "§cBack"));
        player.openInventory(inv);
    }

    public void openConfigMenu(Player player) {
        GUIHolder holder = new GUIHolder(CONFIG_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 27, CONFIG_TITLE);
        holder.setInventory(inv);

        int currentHeadStart = plugin.getGameManager().getHeadStartSeconds();
        Long currentSeed = plugin.getGameManager().getCustomSeed();

        inv.setItem(11, createGuiItem(Material.CLOCK, "§bHead-start: " + currentHeadStart + "s", "Left-Click: +10s", "Right-Click: -10s"));

        String seedText = currentSeed == null ? "Random" : String.valueOf(currentSeed);
        inv.setItem(15, createGuiItem(Material.GRASS_BLOCK, "§bTemp Seed: " + seedText, "Click to set a custom seed in chat"));

        inv.setItem(18, createGuiItem(Material.OAK_DOOR, "§cBack"));

        player.openInventory(inv);
    }

    public void openObjTypeMenu(Player player) {
        GUIHolder holder = new GUIHolder(OBJ_TYPE_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 27, OBJ_TYPE_TITLE);
        holder.setInventory(inv);

        inv.setItem(11, createGuiItem(Material.DIAMOND, "§bItem Objective"));
        inv.setItem(15, createGuiItem(Material.ZOMBIE_HEAD, "§cMob Objective"));

        inv.setItem(18, createGuiItem(Material.OAK_DOOR, "§cBack"));
        player.openInventory(inv);
    }

    public void openObjItemMenu(Player player) {
        GUIHolder holder = new GUIHolder(OBJ_ITEM_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 27, OBJ_ITEM_TITLE);
        holder.setInventory(inv);

        inv.setItem(10, createGuiItem(Material.DIAMOND, "§bDiamond"));
        inv.setItem(11, createGuiItem(Material.NETHERITE_INGOT, "§bNetherite Ingot"));
        inv.setItem(12, createGuiItem(Material.DRAGON_EGG, "§bDragon Egg"));
        inv.setItem(13, createGuiItem(Material.BEACON, "§bBeacon"));
        inv.setItem(14, createGuiItem(Material.NETHER_STAR, "§bNether Star"));
        inv.setItem(15, createGuiItem(Material.ENDER_EYE, "§bEye of Ender"));
        inv.setItem(16, createGuiItem(Material.BLAZE_ROD, "§bBlaze Rod"));

        inv.setItem(18, createGuiItem(Material.OAK_DOOR, "§cBack"));
        player.openInventory(inv);
    }

    public void openObjMobMenu(Player player) {
        GUIHolder holder = new GUIHolder(OBJ_MOB_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 27, OBJ_MOB_TITLE);
        holder.setInventory(inv);

        inv.setItem(11, createGuiItem(Material.DRAGON_HEAD, "§cEnder Dragon"));
        inv.setItem(13, createGuiItem(Material.WITHER_SKELETON_SKULL, "§cWither"));
        inv.setItem(15, createGuiItem(Material.ZOMBIE_HEAD, "§cZombie")); // Just for testing

        inv.setItem(18, createGuiItem(Material.OAK_DOOR, "§cBack"));
        player.openInventory(inv);
    }

    public void openChatPromptMode(Player player, ObjectiveType type) {
        player.closeInventory();
        player.sendMessage("§ePlease type the seed in chat (or CANCEL):");
        ChatPrompt.addPrompt(player.getUniqueId(), type);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof GUIHolder)) return;

        GUIHolder holder = (GUIHolder) inv.getHolder();
        String title = holder.getGuiName();

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Back button check
        if (clicked.getType() == Material.OAK_DOOR && clicked.getItemMeta().getDisplayName().contains("Back")) {
            openMainMenu(player);
            return;
        }

        if (title.equals(MAIN_TITLE)) {
            if (clicked.getType() == Material.PLAYER_HEAD) openRolesMenu(player);
            else if (clicked.getType() == Material.TARGET) openObjTypeMenu(player);
            else if (clicked.getType() == Material.REPEATER) openConfigMenu(player);
            else if (clicked.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                plugin.getGameManager().startGame();
            }
        } else if (title.equals(CONFIG_TITLE)) {
            if (clicked.getType() == Material.CLOCK) {
                int current = plugin.getGameManager().getHeadStartSeconds();
                if (event.isLeftClick()) {
                    plugin.getGameManager().setHeadStartSeconds(current + 10);
                } else if (event.isRightClick()) {
                    if (current >= 10) plugin.getGameManager().setHeadStartSeconds(current - 10);
                }
                openConfigMenu(player);
            } else if (clicked.getType() == Material.GRASS_BLOCK) {
                openChatPromptMode(player, ObjectiveType.SEED);
            }
        } else if (title.equals(ROLES_TITLE)) {
            if (clicked.getType() == Material.IRON_SWORD && clicked.getItemMeta().getDisplayName().contains("Mass Assign")) {
                int count = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getGameManager().getRole(p) != Role.RUNNER) {
                        plugin.getGameManager().setRole(p, Role.HUNTER);
                        count++;
                    }
                }
                player.sendMessage("§aAssigned " + count + " players to Hunter.");
                openRolesMenu(player);
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                String targetName = clicked.getItemMeta().getDisplayName().substring(2); // Remove color codes
                openPlayerRoleMenu(player, targetName);
            }
        } else if (title.startsWith(PLAYER_ROLE_TITLE)) {
            String targetName = title.substring(PLAYER_ROLE_TITLE.length());
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null) {
                if (clicked.getType() == Material.DIAMOND_BOOTS) {
                    plugin.getGameManager().setRole(target, Role.RUNNER);
                    player.sendMessage("§aSet " + targetName + " as Runner.");
                } else if (clicked.getType() == Material.IRON_SWORD) {
                    plugin.getGameManager().setRole(target, Role.HUNTER);
                    player.sendMessage("§cSet " + targetName + " as Hunter.");
                } else if (clicked.getType() == Material.ENDER_EYE) {
                    plugin.getGameManager().setRole(target, Role.SPECTATOR);
                    player.sendMessage("§7Set " + targetName + " as Spectator.");
                }
            }
            openRolesMenu(player);
        } else if (title.equals(OBJ_TYPE_TITLE)) {
            if (clicked.getType() == Material.DIAMOND) openObjItemMenu(player);
            else if (clicked.getType() == Material.ZOMBIE_HEAD) openObjMobMenu(player);
        } else if (title.equals(OBJ_ITEM_TITLE)) {
            plugin.getGameManager().setObjective(new Objective(clicked.getType()));
            player.sendMessage("§aObjective set to: " + clicked.getType().name());
            openMainMenu(player);
        } else if (title.equals(OBJ_MOB_TITLE)) {
            EntityType type = null;
            if (clicked.getType() == Material.DRAGON_HEAD) type = EntityType.ENDER_DRAGON;
            else if (clicked.getType() == Material.WITHER_SKELETON_SKULL) type = EntityType.WITHER;
            else if (clicked.getType() == Material.ZOMBIE_HEAD) type = EntityType.ZOMBIE;

            if (type != null) {
                plugin.getGameManager().setObjective(new Objective(type));
                player.sendMessage("§aObjective set to: " + type.name());
                openMainMenu(player);
            }
        }
    }
}
