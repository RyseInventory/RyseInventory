package com.github.rysefoxx.pagination;

import com.github.rysefoxx.SlotIterator;
import com.github.rysefoxx.content.IntelligentItem;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/19/2022
 */

public class Pagination implements Cloneable {

    private @Setter
    @Getter
    @Nonnegative
    int itemsPerPage;
    private List<IntelligentItem> items;

    private @Getter
    @Nullable
    SlotIterator slotIterator;

    private int page;
    private final RyseInventory inventory;
    protected HashMap<Integer, HashMap<Integer, IntelligentItem>> pageItems;

    /**
     * Pagination constructor with a default size of 1 element per page.
     */
    @Contract(pure = true)
    public Pagination(@NotNull RyseInventory inventory) {
        this.inventory = inventory;
        this.itemsPerPage = 1;
        this.page = 0;
        this.items = new ArrayList<>();
        this.pageItems = new HashMap<>();
        this.pageItems.put(page(), new HashMap<>());
    }

    /**
     * Clones the pagination so that the original is not changed.
     *
     * @return cloned pagination
     */
    public Pagination copy() {
        try {
            return (Pagination) clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * @return the current page.
     */
    public @Nonnegative
    int page() {
        return this.page + 1;
    }

    /**
     * @return the current inventory.
     */
    public @NotNull RyseInventory inventory() {
        return this.inventory;
    }

    /**
     * @return true if you are on the last page.
     */
    public boolean isLast() {
        return (this.page + 1) == (int) Math.ceil((double) this.items.size() / this.itemsPerPage);
    }

    /**
     * @return true if you are on the first page.
     */
    public boolean isFirst() {
        return this.page <= 0;
    }

    /**
     * Increases the current page by 1
     *
     * @return the new Pagination
     */
    public Pagination next() {
        if (isLast()) return this;
        this.page++;
        return this;
    }

    /**
     * Decreases the current page by 1
     *
     * @return the new Pagination
     */
    public Pagination previous() {
        if (isFirst()) return this;
        this.page--;
        return this;
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items A list of intelligent ItemStacks
     * @throws IllegalArgumentException when items is null
     */
    public void setItems(@NotNull List<IntelligentItem> items) throws IllegalArgumentException {
        Validate.notNull(items, "List<IntelligentItem> must not be null.");
        this.items = items;
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items An array of smart ItemStacks
     * @throws IllegalArgumentException when items is null
     */
    public void setItems(@NotNull IntelligentItem[] items) throws IllegalArgumentException {
        Validate.notNull(items, "IntelligentItem[] must not be null.");
        this.items = new ArrayList<>(Arrays.stream(items).toList());
    }

    /**
     * Adds a single intelligent ItemStack.
     *
     * @param item the intelligent ItemStack
     * @throws IllegalArgumentException when item is null
     */
    public void addItem(@NotNull IntelligentItem item) throws IllegalArgumentException {
        Validate.notNull(item, "IntelligentItem must not be null.");
        this.items.add(item);
    }

    /**
     * Sets the SlotIterator for the pagination
     *
     * @param slotIterator the SlotIterator
     * @throws IllegalArgumentException when item is null
     */
    public void iterator(@NotNull SlotIterator slotIterator) throws IllegalArgumentException {
        Validate.notNull(slotIterator, "SlotIterator must not be null.");
        this.slotIterator = slotIterator;
    }

    /**
     * Sets a new item at a slot.
     *
     * @param slot    The slot
     * @param newItem The Item
     * @throws IllegalArgumentException when item is null or slot > 53
     */
    public void setItem(@Nonnegative int slot, @NotNull IntelligentItem newItem) throws IllegalArgumentException {
        Validate.notNull(newItem, "SlotIterator must not be null.");

        if (slot > 53) {
            throw new IllegalArgumentException("The slot must not be larger than 53.");
        }

        this.pageItems.get(this.page).put(slot, newItem);
    }

    protected @NotNull
    List<IntelligentItem> getItems() {
        return items;
    }

    protected @Nonnegative
    int lastPage() {
        return (int) Math.ceil((double) this.items.size() / this.itemsPerPage);
    }

    protected void setPage(@Nonnegative int page) {
        this.page = page;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }
}
