package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Tracker {
    private final Main plugin;
    // Maps Runner UUID to their last portal location per dimension
    private final Map<UUID, Map<String, Location>> lastPortalLocations = new HashMap<>();
    // Maps Runner UUID to their BossBar for tracking hunters
    private final Map<UUID, BossBar> runnerBossBars = new HashMap<>();

    public Tracker(Main plugin) {
        this.plugin = plugin;
    }

    public void giveCompass(Player hunter) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        hunter.getInventory().addItem(compass);
    }

    public void setLastPortalLocation(Player runner, Location location) {
        lastPortalLocations.computeIfAbsent(runner.getUniqueId(), k -> new HashMap<>())
                .put(location.getWorld().getName(), location);
    }

    public void clearPortalData() {
        lastPortalLocations.clear();
        for (BossBar bar : runnerBossBars.values()) {
            bar.removeAll();
        }
        runnerBossBars.clear();
    }

    public void updateCompasses() {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;

        // --- BossBar Logic for Runners ---
        if (plugin.getGameManager().areHuntersReleased()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (plugin.getGameManager().getRole(player) == Role.RUNNER) {
                    Player nearestHunter = null;
                    double nearestDistance = Double.MAX_VALUE;
                    boolean sameDimension = false;

                    for (Player hunter : Bukkit.getOnlinePlayers()) {
                        if (plugin.getGameManager().getRole(hunter) == Role.HUNTER && !plugin.getGameManager().hasCooldown(hunter.getUniqueId())) {
                            if (player.getWorld().equals(hunter.getWorld())) {
                                double dist = player.getLocation().distanceSquared(hunter.getLocation());
                                if (dist < nearestDistance) {
                                    nearestDistance = dist;
                                    nearestHunter = hunter;
                                    sameDimension = true;
                                }
                            } else if (!sameDimension) {
                                // Only consider cross-dimension if we haven't found a same-dimension hunter yet
                                nearestHunter = hunter;
                            }
                        }
                    }

                    BossBar bar = runnerBossBars.computeIfAbsent(uuid, k -> Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID));

                    if (!bar.getPlayers().contains(player)) {
                        bar.addPlayer(player);
                    }

                    if (nearestHunter != null) {
                        bar.setVisible(true);
                        if (sameDimension) {
                            int distMeters = (int) Math.sqrt(nearestDistance);
                            bar.setTitle("§cNächster Hunter: §e" + nearestHunter.getName() + " §7(§e" + distMeters + "m§7)");

                            // Fill BossBar closer the hunter is (full at 0m, empty at 500m)
                            double progress = Math.max(0.0, 1.0 - ((double) distMeters / 500.0));
                            bar.setProgress(progress);
                        } else {
                            bar.setTitle("§cNächster Hunter: §e" + nearestHunter.getName());
                            bar.setProgress(0.0); // Empty when in different dimension
                        }
                    } else {
                        bar.setVisible(false);
                    }
                } else if (runnerBossBars.containsKey(uuid)) {
                    // Player is no longer a runner, remove and hide bar
                    BossBar bar = runnerBossBars.remove(uuid);
                    if (bar != null) {
                        bar.removeAll();
                    }
                }
            }
        }

        // --- Compass Logic for Hunters ---
        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().getRole(hunter) != Role.HUNTER) continue;

            Player nearestRunner = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Player runner : Bukkit.getOnlinePlayers()) {
                if (plugin.getGameManager().getRole(runner) == Role.RUNNER && runner.getGameMode() == GameMode.SURVIVAL) {
                    if (hunter.getWorld().equals(runner.getWorld())) {
                        double dist = hunter.getLocation().distanceSquared(runner.getLocation());
                        if (dist < nearestDistance) {
                            nearestDistance = dist;
                            nearestRunner = runner;
                        }
                    }
                }
            }

            Location targetLocation = null;

            if (nearestRunner != null) {
                targetLocation = nearestRunner.getLocation();
            } else {
                // No runner in same world, check for nearest runner portal
                double nearestPortalDist = Double.MAX_VALUE;
                for (Player runner : Bukkit.getOnlinePlayers()) {
                    if (plugin.getGameManager().getRole(runner) == Role.RUNNER && runner.getGameMode() == GameMode.SURVIVAL) {
                        Map<String, Location> portals = lastPortalLocations.get(runner.getUniqueId());
                        if (portals != null) {
                            Location portalLoc = portals.get(hunter.getWorld().getName());
                            if (portalLoc != null) {
                                double dist = hunter.getLocation().distanceSquared(portalLoc);
                                if (dist < nearestPortalDist) {
                                    nearestPortalDist = dist;
                                    targetLocation = portalLoc;
                                }
                            }
                        }
                    }
                }
            }

            if (targetLocation != null) {
                hunter.setCompassTarget(targetLocation);

                // For compass in offhand or main hand, we can set the lodestone target to make it work in all dimensions
                for (ItemStack item : hunter.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.COMPASS) {
                        if (item.getItemMeta() instanceof CompassMeta) {
                            CompassMeta meta = (CompassMeta) item.getItemMeta();
                            meta.setLodestone(targetLocation);
                            meta.setLodestoneTracked(false);
                            item.setItemMeta(meta);
                        }
                    }
                }
            }
        }
    }
}
