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
            if (slot >= 45) break;

            Role r = plugin.getGameManager().getRole(p);

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

            Material paneMat = Material.GRAY_STAINED_GLASS_PANE;
            if (r == Role.RUNNER) paneMat = Material.LIME_STAINED_GLASS_PANE;
            else if (r == Role.HUNTER) paneMat = Material.RED_STAINED_GLASS_PANE;

            int row = (slot / 9);
            int col = slot % 9;

            inv.setItem(row * 18 + col, head);
            inv.setItem(row * 18 + col + 9, createGuiItem(paneMat, "§7Status Indicator"));

            slot++;
            if (slot >= 18) break;
        }
        player.openInventory(inv);
    }

    public void openPlayerRoleMenu(Player player, String targetName) {
        String title = PLAYER_ROLE_TITLE + targetName;
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

        inv.setItem(4, createGuiItem(Material.BARRIER, "§cReset Timer", "Resets to default"));

        inv.setItem(11, createGuiItem(Material.RED_DYE, "§c-1 Minute", "Subtracts 60 seconds"));
        inv.setItem(12, createGuiItem(Material.ORANGE_DYE, "§6-10 Seconds", "Subtracts 10 seconds"));
        inv.setItem(13, createGuiItem(Material.CLOCK, "§bHead-start: " + currentHeadStart + "s", "Current Timer"));
        inv.setItem(14, createGuiItem(Material.YELLOW_DYE, "§e+10 Seconds", "Adds 10 seconds"));
        inv.setItem(15, createGuiItem(Material.LIME_DYE, "§a+1 Minute", "Adds 60 seconds"));

        String seedText = currentSeed == null ? "Random" : String.valueOf(currentSeed);
        inv.setItem(22, createGuiItem(Material.GRASS_BLOCK, "§bTemp Seed: " + seedText, "Click to set a custom seed in chat"));

        inv.setItem(18, createGuiItem(Material.OAK_DOOR, "§cBack"));

        player.openInventory(inv);
    }

    public void openObjTypeMenu(Player player) {
        GUIHolder holder = new GUIHolder(OBJ_TYPE_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 27, OBJ_TYPE_TITLE);
        holder.setInventory(inv);

        Objective obj = plugin.getGameManager().getObjective();
        boolean isItem = obj != null && obj.getType() == ObjectiveType.ITEM;
        boolean isMob = obj != null && obj.getType() == ObjectiveType.MOB;

        ItemStack itemOpt = createGuiItem(Material.DIAMOND, "§bItem Objective");
        if (isItem) addGlow(itemOpt);

        ItemStack mobOpt = createGuiItem(Material.ZOMBIE_HEAD, "§cMob Objective");
        if (isMob) addGlow(mobOpt);

        inv.setItem(11, itemOpt);
        inv.setItem(15, mobOpt);

        inv.setItem(18, createGuiItem(Material.OAK_DOOR, "§cBack"));
        player.openInventory(inv);
    }

    public void openObjItemMenu(Player player) {
        GUIHolder holder = new GUIHolder(OBJ_ITEM_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 54, OBJ_ITEM_TITLE);
        holder.setInventory(inv);
        Objective obj = plugin.getGameManager().getObjective();

        Material[] items = {
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.SLIME_BALL, Material.EMERALD, Material.BRAIN_CORAL_BLOCK,
            Material.DIAMOND, Material.ENDER_PEARL, Material.GOLDEN_CARROT, Material.TRIDENT,
            Material.NETHERITE_INGOT, Material.TOTEM_OF_UNDYING, Material.ELYTRA, Material.BEACON, Material.ENCHANTED_GOLDEN_APPLE
        };

        int slot = 0;
        for (Material m : items) {
            String diff = "EASY";
            if (slot > 4 && slot <= 8) diff = "MEDIUM";
            if (slot > 8) diff = "HARD";

            addItemToMenu(inv, slot, m, diff, obj);
            slot++;
            if (slot == 9) slot = 18; // jump to second item row
        }

        inv.setItem(53, createGuiItem(Material.OAK_DOOR, "§cBack"));
        player.openInventory(inv);
    }

    public void openObjMobMenu(Player player) {
        GUIHolder holder = new GUIHolder(OBJ_MOB_TITLE);
        Inventory inv = Bukkit.createInventory(holder, 54, OBJ_MOB_TITLE);
        holder.setInventory(inv);
        Objective obj = plugin.getGameManager().getObjective();

        EntityType[] mobs = {
            EntityType.COW, EntityType.BEE, EntityType.GOAT, EntityType.ARMADILLO, EntityType.PILLAGER,
            EntityType.ENDERMAN, EntityType.WITCH, EntityType.SHULKER, EntityType.SLIME, EntityType.ENDERMITE,
            EntityType.WITHER_SKELETON, EntityType.WITHER, EntityType.ENDER_DRAGON
        };

        int slot = 0;
        for (EntityType m : mobs) {
            String diff = "EASY";
            if (slot > 4 && slot <= 9) diff = "MEDIUM";
            if (slot > 9) diff = "HARD";

            addMobToMenu(inv, slot, m, diff, obj);
            slot++;
            if (slot == 9) slot = 18; // jump to second mob row
        }

        inv.setItem(53, createGuiItem(Material.OAK_DOOR, "§cBack"));
        player.openInventory(inv);
    }

    private void addItemToMenu(Inventory inv, int slot, Material m, String diff, Objective obj) {
        int req = obj != null ? obj.getItemRequirement(m) : 0;
        ItemStack item = createGuiItem(m, "§b" + m.name(), "§7Difficulty: " + diff, "§eRequired: " + req, "§7Left-Click: +1", "§7Right-Click: -1");
        if (req > 0) addGlow(item);
        inv.setItem(slot, item);
        inv.setItem(slot + 9, createGuiItem(req > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, "§7Status Indicator"));
    }

    private void addMobToMenu(Inventory inv, int slot, EntityType m, String diff, Objective obj) {
        Material display = Material.ZOMBIE_HEAD;
        if (m == EntityType.COW) display = Material.BEEF;
        else if (m == EntityType.BEE) display = Material.HONEYCOMB;
        else if (m == EntityType.GOAT) display = Material.GOAT_HORN;
        else if (m == EntityType.ARMADILLO) display = Material.ARMADILLO_SCUTE;
        else if (m == EntityType.PILLAGER) display = Material.CROSSBOW;
        else if (m == EntityType.ENDERMAN) display = Material.ENDER_PEARL;
        else if (m == EntityType.WITCH) display = Material.POTION;
        else if (m == EntityType.SHULKER) display = Material.SHULKER_SHELL;
        else if (m == EntityType.SLIME) display = Material.SLIME_BALL;
        else if (m == EntityType.ENDERMITE) display = Material.END_STONE;
        else if (m == EntityType.WITHER_SKELETON) display = Material.WITHER_SKELETON_SKULL;
        else if (m == EntityType.WITHER) display = Material.NETHER_STAR;
        else if (m == EntityType.ENDER_DRAGON) display = Material.DRAGON_HEAD;
        else display = Material.PAPER;

        int req = obj != null ? obj.getMobRequirement(m) : 0;
        ItemStack item = createGuiItem(display, "§c" + m.name(), "§7Difficulty: " + diff, "§eRequired: " + req, "§7Left-Click: +1", "§7Right-Click: -1");
        if (req > 0) addGlow(item);
        inv.setItem(slot, item);
        inv.setItem(slot + 9, createGuiItem(req > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, "§7Status Indicator"));
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

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
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
                plugin.getGameManager().startGame(player);
            }
        } else if (title.equals(CONFIG_TITLE)) {
            int current = plugin.getGameManager().getHeadStartSeconds();
            if (clicked.getType() == Material.RED_DYE) plugin.getGameManager().setHeadStartSeconds(current - 60);
            else if (clicked.getType() == Material.ORANGE_DYE) plugin.getGameManager().setHeadStartSeconds(current - 10);
            else if (clicked.getType() == Material.YELLOW_DYE) plugin.getGameManager().setHeadStartSeconds(current + 10);
            else if (clicked.getType() == Material.LIME_DYE) plugin.getGameManager().setHeadStartSeconds(current + 60);
            else if (clicked.getType() == Material.BARRIER) plugin.getGameManager().setHeadStartSeconds(plugin.getConfig().getInt("default-headstart", 120));
            else if (clicked.getType() == Material.GRASS_BLOCK) openChatPromptMode(player, ObjectiveType.SEED);
            openConfigMenu(player);
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
                plugin.updateScoreboard();
                openRolesMenu(player);
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                String targetName = clicked.getItemMeta().getDisplayName().substring(2);
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
                plugin.updateScoreboard();
            }
            openRolesMenu(player);
        } else if (title.equals(OBJ_TYPE_TITLE)) {
            if (clicked.getType() == Material.DIAMOND) {
                if (plugin.getGameManager().getObjective() == null) plugin.getGameManager().setObjective(new Objective());
                plugin.getGameManager().getObjective().setType(ObjectiveType.ITEM);
                openObjItemMenu(player);
            }
            else if (clicked.getType() == Material.ZOMBIE_HEAD) {
                if (plugin.getGameManager().getObjective() == null) plugin.getGameManager().setObjective(new Objective());
                plugin.getGameManager().getObjective().setType(ObjectiveType.MOB);
                openObjMobMenu(player);
            }
        } else if (title.equals(OBJ_ITEM_TITLE)) {
            Material m = clicked.getType();
            if (m != Material.LIME_STAINED_GLASS_PANE && m != Material.GRAY_STAINED_GLASS_PANE && m != Material.OAK_DOOR) {
                Objective obj = plugin.getGameManager().getObjective();
                if (obj != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                    // Extract material name from display name (stripping colors)
                    String name = clicked.getItemMeta().getDisplayName().substring(2);
                    try {
                        Material target = Material.valueOf(name);
                        int current = obj.getItemRequirement(target);
                        if (event.isLeftClick()) obj.setItemRequirement(target, current + 1);
                        else if (event.isRightClick()) obj.setItemRequirement(target, Math.max(0, current - 1));
                    } catch (Exception e) {}
                    openObjItemMenu(player);
                }
            }
        } else if (title.equals(OBJ_MOB_TITLE)) {
            Material m = clicked.getType();
            if (m != Material.LIME_STAINED_GLASS_PANE && m != Material.GRAY_STAINED_GLASS_PANE && m != Material.OAK_DOOR) {
                Objective obj = plugin.getGameManager().getObjective();
                if (obj != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                    String name = clicked.getItemMeta().getDisplayName().substring(2);
                    try {
                        EntityType target = EntityType.valueOf(name);
                        int current = obj.getMobRequirement(target);
                        if (event.isLeftClick()) obj.setMobRequirement(target, current + 1);
                        else if (event.isRightClick()) obj.setMobRequirement(target, Math.max(0, current - 1));
                    } catch (Exception e) {}
                    openObjMobMenu(player);
                }
            }
        }
    }
}
