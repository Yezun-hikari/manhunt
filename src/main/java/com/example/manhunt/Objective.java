package com.example.manhunt;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class Objective {
    private ObjectiveType type;
    private final Map<Material, Integer> targetItems = new HashMap<>();
    private final Map<EntityType, Integer> targetMobs = new HashMap<>();

    // Tracks current team progress
    private final Map<EntityType, Integer> currentMobs = new HashMap<>();

    public Objective() {
        this.type = null; // Unset initially
    }

    public ObjectiveType getType() {
        return type;
    }

    public void setType(ObjectiveType type) {
        if (this.type != type) {
            this.type = type;
            targetItems.clear();
            targetMobs.clear();
            currentMobs.clear();
        }
    }

    public void setItemRequirement(Material item, int count) {
        if (count <= 0) {
            targetItems.remove(item);
        } else {
            targetItems.put(item, count);
        }
    }

    public void setMobRequirement(EntityType mob, int count) {
        if (count <= 0) {
            targetMobs.remove(mob);
        } else {
            targetMobs.put(mob, count);
        }
    }

    public int getItemRequirement(Material item) {
        return targetItems.getOrDefault(item, 0);
    }

    public int getMobRequirement(EntityType mob) {
        return targetMobs.getOrDefault(mob, 0);
    }

    public void addProgress(EntityType mob, int amount) {
        if (type == ObjectiveType.MOB && targetMobs.containsKey(mob)) {
            int current = currentMobs.getOrDefault(mob, 0);
            currentMobs.put(mob, current + amount);
        }
    }

    public boolean isValid() {
        if (type == ObjectiveType.ITEM) return !targetItems.isEmpty();
        if (type == ObjectiveType.MOB) return !targetMobs.isEmpty();
        return false;
    }

    public boolean isCompleted() {
        // Item check is done manually via inventory checks in Events.java
        // So we only return true here for MOBs if satisfied.
        if (type == ObjectiveType.MOB) {
            for (Map.Entry<EntityType, Integer> entry : targetMobs.entrySet()) {
                if (currentMobs.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
