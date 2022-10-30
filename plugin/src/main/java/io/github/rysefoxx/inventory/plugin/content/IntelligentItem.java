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

package io.github.rysefoxx.inventory.plugin.content;

import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/18/2022
 */
@Getter
public class IntelligentItem {

    private final ItemStack itemStack;
    private IntelligentItemError error;

    private Consumer<InventoryClickEvent> defaultConsumer;

    private boolean canClick = true;
    private boolean canSee = true;
    private boolean advanced = false;

    private @Nullable Object id;

    //For serialization
    @Contract(pure = true)
    private IntelligentItem(@NotNull ItemStack itemStack, @NotNull IntelligentItemError error) {
        this.itemStack = itemStack;
        this.error = error;
    }

    @Contract(pure = true)
    public IntelligentItem(@NotNull ItemStack itemStack, Consumer<InventoryClickEvent> eventConsumer, IntelligentItemError error) {
        this.itemStack = itemStack;
        this.defaultConsumer = eventConsumer;
        this.error = error;
    }

    /**
     * @param itemStack     The item stack that will be used to create the intelligent item.
     * @param error         The error that will be displayed if the player doesn't have the required permission.
     * @param eventConsumer The consumer that will be called when the item is clicked.
     * @return A new instance of IntelligentItem
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull IntelligentItem of(@NotNull ItemStack itemStack, @NotNull IntelligentItemError error, @NotNull Consumer<InventoryClickEvent> eventConsumer) {
        return new IntelligentItem(itemStack, eventConsumer, error);
    }


    /**
     * @param itemStack     The item that will be displayed in the inventory.
     * @param eventConsumer The consumer that will be called when the item is clicked.
     * @return A new instance of IntelligentItem
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull IntelligentItem of(@NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> eventConsumer) {
        return new IntelligentItem(itemStack, eventConsumer, null);
    }


    /**
     * This function returns a new IntelligentItem with no actions.
     *
     * @param itemStack The itemstack that will be used for the item.
     * @return A new IntelligentItem object.
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull IntelligentItem empty(@NotNull ItemStack itemStack) {
        return new IntelligentItem(itemStack, event -> {
        }, null);
    }

    /**
     * This function returns an IntelligentItem that does nothing when clicked. And displays an error when the given condition does not apply.
     *
     * @param itemStack The itemstack that will be used to create the IntelligentItem.
     * @param error     The error when the given condition does not apply.
     * @return A new instance of IntelligentItem
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull IntelligentItem empty(@NotNull ItemStack itemStack, @NotNull IntelligentItemError error) {
        return new IntelligentItem(itemStack, event -> {
        }, error);
    }


    /**
     * This function takes an ItemStack and returns an IntelligentItem that is ignored.
     * This allows the player who has the inventory open to take the item out.
     *
     * @param itemStack The item to be ignored.
     * @return A new IntelligentItem object.
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull IntelligentItem ignored(@NotNull ItemStack itemStack) {
        return new IntelligentItem(itemStack, null, null);
    }


    /**
     * This function takes an ItemStack and returns an IntelligentItem that is ignored.
     * This allows the player who has the inventory open to take the item out.
     *
     * @param itemStack The item stack that is being ignored.
     * @param error     The error when the given condition does not apply.
     * @return A new instance of the IntelligentItem class.
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull IntelligentItem ignored(@NotNull ItemStack itemStack, @NotNull IntelligentItemError error) {
        return new IntelligentItem(itemStack, null, error);
    }

    /**
     * Deserializes a map to an IntelligentItem.
     *
     * @param map The map to deserialize.
     * @return The deserialized IntelligentItem.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable IntelligentItem deserialize(@NotNull Map<String, Object> map) {
        if (map.isEmpty()) return null;
        IntelligentItem intelligentItem = new IntelligentItem((ItemStack) map.get("item"), (IntelligentItemError) map.get("error"));
        intelligentItem.defaultConsumer = (Consumer<InventoryClickEvent>) map.get("consumer");
        intelligentItem.canClick = (boolean) map.get("can-click");
        intelligentItem.canSee = (boolean) map.get("can-see");
        intelligentItem.id = map.get("id");
        return intelligentItem;
    }

    /**
     * Removes the consumer from an IntelligentItem
     */
    public void clearConsumer() {
        this.defaultConsumer = event -> {
        };
    }

    /**
     * Sets the id of an IntelligentItem
     *
     * @param id      The id of the item
     * @param manager The manager that will be used to update the inventory.
     * @return The IntelligentItem object.
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
        return new IntelligentItem(newItemStack, this.defaultConsumer, this.error);
    }

    /**
     * Changes the ItemStack of an existing Intelligent with changing the consumer.
     *
     * @param newIntelligentItem The new IntelligentItem
     * @return The new intelligent ItemStack
     */
    public @NotNull IntelligentItem update(@NotNull IntelligentItem newIntelligentItem) {
        return new IntelligentItem(newIntelligentItem.getItemStack(), newIntelligentItem.getDefaultConsumer(), this.error);
    }

    /**
     * Serializes the IntelligentItem to a map.
     *
     * @return The serialized map.
     * @see #deserialize(Map) to get back the IntelligentItem.
     */
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("item", this.itemStack);
        map.put("consumer", this.defaultConsumer);
        map.put("error", this.error);
        map.put("can-click", this.canClick);
        map.put("can-see", this.canSee);
        map.put("id", this.id);
        return map;
    }
}
