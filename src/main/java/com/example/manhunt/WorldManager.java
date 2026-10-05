package com.example.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.Difficulty;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

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
        boolean overworldUnloaded = unloadSync(currentOverworldName);
        boolean netherUnloaded = unloadSync(currentNetherName);
        boolean endUnloaded = unloadSync(currentEndName);

        // Delete old folders asynchronously with retry mechanism
        File targetOverworld = overworldUnloaded ? oldOverworldFolder : null;
        File targetNether = netherUnloaded ? oldNetherFolder : null;
        File targetEnd = endUnloaded ? oldEndFolder : null;

        if (targetOverworld != null || targetNether != null || targetEnd != null) {
            deleteFoldersWithRetry(targetOverworld, targetNether, targetEnd, 0);
        }

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

        boolean overworldUnloaded = unloadSync(currentOverworldName);
        boolean netherUnloaded = unloadSync(currentNetherName);
        boolean endUnloaded = unloadSync(currentEndName);

        File targetOverworld = overworldUnloaded ? oldOverworldFolder : null;
        File targetNether = netherUnloaded ? oldNetherFolder : null;
        File targetEnd = endUnloaded ? oldEndFolder : null;

        if (targetOverworld != null || targetNether != null || targetEnd != null) {
            deleteFoldersWithRetry(targetOverworld, targetNether, targetEnd, 0);
        }

        currentOverworldName = null;
        currentNetherName = null;
        currentEndName = null;
        currentOverworldFolder = null;
        currentNetherFolder = null;
        currentEndFolder = null;
    }

    private boolean unloadSync(String name) {
        if (name == null) return false;
        World world = Bukkit.getWorld(name);
        if (world != null) {
            // Kick players out to lobby immediately
            for (Player p : world.getPlayers()) {
                if (p.isDead()) {
                    p.spigot().respawn();
                }
                p.teleport(plugin.getLobbyLocation());
            }

            world.setKeepSpawnInMemory(false);

            boolean unloaded = Bukkit.unloadWorld(world, false); // false = do not save chunks!
            if (!unloaded) {
                plugin.getLogger().warning("Konnte Welt nicht entladen: " + world.getName());
            }
            return unloaded;
        }
        return false;
    }

    private void deleteFoldersWithRetry(File overworld, File nether, File end, int attempts) {
        if (attempts >= 6) {
            plugin.getLogger().warning("Failed to delete old world folders after maximum retries.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = true;

            try {
                if (overworld != null && overworld.exists()) {
                    success &= deleteDirectoryNIO(overworld.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "IOException while deleting overworld folder. File locks might still be active.", e);
                success = false;
            }

            try {
                if (nether != null && nether.exists()) {
                    success &= deleteDirectoryNIO(nether.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "IOException while deleting nether folder. File locks might still be active.", e);
                success = false;
            }

            try {
                if (end != null && end.exists()) {
                    success &= deleteDirectoryNIO(end.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "IOException while deleting end folder. File locks might still be active.", e);
                success = false;
            }

            if (!success) {
                plugin.getLogger().info("Retrying folder deletion in 10 seconds...");
                // Retry after 10 seconds (200 ticks)
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    deleteFoldersWithRetry(overworld, nether, end, attempts + 1);
                }, 200L);
            } else {
                plugin.getLogger().info("Successfully deleted old world folders.");
            }
        });
    }

    private boolean deleteDirectoryNIO(Path directory) throws IOException {
        if (!Files.exists(directory)) return true;

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        return !Files.exists(directory);
    }
}