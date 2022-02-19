package com.github.rysefoxx.pagination;

import com.github.rysefoxx.SlotIterator;
import com.github.rysefoxx.content.IntelligentItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.HashMap;
import java.util.Optional;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public class InventoryContents {

    private final Player player;
    private final HashMap<Integer, IntelligentItem> items;
    private final Pagination pagination;

    private IntelligentItem fillBorder;

    @Contract(pure = true)
    public InventoryContents(@NotNull Player player) {
        this.player = player;
        this.items = new HashMap<>();
        this.pagination = new Pagination();
    }

    /**
     * Fills the Border with a smart ItemStack regardless of inventory size.
     *
     * @param item The ItemStack which should represent the border
     */
    public void fillBorders(@NotNull IntelligentItem item) {
        this.fillBorder = item;
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param slot Where should the item be placed?
     * @param item The ItemStack to be displayed in the inventory
     */
    public void set(@Nonnegative int slot, @NotNull IntelligentItem item) {
        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }
        this.items.put(slot, item);
    }

    /**
     * Sets a fixed intelligent ItemStack in the inventory.
     *
     * @param row    The row
     * @param column The column
     * @param item   The ItemStack to be displayed in the inventory
     */
    public void set(@Nonnegative int row, @Nonnegative int column, @NotNull IntelligentItem item) {
        set(row * 9 + column, item);
    }

    /**
     * Get a intelligent ItemStack based on the slot.
     *
     * @param slot The slot
     * @return The intelligent ItemStack or an empty Optional instance.
     */
    public Optional<IntelligentItem> get(@Nonnegative int slot) {
        if (!this.items.containsKey(slot)) return Optional.empty();
        return Optional.ofNullable(this.items.get(slot));
    }

    /**
     * Get a intelligent ItemStack based on the row and column.
     *
     * @param row    The row
     * @param column The column
     * @return The intelligent ItemStack or an empty Optional instance.
     */
    public Optional<IntelligentItem> get(@Nonnegative int row, @Nonnegative int column) {
        return get(row * 9 + column);
    }

    /**
     * Updates the ItemStack in the same place with a new ItemStack.
     *
     * @param slot      The slot
     * @param itemStack The new ItemStack what should be displayed.
     */
    public void update(@Nonnegative int slot, @NotNull ItemStack itemStack) {
        Optional<IntelligentItem> itemOptional = get(slot);
        if (itemOptional.isEmpty()) return;

        IntelligentItem item = itemOptional.get();
        IntelligentItem newItem = item.update(itemStack);
        this.items.put(slot, newItem);
        this.player.getOpenInventory().getTopInventory().setItem(slot, newItem.getItemStack());
    }

    /**
     * Updates item and puts it in a new place in the inventory.
     *
     * @param itemSlot  The slot from the old ItemStack
     * @param newSlot   The slot where the new ItemStack will be placed.
     * @param itemStack The new ItemStack what should be displayed.
     */
    public void update(@Nonnegative int itemSlot, @Nonnegative int newSlot, @NotNull ItemStack itemStack) {
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

    protected void fillBorders(@NotNull Inventory inventory) {
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
}
