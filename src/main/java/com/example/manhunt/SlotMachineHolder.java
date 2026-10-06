package com.example.manhunt;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class SlotMachineHolder implements InventoryHolder {
    private Inventory inventory;
    private final RunnerSlotMachine slotMachine;

    public SlotMachineHolder(RunnerSlotMachine slotMachine) {
        this.slotMachine = slotMachine;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public RunnerSlotMachine getSlotMachine() {
        return slotMachine;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
