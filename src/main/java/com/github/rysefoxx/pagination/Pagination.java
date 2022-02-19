package com.github.rysefoxx.pagination;

import com.github.rysefoxx.SlotIterator;
import com.github.rysefoxx.content.IntelligentItem;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/19/2022
 */

public class Pagination {

    private @Setter
    @Getter
    @Nonnegative
    int itemsPerPage;
    private List<IntelligentItem> items;

    private @Getter
    SlotIterator slotIterator;

    private boolean last;
    private boolean first;

    protected HashMap<Integer, HashMap<Integer, IntelligentItem>> pageItems;

    /**
     * Pagination constructor with a default size of 1 element per page.
     */
    @Contract(pure = true)
    public Pagination() {
        this.itemsPerPage = 1;
        this.items = new ArrayList<>();
        this.pageItems = new HashMap<>();
    }

    /**
     * @return true if you are on the last page.
     */
    public boolean isLast() {
        return this.last;
    }

    /**
     * @return true if you are on the first page.
     */
    public boolean isFirst() {
        return this.first;
    }

    public void next() {
//todo:
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items A list of intelligent ItemStacks
     */
    public void setItems(@NotNull List<IntelligentItem> items) {
        this.items = items;
    }

    /**
     * Sets a complete list of smart ItemStacks
     *
     * @param items An array of smart ItemStacks
     */
    public void setItems(@NotNull IntelligentItem[] items) {
        this.items = new ArrayList<>(Arrays.stream(items).toList());
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

    protected @NotNull
    List<IntelligentItem> getItems() {
        return items;
    }
}
