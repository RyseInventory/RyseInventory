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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum CloseReason {

    /**
     * When the player clicks outside the inventory.
     */
    CLICK_OUTSIDE,
    /**
     * When the player clicks an empty slot in the upper inventory.
     */
    CLICK_EMPTY_SLOT,
    /**
     * When the player clicks an slot in the lower inventory.
     */
    CLICK_BOTTOM_INVENTORY;

    /**
     * Return the first CloseReason whose name matches the given name, or null if no such CloseReason exists.
     *
     * @param name The name of the enum constant, exactly as declared in its enum declaration.
     * @return A CloseReason enum value.
     */
    public static @Nullable CloseReason fromName(@NotNull String name) {
        return Arrays.stream(values())
                .filter(closeReason -> closeReason.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

}
