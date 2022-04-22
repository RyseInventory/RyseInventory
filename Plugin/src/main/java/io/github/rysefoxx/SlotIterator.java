package io.github.rysefoxx;

import io.github.rysefoxx.pagination.Pagination;

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

    public static Builder builder() {
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

        /**
         * Adds a slot to the blacklist.
         *
         * @param slot The slot to add to the blacklist.
         * @return The builder object itself.
         */
        public Builder addBlackList(@Nonnegative int slot) {
            this.blackList.add(slot);
            return this;
        }

        /**
         * This can be used to block multiple slots.
         *
         * @param slots The slots to be used for the inventory.
         * @return The builder object itself.
         */
        public Builder blackList(List<Integer> slots) {
            this.blackList = new ArrayList<>(slots);
            return this;
        }

        /**
         * This tells us where the item should stop.
         *
         * @param slot
         * @return The Builder object itself.
         * @apiNote If this method is used, {@link Pagination#setItemsPerPage(int)} is ignored.
         */
        public Builder endPosition(@Nonnegative int slot) {
            this.endPosition = slot;
            return this;
        }

        /**
         * This tells us where the item should stop.
         *
         * @param row
         * @param column
         * @return The Builder object itself.
         * @apiNote If this method is used, {@link Pagination#setItemsPerPage(int)} is ignored.
         */
        public Builder endPosition(@Nonnegative int row, @Nonnegative int column) {
            this.endPosition = row * 9 + column;
            return this;
        }

        /**
         * Sets the slot to start at.
         *
         * @param startSlot The slot to start at.
         * @return The Builder object itself.
         */
        public Builder slot(@Nonnegative int startSlot) {
            this.slot = startSlot;
            return this;
        }

        /**
         * Sets the slot to start at.
         *
         * @param row
         * @param column
         * @return The Builder object itself.
         */
        public Builder slot(@Nonnegative int row, @Nonnegative int column) {
            this.row = row;
            this.column = column;
            return this;
        }

        /**
         * This tells us whether the items should be placed vertically or horizontally.
         *
         * @param type
         * @return The builder object itself.
         */
        public Builder type(SlotIteratorType type) {
            this.type = type;
            return this;
        }

        /**
         * This is used to overwrite items.
         *
         * @return The Builder object itself.
         */
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
