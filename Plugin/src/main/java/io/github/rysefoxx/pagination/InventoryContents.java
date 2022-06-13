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
import io.github.rysefoxx.enums.IntelligentType;
import io.github.rysefoxx.enums.InventoryOpenerType;
import io.github.rysefoxx.pattern.ContentPattern;
import io.github.rysefoxx.pattern.SearchPattern;
import io.github.rysefoxx.util.SlotUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
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
     * @throws IllegalArgumentException if slot > 53
     */
    public boolean hasSlot(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }
        return slot <= this.inventory.size();
    }

    /**
     * Removes the item from the inventory and the associated consumer.
     *
     * @param slot Which slot should be removed?
     * @return true if the item was removed.
     * @throws IllegalArgumentException When slot is greater than 53
     */
    public boolean removeItemWithConsumer(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }
        this.pagination.getPermanentItems().remove(slot);

        get(slot).ifPresent(IntelligentItem::clearConsumer);

        Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
        if (!inventoryOptional.isPresent())
            return false;

        inventoryOptional.get().setItem(slot, null);
        return true;
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
     * @param item   The itemstack what should be reduced.
     * @param amount How much to remove.
     * @throws IllegalArgumentException if amount > 64
     * @apiNote If the itemstack Amount is < 1, the ItemStack will be deleted from the inventory.
     */
    public void subtractFirst(@NotNull ItemStack item, @Nonnegative int amount) throws IllegalArgumentException {
        if (amount > 64) {
            throw new IllegalArgumentException("Amount must not be larger than 64.");
        }

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
        if (amount > 64) {
            throw new IllegalArgumentException("Amount must not be larger than 64.");
        }

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
        if (amount > 64) {
            throw new IllegalArgumentException("Amount must not be larger than 64.");
        }
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
        for (int i = 0; i < this.inventory.size(); i++) {
            slots.add(i);
        }
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
     * With this method you can update the inventory title.
     *
     * @param plugin   The JavaPlugin
     * @param newTitle The new title
     * @deprecated Use {@link #updateTitle(String)} instead.
     */
    @Deprecated
    public void updateTitle(JavaPlugin plugin, @NotNull String newTitle) {
        updateTitle(newTitle);
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
    public void clearData(@NotNull Consumer<List<?>> consumer) {
        List<Object> data = Arrays.asList(this.data.values().toArray());
        consumer.accept(data);

        this.data.clear();
    }

    /**
     * Removes all data in the inventory and returns all keys and values in the consumer.
     */
    public void clearData(@NotNull BiConsumer<List<String>, List<?>> consumer) {
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
     * @return true if the specified slot is on the right side.
     * @throws IllegalArgumentException if slot > 53
     */
    public boolean isRightBorder(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

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
     * @throws IllegalArgumentException if row is > 5 or if column > 8
     */
    public boolean isRightBorder(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

        return isRightBorder(SlotUtils.toSlot(row, column));
    }

    /**
     * @return true if the specified slot is on the left side.
     * @throws IllegalArgumentException if slot < 0
     */
    public boolean isLeftBorder(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

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
     * @throws IllegalArgumentException if row is > 5 or if column > 8
     */
    public boolean isLeftBorder(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

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
        firstEmpty().ifPresent(integer -> this.pagination.setItem(integer, item));
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

        this.inventory.splitInventory(contents);
        this.inventory.load(contents.pagination(), this.player, contents.pagination().page() - 1);
    }

    /**
     * Sets a fixed smart ItemStack in the inventory on a specified page.
     *
     * @param slot Where should the item be placed?
     * @param page On which page should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException when slot > 53
     * @apiNote When you define a page please start at 1.
     */
    public void setWithinPage(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        if (slot > this.inventory.size())
            throw new IllegalArgumentException("The slot must not be larger than the inventory size.");

        this.pagination.setItem(slot, page, item);
    }

    /**
     * Sets a fixed IntelligentItem in the inventory on a specified page.
     *
     * @param row    The row of the inventory
     * @param column The column of the inventory
     * @param page   On which page should the item be placed?
     * @param item   The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException when slot > 53
     * @apiNote When you define a page please start at 1.
     */
    public void setWithinPage(@Nonnegative int row, @Nonnegative int column, @Nonnegative int page, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

        setWithinPage(SlotUtils.toSlot(row, column), page, item);
    }

    /**
     * Sets an item to multiple slots within a page.
     *
     * @param slots Where should the item be placed everywhere?
     * @param page  On which page should the item be placed?
     * @param item  The ItemStack to be displayed in the inventory
     * @apiNote When you define a page please start at 1.
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
     * @apiNote When you define a page please start at 1.
     */
    public void setWithinPage(@NotNull List<Integer> slots, @NotNull List<Integer> pages, @NotNull IntelligentItem item) {
        slots.forEach(slot -> pages.forEach(page -> setWithinPage(slot, page, item)));
    }

    /**
     * Fills the whole inventory with an ItemStack.
     *
     * @param item The item with which the inventory should be filled.
     */
    public void fill(@NotNull IntelligentItem item) {
        for (int i = 0; i < this.inventory.size(); i++)
            this.pagination.setItem(i, item);
    }

    /**
     * Fills the whole inventory with an ItemStack.
     *
     * @param item The item with which the inventory should be filled.
     */
    public void fill(@NotNull ItemStack item) {
        for (int i = 0; i < this.inventory.size(); i++)
            this.pagination.setItem(i, IntelligentItem.empty(item));
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
                this.pagination.setItem(i, IntelligentItem.empty(item));
                continue;
            }
            if (type == IntelligentType.IGNORED)
                this.pagination.setItem(i, IntelligentItem.ignored(item));
        }
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param slot Where should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException when slot > 53 or > inventory size
     */
    public void set(@Nonnegative int slot, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        if (slot > this.inventory.size())
            throw new IllegalArgumentException("The slot must not be larger than the inventory size.");

        this.pagination.setItem(slot, item);
    }

    /**
     * Sets a fixed ItemStack in the inventory.
     *
     * @param slot      Where should the item be placed?
     * @param itemStack The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException when slot > 53 or > inventory size
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
     * @throws IllegalArgumentException when slot > 53 or > inventory size
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

        set(SlotUtils.toSlot(row, column), IntelligentItem.empty(itemStack));
    }

    /**
     * Sets a fixed ItemStack in the inventory.
     *
     * @param slot      Where should the item be placed?
     * @param itemStack The ItemStack to be displayed in the inventory
     * @param type      The type of the item
     * @throws IllegalArgumentException when slot > 53 or > inventory size
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
     * @throws IllegalArgumentException when slot > 53 or > inventory size
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull ItemStack itemStack, @NotNull IntelligentType type) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

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
     * @throws IllegalArgumentException if row > 5, column > 8 or > inventory size
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull IntelligentItem item) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

        int slot = SlotUtils.toSlot(row, column);
        if (slot > this.inventory.size())
            throw new IllegalArgumentException("The slot must not be larger than the inventory size.");

        set(slot, item);
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
     * Fetches a intelligent ItemStack based on the slot.
     *
     * @param slot The slot
     * @return The intelligent ItemStack or an empty Optional instance.
     * @throws IllegalArgumentException if slot > 53
     */
    public Optional<IntelligentItem> get(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        IntelligentItem intelligentItem = getFromItems(this.inventory.getInventory().getItem(slot));
        if (intelligentItem != null)
            return Optional.of(intelligentItem);


        if (this.pagination.getPermanentItems().containsKey(slot))
            return Optional.ofNullable(this.pagination.getPermanentItems().get(slot));

        int page = this.pagination.page() - 1;

        if (!this.pagination.getPageItems().containsKey(page)) return Optional.empty();
        return Optional.ofNullable(this.pagination.getPageItems().get(page).get(slot));
    }

    /**
     * Get a intelligent ItemStack based on the row and column.
     *
     * @param row    The row
     * @param column The column
     * @return The intelligent ItemStack or an empty Optional instance.
     * @throws IllegalArgumentException if row > 5 or column > 8
     */
    public @NotNull Optional<IntelligentItem> get(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

        return get(SlotUtils.toSlot(row, column));
    }

    /**
     * Get all intelligent ItemStacks.
     *
     * @param slots All slots where you want to watch.
     * @return All intelligent ItemStacks that could be found.
     */
    public List<IntelligentItem> get(@NotNull List<Integer> slots) {
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
    public List<IntelligentItem> getInArea(@Nonnegative int areaStart, @Nonnegative int areaStop) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if slot > 53 or index >= lore.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public void updateLore(@Nonnegative int row, @Nonnegative int column, @Nonnegative int index, @NotNull String line) throws IllegalArgumentException, IllegalStateException {
        updateLore(SlotUtils.toSlot(row, column), index, line);
    }

    /**
     * Updates the lore of the item.
     *
     * @param slot In which slot in the inventory is the ItemStack located.
     * @param lore The new lore
     * @throws IllegalArgumentException if slot > 53 or index >= lore.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public void updateLore(@Nonnegative int slot, @NotNull List<String> lore) throws IllegalArgumentException, IllegalStateException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        Optional<IntelligentItem> itemOptional = get(slot);
        if (!itemOptional.isPresent()) return;

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
    }

    /**
     * Updates the lore of the item.
     *
     * @param slot  In which slot in the inventory is the ItemStack located.
     * @param index Where in the lore should the new line be located.
     * @param line  The new line in the ItemStack
     * @throws IllegalArgumentException if slot > 53 or index >= lore.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public void updateLore(@Nonnegative int slot, @Nonnegative int index, @NotNull String line) throws IllegalArgumentException, IllegalStateException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        Optional<IntelligentItem> itemOptional = get(slot);
        if (!itemOptional.isPresent()) return;

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
    }

    /**
     * Updates multiple lore lines at once.
     *
     * @param slots   The slots where the items are located.
     * @param indexes The indexes where the lines should be located.
     * @param lines   The lines that should be updated.
     * @throws IllegalArgumentException if slot > 53 or index >= lore.size() or lines.size() != slots.size() or indexes.size() != slots.size()
     * @throws IllegalStateException    if ItemStack has no ItemMeta or no Lore
     */
    public void updateLore(@NotNull List<Integer> slots, @NotNull List<Integer> indexes, @NotNull List<String> lines) throws IllegalArgumentException, IllegalStateException {
        int slotsSize = slots.size();
        int indexesSize = indexes.size();
        int linesSize = lines.size();

        if (slotsSize != indexesSize || slotsSize != linesSize)
            throw new IllegalArgumentException("slots, indexes and lines must have the same size.");

        for (int i = 0; i < slotsSize; i++)
            updateLore(slots.get(i), indexes.get(i), lines.get(i));
    }

    /**
     * Updates the ItemStack in the same place with a new ItemStack.
     *
     * @param slot      The slot
     * @param itemStack The new ItemStack what should be displayed.
     * @throws IllegalArgumentException if slot > 53
     */
    public void update(@Nonnegative int slot, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        Optional<IntelligentItem> itemOptional = get(slot);
        if (!itemOptional.isPresent()) return;

        IntelligentItem item = itemOptional.get();
        IntelligentItem newItem = item.update(itemStack);

        if (this.pagination.getPageItems().get(this.pagination.page() - 1).containsKey(slot)) {
            this.pagination.getPageItems().get(this.pagination.page() - 1).put(slot, newItem);
        } else {
            set(slot, newItem);
        }
        Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
        inventoryOptional.ifPresent(savedInventory -> savedInventory.setItem(slot, newItem.getItemStack()));
    }

    /**
     * Updates the ItemStack in the same place with a new IntelligentItem.
     *
     * @param slot            The slot
     * @param intelligentItem The new IntelligentItem what should be displayed.
     * @throws IllegalArgumentException if slot > 53
     */
    public void update(@Nonnegative int slot, @NotNull IntelligentItem intelligentItem) throws IllegalArgumentException {
        update(slot, intelligentItem.getItemStack());
    }

    /**
     * Updates the ItemStack in the same place with a new IntelligentItem.
     *
     * @param row             The row
     * @param column          The column
     * @param intelligentItem The new IntelligentItem what should be displayed.
     * @throws IllegalArgumentException if slot > 53 or row > 5 or column > 9
     */
    public void update(@Nonnegative int row, @Nonnegative int column, @NotNull IntelligentItem intelligentItem) throws IllegalArgumentException {
        if (row > 5)
            throw new IllegalArgumentException("The row must not be larger than 5.");

        if (column > 8)
            throw new IllegalArgumentException("The column must not be larger than 9.");

        update(SlotUtils.toSlot(row, column), intelligentItem.getItemStack());
    }

    /**
     * Update multiple items at once, with a new ItemStack.
     *
     * @param slots     The slots
     * @param itemStack The new ItemStack what should be displayed.
     */
    public void update(@NotNull List<Integer> slots, @NotNull ItemStack itemStack) {
        slots.forEach(integer -> update(integer, itemStack));
    }

    /**
     * Updates the ItemStack in the same place with a new ItemStack.
     *
     * @param column    The column
     * @param row       The row
     * @param itemStack The new ItemStack what should be displayed.
     * @throws IllegalArgumentException if slot > 53 or row > 5 or column > 9
     */
    public void updateViaCoordination(@Nonnegative int row, @Nonnegative int column, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        update(SlotUtils.toSlot(row, column), itemStack);
    }

    /**
     * Update multiple items at once, with a new ItemStack.
     *
     * @param pairs     First value is your row, second value is your column.
     * @param itemStack The new ItemStack what should be displayed.
     */
    public void updateViaCoordination(@NotNull Collection<ImmutablePair<Integer, Integer>> pairs, @NotNull ItemStack itemStack) {
        pairs.forEach(pair -> updateViaCoordination(pair.getLeft(), pair.getRight(), itemStack));
    }

    /**
     * Updates item and puts it in a new place in the inventory.
     *
     * @param itemSlot  The slot from the old ItemStack
     * @param newSlot   The slot where the new ItemStack will be placed.
     * @param itemStack The new ItemStack what should be displayed.
     * @throws IllegalArgumentException if itemSlot > 53 or newSlot > 53
     */
    public void update(@Nonnegative int itemSlot, @Nonnegative int newSlot, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        if (itemSlot > 53)
            throw new IllegalArgumentException("The itemSlot must not be larger than 53.");

        if (newSlot > 53)
            throw new IllegalArgumentException("The newSlot must not be larger than 53.");

        Optional<IntelligentItem> itemOptional = get(itemSlot);
        if (!itemOptional.isPresent()) return;

        IntelligentItem item = itemOptional.get();
        IntelligentItem newItem = item.update(itemStack);

        if (this.pagination.getPageItems().get(this.pagination.page() - 1).containsKey(itemSlot)) {
            this.pagination.getPageItems().get(this.pagination.page() - 1).put(newSlot, newItem);
        } else {
            set(newSlot, newItem);
        }

        Optional<Inventory> inventoryOptional = this.inventory.inventoryBasedOnOption(this.player.getUniqueId());
        inventoryOptional.ifPresent(savedInventory -> {
            savedInventory.setItem(itemSlot, null);
            savedInventory.setItem(newSlot, newItem.getItemStack());
        });
    }

    /**
     * Updates item position and puts it in a new place in the inventory.
     *
     * @param itemSlot The slot from the old ItemStack
     * @param newSlot  The slot where the new ItemStack will be placed.
     * @throws IllegalArgumentException if itemSlot > 53 or newSlot > 53
     */
    public void updatePosition(@Nonnegative int itemSlot, @Nonnegative int newSlot) throws IllegalArgumentException {
        update(itemSlot, newSlot, new ItemStack(Material.AIR));
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
        this.pagination.getPermanentItems().remove(i);
        this.pagination.getPageItems().get(this.pagination.page() - 1).remove(i);
    }

    protected Optional<IntelligentItem> getInPage(@Nonnegative int page, @Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }
        if (this.pagination.getPermanentItems().containsKey(slot))
            return Optional.ofNullable(this.pagination.getPermanentItems().get(slot));


        if (!this.pagination.getPageItems().containsKey(page)) return Optional.empty();
        return Optional.ofNullable(this.pagination.getPageItems().get(page).get(slot));
    }

    private void clear(int page) {
        this.pagination.getPermanentItems().forEach((integer, item) -> removeItemWithConsumer(integer));
        this.pagination.getPageItems().get(page).forEach((integer, item) -> removeItemWithConsumer(integer));
    }

    private @Nullable IntelligentItem getFromItems(ItemStack itemStack) {
        for (IntelligentItem item : this.pagination.getItems()) {
            if (!item.getItemStack().isSimilar(itemStack)) continue;
            return item;
        }
        return null;
    }
}
