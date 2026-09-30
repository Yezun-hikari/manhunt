package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class Events implements Listener {
    private final Main plugin;

    public Events(Main plugin) {
        this.plugin = plugin;
    }

    // Chat prompt for objectives
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ObjectiveType type = ChatPrompt.getPrompt(player.getUniqueId());
        if (type != null) {
            event.setCancelled(true);
            String input = event.getMessage().toUpperCase().replace(" ", "_");

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (type == ObjectiveType.ITEM) {
                    try {
                        Material mat = Material.valueOf(input);
                        plugin.getGameManager().setObjective(new Objective(mat));
                        player.sendMessage("§aObjective set to: " + mat.name());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid item name. Try again or type CANCEL.");
                        if (input.equals("CANCEL")) ChatPrompt.removePrompt(player.getUniqueId());
                        return;
                    }
                } else if (type == ObjectiveType.MOB) {
                    try {
                        EntityType entity = EntityType.valueOf(input);
                        plugin.getGameManager().setObjective(new Objective(entity));
                        player.sendMessage("§aObjective set to: " + entity.name());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid mob name. Try again or type CANCEL.");
                        if (input.equals("CANCEL")) ChatPrompt.removePrompt(player.getUniqueId());
                        return;
                    }
                }
                ChatPrompt.removePrompt(player.getUniqueId());
                plugin.getGuiManager().openMainMenu(player);
            });
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() == GameState.PLAYING) {
            if (plugin.getGameManager().getRole(player) == Role.SPECTATOR) {
                // Late joiner or explicitly set to spectator
                player.setGameMode(GameMode.SPECTATOR);
                if (plugin.getWorldManager().getGameWorld() != null) {
                    player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                }
            }
        } else {
            // Lobby logic
            if (plugin.getGameManager().getRole(player) == Role.SPECTATOR && !plugin.getGameManager().getPlayerRoles().containsKey(player.getUniqueId())) {
                player.teleport(plugin.getLobbyLocation());
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player player = event.getEntity();
        Role role = plugin.getGameManager().getRole(player);

        if (role == Role.RUNNER) {
            // Permadeath for runners, handled on respawn
            event.setDeathMessage("§cRunner " + player.getName() + " has died!");
        } else if (role == Role.HUNTER) {
            event.setDeathMessage("§eHunter " + player.getName() + " has died! Cooldown started.");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player player = event.getPlayer();
        Role role = plugin.getGameManager().getRole(player);

        if (role == Role.RUNNER) {
            player.setGameMode(GameMode.SPECTATOR);
            event.setRespawnLocation(player.getLocation()); // Respawn where died to spectate
            plugin.getGameManager().checkWinCondition();
        } else if (role == Role.HUNTER) {
            event.setRespawnLocation(plugin.getLobbyLocation());
            player.setGameMode(GameMode.SPECTATOR); // Wait in lobby

            int cooldown = plugin.getConfig().getInt("cooldowns.hunter-respawn-seconds", 30);
            player.sendMessage("§cYou are on cooldown for " + cooldown + " seconds.");

            java.util.UUID playerId = player.getUniqueId();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getGameManager().getState() == GameState.PLAYING) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && p.isOnline()) {
                        // Respawn in world spawn
                        p.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                        p.setGameMode(GameMode.SURVIVAL);
                        plugin.getTracker().giveCompass(p);
                        p.sendMessage("§aYou have respawned!");
                    }
                }
            }, cooldown * 20L);
        }
    }

    // Tracker portal logging
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player player = event.getPlayer();
        if (plugin.getGameManager().getRole(player) == Role.RUNNER) {
            plugin.getTracker().setLastPortalLocation(player, event.getFrom());
        }
    }

    // Win condition check - Mob
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Objective obj = plugin.getGameManager().getObjective();

        if (obj != null && obj.getType() == ObjectiveType.MOB) {
            if (event.getEntity().getType() == obj.getTargetMob()) {
                if (event.getEntity().getKiller() != null) {
                    Player killer = event.getEntity().getKiller();
                    if (plugin.getGameManager().getRole(killer) == Role.RUNNER) {
                        plugin.getGameManager().endGame(Role.RUNNER);
                    }
                }
            }
        }
    }

    // Win condition check - Item pickup
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (plugin.getGameManager().getRole(player) == Role.RUNNER) {
            checkItemObjective(player, event.getItem().getItemStack());
        }
    }

    // Win condition check - Inventory click (e.g., crafting or chest)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (plugin.getGameManager().getRole(player) == Role.RUNNER) {
            ItemStack clicked = event.getCurrentItem();
            checkItemObjective(player, clicked);
        }
    }

    private void checkItemObjective(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Objective obj = plugin.getGameManager().getObjective();

        if (obj != null && obj.getType() == ObjectiveType.ITEM) {
            if (item.getType() == obj.getTargetItem()) {
                // Must wrap in task so we don't end game mid-event
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getGameManager().endGame(Role.RUNNER);
                });
            }
        }
    }
}
