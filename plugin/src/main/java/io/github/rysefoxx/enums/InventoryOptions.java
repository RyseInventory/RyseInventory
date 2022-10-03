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

package io.github.rysefoxx.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/25/2022
 */
public enum InventoryOptions {

    /**
     * The player does not receive any damage while he has the inventory open.
     */
    NO_DAMAGE,
    /**
     * The player cannot pick up items while having the inventory open.
     */
    NO_ITEM_PICKUP,
    /**
     * The player cannot be affected by any Potioneffect.
     */
    NO_POTION_EFFECT,
    /**
     * The block under the player can not be removed as long as he has the inventory open.
     */
    NO_BLOCK_BREAK,
    /**
     * The player does not get hungry as long as the inventory is open.
     */
    NO_HUNGER,
    ;

    /**
     * Return the InventoryOption with the given name, or null if there is no such option.
     *
     * @param name The name of the option.
     * @return The first value in the array that matches the name.
     */
    public static @Nullable InventoryOptions fromName(@NotNull String name) {
        return Arrays.stream(values())
                .filter(option -> option.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

}
