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

import io.github.rysefoxx.SlotIterator;
import io.github.rysefoxx.content.IntelligentItem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/19/2022
 */

public class Pagination implements Cloneable {

    private
    @Getter
    @Nonnegative
    int itemsPerPage;
    private List<IntelligentItem> items;

    private @Getter SlotIterator slotIterator;

    private int page;
    private final RyseInventory inventory;

    private HashMap<Integer, HashMap<Integer, IntelligentItem>> pageItems;
    private final ConcurrentHashMap<Integer, IntelligentItem> permanentItems;

    /**
     * Pagination constructor with a default size of 1 element per page.
     */

    public Pagination(@NotNull RyseInventory inventory) {
        this.inventory = inventory;
        this.itemsPerPage = 1;
        this.page = 0;
        this.items = new ArrayList<>();
        this.permanentItems = new ConcurrentHashMap<>();
        this.pageItems = new HashMap<>();
        this.pageItems.put(this.page, new HashMap<>());
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
     * @return the last page.
     */
    public @Nonnegative
    int lastPage() {
        return (int) Math.ceil((double) this.items.size() / calculateValueForPage());
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
        int value = calculateValueForPage();
        int slide = (int) Math.ceil((double) this.items.size() / value);

        return this.page == (slide != 0 ? slide - 1 : 0);
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
    public @NotNull Pagination next() {
        if (isLast()) return this;
        this.page++;
        return this;
    }

    /**
     * Decreases the current page by 1
     *
     * @return the new Pagination
     */
    public @NotNull Pagination previous() {
        if (isFirst()) return this;
        this.page--;
        return this;
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items A list of intelligent ItemStacks
     */
    public void setItems(@NotNull List<IntelligentItem> items) {
        this.items = new ArrayList<>(items);
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items An array of smart ItemStacks
     */
    public void setItems(@NotNull IntelligentItem[] items) {
        this.items = Arrays.stream(items).collect(Collectors.toList());
    }

    /**
     * Adds a single intelligent ItemStack.
     *
     * @param item the intelligent ItemStack
     */
    public void addItem(@NotNull IntelligentItem item) {
        this.items.add(item);
    }

    /**
     * Sets the SlotIterator for the pagination
     *
     * @param slotIterator the SlotIterator
     */
    public void iterator(@NotNull SlotIterator slotIterator) {
        this.slotIterator = slotIterator;
    }

    /**
     * Sets a new item at a slot.
     *
     * @param slot    The slot
     * @param newItem The Item
     * @throws IllegalArgumentException if slot > 53
     */
    protected void setItem(@Nonnegative int slot, @NotNull IntelligentItem newItem) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        this.permanentItems.put(slot, newItem);
    }

    /**
     * Sets a new item at a slot with defined a page.
     *
     * @param slot    The slot
     * @param page    The page
     * @param newItem The Item
     * @throws IllegalArgumentException if slot > 53
     */
    protected void setItem(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentItem newItem) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException("The slot must not be larger than 53.");

        page--;
        if (!this.pageItems.containsKey(page))
            this.pageItems.put(page, new HashMap<>());

        this.pageItems.get(page).put(slot, newItem);
    }

    /**
     * @param itemsPerPage How many items may be per page.
     * @apiNote If you have set the endPosition at the SlotIterator, it will be preferred.
     */
    public void setItemsPerPage(@Nonnegative int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    protected @NotNull HashMap<Integer, HashMap<Integer, IntelligentItem>> getPageItems() {
        return pageItems;
    }

    protected void setPageItems(HashMap<Integer, HashMap<Integer, IntelligentItem>> items) {
        this.pageItems = items;
    }

    protected @NotNull ConcurrentHashMap<Integer, IntelligentItem> getPermanentItems() {
        return permanentItems;
    }

    protected @NotNull List<IntelligentItem> getItems() {
        return items;
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

    private int calculateValueForPage() {
        int value;

        if (this.slotIterator == null || this.slotIterator.getEndPosition() == -1) {
            value = this.itemsPerPage;
        } else {
            value = this.slotIterator.getEndPosition() - (this.slotIterator.getSlot() == -1 ? 9 * this.slotIterator.getRow() + this.slotIterator.getColumn() : this.slotIterator.getSlot());
        }
        return value;
    }
}
