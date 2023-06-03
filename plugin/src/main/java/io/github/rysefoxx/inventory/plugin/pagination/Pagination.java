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

package io.github.rysefoxx.inventory.plugin.pagination;

import io.github.rysefoxx.inventory.plugin.content.IntelligentItem;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItemData;
import io.github.rysefoxx.inventory.plugin.enums.IntelligentType;
import io.github.rysefoxx.inventory.plugin.util.StringConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/19/2022
 */

public class Pagination {

    private final RyseInventory inventory;
    private int page;

    @Getter
    private SlotIterator slotIterator;

    @Getter
    @Nonnegative
    private int itemsPerPage;

    @Getter(AccessLevel.PROTECTED)
    private boolean calledItemsPerPage;

    @Setter(AccessLevel.PROTECTED)
    private List<IntelligentItemData> inventoryData = new ArrayList<>();

    /**
     * @param inventory The inventory where the pagination is used.
     *                  Pagination constructor with a default size of 1 element per page.
     */
    public Pagination(@NotNull RyseInventory inventory) {
        this.inventory = inventory;
        this.itemsPerPage = 1;
        this.page = 0;
    }

    public Pagination(@NotNull Pagination pagination) {
        this.inventory = pagination.inventory;
        this.itemsPerPage = pagination.itemsPerPage;
        this.page = pagination.page;
        this.slotIterator = pagination.slotIterator;
        this.inventoryData = pagination.inventoryData;
        this.calledItemsPerPage = pagination.calledItemsPerPage;
    }

    /**
     * Creates a new instance of Pagination where all data is transferred along.
     *
     * @param pagination The pagination to copy from.
     * @return The new instance of Pagination.
     */
    public @NotNull Pagination newInstance(@NotNull Pagination pagination) {
        return new Pagination(pagination);
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
        if (this.inventory.getFixedPageSize() != -1)
            return this.inventory.getFixedPageSize();

        return (int) Math.ceil((double) this.inventoryData.stream()
                .filter(data -> data.getOriginalSlot() == -1).count() / calculateValueForPage());
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
        if (this.inventory.getFixedPageSize() != -1)
            return this.page == this.inventory.getFixedPageSize() - 1;

        int slide = (int) Math.ceil((double) this.inventoryData.stream()
                .filter(data -> data.getOriginalSlot() == -1).count() / calculateValueForPage());

        return this.page >= (slide != 0 ? slide - 1 : 0);
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
     * @throws IllegalStateException if you are on the last page and you try to increase the page.
     */
    public @NotNull Pagination next() throws IllegalStateException {
        this.page++;
        return this;
    }

    /**
     * Decreases the current page by 1
     *
     * @return the new Pagination
     * @throws IllegalStateException if you are on the first page and you try to decrease the page.
     */
    public @NotNull Pagination previous() throws IllegalStateException {
        if (isFirst())
            throw new IllegalStateException("You tried to go to the previous page, although you are already on the first page.");

        this.page--;
        return this;
    }

    /**
     * Sets the current page to the specified page.
     *
     * @param page The page to set to.
     * @return the new Pagination
     * <p>
     * This will not check if the page is valid.
     */
    public Pagination page(@Nonnegative int page) {
        this.page = page;
        return this;
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items A list of intelligent ItemStacks
     */
    public void setItems(@NotNull List<IntelligentItem> items) {
        for (IntelligentItem item : items)
            this.inventoryData.add(new IntelligentItemData(item, this.page, -1, false, false));
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items An array of smart ItemStacks
     */
    public void setItems(IntelligentItem @NotNull [] items) {
        for (IntelligentItem item : items)
            this.inventoryData.add(new IntelligentItemData(item, this.page, -1, false, false));
    }

    /**
     * Adds a single intelligent ItemStack.
     *
     * @param item the intelligent ItemStack
     */
    public void addItem(@NotNull IntelligentItem item) {
        this.inventoryData.add(new IntelligentItemData(item, this.page, -1, false, false));
    }

    /**
     * Adds a single ItemStack.
     *
     * @param itemStack the ItemStack
     */
    public void addItem(@NotNull ItemStack itemStack) {
        this.inventoryData.add(new IntelligentItemData(IntelligentItem.empty(itemStack), this.page, -1, false, false));
    }

    /**
     * Adds a single ItemStack.
     *
     * @param itemStack the ItemStack
     * @param type      the type of the ItemStack
     */
    public void addItem(@NotNull ItemStack itemStack, @NotNull IntelligentType type) {
        IntelligentItem item = type == IntelligentType.EMPTY
                ? IntelligentItem.empty(itemStack)
                : IntelligentItem.ignored(itemStack);

        this.inventoryData.add(new IntelligentItemData(item, this.page, -1, false, false));
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
     * @throws IllegalArgumentException if slot greater than 53
     */
    public void setItem(@Nonnegative int slot, @NotNull IntelligentItem newItem) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        remove(slot);

        this.inventoryData.add(new IntelligentItemData(newItem, this.page, slot, false, true));
    }

    /**
     * Sets a new item at a slot with defined a page.
     *
     * @param slot     The slot to set the item at.
     * @param page     The page to set the item to
     * @param newItem  The Item to set
     * @param transfer If the item should be transferred to the next page.
     * @throws IllegalArgumentException if slot greater than 53
     *                                  <p>
     *                                  First page is 0
     */
    @ApiStatus.Internal
    public void setItem(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentItem newItem, boolean transfer) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        remove(slot, page);

        this.inventoryData.add(new IntelligentItemData(newItem, page, slot, transfer, false));
    }

    /**
     * @param itemsPerPage How many items may be per page.
     *                     <p>
     *                     If you have set the endPosition at the SlotIterator, it will be preferred.
     */
    public void setItemsPerPage(@Nonnegative int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
        this.calledItemsPerPage = true;
    }

    /**
     * It removes all inventory data from the inventory data list that has the same page and slot as the page and slot that
     * was passed into the function
     *
     * @param slot The slot to remove the item from.
     */
    @ApiStatus.Internal
    public void remove(@Nonnegative int slot) {
        this.inventoryData.removeIf(data -> data.getPage() == this.page && data.getModifiedSlot() == slot);
    }

    /**
     * Removes all inventory data from the list that matches the given page and slot
     *
     * @param slot The slot that was modified
     * @param page The page of the inventory.
     */
    protected void remove(@Nonnegative int slot, @Nonnegative int page) {
        this.inventoryData.removeIf(data -> data.getPage() == page && data.getModifiedSlot() == slot);
    }

    /**
     * Returns the item in the specified slot, or null if the slot is empty.
     *
     * @param slot The slot number of the item you want to get.
     * @return The item in the slot.
     */
    @ApiStatus.Internal
    public @Nullable IntelligentItem get(@Nonnegative int slot) {
        return get(slot, this.page);
    }

    /**
     * Returns the item in the specified slot, or null if the slot is empty.
     *
     * @param slot The slot number of the item you want to get.
     * @return The item in the slot.
     */
    @ApiStatus.Internal
    public @Nullable IntelligentItem getPresent(@Nonnegative int slot) {
        return this.inventoryData.stream()
                .filter(data -> data.getPage() == page && data.getModifiedSlot() == slot && data.isPresetOnAllPages())
                .findFirst()
                .map(IntelligentItemData::getItem)
                .orElse(null);
    }

    /**
     * Return the item in the given slot on the given page, or null if there is no item in that slot.
     * <p>
     * The first thing we do is filter the inventory data to only include data that matches the given page and slot. Then
     * we use `findFirst()` to get the first item in the stream, if there is one. If there is an item, we use `map()` to
     * get the item from the data. If there is no item, we use `orElse()` to return null
     *
     * @param slot The slot number of the item you want to get.
     * @param page The page number of the inventory.
     * @return The item in the slot and page.
     */
    @ApiStatus.Internal
    public @Nullable IntelligentItem get(@Nonnegative int slot, @Nonnegative int page) {
        for (IntelligentItemData data : this.inventoryData) {
            if(data.isPresetOnAllPages() && data.getModifiedSlot() == slot)
                return data.getItem();

            if (data.getPage() == page && data.getModifiedSlot() == slot)
                return data.getItem();
        }

        return null;
    }

    /**
     * Returns the inventory data of the player.
     *
     * @return A list of IntelligentItemData objects.
     */
    @ApiStatus.Internal
    public @NotNull List<IntelligentItemData> getInventoryData() {
        return this.inventoryData;
    }

    /**
     * Adds the given item data to the inventory data.
     *
     * @param itemData The IntelligentItemData object that you want to add to the inventory.
     */
    protected void addInventoryData(IntelligentItemData itemData) {
        this.inventoryData.add(itemData);
    }

    /**
     * This function returns a list of all the items on the specified page.
     *
     * @param page The page number to get the data from.
     * @return A list of IntelligentItemData objects.
     */
    protected @NotNull List<IntelligentItemData> getDataByPage(@Nonnegative int page) {
        return this.inventoryData.stream()
                .filter(item -> item.getPage() == page)
                .collect(Collectors.toList());
    }

    /**
     * This function sets the page number.
     *
     * @param page The page number to get.
     */
    protected void setPage(@Nonnegative int page) {
        this.page = page;
    }

    /**
     * If the slotIterator is null or the end position is -1, return the itemsPerPage, otherwise return the end position
     * minus the slot.
     *
     * @return The number of items per page.
     */
    private int calculateValueForPage() {
        SlotIterator iterator = this.slotIterator;
        int itemsPerPage = this.itemsPerPage;

        if (iterator == null) {
            return itemsPerPage;
        } else if (iterator.getEndPosition() == -1) {
            return itemsPerPage - iterator.getBlackListInternal().size();
        } else {
            return iterator.getEndPosition() - iterator.getSlot();
        }
    }


}
