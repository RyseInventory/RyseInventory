package com.github.rysefoxx.pagination;

import com.github.rysefoxx.SlotIterator;
import com.github.rysefoxx.content.IntelligentItem;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public class InventoryContents {

    private final Player player;
    private final Pagination pagination;
    private final HashMap<Integer, IntelligentItem> items;
    private final HashMap<String, Object> data;
    private final RyseInventory inventory;

    private IntelligentItem fillBorder;
    private final List<IntelligentItem> addList;

    @Contract(pure = true)
    public InventoryContents(@NotNull Player player, @NotNull RyseInventory inventory) {
        this.player = player;
        this.inventory = inventory;
        this.pagination = new Pagination(inventory);
        this.items = new HashMap<>();
        this.data = new HashMap<>();
        this.addList = new ArrayList<>();
    }

    /**
     * Removes the first item that matches the parameter.
     *
     * @param item The item to filter for.
     * @throws IllegalArgumentException if item is null
     */
    public void removeFirst(@NotNull ItemStack item) throws IllegalArgumentException {
        Validate.notNull(item, "ItemStack must not be null.");
        Inventory inventory = this.inventory.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            if (!itemStack.isSimilar(item)) continue;

            this.inventory.getInventory().setItem(i, null);
            break;
        }
    }

    /**
     * Removes the first item that matches the parameter.
     *
     * @param material The material to filter for.
     * @throws IllegalArgumentException if material is null
     */
    public void removeFirst(@NotNull Material material) throws IllegalArgumentException {
        Validate.notNull(material, "Material must not be null.");
        Inventory inventory = this.inventory.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            if (itemStack.getType() != material) continue;
            this.inventory.getInventory().setItem(i, null);
            break;
        }
    }

    /**
     * Subtracts a value from an ItemStack.
     *
     * @param item   The itemstack what should be reduced.
     * @param amount How much to remove.
     * @throws IllegalArgumentException if item is null or if amount > 64
     * @apiNote If the itemstack Amount is < 1, the ItemStack will be deleted from the inventory.
     */
    public void subtractFirst(@NotNull ItemStack item, @Nonnegative int amount) throws IllegalArgumentException {
        Validate.notNull(item, "ItemStack must not be null.");

        if (amount > 64) {
            throw new IllegalArgumentException("Amount must not be larger than 64.");
        }

        Inventory inventory = this.inventory.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            if (!itemStack.isSimilar(item)) continue;

            if (itemStack.getAmount() - amount < 1) {
                this.inventory.getInventory().setItem(i, null);
                continue;
            }
            itemStack.setAmount(itemStack.getAmount() - amount);
            this.inventory.getInventory().setItem(i, itemStack);
            break;
        }
    }

    /**
     * Removes all items that match the parameter.
     *
     * @param item The item to filter for.
     * @throws IllegalArgumentException if item is null
     */
    public void removeAll(@NotNull ItemStack item) throws IllegalArgumentException {
        Validate.notNull(item, "ItemStack must not be null");
        Inventory inventory = this.inventory.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            if (!itemStack.isSimilar(item)) continue;

            this.inventory.getInventory().setItem(i, null);
        }
    }

    /**
     * Removes all items that match the parameter.
     *
     * @param item   The item to filter for.
     * @param amount How much to remove.
     * @throws IllegalArgumentException if item is null or amount > 64
     */
    public void removeAll(@NotNull ItemStack item, @Nonnegative int amount) throws IllegalArgumentException {
        Validate.notNull(item, "ItemStack must not be null.");

        if (amount > 64) {
            throw new IllegalArgumentException("Amount must not be larger than 64.");
        }
        Inventory inventory = this.inventory.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            if (!itemStack.isSimilar(item)) continue;

            if (itemStack.getAmount() - amount < 1) {
                this.inventory.getInventory().setItem(i, null);
                continue;
            }
            itemStack.setAmount(itemStack.getAmount() - amount);
        }
    }

    /**
     * Removes the very first item that can be found.
     */
    public void removeFirst() {
        Inventory inventory = this.inventory.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().isAir()) continue;

            this.inventory.getInventory().setItem(i, null);
            break;
        }
    }

    /**
     * Removes the very first item that can be found.
     *
     * @param amount How much to remove
     */
    public void removeFirst(@Nonnegative int amount) {
        Inventory inventory = this.inventory.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType().isAir()) continue;

            if (itemStack.getAmount() - amount < 1) {
                this.inventory.getInventory().setItem(i, null);
                break;
            }
            itemStack.setAmount(itemStack.getAmount() - amount);
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
     * @param plugin   The JavaPlugin
     * @param newTitle The new title
     * @throws IllegalArgumentException when plugin, player or newTitle is null
     */
    public void updateTitle(@NotNull JavaPlugin plugin, @NotNull String newTitle) {
        this.inventory.updateTitle(plugin, this.player, newTitle);
    }

    /**
     * Fills the Border with a intelligent ItemStack regardless of inventory size.
     *
     * @param item The ItemStack which should represent the border
     * @throws IllegalArgumentException when item is null
     */
    public void fillBorders(@NotNull IntelligentItem item) throws IllegalArgumentException {
        Validate.notNull(item, "IntelligentItem must not be null.");
        this.fillBorder = item;
    }

    /**
     * Method to cache data in the content
     *
     * @param key
     * @param value
     * @throws IllegalArgumentException if key or value is null
     */
    public <T> void setData(@NotNull String key, @NotNull T value) throws IllegalArgumentException {
        Validate.notNull(key, "String must not be null.");
        Validate.notNull(value, "Object must not be null.");
        this.data.put(key, value);
    }

    /**
     * Method to get data in the content
     *
     * @param key
     * @return The cached value
     * @throws IllegalArgumentException when key is null
     */
    public @Nullable Object getData(@NotNull String key) throws IllegalArgumentException {
        Validate.notNull(key, "String must not be null.");
        if (!this.data.containsKey(key)) return null;

        return this.data.get(key);
    }

    /**
     * Method to get data in the content
     *
     * @param key
     * @param defaultValue value when key is invalid
     * @return The cached value
     * @throws IllegalArgumentException when key or defaultValue is null
     */
    @SuppressWarnings("unchecked")
    public @NotNull <T> T getData(@NotNull String key, @NotNull Object defaultValue) throws IllegalArgumentException {
        Validate.notNull(key, "String must not be null.");
        Validate.notNull(defaultValue, "Object must not be null.");
        if (!this.data.containsKey(key)) return (T) defaultValue;

        return (T) this.data.get(key);
    }

    /**
     * @return The first slot that is empty.
     */
    public Optional<Integer> firstEmpty() {
        int nextSlot = 0;
        Inventory inventory = this.inventory.getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null && !itemStack.getType().isAir()) continue;
            nextSlot = i;
            break;
        }
        return Optional.of(nextSlot);
    }

    /**
     * @return The last slot that is empty.
     */
    public Optional<Integer> lastEmpty() {
        int nextSlot = 0;
        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null && !itemStack.getType().isAir()) continue;
            nextSlot = i;
        }
        return Optional.of(nextSlot);
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param slot Where should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException when item is null
     */
    public void set(@Nonnegative int slot, @NotNull IntelligentItem item) throws IllegalArgumentException {
        Validate.notNull(item, "IntelligentItem must not be null.");
        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }
        this.items.put(slot, item);
    }

    /**
     * Adds an item to the inventory in the first free place.
     *
     * @param item The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException when item is null
     */
    public void add(@NotNull IntelligentItem item) throws IllegalArgumentException {
        this.addList.add(item);
    }

    /**
     * Add multiple items to the inventory in the first free place.
     *
     * @param items The ItemStacks to be displayed in the inventory
     * @throws IllegalArgumentException when one element is null
     */
    public void add(@NotNull IntelligentItem @NotNull ... items) throws IllegalArgumentException {
        for (IntelligentItem item : items) {
            add(item);
        }
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param row    The row
     * @param column The column
     * @param item   The ItemStack to be displayed in the inventory
     * @throws IllegalArgumentException when item is null or row > 5 or column > 8
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull IntelligentItem item) throws IllegalArgumentException {
        Validate.notNull(item, "IntelligentItem must not be null.");
        if (row > 5) {
            throw new IllegalArgumentException("The row must not be larger than 5.");
        }
        if (column > 8) {
            throw new IllegalArgumentException("The column must not be larger than 9.");
        }

        set(row * 9 + column, item);
    }

    /**
     * Get a intelligent ItemStack based on the slot.
     *
     * @param slot The slot
     * @return The intelligent ItemStack or an empty Optional instance.
     * @throws IllegalArgumentException when slot > 53
     */
    public Optional<IntelligentItem> get(@Nonnegative int slot) throws IllegalArgumentException {
        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }

        if (!this.items.containsKey(slot)) return Optional.empty();
        return Optional.ofNullable(this.items.get(slot));
    }

    /**
     * Get a intelligent ItemStack based on the row and column.
     *
     * @param row    The row
     * @param column The column
     * @return The intelligent ItemStack or an empty Optional instance.
     * @throws IllegalArgumentException when row > 5 or column > 8
     */
    public Optional<IntelligentItem> get(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
        if (row > 5) {
            throw new IllegalArgumentException("The row must not be larger than 5.");
        }
        if (column > 8) {
            throw new IllegalArgumentException("The column must not be larger than 9.");
        }
        return get(row * 9 + column);
    }

    /**
     * Updates the ItemStack in the same place with a new ItemStack.
     *
     * @param slot      The slot
     * @param itemStack The new ItemStack what should be displayed.
     * @throws IllegalArgumentException when item is null or slot > 53
     */
    public void update(@Nonnegative int slot, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        Validate.notNull(itemStack, "ItemStack must not be null.");
        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }
        Optional<IntelligentItem> itemOptional = get(slot);
        if (itemOptional.isEmpty()) return;

        IntelligentItem item = itemOptional.get();
        IntelligentItem newItem = item.update(itemStack);
        this.pagination.setItem(slot, newItem);
        this.player.getOpenInventory().getTopInventory().setItem(slot, newItem.getItemStack());
    }

    /**
     * Updates item and puts it in a new place in the inventory.
     *
     * @param itemSlot  The slot from the old ItemStack
     * @param newSlot   The slot where the new ItemStack will be placed.
     * @param itemStack The new ItemStack what should be displayed.
     * @throws IllegalArgumentException when item is null or itemSlot > 53 or newSlot > 53
     */
    public void update(@Nonnegative int itemSlot, @Nonnegative int newSlot, @NotNull ItemStack itemStack) throws IllegalArgumentException {
        Validate.notNull(itemStack, "ItemStack must not be null.");
        if (itemSlot > 53) {
            throw new IllegalArgumentException("The itemSlot must not be larger than 53.");
        }
        if (newSlot > 53) {
            throw new IllegalArgumentException("The newSlot must not be larger than 53.");
        }

        Optional<IntelligentItem> itemOptional = get(itemSlot);
        if (itemOptional.isEmpty()) return;

        IntelligentItem item = itemOptional.get();
        IntelligentItem newItem = item.update(itemStack);
        this.items.put(newSlot, newItem);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        inventory.setItem(itemSlot, null);
        inventory.setItem(newSlot, newItem.getItemStack());
    }

    /**
     * The pagination of the inventory.
     *
     * @return The pagination
     */
    public @NotNull Pagination pagination() {
        return this.pagination;
    }

    /**
     * The SlotIterator of the inventory.
     *
     * @return null if SlotIterator is not defined
     */
    public @Nullable SlotIterator iterator() {
        return this.pagination.getSlotIterator();
    }

    protected HashMap<Integer, IntelligentItem> getItems() {
        return this.items;
    }

    protected @Nullable IntelligentItem getFillBorder() {
        return fillBorder;
    }

    protected void transferData(@NotNull InventoryContents transferTo) {
        if (this.data.isEmpty()) return;

        this.data.forEach(transferTo::setData);
    }

    protected void fillBorders(@NotNull Inventory inventory) throws IllegalArgumentException {
        Validate.notNull(inventory, "Inventory must not be null.");
        int size = inventory.getSize();
        int rows = (size + 1) / 9;

        for (int i = 0; i < rows * 9; i++) {
            if ((i <= 8) || (i >= (rows * 9) - 9)
                    || i == 9 || i == 18
                    || i == 27 || i == 36
                    || i == 17 || i == 26
                    || i == 35 || i == 44)
                inventory.setItem(i, this.fillBorder.getItemStack());
        }
    }

    protected void add(@NotNull Inventory inventory) throws IllegalArgumentException {
        Validate.notNull(inventory, "Inventory must not be null.");

        for (IntelligentItem item : addList) {
            inventory.addItem(item.getItemStack());
        }
    }
}
