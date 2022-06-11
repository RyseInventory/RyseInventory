package io.github.rysefoxx.enums;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryType;

public enum InventoryOpenerType {

    CHEST(InventoryType.CHEST),
    DISPENSER(InventoryType.DISPENSER),
    DROPPER(InventoryType.DROPPER),
    BREWING_STAND(InventoryType.BREWING),
    CRAFTING_TABLE(InventoryType.WORKBENCH),
    ENCHANTMENT_TABLE(InventoryType.ENCHANTING),
    FURNACE(InventoryType.FURNACE),
    HOPPER(InventoryType.HOPPER),
    ENDER_CHEST(InventoryType.ENDER_CHEST),
    ;
    private @Getter
    final InventoryType type;

    InventoryOpenerType(InventoryType type) {
        this.type = type;
    }
}
