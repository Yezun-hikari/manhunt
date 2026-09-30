package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.Difficulty;

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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Delete old worlds if they exist (sync wait for async IO safely)
            // But we can just do deleteDirectory here. Unload must be sync.
            Bukkit.getScheduler().runTask(plugin, () -> {
                deleteGameWorlds();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    // Wait for file system locks just in case
                    try { Thread.sleep(500); } catch (InterruptedException e) {}

                    Long custom = plugin.getGameManager().getCustomSeed();
                    long seed = custom != null ? custom : new Random().nextLong();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Generate Overworld
                        WorldCreator overworldCreator = new WorldCreator(WORLD_NAME);
                overworldCreator.seed(seed);
                overworldCreator.environment(World.Environment.NORMAL);
                overworldCreator.type(WorldType.NORMAL);
                gameWorld = overworldCreator.createWorld();

                if (gameWorld != null) {
                    gameWorld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
                    gameWorld.setGameRuleValue("locator_bar", "false");
                    gameWorld.setDifficulty(Difficulty.NORMAL);
                }

                // Generate Nether
                WorldCreator netherCreator = new WorldCreator(WORLD_NETHER);
                netherCreator.seed(seed);
                netherCreator.environment(World.Environment.NETHER);
                World nether = netherCreator.createWorld();
                if (nether != null) nether.setDifficulty(Difficulty.NORMAL);

                // Generate End
                WorldCreator endCreator = new WorldCreator(WORLD_THE_END);
                endCreator.seed(seed);
                endCreator.environment(World.Environment.THE_END);
                World end = endCreator.createWorld();
                if (end != null) end.setDifficulty(Difficulty.NORMAL);

                if (onComplete != null) {
                    onComplete.run();
                }
                    });
                });
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
