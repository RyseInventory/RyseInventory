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

package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.SlotIterator;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.IntelligentItemData;
import io.github.rysefoxx.enums.IntelligentType;
import io.github.rysefoxx.enums.InventoryOpenerType;
import io.github.rysefoxx.pattern.ContentPattern;
import io.github.rysefoxx.pattern.SearchPattern;
import io.github.rysefoxx.util.PlaceHolderConstants;
import io.github.rysefoxx.util.SlotUtils;
import io.github.rysefoxx.util.StringConstants;
import io.github.rysefoxx.util.Utils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
@SuppressWarnings("unused")
public class InventoryContents {

    private final Player player;
    private final Pagination pagination;
    private final RyseInventory inventory;

    private final HashMap<String, Object> data = new HashMap<>();
    private final SearchPattern searchPattern = new SearchPattern(this);
    private final ContentPattern contentPattern = new ContentPattern(this);

    public InventoryContents(@NotNull Player player, @NotNull RyseInventory inventory) {
        this.player = player;
        this.inventory = inventory;
        this.pagination = new Pagination(inventory);
    }

    /**
     * With this method you can see if a slot exists in the inventory.
     *
     * @param slot Which slot to look for.
     * @return true if the slot exists in the inventory.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean hasSlot(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        return slot <= this.inventory.size();
    }

    /**
     * With this method you can see if a slot exists in the inventory.
     *
     * @param row    Which row to look for.
     * @param column Which column to look for.
     * @return true if the slot exists in the inventory.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean hasSlot(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return hasSlot(SlotUtils.toSlot(row, column));
    }

    /**
     * Removes the item from the inventory and the associated consumer.
     *
     * @param slot Which slot should be removed?
     * @return true if the item was removed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean removeItemWithConsumer(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        get(slot).ifPresent(IntelligentItem::clearConsumer);
        this.pagination.remove(slot);

        Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
        if (!inventoryOptional.isPresent())
            return false;

        inventoryOptional.get().setItem(slot, null);
        return true;
    }

    /**
     * Removes the item from the inventory and the associated consumer.
     *
     * @param row    The row where the item is located.
     * @param column The column where the item is located.
     * @return true if the item was removed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean removeItemWithConsumer(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return removeItemWithConsumer(SlotUtils.toSlot(row, column));
    }

    /**
     * Removes the first item that matches the parameter.
     *
     * @param item The item to filter for.
     */
    public void removeFirst(@NotNull ItemStack item) {
        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> optional = get(i);
            if (!optional.isPresent()) continue;

            ItemStack itemStack = optional.get().getItemStack();
            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;
            if (!itemStack.isSimilar(item)) continue;

            remove(i);

            Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
            if (!inventoryOptional.isPresent()) break;
            inventoryOptional.get().setItem(i, null);
            optional.get().clearConsumer();
            break;
        }
    }

    /**
     * Removes the first item that matches the parameter.
     *
     * @param material The material to filter for.
     */
    public void removeFirst(@NotNull Material material) {
        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> optional = get(i);
            if (!optional.isPresent()) continue;

            ItemStack itemStack = optional.get().getItemStack();
            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;
            if (itemStack.getType() != material) continue;

            remove(i);

            Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
            if (!inventoryOptional.isPresent()) break;
            inventoryOptional.get().setItem(i, null);
            optional.get().clearConsumer();
            break;
        }
    }

    /**
     * Subtracts a value from an ItemStack.
     *
     * @param item   The ItemStack what should be reduced.
     * @param amount How much to remove.
     * @throws IllegalArgumentException if amount > 64
     * @apiNote If the ItemStack Amount is < 1, the ItemStack will be deleted from the inventory.
     */
    public void subtractFirst(@NotNull ItemStack item, @Nonnegative int amount) throws IllegalArgumentException {
        if (amount > 64)
            throw new IllegalArgumentException(StringConstants.INVALID_AMOUNT);

        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> optional = get(i);
            if (!optional.isPresent()) continue;

            ItemStack itemStack = optional.get().getItemStack();
            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;
            if (!itemStack.isSimilar(item)) continue;

            Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
            if (itemStack.getAmount() - amount < 1) {
                remove(i);
                if (!inventoryOptional.isPresent()) continue;
                inventoryOptional.get().setItem(i, null);
                continue;
            }
            if (!inventoryOptional.isPresent()) continue;
            itemStack.setAmount(itemStack.getAmount() - amount);
            inventoryOptional.get().setItem(i, itemStack);
            break;
        }
    }

    /**
     * Removes all items that match the parameter.
     *
     * @param item The item to filter for.
     */
    public void removeAll(@NotNull ItemStack item) {
        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> optional = get(i);
            if (!optional.isPresent()) continue;

            ItemStack itemStack = optional.get().getItemStack();
            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;
            if (!itemStack.isSimilar(item)) continue;

            remove(i);

            Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
            if (!inventoryOptional.isPresent()) break;
            inventoryOptional.get().setItem(i, null);
            optional.get().clearConsumer();
        }
    }

    /**
     * Removes all items that match the parameter.
     *
     * @param item   The item to filter for.
     * @param amount How much to remove.
     * @throws IllegalArgumentException if amount > 64
     */
    public void removeAll(@NotNull ItemStack item, @Nonnegative int amount) throws IllegalArgumentException {
        if (amount > 64)
            throw new IllegalArgumentException(StringConstants.INVALID_AMOUNT);

        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> optional = get(i);
            if (!optional.isPresent()) continue;

            ItemStack itemStack = optional.get().getItemStack();
            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;
            if (!itemStack.isSimilar(item)) continue;

            Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
            if (itemStack.getAmount() - amount < 1) {
                remove(i);
                if (!inventoryOptional.isPresent()) continue;
                inventoryOptional.get().setItem(i, null);
                optional.get().clearConsumer();
                continue;
            }
            if (!inventoryOptional.isPresent()) continue;
            itemStack.setAmount(itemStack.getAmount() - amount);
            inventoryOptional.get().setItem(i, itemStack);
        }
    }

    /**
     * Removes the very first item that can be found.
     */
    public void removeFirst() {
        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> optional = get(i);
            if (!optional.isPresent()) continue;

            ItemStack itemStack = optional.get().getItemStack();
            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;

            remove(i);

            Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
            if (!inventoryOptional.isPresent()) break;
            inventoryOptional.get().setItem(i, null);
            optional.get().clearConsumer();
            break;
        }
    }

    /**
     * Removes the very first item that can be found.
     *
     * @param amount How much to remove
     * @throws IllegalArgumentException if amount > 64
     */
    public void removeFirst(@Nonnegative int amount) throws IllegalArgumentException {
        if (amount > 64)
            throw new IllegalArgumentException(StringConstants.INVALID_AMOUNT);

        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> optional = get(i);
            if (!optional.isPresent()) continue;

            ItemStack itemStack = optional.get().getItemStack();
            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;

            Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
            if (itemStack.getAmount() - amount < 1) {
                remove(i);
                if (!inventoryOptional.isPresent()) break;
                inventoryOptional.get().setItem(i, null);
                optional.get().clearConsumer();
                break;
            }
            if (!inventoryOptional.isPresent()) break;
            itemStack.setAmount(itemStack.getAmount() - amount);
            inventoryOptional.get().setItem(i, itemStack);
            break;
        }
    }

    /**
     * Outputs all slots of the inventory to perform operations on the slots.
     *
     * @return a list with all slots
     */
    public @NotNull List<Integer> slots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < this.inventory.size(); i++)
            slots.add(i);

        return slots;
    }

    /**
     * With this method you can update the inventory title.
     *
     * @param newTitle The new title
     */
    public void updateTitle(@NotNull String newTitle) {
        this.inventory.updateTitle(this.player, newTitle);
    }

    /**
     * Fills the Border with a intelligent ItemStack regardless of inventory size.
     *
     * @param item The ItemStack which should represent the border
     */
    public void fillBorders(@NotNull IntelligentItem item) {
        int size = this.inventory.size();
        int rows = (size + 1) / 9;

        for (int i = 0; i < rows * 9; i++) {
            if ((i <= 8) || (i >= (rows * 9) - 9)
                    || i == 9 || i == 18
                    || i == 27 || i == 36
                    || i == 17 || i == 26
                    || i == 35 || i == 44)
                set(i, item);
        }
    }

    /**
     * Fills the Border with a ItemStack regardless of inventory size.
     *
     * @param itemStack The ItemStack which should represent the border
     */
    public void fillBorders(@NotNull ItemStack itemStack) {
        fillBorders(IntelligentItem.empty(itemStack));
    }

    /**
     * Fills the Border with a ItemStack regardless of inventory size.
     *
     * @param itemStack The ItemStack which should represent the border
     * @param type      The type of the item
     */
    public void fillBorders(@NotNull ItemStack itemStack, @NotNull IntelligentType type) {
        if (type == IntelligentType.EMPTY) {
            fillBorders(IntelligentItem.empty(itemStack));
            return;
        }
        if (type == IntelligentType.IGNORED)
            fillBorders(IntelligentItem.ignored(itemStack));
    }

    /**
     * Method to cache data in the content
     *
     * @param key   The key of the data
     * @param value The value of the data
     */
    public <T> void setData(@NotNull String key, @NotNull T value) {
        this.data.put(key, value);
    }

    /**
     * Method to get data in the content
     *
     * @param key The key of the data
     * @return The cached value, or null if not cached
     */
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getData(@NotNull String key) {
        if (!this.data.containsKey(key)) return null;

        return (T) this.data.get(key);
    }

    /**
     * Removes data with the associated key.
     *
     * @param key The key that will be removed together with the value.
     * @return true if the key was found and removed, false if not.
     */
    public boolean removeData(@NotNull String key) {
        if (!this.data.containsKey(key)) return false;

        this.data.remove(key);
        return true;
    }

    /**
     * Removes all data in the inventory.
     */
    public void clearData() {
        this.data.clear();
    }

    /**
     * Removes all data in the inventory and returns all values in the consumer.
     */
    public void clearData(@NotNull Consumer<List<Object>> consumer) {
        List<Object> objects = Arrays.asList(this.data.values().toArray());
        consumer.accept(objects);

        this.data.clear();
    }

    /**
     * Removes all data in the inventory and returns all keys and values in the consumer.
     */
    public void clearData(@NotNull BiConsumer<List<String>, List<Object>> consumer) {
        List<String> keys = new ArrayList<>(this.data.keySet());
        List<Object> values = Arrays.asList(this.data.values().toArray());

        consumer.accept(keys, values);
        this.data.clear();
    }

    /**
     * Removes data with the associated key. The value is then passed in the consumer.
     *
     * @param key      The key that will be removed together with the value.
     * @param consumer The value that is removed.
     */
    @SuppressWarnings("unchecked")
    public <T> void removeData(@NotNull String key, @NotNull Consumer<T> consumer) {
        if (!this.data.containsKey(key)) return;

        consumer.accept((T) this.data.remove(key));
    }

    /**
     * Method to get data in the content
     *
     * @param key          The key of the data
     * @param defaultValue value when key is invalid
     * @return The cached value
     */
    @SuppressWarnings("unchecked")
    public @NotNull <T> T getData(@NotNull String key, @NotNull Object defaultValue) {
        if (!this.data.containsKey(key)) return (T) defaultValue;

        return (T) this.data.get(key);
    }

    /**
     * Finds the right border based on the start slot and returns it.
     *
     * @param startSlot The start slot.
     * @return the right border index.
     * @throws IllegalArgumentException when startSlot > 53
     */
    public int findRightBorder(@Nonnegative int startSlot) throws IllegalArgumentException {
        if (startSlot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if (isRightBorder(startSlot)) return startSlot;

        while (!isRightBorder(startSlot))
            startSlot++;
        return startSlot;
    }

    /**
     * Finds the right border based on the start slot and returns it.
     *
     * @param row    The row of the start slot.
     * @param column The column of the start slot.
     * @return the right border index.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public int findRightBorder(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return findRightBorder(SlotUtils.toSlot(row, column));
    }

    /**
     * Finds the left border based on the start slot and returns it.
     *
     * @param startSlot The start slot.
     * @return the left border index.
     * @throws IllegalArgumentException when startSlot > 53
     */
    public int findLeftBorder(@Nonnegative int startSlot) throws IllegalArgumentException {
        if (startSlot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if (isLeftBorder(startSlot)) return startSlot;

        while (!isLeftBorder(startSlot))
            startSlot--;
        return startSlot;
    }

    /**
     * Finds the left border based on the start slot and returns it.
     *
     * @param row    The row of the start slot.
     * @param column The column of the start slot.
     * @return the left border index.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public int findLeftBorder(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return findLeftBorder(SlotUtils.toSlot(row, column));
    }

    /**
     * @param slot The slot to check
     * @return true if the slot is in the middle of the inventory, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @throws IllegalStateException    If the inventory type is not supported.
     */
    public boolean isMiddle(@Nonnegative int slot) throws IllegalArgumentException, IllegalStateException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        InventoryOpenerType type = this.inventory.getInventoryOpenerType();
        int inventorySize = this.inventory.size();

        if (type != InventoryOpenerType.CHEST
                && type != InventoryOpenerType.ENDER_CHEST
                && type != InventoryOpenerType.DROPPER
                && type != InventoryOpenerType.DISPENSER
                && type != InventoryOpenerType.CRAFTING_TABLE
                && type != InventoryOpenerType.HOPPER)
            throw new IllegalStateException("isMiddle only works for chests, ender chests, hoppers, dispensers, crafting tables, and droppers");

        int rows = inventorySize / 9;
        double middle = rows / 2D;
        int endSlot;
        boolean even;

        even = !String.valueOf(middle).contains(".") || Integer.parseInt(String.valueOf(middle).split("\\.")[1]) <= 0;

        if (!even)
            middle = Math.round(middle);

        endSlot = even ? (int) (((middle * 9) + 9) - 1) : (int) ((middle * 9) - 1);

        switch (type) {
            case CHEST:
            case ENDER_CHEST: {
                return (even ? slot >= endSlot - 18 : slot >= endSlot - 9) && (slot <= endSlot);
            }
            case HOPPER: {
                return slot == 2;
            }
            case DROPPER:
            case DISPENSER:
            case CRAFTING_TABLE: {
                return slot >= 3 && slot <= 5;
            }
        }
        return false;

    }

    /**
     * @param row    The row to check
     * @param column The column to check
     * @return true if the slot is in the middle of the inventory, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @throws IllegalStateException    If the inventory type is not supported.
     */
    public boolean isMiddle(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException, IllegalStateException {
        return isMiddle(SlotUtils.toSlot(row, column));
    }

    /**
     * @return true if the specified slot is on the right side.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isRightBorder(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        InventoryOpenerType type = this.inventory.getInventoryOpenerType();

        switch (type) {
            case CHEST:
            case ENDER_CHEST: {
                return slot == 8 || slot == 17 || slot == 26 || slot == 35 || slot == 44 || slot == 53;
            }
            case HOPPER: {
                return slot == 4;
            }
            case DROPPER:
            case DISPENSER:
            case CRAFTING_TABLE: {
                return slot == 2 || slot == 5 || slot == 8;
            }
            case FURNACE:
            case BREWING_STAND: {
                return slot == 2;
            }
            case ENCHANTMENT_TABLE: {
                return slot == 1;
            }
        }
        return false;
    }

    /**
     * @return true if the specified slot is on the right side.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isRightBorder(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return isRightBorder(SlotUtils.toSlot(row, column));
    }

    /**
     * @param slot The slot to check
     * @return true if the slot is in the corner, false if not
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @throws IllegalStateException    If the inventory type is not supported.
     */
    public boolean isCorner(@Nonnegative int slot) throws IllegalArgumentException, IllegalStateException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        InventoryOpenerType type = this.inventory.getInventoryOpenerType();
        int inventorySize = this.inventory.size();

        if (type != InventoryOpenerType.CHEST
                && type != InventoryOpenerType.ENDER_CHEST
                && type != InventoryOpenerType.DROPPER
                && type != InventoryOpenerType.DISPENSER
                && type != InventoryOpenerType.CRAFTING_TABLE
                && type != InventoryOpenerType.HOPPER)
            throw new IllegalStateException("isCorner only works for chests, ender chests, hoppers, dispensers, crafting tables, and droppers");

        switch (type) {
            case CHEST:
            case ENDER_CHEST: {
                return slot == 0 || slot == 8 || slot == inventorySize - 1 || slot == inventorySize - 9;
            }
            case HOPPER: {
                return slot == 0 || slot == 4;
            }
            case DROPPER:
            case DISPENSER:
            case CRAFTING_TABLE: {
                return slot == 0 || slot == 2 || slot == 6 || slot == 8;
            }
        }
        return false;
    }

    /**
     * @param row    The row to check
     * @param column The column to check
     * @return true if the slot is in the corner, false if not
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @throws IllegalStateException    If the inventory type is not supported.
     */
    public boolean isCorner(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException, IllegalStateException {
        return isCorner(SlotUtils.toSlot(row, column));
    }

    /**
     * @param slot The slot to check
     * @return true when the slot is at the top of the inventory, false otherwise.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isTop(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        InventoryOpenerType type = this.inventory.getInventoryOpenerType();

        switch (type) {
            case CHEST:
            case ENDER_CHEST: {
                return slot <= 8;
            }
            case HOPPER:
            case ENCHANTMENT_TABLE: {
                return true;
            }
            case FURNACE: {
                return slot == 0;
            }
            case BREWING_STAND: {
                return slot == 3 || slot == 4;
            }
            case DROPPER:
            case DISPENSER:
            case CRAFTING_TABLE: {
                return slot <= 2;
            }
        }
        return false;
    }

    /**
     * @param row    The row to check
     * @param column The column to check
     * @return true when the slot is at the top of the inventory, false otherwise.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isTop(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return isTop(SlotUtils.toSlot(row, column));
    }

    /**
     * @param slot The slot to check
     * @return true when the slot is at the bottom of the inventory, false otherwise.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isBottom(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        InventoryOpenerType type = this.inventory.getInventoryOpenerType();
        int inventorySize = this.inventory.size();

        switch (type) {
            case CHEST:
            case ENDER_CHEST: {
                return (slot >= inventorySize - 9) && (slot <= inventorySize);
            }
            case HOPPER:
            case ENCHANTMENT_TABLE: {
                return true;
            }
            case FURNACE:
            case BREWING_STAND: {
                return slot == 1;
            }
            case DROPPER:
            case DISPENSER:
            case CRAFTING_TABLE: {
                return (slot >= inventorySize - 3) && (slot <= inventorySize);
            }
        }
        return false;
    }

    /**
     * @param row    The row to check
     * @param column The column to check
     * @return true when the slot is at the bottom of the inventory, false otherwise.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isBottom(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return isBottom(SlotUtils.toSlot(row, column));
    }

    /**
     * @return true if the specified slot is on the left side.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isLeftBorder(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        InventoryOpenerType type = this.inventory.getInventoryOpenerType();

        switch (type) {
            case CHEST:
            case ENDER_CHEST: {
                return slot == 0 || slot == 9 || slot == 18 || slot == 27 || slot == 36 || slot == 45;
            }
            case HOPPER:
            case ENCHANTMENT_TABLE:
            case FURNACE:
            case BREWING_STAND: {
                return slot == 0;
            }
            case DROPPER:
            case DISPENSER:
            case CRAFTING_TABLE: {
                return slot == 0 || slot == 3 || slot == 6;
            }
        }
        return false;
    }

    /**
     * @return true if the specified slot is on the left side.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean isLeftBorder(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return isLeftBorder(SlotUtils.toSlot(row, column));
    }

    /**
     * @return The first slot that is empty.
     */
    public @NotNull Optional<Integer> firstEmpty() {
        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> item = get(i);

            if (item.isPresent()) continue;
            return Optional.of(i);
        }
        return Optional.empty();
    }

    /**
     * @return The last slot that is empty.
     */
    public @NotNull Optional<Integer> lastEmpty() {
        int nextSlot = -1;

        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> item = get(i);

            if (item.isPresent()) continue;
            nextSlot = i;
        }
        return Optional.of(nextSlot);
    }

    /**
     * Adds an item to the inventory in the first free place.
     *
     * @param item The ItemStack to be displayed in the inventory
     */
    public void add(@NotNull IntelligentItem item) {
        firstEmpty().ifPresent(slot -> this.pagination.setItem(slot, item));
    }

    /**
     * Add multiple items to the inventory in the first free place.
     *
     * @param items The ItemStacks to be displayed in the inventory
     */
    public void add(IntelligentItem @NotNull ... items) {
        for (IntelligentItem item : items)
            add(item);
    }

    /**
     * Adds an item to the inventory in the first free place.
     *
     * @param itemStack The ItemStack to be displayed in the inventory
     */
    public void add(@NotNull ItemStack itemStack) {
        add(IntelligentItem.empty(itemStack));
    }

    /**
     * Add multiple items to the inventory in the first free place.
     *
     * @param items The ItemStacks to be displayed in the inventory
     */
    public void add(ItemStack @NotNull ... items) {
        for (ItemStack item : items)
            add(IntelligentItem.empty(item));
    }

    /**
     * Adds an item to the inventory in the first free place.
     *
     * @param itemStack The ItemStack to be displayed in the inventory
     * @param type      The type of the item
     */
    public void add(@NotNull ItemStack itemStack, @NotNull IntelligentType type) {
        if (type == IntelligentType.EMPTY) {
            add(IntelligentItem.empty(itemStack));
            return;
        }
        if (type == IntelligentType.IGNORED)
            add(IntelligentItem.ignored(itemStack));
    }

    /**
     * Adds an item to the inventory in the first free place.
     *
     * @param items The ItemStacks to be displayed in the inventory
     * @param type  The type of the item
     */
    public void add(@NotNull IntelligentType type, ItemStack @NotNull ... items) {
        for (ItemStack item : items) {
            if (type == IntelligentType.EMPTY) {
                add(IntelligentItem.empty(item));
                continue;
            }
            if (type == IntelligentType.IGNORED)
                add(IntelligentItem.ignored(item));
        }
    }

    /**
     * This method allows you to reload the contents of the inventory.
     */
    public void reload() {
        if (this.inventory.getProvider() == null) return;
        clear(this.pagination.page() - 1);

        InventoryContents contents = new InventoryContents(this.player, this.inventory);
        this.inventory.getManager().setContents(this.player.getUniqueId(), contents);

        if (this.inventory.getSlideAnimator() == null) {
            this.inventory.getProvider().init(this.player, contents);
        } else {
            this.inventory.getProvider().init(this.player, contents, this.inventory.getSlideAnimator());
        }

        this.inventory.loadByPage(contents);
        this.inventory.load(contents.pagination(), this.player, contents.pagination().page() - 1);
    }

    /**
     * Sets a fixed smart ItemStack in the inventory on a specified page.
     *
     * @param slot Where should the item be placed?
     * @param page On which page should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @apiNote First page is 0, second page is 1, etc.
     */
    public void setWithinPage(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        this.pagination.setItem(slot, page, item);
    }

    /**
     * Sets a fixed IntelligentItem in the inventory on a specified page.
     *
     * @param row    The row of the inventory
     * @param column The column of the inventory
     * @param page   On which page should the item be placed?
     * @param item   The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @apiNote First page is 0, second page is 1, etc.
     */
    public void setWithinPage(@Nonnegative int row, @Nonnegative int column, @Nonnegative int page, @NotNull IntelligentItem item) throws IllegalArgumentException {
        setWithinPage(SlotUtils.toSlot(row, column), page, item);
    }

    /**
     * Sets an item to multiple slots within a page.
     *
     * @param slots Where should the item be placed everywhere?
     * @param page  On which page should the item be placed?
     * @param item  The ItemStack to be displayed in the inventory
     * @apiNote First page is 0, second page is 1, etc.
     */
    public void setWithinPage(@NotNull List<Integer> slots, @Nonnegative int page, @NotNull IntelligentItem item) {
        slots.forEach(slot -> setWithinPage(slot, page, item));
    }

    /**
     * Places an item on multiple pages in multiple slots.
     *
     * @param slots Where should the item be placed everywhere?
     * @param pages On which pages should the item be placed?
     * @param item  The ItemStack to be displayed in the inventory
     * @apiNote First page is 0, second page is 1, etc.
     */
    public void setWithinPage(@NotNull List<Integer> slots, @NotNull List<Integer> pages, @NotNull IntelligentItem item) {
        slots.forEach(slot -> pages.forEach(page -> setWithinPage(slot, page, item)));
    }

    /**
     * Fills a row in the specified page.
     *
     * @param slot The slot where the item should be placed
     * @param page On which page should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++)
            setWithinPage(slot, page, item);
    }

    /**
     * Fills a row in the specified page.
     *
     * @param slot The slot where the item should be placed
     * @param page On which page should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @Nonnegative int page, @NotNull ItemStack item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++)
            setWithinPage(slot, page, IntelligentItem.empty(item));
    }

    /**
     * Fills a row in the specified page.
     *
     * @param slot The slot where the item should be placed
     * @param page On which page should the item be placed?
     * @param type The type of the item
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentType type, @NotNull ItemStack item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        IntelligentItem intelligentItem;

        if (type == IntelligentType.IGNORED) {
            intelligentItem = IntelligentItem.ignored(item);
        } else {
            intelligentItem = IntelligentItem.empty(item);
        }

        for (int i = slot; i < difference + 1; i++)
            setWithinPage(slot, page, intelligentItem);
    }

    /**
     * Starting at the slot, the row is completely filled from left to right.
     *
     * @param slot Where to start placing the items.
     * @param item The item to be placed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++)
            set(i, item);
    }

    /**
     * Starting at the slot, the row is completely filled from left to right.
     *
     * @param slot      Where to start placing the items.
     * @param item      The item to be placed.
     * @param appliedTo A consumer that returns slot and item.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @NotNull IntelligentItem item, @NotNull BiConsumer<Integer, @NotNull IntelligentItem> appliedTo) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++) {
            set(i, item);
            appliedTo.accept(i, item);
        }
    }

    /**
     * Starting at the slot, the row is completely filled from left to right.
     *
     * @param slot Where to start placing the items.
     * @param item The item to be placed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @NotNull ItemStack item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++)
            set(i, item);
    }

    /**
     * Starting at the slot, the row is completely filled from left to right.
     *
     * @param slot      Where to start placing the items.
     * @param item      The item to be placed.
     * @param appliedTo A consumer that returns slot and item.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @NotNull ItemStack item, @NotNull BiConsumer<Integer, @NotNull ItemStack> appliedTo) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++) {
            set(i, item);
            appliedTo.accept(i, item);
        }
    }

    /**
     * Starting at the slot, the row is completely filled from left to right.
     *
     * @param slot Where to start placing the items.
     * @param item The item to be placed.
     * @param type The type of the item
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @NotNull ItemStack item, @NotNull IntelligentType type) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++)
            set(i, item, type);
    }

    /**
     * Starting at the slot, the row is completely filled from left to right.
     *
     * @param slot      Where to start placing the items.
     * @param item      The item to be placed.
     * @param type      The type of the item
     * @param appliedTo A consumer that returns slot and item.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillRow(@Nonnegative int slot, @NotNull ItemStack item, @NotNull IntelligentType type, @NotNull BiConsumer<Integer, @NotNull ItemStack> appliedTo) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        int difference = slot + (findRightBorder(slot) - slot);
        for (int i = slot; i < difference + 1; i++) {
            set(i, item, type);
            appliedTo.accept(i, item);
        }
    }

    /**
     * Starting at the slot, all the way down the item is placed in the same column.
     *
     * @param slot Where to start placing the items.
     * @param item The item to be placed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9)
            set(i, item);
    }

    /**
     * Starting with the slot, the object is placed in the same column from top to bottom, but only in the specified page.
     *
     * @param slot Where to start placing the items.
     * @param page On which page should the item be placed?
     * @param item The item to be placed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9)
            setWithinPage(i, page, item);
    }

    /**
     * Starting with the slot, the object is placed in the same column from top to bottom, but only in the specified page.
     *
     * @param slot Where to start placing the items.
     * @param page On which page should the item be placed?
     * @param item The item to be placed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @Nonnegative int page, @NotNull ItemStack item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9)
            setWithinPage(i, page, IntelligentItem.empty(item));
    }

    /**
     * Starting with the slot, the object is placed in the same column from top to bottom, but only in the specified page.
     *
     * @param slot Where to start placing the items.
     * @param page On which page should the item be placed?
     * @param type The type of the item
     * @param item The item to be placed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentType type, @NotNull ItemStack item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        IntelligentItem intelligentItem;

        if (type == IntelligentType.IGNORED) {
            intelligentItem = IntelligentItem.ignored(item);
        } else {
            intelligentItem = IntelligentItem.empty(item);
        }

        for (int i = slot; i < this.inventory.size(); i += 9)
            setWithinPage(i, page, intelligentItem);
    }

    /**
     * Starting at the slot, all the way down the item is placed in the same column.
     *
     * @param slot      Where to start placing the items.
     * @param item      The item to be placed.
     * @param appliedTo A consumer that returns slot and item.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @NotNull IntelligentItem item, @NotNull BiConsumer<Integer, @NotNull IntelligentItem> appliedTo) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9) {
            set(i, item);
            appliedTo.accept(i, item);
        }
    }

    /**
     * Starting at the slot, all the way down the item is placed in the same column.
     *
     * @param slot Where to start placing the items.
     * @param item The item to be placed.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @NotNull ItemStack item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9)
            set(i, item);
    }

    /**
     * Starting at the slot, all the way down the item is placed in the same column.
     *
     * @param slot      Where to start placing the items.
     * @param item      The item to be placed.
     * @param appliedTo A consumer that returns slot and item.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @NotNull ItemStack item, @NotNull BiConsumer<Integer, @NotNull ItemStack> appliedTo) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9) {
            set(i, item);
            appliedTo.accept(i, item);
        }
    }

    /**
     * Starting at the slot, all the way down the item is placed in the same column.
     *
     * @param slot Where to start placing the items.
     * @param item The item to be placed.
     * @param type The type of the item
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @NotNull ItemStack item, @NotNull IntelligentType type) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9)
            set(i, item, type);
    }

    /**
     * Starting at the slot, all the way down the item is placed in the same column.
     *
     * @param slot      Where to start placing the items.
     * @param item      The item to be placed.
     * @param type      The type of the item
     * @param appliedTo A consumer that returns slot and item.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void fillColumn(@Nonnegative int slot, @NotNull ItemStack item, @NotNull IntelligentType type, @NotNull BiConsumer<Integer, @NotNull ItemStack> appliedTo) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        for (int i = slot; i < this.inventory.size(); i += 9) {
            set(i, item, type);
            appliedTo.accept(i, item);
        }
    }

    /**
     * Fills the whole inventory with an ItemStack.
     *
     * @param item The item with which the inventory should be filled.
     */
    public void fill(@NotNull IntelligentItem item) {
        for (int i = 0; i < this.inventory.size(); i++)
            set(i, item);
    }

    /**
     * Fills the whole inventory with an ItemStack.
     *
     * @param item The item with which the inventory should be filled.
     */
    public void fill(@NotNull ItemStack item) {
        for (int i = 0; i < this.inventory.size(); i++)
            set(i, IntelligentItem.empty(item));
    }

    /**
     * Fills the whole inventory with an ItemStack.
     *
     * @param item The item with which the inventory should be filled.
     * @param type The type of the item
     */
    public void fill(@NotNull ItemStack item, @NotNull IntelligentType type) {
        for (int i = 0; i < this.inventory.size(); i++) {
            if (type == IntelligentType.EMPTY) {
                set(i, IntelligentItem.empty(item));
                continue;
            }
            if (type == IntelligentType.IGNORED)
                set(i, IntelligentItem.ignored(item));
        }
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param slot Where should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void set(@Nonnegative int slot, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        this.pagination.setItem(slot, item);
    }

    /**
     * Sets a fixed ItemStack in the inventory.
     *
     * @param slot      Where should the item be placed?
     * @param itemStack The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void set(@Nonnegative int slot, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        set(slot, IntelligentItem.empty(itemStack));
    }

    /**
     * Sets a fixed ItemStack in the inventory.
     *
     * @param row       The row where the item should be placed
     * @param column    The column where the item should be placed
     * @param itemStack The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        set(SlotUtils.toSlot(row, column), IntelligentItem.empty(itemStack));
    }

    /**
     * Sets a fixed ItemStack in the inventory.
     *
     * @param slot      Where should the item be placed?
     * @param itemStack The ItemStack to be displayed in the inventory
     * @param type      The type of the item
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void set(@Nonnegative int slot, @NotNull ItemStack itemStack, @NotNull IntelligentType type) throws IllegalArgumentException {
        if (type == IntelligentType.EMPTY) {
            set(slot, IntelligentItem.empty(itemStack));
            return;
        }
        if (type == IntelligentType.IGNORED)
            set(slot, IntelligentItem.ignored(itemStack));
    }

    /**
     * Sets a fixed ItemStack in the inventory.
     *
     * @param row       The row where the item should be placed
     * @param column    The column where the item should be placed
     * @param itemStack The ItemStack to be displayed in the inventory
     * @param type      The type of the item
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull ItemStack itemStack, @NotNull IntelligentType type) throws IllegalArgumentException {
        set(SlotUtils.toSlot(row, column), itemStack, type);
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param slots Where should the item be placed everywhere?
     * @param item  The ItemStack to be displayed in the inventory
     */
    public void set(@NotNull List<Integer> slots, @NotNull IntelligentItem item) {
        slots.forEach(slot -> set(slot, item));
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param row    The row
     * @param column The column
     * @param item   The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull IntelligentItem item) throws IllegalArgumentException {
        set(SlotUtils.toSlot(row, column), item);
    }

    /**
     * Preserve the position of the item in the inventory.
     *
     * @param itemStack ItemStack to look for in the inventory.
     * @return The slot of the item or empty Optional if the item was not found.
     */
    public @NotNull Optional<Integer> getPositionOfItem(@NotNull ItemStack itemStack) {
        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> item = get(i);
            if (!item.isPresent()) continue;
            if (!item.get().getItemStack().isSimilar(itemStack)) continue;

            return Optional.of(i);
        }
        return Optional.empty();
    }

    /**
     * Preserve the position of the item in the inventory.
     *
     * @param intelligentItem IntelligentItem to look for in the inventory.
     * @return The slot of the item or empty Optional if the item was not found.
     */
    public @NotNull Optional<Integer> getPositionOfItem(@NotNull IntelligentItem intelligentItem) {
        return getPositionOfItem(intelligentItem.getItemStack());
    }

    /**
     * Preserve the position of the item in the inventory.
     *
     * @param itemStack ItemStack to look for in the inventory.
     * @return Returns a pair, or nothing if nothing could be found.
     * @implNote The pair contains the row and column of the item. Pair#getLeft() is the row and Pair#getRight() is the column.
     */
    public @NotNull Optional<Pair<Integer, Integer>> getCoordinationOfItem(@NotNull ItemStack itemStack) {
        for (int i = 0; i < this.inventory.size(); i++) {
            Optional<IntelligentItem> item = get(i);
            if (!item.isPresent()) continue;
            if (!item.get().getItemStack().isSimilar(itemStack)) continue;

            return Optional.of(Pair.of(i / 9, i % 9));
        }
        return Optional.empty();
    }

    /**
     * Preserve the position of the item in the inventory.
     *
     * @param intelligentItem IntelligentItem to look for in the inventory.
     * @return Returns a pair, or nothing if nothing could be found.
     * @implNote The pair contains the row and column of the item. Pair#getLeft() is the row and Pair#getRight() is the column.
     */
    public @NotNull Optional<Pair<Integer, Integer>> getCoordinationOfItem(@NotNull IntelligentItem intelligentItem) {
        return getCoordinationOfItem(intelligentItem.getItemStack());
    }

    /**
     * @return A list of all the items in the inventory.
     */
    public @NotNull List<IntelligentItem> getAll() {
        List<IntelligentItem> items = new ArrayList<>();
        for (int i = 0; i < this.inventory.size(); i++)
            get(i).ifPresent(items::add);

        return items;
    }

    /**
     * @param page The page to look for.
     * @return All items on a given page.
     */
    public @NotNull List<IntelligentItem> getAllWithinPage(@Nonnegative int page) {
        List<IntelligentItem> items = new ArrayList<>();
        for (int i = 0; i < this.inventory.size(); i++)
            getWithinPage(i, page).ifPresent(items::add);

        return items;
    }

    /**
     * @param slot The slot to get the item from.
     * @param page The page to look for the item in.
     * @return The item in the slot on the page, or empty Optional if the slot is empty.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @apiNote First page is 0, second page is 1, etc.
     */
    public Optional<IntelligentItem> getWithinPage(@Nonnegative int slot, @Nonnegative int page) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        return Optional.ofNullable(this.pagination.get(slot, page));
    }

    /**
     * @param row    The row to get the item from.
     * @param column The column to look for the item in.
     * @param page   The page to look for the item in.
     * @return The item in the slot on the page, or empty Optional if the slot is empty.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     * @apiNote First page is 0, second page is 1, etc.
     */
    public Optional<IntelligentItem> getWithinPage(@Nonnegative int row, @Nonnegative int column, @Nonnegative int page) throws IllegalArgumentException {
        return getWithinPage(SlotUtils.toSlot(row, column), page);
    }

    /**
     * Fetches a intelligent ItemStack based on the slot.
     *
     * @param slot The slot
     * @return The intelligent ItemStack or an empty Optional instance.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public Optional<IntelligentItem> get(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        return Optional.ofNullable(this.pagination.get(slot));
    }

    /**
     * Get a intelligent ItemStack based on the row and column.
     *
     * @param row    The row
     * @param column The column
     * @return The intelligent ItemStack or an empty Optional instance.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public @NotNull Optional<IntelligentItem> get(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        return get(SlotUtils.toSlot(row, column));
    }

    /**
     * Get all intelligent ItemStacks.
     *
     * @param slots All slots where you want to watch.
     * @return All intelligent ItemStacks that could be found.
     */
    public @NotNull List<IntelligentItem> get(@NotNull List<Integer> slots) {
        List<IntelligentItem> items = new ArrayList<>();
        slots.forEach(slot -> get(slot).ifPresent(items::add));

        return items;
    }

    /**
     * Get multiple Intelligent ItemStacks within the specified range.
     *
     * @param areaStart The start slot
     * @param areaStop  The end slot
     * @return All intelligent ItemStacks that could be found.
     * @throws IllegalArgumentException if areaStart, areaStop > 53
     */
    public @NotNull List<IntelligentItem> findInArea(@Nonnegative int areaStart, @Nonnegative int areaStop) throws IllegalArgumentException {
        if (areaStart > 53)
            throw new IllegalArgumentException("The areaStart must not be larger than 53.");

        if (areaStop > 53)
            throw new IllegalArgumentException("The areaStop must not be larger than 53.");

        Preconditions.checkArgument(areaStart <= areaStop, "areaStop must be at least 1 greater than areaStart.");

        List<IntelligentItem> items = new ArrayList<>();
        for (int i = areaStart; i <= areaStop; i++)
            get(i).ifPresent(items::add);

        return items;
    }

    /**
     * Updates the lore of the item.
     *
     * @param index Where in the lore should the new line be located.
     * @param line  The new line in the ItemStack
     * @return true if the lore was updated, false if the lore was not updated.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size or index >= lore.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public boolean updateLore(@Nonnegative int row, @Nonnegative int column, @Nonnegative int index, @NotNull String line) throws IllegalArgumentException, IllegalStateException {
        return updateLore(SlotUtils.toSlot(row, column), index, line);
    }

    /**
     * Updates the lore of the item.
     *
     * @param slot In which slot in the inventory is the ItemStack located.
     * @param lore The new lore
     * @return true if the lore was updated, false if the lore was not updated.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size or index >= lore.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public boolean updateLore(@Nonnegative int slot, @NotNull List<String> lore) throws IllegalArgumentException, IllegalStateException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        Optional<IntelligentItem> itemOptional = get(slot);
        if (!itemOptional.isPresent()) return false;

        IntelligentItem item = itemOptional.get();
        ItemStack itemStack = item.getItemStack();
        if (!itemStack.hasItemMeta())
            throw new IllegalStateException("ItemStack has no ItemMeta");

        if (!itemStack.getItemMeta().hasLore())
            throw new IllegalStateException("ItemStack has no lore");

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(lore);

        itemStack.setItemMeta(itemMeta);
        update(slot, itemStack);
        return true;
    }

    /**
     * Updates the lore of the item.
     *
     * @param slot  In which slot in the inventory is the ItemStack located.
     * @param index Where in the lore should the new line be located.
     * @param line  The new line in the ItemStack
     * @return true if the lore was updated, false if the lore was not updated.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size or index >= lore.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public boolean updateLore(@Nonnegative int slot, @Nonnegative int index, @NotNull String line) throws IllegalArgumentException, IllegalStateException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        Optional<IntelligentItem> itemOptional = get(slot);
        if (!itemOptional.isPresent()) return false;

        IntelligentItem item = itemOptional.get();
        ItemStack itemStack = item.getItemStack();
        if (!itemStack.hasItemMeta())
            throw new IllegalStateException("ItemStack has no ItemMeta");

        if (!itemStack.getItemMeta().hasLore())
            throw new IllegalStateException("ItemStack has no lore");

        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = itemMeta.getLore();

        if (index >= lore.size())
            throw new IllegalArgumentException("The index must not be larger than " + lore.size());

        lore.set(index, line);
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);

        update(slot, itemStack);
        return true;
    }

    /**
     * Updates multiple lore lines at once.
     *
     * @param slots   The slots where the items are located.
     * @param indexes The indexes where the lines should be located.
     * @param lines   The lines that should be updated.
     * @return true if all lines of all items were updated, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size or index >= lore.size() or lines.size() != slots.size() or indexes.size() != slots.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public boolean updateLore(@NotNull List<Integer> slots, @NotNull List<Integer> indexes, @NotNull List<String> lines) throws IllegalArgumentException, IllegalStateException {
        int slotsSize = slots.size();
        int indexesSize = indexes.size();
        int linesSize = lines.size();

        if (slotsSize != indexesSize || slotsSize != linesSize)
            throw new IllegalArgumentException("slots, indexes and lines must have the same size.");

        int updated = 0;

        for (int i = 0; i < slotsSize; i++) {
            updateLore(slots.get(i), indexes.get(i), lines.get(i));
            updated++;
        }
        return updated >= slotsSize;
    }

    /**
     * Updates the ItemStack in the same place with a new ItemStack.
     *
     * @param slot      The slot
     * @param itemStack The new ItemStack what should be displayed.
     * @return true if the ItemStack was updated, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean update(@Nonnegative int slot, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        Optional<IntelligentItem> itemOptional = get(slot);
        if (!itemOptional.isPresent()) return false;

        IntelligentItem item = itemOptional.get();
        IntelligentItem newItem = item.update(itemStack);

        set(slot, newItem);

        Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
        inventoryOptional.ifPresent(savedInventory -> savedInventory.setItem(slot, newItem.getItemStack()));
        return true;
    }

    /**
     * The display name of the ItemStack is updated.
     *
     * @param slot        In which slot is the item located?
     * @param displayName The new display name
     * @return true if the display name was updated, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean updateDisplayName(@Nonnegative int slot, @NotNull String displayName) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        Optional<IntelligentItem> itemOptional = get(slot);
        if (!itemOptional.isPresent()) return false;

        IntelligentItem item = itemOptional.get();
        ItemStack itemStack = item.getItemStack();

        if (!itemStack.hasItemMeta())
            itemStack.setItemMeta(Bukkit.getItemFactory().getItemMeta(itemStack.getType()));

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemStack.setItemMeta(itemMeta);

        update(slot, itemStack);
        return true;
    }

    /**
     * Updates the display name of the passed item and places it at the slot.
     *
     * @param slot        Where should the item be placed?
     * @param item        The item which display name should be updated.
     * @param displayName The new display name
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public void updateDisplayName(@Nonnegative int slot, @NotNull IntelligentItem item, @NotNull String displayName) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        if(slot > this.inventory.size())
            throw new IllegalArgumentException(Utils.replace(PlaceHolderConstants.INVALID_SLOT, "%temp%", this.inventory.size()));

        ItemStack itemStack = item.getItemStack();

        if (!itemStack.hasItemMeta())
            itemStack.setItemMeta(Bukkit.getItemFactory().getItemMeta(itemStack.getType()));

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemStack.setItemMeta(itemMeta);

        update(slot, itemStack);
    }

    /**
     * Updates the display name of the passed item.
     *
     * @param item        The item which display name should be updated.
     * @param displayName The new display name
     * @return The updated item
     */
    public @NotNull IntelligentItem updateDisplayName(@NotNull IntelligentItem item, @NotNull String displayName) throws IllegalArgumentException {
        ItemStack itemStack = item.getItemStack();

        if (!itemStack.hasItemMeta())
            itemStack.setItemMeta(Bukkit.getItemFactory().getItemMeta(itemStack.getType()));

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemStack.setItemMeta(itemMeta);

        return item.update(itemStack);
    }

    /**
     * Updates the display name of the passed item.
     *
     * @param itemStack   The item which display name should be updated.
     * @param displayName The new display name
     * @return The updated item
     */
    public @NotNull ItemStack updateDisplayName(@NotNull ItemStack itemStack, @NotNull String displayName) throws IllegalArgumentException {
        if (!itemStack.hasItemMeta())
            itemStack.setItemMeta(Bukkit.getItemFactory().getItemMeta(itemStack.getType()));

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Updates the ItemStack in the same place with a new IntelligentItem.
     *
     * @param slot            The slot
     * @param intelligentItem The new IntelligentItem what should be displayed.
     * @return true if the ItemStack was updated, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean update(@Nonnegative int slot, @NotNull IntelligentItem intelligentItem) throws IllegalArgumentException {
        return update(slot, intelligentItem.getItemStack());
    }

    /**
     * Updates the ItemStack in the same place with a new IntelligentItem.
     *
     * @param row             The row
     * @param column          The column
     * @param intelligentItem The new IntelligentItem what should be displayed.
     * @return true if the ItemStack was updated, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean update(@Nonnegative int row, @Nonnegative int column, @NotNull IntelligentItem intelligentItem) throws IllegalArgumentException {
        return update(SlotUtils.toSlot(row, column), intelligentItem.getItemStack());
    }

    /**
     * Update multiple items at once, with a new ItemStack.
     *
     * @param slots     The slots
     * @param itemStack The new ItemStack what should be displayed.
     * @return true if all items were updated, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    public boolean update(@NotNull List<Integer> slots, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        AtomicInteger updated = new AtomicInteger();
        slots.forEach(integer -> {
            update(integer, itemStack);
            updated.getAndIncrement();
        });
        return updated.get() >= slots.size();
    }

    /**
     * Updates the ItemStack in the same place with a new ItemStack.
     *
     * @param column    The column
     * @param row       The row
     * @param itemStack The new ItemStack what should be displayed.
     * @return true if the ItemStack was updated, false if not.
     * @throws IllegalArgumentException if slot > 53 or slot > inventory size
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean updateViaCoordination(@Nonnegative int row, @Nonnegative int column, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        return update(SlotUtils.toSlot(row, column), itemStack);
    }

    /**
     * Update multiple items at once, with a new ItemStack.
     *
     * @param pairs     First value is your row, second value is your column.
     * @param itemStack The new ItemStack what should be displayed.
     * @return true if all items were updated, false if not.
     */
    public boolean updateViaCoordination(@NotNull Collection<ImmutablePair<Integer, Integer>> pairs, @NotNull ItemStack itemStack) {
        AtomicInteger updated = new AtomicInteger();
        pairs.forEach(pair -> {
            updateViaCoordination(pair.getLeft(), pair.getRight(), itemStack);
            updated.getAndIncrement();
        });
        return updated.get() >= pairs.size();
    }

    /**
     * Updates item and puts it in a new place in the inventory.
     *
     * @param itemSlot  The slot from the old ItemStack
     * @param newSlot   The slot where the new ItemStack will be placed.
     * @param itemStack The new ItemStack what should be displayed.
     * @return true if the ItemStack was updated, false if not.
     * @throws IllegalArgumentException if itemSlot > 53 or newSlot > 53
     */
    public boolean update(@Nonnegative int itemSlot, @Nonnegative int newSlot, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        if (itemSlot > 53)
            throw new IllegalArgumentException("The itemSlot must not be larger than 53.");

        if (newSlot > 53)
            throw new IllegalArgumentException("The newSlot must not be larger than 53.");

        Optional<IntelligentItem> itemOptional = get(itemSlot);
        if (!itemOptional.isPresent()) return false;

        IntelligentItem item = itemOptional.get();
        IntelligentItem newItem = item.update(itemStack);

        set(newSlot, newItem);

        Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
        inventoryOptional.ifPresent(savedInventory -> {
            savedInventory.setItem(itemSlot, null);
            savedInventory.setItem(newSlot, newItem.getItemStack());
        });
        return true;
    }

    /**
     * Updates item position and puts it in a new place in the inventory.
     *
     * @param itemSlot The slot from the old ItemStack
     * @param newSlot  The slot where the new ItemStack will be placed.
     * @return true if the ItemStack was updated, false if not.
     * @throws IllegalArgumentException if itemSlot > 53 or newSlot > 53
     */
    public boolean updatePosition(@Nonnegative int itemSlot, @Nonnegative int newSlot) throws IllegalArgumentException {
        return update(itemSlot, newSlot, new ItemStack(Material.AIR));
    }

    /**
     * The pagination of the inventory.
     *
     * @return The pagination
     */
    public Pagination pagination() {
        return this.pagination;
    }

    /**
     * The SlotIterator of the inventory.
     *
     * @return null if SlotIterator is not defined
     */
    public SlotIterator iterator() {
        return this.pagination.getSlotIterator();
    }

    /**
     * @return The SearchPattern of the inventory.
     */
    public SearchPattern searchPattern() {
        return this.searchPattern;
    }

    /**
     * @return The ContentPattern of the inventory.
     */
    public ContentPattern contentPattern() {
        return this.contentPattern;
    }

    protected void transferData(InventoryContents transferTo) {
        if (this.data.isEmpty()) return;

        this.data.forEach(transferTo::setData);
    }

    private void remove(@Nonnegative int i) {
        this.pagination.remove(i);
    }

    protected Optional<IntelligentItem> getInPage(@Nonnegative int page, @Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        return Optional.ofNullable(this.pagination.get(slot, page));
    }

    private void clear(int page) {
        for (int i = 0; i < this.pagination.getInventoryData().size(); i++) {
            IntelligentItemData itemData = this.pagination.getInventoryData().get(i);
            if (itemData.getPage() != page) continue;

            int finalI = i;
            get(i).ifPresent(item -> removeItemWithConsumer(finalI));
        }
    }
}
