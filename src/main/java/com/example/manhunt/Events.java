package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class Events implements Listener {
    private final Main plugin;

    public Events(Main plugin) {
        this.plugin = plugin;
    }

    // Chat prompt for seeds
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ObjectiveType type = ChatPrompt.getPrompt(player.getUniqueId());
        if (type != null) {
            event.setCancelled(true);
            String input = event.getMessage().toUpperCase().replace(" ", "_");

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (type == ObjectiveType.SEED) {
                    try {
                        long seed = Long.parseLong(event.getMessage());
                        plugin.getGameManager().setCustomSeed(seed);
                        player.sendMessage("§aTemporary seed set to: " + seed);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid number. Try again or type CANCEL.");
                        if (input.equals("CANCEL")) ChatPrompt.removePrompt(player.getUniqueId());
                        return;
                    }
                    ChatPrompt.removePrompt(player.getUniqueId());
                    plugin.getGuiManager().openConfigMenu(player); // Go back to config
                    return;
                }
            });
        }
    }

    // Lobby Protection

    private boolean isInLobby(Player player) {
        if (plugin.getLobbyLocation().getWorld() != null) {
            return player.getWorld().equals(plugin.getLobbyLocation().getWorld());
        }
        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInLobby(event.getPlayer()) && plugin.getGameManager().getState() != GameState.PLAYING) {
            if (!event.getPlayer().hasPermission("manhunt.admin") || event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInLobby(event.getPlayer()) && plugin.getGameManager().getState() != GameState.PLAYING) {
            if (!event.getPlayer().hasPermission("manhunt.admin") || event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInLobby(player) && plugin.getGameManager().getState() != GameState.PLAYING) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInLobby(player) && plugin.getGameManager().getState() != GameState.PLAYING) {
                event.setCancelled(true);
            }
        }

        // Check for hunter damaging runner
        if (plugin.getGameManager().getState() == GameState.PLAYING) {
            if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                org.bukkit.event.entity.EntityDamageByEntityEvent damageEvent = (org.bukkit.event.entity.EntityDamageByEntityEvent) event;
                if (damageEvent.getEntity() instanceof Player) {
                    Player damagerPlayer = null;
                    if (damageEvent.getDamager() instanceof Player) {
                        damagerPlayer = (Player) damageEvent.getDamager();
                    } else if (damageEvent.getDamager() instanceof org.bukkit.entity.Projectile) {
                        org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) damageEvent.getDamager();
                        if (projectile.getShooter() instanceof Player) {
                            damagerPlayer = (Player) projectile.getShooter();
                        }
                    }

                    if (damagerPlayer != null) {
                        if (plugin.getGameManager().getRole(damagerPlayer) == Role.HUNTER && plugin.getGameManager().getRole((Player) damageEvent.getEntity()) == Role.RUNNER) {
                            plugin.getGameManager().addDamageDealingHunter(damagerPlayer.getUniqueId());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (plugin.getLobbyLocation().getWorld() != null) {
            if (event.getEntity().getWorld().equals(plugin.getLobbyLocation().getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().updateScoreboard();

        if (plugin.getGameManager().getState() == GameState.PLAYING) {
            if (plugin.getGameManager().getRole(player) == Role.SPECTATOR) {
                // Late joiner or explicitly set to spectator
                player.setGameMode(GameMode.SPECTATOR);
                if (plugin.getWorldManager().getGameWorld() != null) {
                    player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                }
            } else if (plugin.getGameManager().getRole(player) == Role.HUNTER) {
                // Check if they have a cooldown still running
                if (plugin.getGameManager().hasCooldown(player.getUniqueId())) {
                    player.teleport(plugin.getLobbyLocation());
                    player.setGameMode(GameMode.SPECTATOR);
                } else if (!plugin.getGameManager().areHuntersReleased()) {
                    player.teleport(plugin.getLobbyLocation());
                    player.setGameMode(GameMode.SPECTATOR);
                } else if (isInLobby(player)) {
                    // Let's teleport them back to game world
                    player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                    player.setGameMode(GameMode.SURVIVAL);
                    plugin.getTracker().giveCompass(player);
                }
            }
        } else {
            // Lobby logic
            if (plugin.getGameManager().getRole(player) == Role.SPECTATOR && !plugin.getGameManager().getPlayerRoles().containsKey(player.getUniqueId())) {
                player.teleport(plugin.getLobbyLocation());
                player.setGameMode(GameMode.SURVIVAL);
            }

            if (plugin.getGameManager().getRecentWinners().contains(player.getUniqueId())) {
                plugin.getGameManager().giveCrown(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getGameManager().getState() == GameState.PLAYING) {
            Player player = event.getPlayer();
            Role role = plugin.getGameManager().getRole(player);
            if (role == Role.RUNNER) {
                // A runner left, let's delay 1 tick and check win condition
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getGameManager().checkWinCondition();
                });
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player player = event.getEntity();
        Role role = plugin.getGameManager().getRole(player);

        // Play explosion sound for the dying player
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Keep crown from dropping
        event.getDrops().removeIf(item -> isCrown(item));

        if (role == Role.RUNNER) {
            // Permadeath for runners, handled on respawn, but we also check win condition here
            event.setDeathMessage("§cRunner " + player.getName() + " has died!");
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getGameManager().checkWinCondition();
            });
        } else if (role == Role.HUNTER) {
            event.setDeathMessage("§eHunter " + player.getName() + " has died! Cooldown started.");
            plugin.getGameManager().addHunterCooldown(player.getUniqueId());

            // Remove compass from drops
            event.getDrops().removeIf(item -> item != null && item.getType() == Material.COMPASS);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;
        Player player = event.getPlayer();
        Role role = plugin.getGameManager().getRole(player);

        if (role == Role.RUNNER) {
            event.setRespawnLocation(player.getLocation()); // Respawn where died to spectate
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
            });
        } else if (role == Role.HUNTER) {
            event.setRespawnLocation(plugin.getLobbyLocation());

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR); // Wait in lobby
            });
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
            if (obj.getMobRequirement(event.getEntity().getType()) > 0) {
                if (event.getEntity().getKiller() != null) {
                    Player killer = event.getEntity().getKiller();
                    if (plugin.getGameManager().getRole(killer) == Role.RUNNER) {
                        obj.addProgress(event.getEntity().getType(), 1);
                        plugin.getGameManager().checkWinCondition();
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
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SlotMachineHolder) {
            SlotMachineHolder holder = (SlotMachineHolder) event.getInventory().getHolder();
            holder.getSlotMachine().finish(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SlotMachineHolder) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) return;

        // Protect crown (even outside of PLAYING state)
        if (isCrown(event.getCurrentItem()) || isCrown(event.getCursor())) {
            event.setCancelled(true);
        }
        if (event.getHotbarButton() != -1) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (isCrown(hotbarItem)) {
                event.setCancelled(true);
            }
        }

        if (plugin.getGameManager().getState() != GameState.PLAYING) return;

        Player player = (Player) event.getWhoClicked();
        if (plugin.getGameManager().getRole(player) == Role.RUNNER) {
            ItemStack clicked = event.getCurrentItem();
            checkItemObjective(player, clicked);
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (isCrown(event.getOldCursor()) || isCrown(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (isCrown(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    private boolean isCrown(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelDataComponent()) return false;
        org.bukkit.inventory.meta.components.CustomModelDataComponent comp = meta.getCustomModelDataComponent();
        return comp.getStrings().contains("crown");
    }

    private void checkItemObjective(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Objective obj = plugin.getGameManager().getObjective();

        if (obj != null && obj.getType() == ObjectiveType.ITEM) {
            if (obj.getItemRequirement(item.getType()) > 0) {
                // Actually wait, for items, do we accumulate pickups?
                // It's safer to just iterate over all runners inventories and sum up the totals periodically or on event.
                // Let's do it via task.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    boolean won = true;
                    for (Material mat : org.bukkit.Material.values()) {
                        if (obj.getItemRequirement(mat) > 0) {
                            int total = 0;
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (plugin.getGameManager().getRole(p) == Role.RUNNER && !p.isDead()) {
                                    for (ItemStack invItem : p.getInventory().getContents()) {
                                        if (invItem != null && invItem.getType() == mat) {
                                            total += invItem.getAmount();
                                        }
                                    }
                                }
                            }
                            if (total < obj.getItemRequirement(mat)) {
                                won = false;
                                break;
                            }
                        }
                    }
                    if (won) {
                        plugin.getGameManager().endGame(Role.RUNNER);
                    }
                });
            }
        }
    }
}
