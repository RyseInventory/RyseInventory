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
import io.github.rysefoxx.content.IntelligentItemData;
import io.github.rysefoxx.enums.IntelligentType;
import io.github.rysefoxx.util.StringConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
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

    private
    @Getter
    @Nonnegative
    int itemsPerPage;

    private @Getter SlotIterator slotIterator;

    private int page;
    private final RyseInventory inventory;

    private @Setter(AccessLevel.PROTECTED) List<IntelligentItemData> inventoryData = new ArrayList<>();

    /**
     * Pagination constructor with a default size of 1 element per page.
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

        return (int) Math.ceil((double) this.inventoryData.stream().filter(data -> data.getOriginalSlot() == -1).count() / calculateValueForPage());
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
     * Sets the current page to the specified page.
     *
     * @param page The page to set to.
     * @return the new Pagination
     * @apiNote This will not check if the page is valid.
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
            this.inventoryData.add(new IntelligentItemData(item, this.page, -1));
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items An array of smart ItemStacks
     */
    public void setItems(IntelligentItem @NotNull [] items) {
        for (IntelligentItem item : items)
            this.inventoryData.add(new IntelligentItemData(item, this.page, -1));
    }

    /**
     * Adds a single intelligent ItemStack.
     *
     * @param item the intelligent ItemStack
     */
    public void addItem(@NotNull IntelligentItem item) {
        this.inventoryData.add(new IntelligentItemData(item, this.page, -1));
    }

    /**
     * Adds a single ItemStack.
     *
     * @param itemStack the ItemStack
     */
    public void addItem(@NotNull ItemStack itemStack) {
        this.inventoryData.add(new IntelligentItemData(IntelligentItem.empty(itemStack), this.page, -1));
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

        this.inventoryData.add(new IntelligentItemData(item, this.page, -1));
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
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        remove(slot);

        this.inventoryData.add(new IntelligentItemData(newItem, this.page, slot));
    }

    /**
     * Sets a new item at a slot with defined a page.
     *
     * @param slot    The slot
     * @param page    The page
     * @param newItem The Item
     * @throws IllegalArgumentException if slot > 53
     * @apiNote First page is 0
     */
    protected void setItem(@Nonnegative int slot, @Nonnegative int page, @NotNull IntelligentItem newItem) throws IllegalArgumentException {
        if (slot > 53)
            throw new IllegalArgumentException(StringConstants.INVALID_SLOT);

        remove(slot, page);

        this.inventoryData.add(new IntelligentItemData(newItem, page, slot));
    }

    /**
     * @param itemsPerPage How many items may be per page.
     * @apiNote If you have set the endPosition at the SlotIterator, it will be preferred.
     */
    public void setItemsPerPage(@Nonnegative int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    protected void remove(@Nonnegative int slot) {
        this.inventoryData.removeIf(data -> data.getPage() == this.page && data.getModifiedSlot() == slot);
    }

    protected void remove(@Nonnegative int slot, @Nonnegative int page) {
        this.inventoryData.removeIf(data -> data.getPage() == page && data.getModifiedSlot() == slot);
    }

    protected @Nullable IntelligentItem get(@Nonnegative int slot) {
        return get(slot, this.page);
    }

    protected @Nullable IntelligentItem get(@Nonnegative int slot, @Nonnegative int page) {
        return this.inventoryData.stream()
                .filter(data -> data.getPage() == page && data.getModifiedSlot() == slot)
                .findFirst()
                .map(IntelligentItemData::getItem)
                .orElse(null);
    }

    protected @NotNull List<IntelligentItemData> getInventoryData() {
        return this.inventoryData;
    }

    protected @NotNull List<IntelligentItemData> getDataByPage(@Nonnegative int page) {
        return this.inventoryData.stream().filter(item -> item.getPage() == page).collect(Collectors.toList());
    }

    protected void setPage(@Nonnegative int page) {
        this.page = page;
    }

    private int calculateValueForPage() {
        return this.slotIterator == null || this.slotIterator.getEndPosition() == -1
                ? this.itemsPerPage
                : this.slotIterator.getEndPosition() - this.slotIterator.getSlot();
    }

}
