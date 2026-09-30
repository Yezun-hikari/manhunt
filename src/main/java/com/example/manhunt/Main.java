package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private GameManager gameManager;
    private WorldManager worldManager;
    private Tracker tracker;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        gameManager = new GameManager(this);
        worldManager = new WorldManager(this);
        tracker = new Tracker(this);
        guiManager = new GUIManager(this);

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
