package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {
    private final Main plugin;
    private GameState currentState;
    private Objective currentObjective;

    private final Map<UUID, Role> playerRoles = new HashMap<>();

    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.currentState = GameState.LOBBY;
    }

    public GameState getState() {
        return currentState;
    }

    public void setState(GameState state) {
        this.currentState = state;
    }

    public Objective getObjective() {
        return currentObjective;
    }

    public void setObjective(Objective objective) {
        this.currentObjective = objective;
    }

    public Role getRole(Player player) {
        return playerRoles.getOrDefault(player.getUniqueId(), Role.SPECTATOR);
    }

    public void setRole(Player player, Role role) {
        playerRoles.put(player.getUniqueId(), role);
    }

    public Map<UUID, Role> getPlayerRoles() {
        return playerRoles;
    }

    public void startGame() {
        if (currentState != GameState.LOBBY) return;
        if (currentObjective == null) {
            Bukkit.broadcastMessage("§cCannot start game: No objective set!");
            return;
        }

        setState(GameState.GENERATING);
        Bukkit.broadcastMessage("§eGenerating world, please wait...");
        plugin.getWorldManager().generateWorld(() -> {
            setState(GameState.PLAYING);
            teleportPlayersToGame();
            giveCompassToHunters();
            Bukkit.broadcastMessage("§aManhunt started! Objective: " + currentObjective.toString());
        });
    }

    public void endGame(Role winner) {
        if (currentState != GameState.PLAYING) return;
        setState(GameState.LOBBY);

        String winMsg = winner == Role.RUNNER ? "§aRunners won!" : "§cHunters won!";
        Bukkit.broadcastMessage(winMsg);

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Teleport to lobby
            player.teleport(plugin.getLobbyLocation());
            // Clear inventory
            player.getInventory().clear();
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            // Reset gamemode
            if (getRole(player) == Role.RUNNER || getRole(player) == Role.HUNTER || getRole(player) == Role.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        // Unload and delete worlds
        plugin.getWorldManager().deleteGameWorlds();

        // Reset state
        playerRoles.clear();
        currentObjective = null;
        plugin.getTracker().clearPortalData();
    }

    private void teleportPlayersToGame() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Role role = getRole(player);
            if (role == Role.RUNNER || role == Role.HUNTER) {
                player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                player.getInventory().clear();
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(5);
            } else {
                // Spectators
                player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    private void giveCompassToHunters() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (getRole(player) == Role.HUNTER) {
                plugin.getTracker().giveCompass(player);
            }
        }
    }

    public void checkWinCondition() {
        if (currentState != GameState.PLAYING) return;

        boolean aliveRunnerExists = false;
        for (Map.Entry<UUID, Role> entry : playerRoles.entrySet()) {
            if (entry.getValue() == Role.RUNNER) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.getGameMode() == GameMode.SURVIVAL) {
                    aliveRunnerExists = true;
                    break;
                }
            }
        }

        if (!aliveRunnerExists) {
            endGame(Role.HUNTER);
        }
    }
}
