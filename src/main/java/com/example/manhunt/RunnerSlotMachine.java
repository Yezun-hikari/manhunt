package com.example.manhunt;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RunnerSlotMachine {
    private final Main plugin;
    private final Player player;
    private final Inventory inventory;
    private final Random random = new Random();
    private BukkitRunnable animationTask;
    private boolean isFinished = false;
    private ItemStack finalReward;

    public RunnerSlotMachine(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        SlotMachineHolder holder = new SlotMachineHolder(this);
        this.inventory = Bukkit.createInventory(holder, 27, Component.text("§6§lRunner Bonus"));
        holder.setInventory(this.inventory);

        setupStaticItems();
        fillInitialRow();
    }

    private void setupStaticItems() {
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = grayGlass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            grayGlass.setItemMeta(meta);
        }

        ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta limeMeta = limeGlass.getItemMeta();
        if (limeMeta != null) {
            limeMeta.displayName(Component.text("§a§lV"));
            limeGlass.setItemMeta(limeMeta);
        }

        ItemStack limeGlassUp = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta limeUpMeta = limeGlassUp.getItemMeta();
        if (limeUpMeta != null) {
            limeUpMeta.displayName(Component.text("§a§l^"));
            limeGlassUp.setItemMeta(limeUpMeta);
        }

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, grayGlass);
            inventory.setItem(i + 18, grayGlass);
        }

        inventory.setItem(4, limeGlass);
        inventory.setItem(22, limeGlassUp);
    }

    private void fillInitialRow() {
        for (int i = 9; i < 18; i++) {
            inventory.setItem(i, getRandomReward());
        }
    }

    private ItemStack getRandomReward() {
        int chance = random.nextInt(100); // 0-99
        if (chance < 60) {
            return getCommonReward();
        } else if (chance < 90) {
            return getRareReward();
        } else {
            return getJackpotReward();
        }
    }

    private ItemStack getCommonReward() {
        int r = random.nextInt(4);
        switch (r) {
            case 0: return new ItemStack(Material.IRON_PICKAXE);
            case 1:
                return random.nextBoolean() ?
                    new ItemStack(Material.COOKED_BEEF, random.nextInt(9) + 8) :
                    new ItemStack(Material.GOLDEN_CARROT, random.nextInt(9) + 8);
            case 2: return new ItemStack(Material.OAK_BOAT);
            case 3:
            default:
                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 1);
                return boots;
        }
    }

    private ItemStack getRareReward() {
        int r = random.nextInt(4);
        switch (r) {
            case 0: return new ItemStack(Material.WATER_BUCKET);
            case 1: return new ItemStack(Material.SHIELD);
            case 2: return new ItemStack(Material.COBWEB, 3);
            case 3:
            default:
                return random.nextBoolean() ?
                    new ItemStack(Material.SNOWBALL, 16) :
                    new ItemStack(Material.EGG, 16);
        }
    }

    private ItemStack getJackpotReward() {
        int r = random.nextInt(4);
        switch (r) {
            case 0: return new ItemStack(Material.ENDER_PEARL, random.nextInt(2) + 1);
            case 1: return new ItemStack(Material.FLINT_AND_STEEL);
            case 2: return new ItemStack(Material.GOLDEN_APPLE);
            case 3:
            default:
                ItemStack potion = new ItemStack(Material.POTION);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();
                if (meta != null) {
                    meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 1800, 1), true);
                    meta.displayName(Component.text("§bPotion of Speed II"));
                    potion.setItemMeta(meta);
                }
                return potion;
        }
    }

    public void start() {
        player.openInventory(inventory);

        final int targetSpins = random.nextInt(20) + 40; // Total item shifts

        animationTask = new BukkitRunnable() {
            int spins = 0;
            int ticksDelay = 2; // Initial speed
            int nextShift = ticksDelay;

            @Override
            public void run() {
                if (nextShift > 0) {
                    nextShift--;
                    return;
                }

                spins++;

                // Shift items left
                for (int i = 9; i < 17; i++) {
                    inventory.setItem(i, inventory.getItem(i + 1));
                }
                inventory.setItem(17, getRandomReward());

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

                // Slow down calculation
                if (spins > targetSpins * 0.8) {
                    ticksDelay = 6;
                } else if (spins > targetSpins * 0.5) {
                    ticksDelay = 3;
                } else {
                    ticksDelay = 2;
                }

                nextShift = ticksDelay;

                if (spins >= targetSpins) {
                    finish(false);
                    cancel();
                }
            }
        };

        animationTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void finish(boolean earlyClose) {
        if (isFinished) return;
        isFinished = true;

        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }

        if (earlyClose) {
            // Generate the missing final reward instantly
            finalReward = getRandomReward();
        } else {
            // Take the item from slot 13
            finalReward = inventory.getItem(13);
        }

        giveReward(finalReward);

        if (!earlyClose) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Auto close after 1 second
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof SlotMachineHolder) {
                    player.closeInventory();
                }
            }, 20L);
        } else {
             player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private void giveReward(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        // Add to inventory, drop excess
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
