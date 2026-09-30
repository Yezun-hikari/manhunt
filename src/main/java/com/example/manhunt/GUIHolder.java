package com.example.manhunt;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class GUIHolder implements InventoryHolder {
    private Inventory inventory;
    private final String guiName;

    public GUIHolder(String guiName) {
        this.guiName = guiName;
    }

    public String getGuiName() {
        return guiName;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
