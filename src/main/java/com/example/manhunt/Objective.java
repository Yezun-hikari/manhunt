package com.example.manhunt;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class Objective {
    private final ObjectiveType type;
    private final Material targetItem;
    private final EntityType targetMob;

    public Objective(Material item) {
        this.type = ObjectiveType.ITEM;
        this.targetItem = item;
        this.targetMob = null;
    }

    public Objective(EntityType mob) {
        this.type = ObjectiveType.MOB;
        this.targetItem = null;
        this.targetMob = mob;
    }

    public ObjectiveType getType() {
        return type;
    }

    public Material getTargetItem() {
        return targetItem;
    }

    public EntityType getTargetMob() {
        return targetMob;
    }

    @Override
    public String toString() {
        if (type == ObjectiveType.ITEM) {
            return "Item: " + targetItem.name();
        } else {
            return "Mob: " + targetMob.name();
        }
    }
}
