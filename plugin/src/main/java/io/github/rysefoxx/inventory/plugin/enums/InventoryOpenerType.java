/*
 * MIT License
 *
 * Copyright (c) 2022. Rysefoxx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.github.rysefoxx.inventory.plugin.enums;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * All possible inventory types supported by RyseInventory.
 */
@Getter
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
    ANVIL(InventoryType.ANVIL);

    private final InventoryType type;

    @Contract(pure = true)
    InventoryOpenerType(InventoryType type) {
        this.type = type;
    }

    /**
     * It takes a string, and returns the enum value that matches the string
     *
     * @param name The name of the enum.
     * @return The first InventoryOpenerType that matches the name. Null if there is no such value.
     */
    public static @Nullable InventoryOpenerType fromName(@NotNull String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
