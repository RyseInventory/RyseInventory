package com.github.rysefoxx;

import com.github.rysefoxx.pagination.Pagination;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;


public class SlotIterator {

    private int slot = -1;
    private int endPosition = -1;
    private int row;
    private int column;
    private SlotIteratorType type;
    private boolean override;
    private List<Integer> blackList = new ArrayList<>();

    @Contract(value = " -> new", pure = true)
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int slot = -1;
        private int endPosition = -1;
        private int row;
        private int column;
        private SlotIteratorType type;
        private boolean override;
        private List<Integer> blackList = new ArrayList<>();

        public Builder addBlackList(@Nonnegative int slot) {
            this.blackList.add(slot);
            return this;
        }

        public Builder blackList(@NotNull List<Integer> slots) {
            this.blackList = new ArrayList<>(slots);
            return this;
        }

        public Builder endPosition(@Nonnegative int slot) {
            this.endPosition = slot;
            return this;
        }

        public Builder endPosition(@Nonnegative int row, @Nonnegative int column) {
            this.endPosition = row * 9 + column;
            return this;
        }

        public Builder slot(@Nonnegative int startSlot) {
            this.slot = startSlot;
            return this;
        }

        public Builder slot(@Nonnegative int row, @Nonnegative int column) {
            this.row = row;
            this.column = column;
            return this;
        }

        public Builder type(@NotNull SlotIteratorType type) {
            this.type = type;
            return this;
        }

        public Builder override() {
            this.override = true;
            return this;
        }

        public SlotIterator build() {
            SlotIterator slotIterator = new SlotIterator();
            slotIterator.slot = this.slot;
            slotIterator.row = this.row;
            slotIterator.column = this.column;
            slotIterator.type = this.type;
            slotIterator.override = this.override;
            slotIterator.blackList = this.blackList;
            slotIterator.endPosition = this.endPosition;

            if ((this.slot >= this.endPosition) && this.endPosition != -1) {
                throw new IllegalArgumentException("The start slot must be smaller than the end slot");
            }

            return slotIterator;
        }
    }

    /**
     * @return the start slot
     */
    public int getSlot() {
        return this.slot;
    }

    /**
     * @return the start column
     */
    public int getColumn() {
        return this.column;
    }

    /**
     * @return the start row
     */
    public int getRow() {
        return this.row;
    }

    /**
     * @return the SlotIteratorType
     */
    public SlotIteratorType getType() {
        return this.type;
    }

    /**
     * @return true if items can be overwritten.
     */
    public boolean isOverride() {
        return this.override;
    }

    /**
     * @return where the last item should be placed.
     * @apiNote If the endPosition was set, the {@link Pagination#setItemsPerPage(int)} specification is ignored.
     */
    public int getEndPosition() {
        return this.endPosition;
    }

    /**
     *
     * @return all slots where no items should be placed.
     */
    public List<Integer> getBlackList() {
        return this.blackList;
    }

    public enum SlotIteratorType {
        HORIZONTAL,
        VERTICAL
    }

}
