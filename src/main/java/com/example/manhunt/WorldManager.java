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
import java.util.UUID;

public class WorldManager {
    private final Main plugin;

    private String currentOverworldName;
    private String currentNetherName;
    private String currentEndName;

    private File currentOverworldFolder;
    private File currentNetherFolder;
    private File currentEndFolder;

    public WorldManager(Main plugin) {
        this.plugin = plugin;
    }

    public void generateWorld(Runnable onComplete) {
        // Capture old folders before unloading
        File oldOverworldFolder = currentOverworldFolder;
        File oldNetherFolder = currentNetherFolder;
        File oldEndFolder = currentEndFolder;

        // Kick players and unload sync first
        unloadSync(currentOverworldName);
        unloadSync(currentNetherName);
        unloadSync(currentEndName);

        // Delete old folders asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            deleteFolderAsync(oldOverworldFolder);
            deleteFolderAsync(oldNetherFolder);
            deleteFolderAsync(oldEndFolder);
        });

        // Generate new ID and names
        String id = UUID.randomUUID().toString().substring(0, 8);
        currentOverworldName = "manhunt_" + id;
        currentNetherName = "manhunt_" + id + "_nether";
        currentEndName = "manhunt_" + id + "_the_end";

        Long custom = plugin.getGameManager().getCustomSeed();
        long seed = custom != null ? custom : new Random().nextLong();

        // Generate Overworld
        WorldCreator overworldCreator = new WorldCreator(currentOverworldName);
        overworldCreator.seed(seed);
        overworldCreator.environment(World.Environment.NORMAL);
        overworldCreator.type(WorldType.NORMAL);
        World gameWorld = overworldCreator.createWorld();

        if (gameWorld != null) {
            gameWorld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
            gameWorld.setGameRuleValue("locator_bar", "false");
            gameWorld.setDifficulty(Difficulty.NORMAL);
            currentOverworldFolder = gameWorld.getWorldFolder();
        }

        // Generate Nether
        WorldCreator netherCreator = new WorldCreator(currentNetherName);
        netherCreator.seed(seed);
        netherCreator.environment(World.Environment.NETHER);
        World nether = netherCreator.createWorld();
        if (nether != null) {
            nether.setDifficulty(Difficulty.NORMAL);
            currentNetherFolder = nether.getWorldFolder();
        }

        // Generate End
        WorldCreator endCreator = new WorldCreator(currentEndName);
        endCreator.seed(seed);
        endCreator.environment(World.Environment.THE_END);
        World end = endCreator.createWorld();
        if (end != null) {
            end.setDifficulty(Difficulty.NORMAL);
            currentEndFolder = end.getWorldFolder();
        }

        if (onComplete != null) {
            onComplete.run();
        }
    }

    public World getGameWorld() {
        if (currentOverworldName == null) return null;
        return Bukkit.getWorld(currentOverworldName);
    }

    public void deleteGameWorlds() {
        File oldOverworldFolder = currentOverworldFolder;
        File oldNetherFolder = currentNetherFolder;
        File oldEndFolder = currentEndFolder;

        unloadSync(currentOverworldName);
        unloadSync(currentNetherName);
        unloadSync(currentEndName);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            deleteFolderAsync(oldOverworldFolder);
            deleteFolderAsync(oldNetherFolder);
            deleteFolderAsync(oldEndFolder);
        });

        currentOverworldName = null;
        currentNetherName = null;
        currentEndName = null;
        currentOverworldFolder = null;
        currentNetherFolder = null;
        currentEndFolder = null;
    }

    private void unloadSync(String name) {
        if (name == null) return;
        World world = Bukkit.getWorld(name);
        if (world != null) {
            // Kick players out to lobby immediately
            for (Player p : world.getPlayers()) {
                p.teleport(plugin.getLobbyLocation());
            }
            Bukkit.unloadWorld(world, false); // false = do not save chunks!
        }
    }

    private void deleteFolderAsync(File folder) {
        if (folder != null && folder.exists()) {
            deleteDirectory(folder);
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