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

import java.util.Arrays;

public class GUIManager implements Listener {
    private final Main plugin;
    private final String MAIN_TITLE = "Manhunt Admin";
    private final String ROLES_TITLE = "Assign Roles";
    private final String OBJ_TYPE_TITLE = "Select Objective Type";
    private final String OBJ_ITEM_TITLE = "Select Objective Item";
    private final String OBJ_MOB_TITLE = "Select Objective Mob";
    private final String PLAYER_ROLE_TITLE = "Role for: ";

    public GUIManager(Main plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        GUIHolder holder = new GUIHolder(MAIN_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 9, MAIN_TITLE);
        holder.setInventory(inv);

        inv.setItem(2, createGuiItem(Material.PLAYER_HEAD, "§eAssign Roles", "Click to assign Hunter/Runner"));
        inv.setItem(4, createGuiItem(Material.TARGET, "§eSet Objective", "Click to set Item or Mob objective"));
        inv.setItem(6, createGuiItem(Material.EMERALD_BLOCK, "§aStart Game", "Click to generate world and start"));

        player.openInventory(inv);
    }

    public void openRolesMenu(Player player) {
        // Simplified: list all online players
        GUIHolder holder = new GUIHolder(ROLES_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 54, ROLES_TITLE);
        holder.setInventory(inv);

        for (Player p : Bukkit.getOnlinePlayers()) {
            Role r = plugin.getGameManager().getRole(p);
            String roleName = r == Role.RUNNER ? "§aRunner" : (r == Role.HUNTER ? "§cHunter" : "§7Spectator");
            inv.addItem(createGuiItem(Material.PLAYER_HEAD, "§e" + p.getName(), "Current Role: " + roleName));
        }
        player.openInventory(inv);
    }

    public void openPlayerRoleMenu(Player player, String targetName) {
        String title = PLAYER_ROLE_TITLE + targetName;
        GUIHolder holder = new GUIHolder(title);
        Inventory inv = Bukkit.createInventory(holder, 9, title);
        holder.setInventory(inv);

        inv.setItem(2, createGuiItem(Material.DIAMOND_BOOTS, "§aSet as Runner"));
        inv.setItem(4, createGuiItem(Material.IRON_SWORD, "§cSet as Hunter"));
        inv.setItem(6, createGuiItem(Material.ENDER_EYE, "§7Set as Spectator"));
        player.openInventory(inv);
    }

    public void openObjTypeMenu(Player player) {
        GUIHolder holder = new GUIHolder(OBJ_TYPE_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 9, OBJ_TYPE_TITLE);
        holder.setInventory(inv);

        inv.setItem(3, createGuiItem(Material.DIAMOND, "§bItem Objective"));
        inv.setItem(5, createGuiItem(Material.ZOMBIE_HEAD, "§cMob Objective"));
        player.openInventory(inv);
    }

    // Simplified selection: Just giving a few options, realistically would be a paginated GUI or chat prompt.
    // For this implementation, we give a few common items/mobs. The prompt said "jedes item / mob angeben können",
    // so we will actually ask the admin to type it in chat after clicking a button.
    public void openChatPromptMode(Player player, ObjectiveType type) {
        player.closeInventory();
        player.sendMessage("§ePlease type the " + type.name().toLowerCase() + " name in chat (e.g. DIAMOND or ENDER_DRAGON):");
        // We can track this using metadata or a map in GUIManager.
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

        if (title.equals(MAIN_TITLE)) {
            if (clicked.getType() == Material.PLAYER_HEAD) openRolesMenu(player);
            else if (clicked.getType() == Material.TARGET) openObjTypeMenu(player);
            else if (clicked.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                plugin.getGameManager().startGame();
            }
        } else if (title.equals(ROLES_TITLE)) {
            String targetName = clicked.getItemMeta().getDisplayName().substring(2); // Remove color codes
            openPlayerRoleMenu(player, targetName);
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
            if (clicked.getType() == Material.DIAMOND) openChatPromptMode(player, ObjectiveType.ITEM);
            else if (clicked.getType() == Material.ZOMBIE_HEAD) openChatPromptMode(player, ObjectiveType.MOB);
        }
    }
}
