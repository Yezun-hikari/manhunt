package com.example.manhunt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class ManhuntCommand implements CommandExecutor {
    private final Main plugin;

    public ManhuntCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("manhunt.admin")) {
            player.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openMainMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("setlobby")) {
            Location loc = player.getLocation();
            plugin.getConfig().set("lobby.world", loc.getWorld().getName());
            plugin.getConfig().set("lobby.x", loc.getX());
            plugin.getConfig().set("lobby.y", loc.getY());
            plugin.getConfig().set("lobby.z", loc.getZ());
            plugin.getConfig().set("lobby.yaw", loc.getYaw());
            plugin.getConfig().set("lobby.pitch", loc.getPitch());
            plugin.saveConfig();
            loc.getWorld().setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            player.sendMessage("§aLobby location set!");
            return true;
        }

        return false;
    }
}
