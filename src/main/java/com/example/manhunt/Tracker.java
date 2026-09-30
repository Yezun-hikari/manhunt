package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Tracker {
    private final Main plugin;
    // Maps Runner UUID to their last portal location per dimension
    private final Map<UUID, Map<String, Location>> lastPortalLocations = new HashMap<>();

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
    }

    public void updateCompasses() {
        if (plugin.getGameManager().getState() != GameState.PLAYING) return;

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
