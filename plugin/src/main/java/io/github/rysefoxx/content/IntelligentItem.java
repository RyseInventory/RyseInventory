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

package io.github.rysefoxx.content;

import io.github.rysefoxx.pagination.InventoryManager;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/18/2022
 */
@Getter
public class IntelligentItem {

    private final ItemStack itemStack;
    private Consumer<InventoryClickEvent> consumer;
    private final IntelligentItemError error;
    private boolean canClick = true;
    private boolean canSee = true;
    private @Nullable Object id;

    @Contract(pure = true)
    public IntelligentItem(@NotNull ItemStack itemStack, Consumer<InventoryClickEvent> eventConsumer, IntelligentItemError error) {
        this.itemStack = itemStack;
        this.consumer = eventConsumer;
        this.error = error;
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull IntelligentItem of(@NotNull ItemStack itemStack, @NotNull IntelligentItemError error, @NotNull Consumer<InventoryClickEvent> eventConsumer) {
        return new IntelligentItem(itemStack, eventConsumer, error);
    }


    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull IntelligentItem of(@NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> eventConsumer) {
        return new IntelligentItem(itemStack, eventConsumer, null);
    }


    @Contract(value = "_ -> new", pure = true)
    public static @NotNull IntelligentItem empty(@NotNull ItemStack itemStack) {
        return new IntelligentItem(itemStack, event -> {
        }, null);
    }


    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull IntelligentItem empty(@NotNull ItemStack itemStack, @NotNull IntelligentItemError error) {
        return new IntelligentItem(itemStack, event -> {
        }, error);
    }


    @Contract(value = "_ -> new", pure = true)
    public static @NotNull IntelligentItem ignored(@NotNull ItemStack itemStack) {
        return new IntelligentItem(itemStack, null, null);
    }


    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull IntelligentItem ignored(@NotNull ItemStack itemStack, @NotNull IntelligentItemError error) {
        return new IntelligentItem(itemStack, null, error);
    }

    /**
     * Removes the consumer from an IntelligentItem
     */
    public void clearConsumer() {
        this.consumer = event -> {
        };
    }

    /**
     * Sets the id of an IntelligentItem
     *
     * @param id The id of the item
     */
    public IntelligentItem identifier(@NotNull Object id, @NotNull InventoryManager manager) {
        this.id = id;
        manager.register(this);
        return this;
    }

    /**
     * Checks if the item can be clicked.
     *
     * @param supplier The supplier to check.
     * @return The IntelligentItem.
     */
    public @NotNull IntelligentItem canClick(@NotNull BooleanSupplier supplier) {
        this.canClick = supplier.getAsBoolean();
        return this;
    }

    /**
     * Checks if the item is visible to the player.
     *
     * @param supplier The supplier to check.
     * @return The IntelligentItem.
     */
    public @NotNull IntelligentItem canSee(@NotNull BooleanSupplier supplier) {
        this.canSee = supplier.getAsBoolean();
        return this;
    }

    /**
     * Changes the ItemStack of an existing ItemStack without changing the consumer.
     *
     * @param newItemStack The new ItemStack
     * @return The new intelligent ItemStack
     */
    public @NotNull IntelligentItem update(@NotNull ItemStack newItemStack) {
        return new IntelligentItem(newItemStack, this.consumer, this.error);
    }

    /**
     * Changes the ItemStack of an existing Intelligent with changing the consumer.
     *
     * @param newIntelligentItem The new IntelligentItem
     * @return The new intelligent ItemStack
     */
    public @NotNull IntelligentItem update(@NotNull IntelligentItem newIntelligentItem) {
        return new IntelligentItem(newIntelligentItem.getItemStack(), newIntelligentItem.getConsumer(), this.error);
    }
}
