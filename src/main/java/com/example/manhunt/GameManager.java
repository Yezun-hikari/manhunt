package com.example.manhunt;

import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {
    private final Main plugin;
    private GameState currentState;
    private Objective currentObjective;

    private final Map<UUID, Role> playerRoles = new HashMap<>();

    // New fields
    private int headStartSeconds;
    private Long customSeed = null;
    private boolean huntersReleased = false;
    private int currentHeadStart = 0;

    // Maps UUID to remaining cooldown in seconds
    private final Map<UUID, Integer> hunterCooldowns = new HashMap<>();

    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.currentState = GameState.LOBBY;
        this.headStartSeconds = plugin.getConfig().getInt("default-headstart", 120);

        // Unified task for head start and respawns
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public GameState getState() { return currentState; }
    public void setState(GameState state) { this.currentState = state; }

    public Objective getObjective() { return currentObjective; }
    public void setObjective(Objective objective) { this.currentObjective = objective; }

    public int getHeadStartSeconds() { return headStartSeconds; }
    public void setHeadStartSeconds(int seconds) { this.headStartSeconds = Math.max(0, seconds); }

    public Long getCustomSeed() { return customSeed; }
    public void setCustomSeed(Long seed) { this.customSeed = seed; }

    public boolean areHuntersReleased() { return huntersReleased; }

    public Role getRole(Player player) {
        return playerRoles.getOrDefault(player.getUniqueId(), Role.SPECTATOR);
    }

    public void setRole(Player player, Role role) {
        playerRoles.put(player.getUniqueId(), role);
    }

    public Map<UUID, Role> getPlayerRoles() {
        return playerRoles;
    }

    public void addHunterCooldown(UUID uuid) {
        int cooldown = plugin.getConfig().getInt("cooldowns.hunter-respawn-seconds", 30);
        hunterCooldowns.put(uuid, cooldown);
    }

    public boolean hasCooldown(UUID uuid) {
        return hunterCooldowns.containsKey(uuid);
    }

    private void tick() {
        if (currentState == GameState.PLAYING) {
            // Handle head start countdown
            if (!huntersReleased && currentHeadStart > 0) {
                currentHeadStart--;
                if (currentHeadStart == 0) {
                    huntersReleased = true;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (getRole(p) == Role.HUNTER) {
                            p.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                            p.setGameMode(GameMode.SURVIVAL);
                            plugin.getTracker().giveCompass(p);
                        }
                    }
                    Bukkit.broadcastMessage("§cThe Hunters have been released!");
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (getRole(p) == Role.HUNTER) {
                            p.sendActionBar(Component.text("§cReleasing in " + currentHeadStart + "s"));
                        }
                    }
                }
            }

            // Handle hunter death cooldowns
            for (Map.Entry<UUID, Integer> entry : new HashMap<>(hunterCooldowns).entrySet()) {
                UUID uuid = entry.getKey();
                int time = entry.getValue();
                Player p = Bukkit.getPlayer(uuid);

                if (time > 0) {
                    hunterCooldowns.put(uuid, time - 1);
                    if (p != null && p.isOnline()) {
                        Title title = Title.title(
                            Component.text("§cRespawning in..."),
                            Component.text("§e" + time + " seconds"),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)
                        );
                        p.showTitle(title);
                    }
                } else {
                    hunterCooldowns.remove(uuid);
                    if (p != null && p.isOnline()) {
                        p.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                        p.setGameMode(GameMode.SURVIVAL);
                        plugin.getTracker().giveCompass(p);
                        p.sendMessage("§aYou have respawned!");
                        p.clearTitle(); // clears the custom title
                    }
                }
            }
        }
    }

    public void startGame(Player initiator) {
        if (currentState != GameState.LOBBY) return;
        if (currentObjective == null || !currentObjective.isValid()) {
            if (initiator != null) {
                initiator.sendMessage("§cCannot start game: No objective set!");
            } else {
                Bukkit.broadcastMessage("§cCannot start game: No objective set!");
            }
            return;
        }

        if (!playerRoles.containsValue(Role.RUNNER)) {
            if (initiator != null) initiator.sendMessage("§cCannot start game: At least one runner must be selected!");
            return;
        }

        setState(GameState.GENERATING);
        Bukkit.broadcastMessage("§eGenerating world, please wait...");
        plugin.getWorldManager().generateWorld(() -> {
            setState(GameState.PLAYING);
            huntersReleased = (headStartSeconds <= 0);
            currentHeadStart = headStartSeconds;

            teleportPlayersToGame();

            Bukkit.broadcastMessage("§aManhunt started!");
            if (!huntersReleased) {
                Bukkit.broadcastMessage("§eHunters will be released in " + headStartSeconds + " seconds.");
            }

            updateScoreboard();
        });
    }

    public void endGame(Role winner) {
        if (currentState != GameState.PLAYING) return;
        setState(GameState.LOBBY);

        boolean runnerWon = winner == Role.RUNNER;
        String winMsg = runnerWon ? "§aRunners won!" : "§cHunters won!";

        Component titleComp = Component.text(winMsg);
        Title title = Title.title(titleComp, Component.empty());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);

            Role role = getRole(player);
            boolean won = (role == winner);
            if (role != Role.SPECTATOR) {
                if (won) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DEATH, 1f, 1f);
                }
            }

            // Teleport to lobby
            player.teleport(plugin.getLobbyLocation());
            // Clear inventory
            player.getInventory().clear();
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            // Reset gamemode
            if (role == Role.RUNNER || role == Role.HUNTER || role == Role.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        // Unload and delete worlds
        plugin.getWorldManager().deleteGameWorlds();

        // Reset state
        playerRoles.clear();
        hunterCooldowns.clear();
        customSeed = null;
        headStartSeconds = plugin.getConfig().getInt("default-headstart", 120);
        currentObjective = null;
        plugin.getTracker().clearPortalData();
        updateScoreboard();
    }

    private void teleportPlayersToGame() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Role role = getRole(player);
            if (role == Role.RUNNER) {
                player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                player.getInventory().clear();
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(5);
            } else if (role == Role.HUNTER) {
                player.getInventory().clear();
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(5);
                if (huntersReleased) {
                    player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                    player.setGameMode(GameMode.SURVIVAL);
                    plugin.getTracker().giveCompass(player);
                } else {
                    // Stay in lobby
                    player.teleport(plugin.getLobbyLocation());
                    player.setGameMode(GameMode.SPECTATOR); // so they can't break anything
                }
            } else {
                // Spectators
                player.teleport(plugin.getWorldManager().getGameWorld().getSpawnLocation());
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    public void checkWinCondition() {
        if (currentState != GameState.PLAYING) return;

        boolean aliveRunnerExists = false;
        for (Map.Entry<UUID, Role> entry : playerRoles.entrySet()) {
            if (entry.getValue() == Role.RUNNER) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.isOnline() && !p.isDead() && p.getGameMode() == GameMode.SURVIVAL) {
                    aliveRunnerExists = true;
                    break;
                }
            }
        }

        if (!aliveRunnerExists) {
            endGame(Role.HUNTER);
        } else if (currentObjective != null && currentObjective.isCompleted()) {
            endGame(Role.RUNNER);
        }
    }

    public void updateScoreboard() {
        plugin.updateScoreboard();
    }
}
