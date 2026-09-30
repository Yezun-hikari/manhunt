package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Random;

public class WorldManager {
    private final Main plugin;
    private final String WORLD_NAME = "manhunt_world";
    private final String WORLD_NETHER = "manhunt_world_nether";
    private final String WORLD_THE_END = "manhunt_world_the_end";

    private World gameWorld;

    public WorldManager(Main plugin) {
        this.plugin = plugin;
    }

    public void generateWorld(Runnable onComplete) {
        // Delete old worlds if they exist
        deleteGameWorlds();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long seed = new Random().nextLong();

            Bukkit.getScheduler().runTask(plugin, () -> {
                // Generate Overworld
                WorldCreator overworldCreator = new WorldCreator(WORLD_NAME);
                overworldCreator.seed(seed);
                overworldCreator.environment(World.Environment.NORMAL);
                overworldCreator.type(WorldType.NORMAL);
                gameWorld = overworldCreator.createWorld();

                if (gameWorld != null) {
                    gameWorld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true); // Locator bar usually is linked to some client stuff, but locatorBar gamerule is custom or maybe from a specific paper setting?
                    // wait, locatorBar is not a standard gamerule. Wait, "locatorBar" gamerule might be added in newer paper versions, let's use Bukkit.getGameRule or set it if it exists.
                    // If it's a known gamerule, let's try to set it by name.
                    // To avoid compile errors if GameRule enum doesn't have it, we use gameWorld.setGameRuleValue("locatorBar", "false") or similar.
                    gameWorld.setGameRuleValue("locatorBar", "false"); // Old way, but works for custom rules. Or try GameRule.getByName.
                }

                // Generate Nether
                WorldCreator netherCreator = new WorldCreator(WORLD_NETHER);
                netherCreator.seed(seed);
                netherCreator.environment(World.Environment.NETHER);
                netherCreator.createWorld();

                // Generate End
                WorldCreator endCreator = new WorldCreator(WORLD_THE_END);
                endCreator.seed(seed);
                endCreator.environment(World.Environment.THE_END);
                endCreator.createWorld();

                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public void deleteGameWorlds() {
        unloadAndDelete(WORLD_NAME);
        unloadAndDelete(WORLD_NETHER);
        unloadAndDelete(WORLD_THE_END);
    }

    private void unloadAndDelete(String name) {
        World world = Bukkit.getWorld(name);
        if (world != null) {
            // Kick players out
            for (Player p : world.getPlayers()) {
                p.teleport(plugin.getLobbyLocation());
            }
            Bukkit.unloadWorld(world, false);
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), name);
        if (worldFolder.exists()) {
            deleteDirectory(worldFolder);
        }
    }

    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return path.delete();
    }
}
