package io.github.rysefoxx.opener;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum InventoryOpenerType {

    CHEST(InventoryType.CHEST),
    DISPENSER(InventoryType.DISPENSER),
    DROPPER(InventoryType.DROPPER),
    BLAST_FURNACE(InventoryType.BLAST_FURNACE),
    BREWING_STAND(InventoryType.BREWING),
    CRAFTING_TABLE(InventoryType.CRAFTING),
    ENCHANTMENT_TABLE(InventoryType.ENCHANTING),
    FURNACE(InventoryType.FURNACE),
    GRINDSTONE(InventoryType.GRINDSTONE),
    HOPPER(InventoryType.HOPPER),
    LOOM(InventoryType.LOOM),
    SHULKER_BOX(InventoryType.SHULKER_BOX),
    SMITHING_TABLE(InventoryType.SMITHING),
    SMOKER(InventoryType.SMOKER),
    CARTOGRAPHY_TABLE(InventoryType.CARTOGRAPHY),
    STONECUTTER(InventoryType.STONECUTTER),
    ENDER_CHEST(InventoryType.ENDER_CHEST),
    ;
    private @Getter
    final InventoryType type;

    @Contract(pure = true)
    InventoryOpenerType(@NotNull InventoryType type) {
        this.type = type;
    }
}
