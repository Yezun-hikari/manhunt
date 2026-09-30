package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Main extends JavaPlugin {

    private GameManager gameManager;
    private WorldManager worldManager;
    private Tracker tracker;
    private GUIManager guiManager;

    private Scoreboard scoreboard;
    private Team teamRunners;
    private Team teamHunters;
    private Team teamSpectators;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        gameManager = new GameManager(this);
        worldManager = new WorldManager(this);
        tracker = new Tracker(this);
        guiManager = new GUIManager(this);

        setupScoreboard();

        getServer().getPluginManager().registerEvents(new Events(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        getCommand("manhunt").setExecutor(new ManhuntCommand(this));

        // Start compass update task
        Bukkit.getScheduler().runTaskTimer(this, () -> tracker.updateCompasses(), 20L, 20L);

        getLogger().info("Manhunt plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager.getState() == GameState.PLAYING) {
            gameManager.endGame(Role.HUNTER); // Force end on reload/stop
        }
        getLogger().info("Manhunt plugin disabled!");
    }

    private void setupScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        teamRunners = scoreboard.registerNewTeam("Runners");
        teamRunners.color(NamedTextColor.GREEN);
        teamRunners.prefix(Component.text("[Runner] ").color(NamedTextColor.GREEN));

        teamHunters = scoreboard.registerNewTeam("Hunters");
        teamHunters.color(NamedTextColor.RED);
        teamHunters.prefix(Component.text("[Hunter] ").color(NamedTextColor.RED));

        teamSpectators = scoreboard.registerNewTeam("Spectators");
        teamSpectators.color(NamedTextColor.GRAY);
        teamSpectators.prefix(Component.text("[Spec] ").color(NamedTextColor.GRAY));
    }

    public void updateScoreboard() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(scoreboard);
            Role r = gameManager.getRole(p);

            // Remove from all first
            teamRunners.removePlayer(p);
            teamHunters.removePlayer(p);
            teamSpectators.removePlayer(p);

            if (r == Role.RUNNER) {
                teamRunners.addPlayer(p);
            } else if (r == Role.HUNTER) {
                teamHunters.addPlayer(p);
            } else {
                teamSpectators.addPlayer(p);
            }
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public Tracker getTracker() {
        return tracker;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public Location getLobbyLocation() {
        String worldName = getConfig().getString("lobby.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        double x = getConfig().getDouble("lobby.x", 0.0);
        double y = getConfig().getDouble("lobby.y", 64.0);
        double z = getConfig().getDouble("lobby.z", 0.0);
        float yaw = (float) getConfig().getDouble("lobby.yaw", 0.0);
        float pitch = (float) getConfig().getDouble("lobby.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
